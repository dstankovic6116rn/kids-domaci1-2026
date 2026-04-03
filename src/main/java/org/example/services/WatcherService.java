package org.example.services;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.function.BooleanSupplier;

import org.example.model.ProcessData;
import org.example.model.ProcessItem;
import org.example.workers.JsonReader;

public class WatcherService {

  private final ProcessData processData;
  private final JsonReader jsonReader;
  private final BooleanSupplier isSuppressed;
  private final Runnable acquireLock;
  private final Runnable releaseLock;
  private final String filePath;
  private final Runnable clearSuppression;

  private volatile boolean running = true;
  private Thread watcherThread;

  public WatcherService(ProcessData processData, JsonReader jsonReader, BooleanSupplier isSuppressed,
      Runnable acquireLock, Runnable releaseLock, String filePath, Runnable clearSuppression) {
    this.processData = processData;
    this.jsonReader = jsonReader;
    this.isSuppressed = isSuppressed;
    this.acquireLock = acquireLock;
    this.releaseLock = releaseLock;
    this.clearSuppression = clearSuppression;
    this.filePath = filePath;
  }

  public void start() {
    watcherThread = new Thread(this::run, "watcher-thread");
    watcherThread.setDaemon(true);
    watcherThread.start();
  }

  public void shutdown() {
    running = false;
    if (watcherThread != null) {
      watcherThread.interrupt();
    }
  }

  private void run() {
    Path target = Paths.get(filePath).toAbsolutePath();
    Path parentDir = target.getParent();

    if (parentDir == null) {
      System.err.println("[WatcherService] Cannot watch — no parent directory for: "
          + filePath);
      return;
    }

    try (WatchService watcher = FileSystems.getDefault().newWatchService()) {

      parentDir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
      System.out.println("[WatcherService] Watching: " + target);

      while (running) {
        WatchKey key;
        try {
          key = watcher.take();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        }

        for (WatchEvent<?> event : key.pollEvents()) {
          if (event.kind() == StandardWatchEventKinds.OVERFLOW)
            continue;

          @SuppressWarnings("unchecked")
          WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
          Path changed = parentDir.resolve(pathEvent.context());

          // Only care about our specific JSON file
          if (!changed.equals(target))
            continue;

          System.out.println("[WatcherService] External change detected — reloading.");
          applyExternalChange();
        }

        // Reset the key — required to receive further events
        if (!key.reset()) {
          System.err.println("[WatcherService] Watch key invalid — stopping.");
          break;
        }
      }
    } catch (Exception e) {
      System.err.println("[WatcherService] Failed to start watch: " + e.getMessage());
      e.printStackTrace();
    }
  };

  private void applyExternalChange() {
    // Acquire jsonLock before reading — prevents racing with a concurrent
    // app-triggered save that may still be writing the file
    acquireLock.run();
    List<ProcessItem> reloaded;
    try {
      if (isSuppressed.getAsBoolean()) {
        System.out.println("[WatcherService] Suppressed own-write event.");
        // Clear the flag so future external edits are detected correctly
        clearSuppression.run();
        return;
      }

      reloaded = jsonReader.read(filePath);
    } finally {
      releaseLock.run();
    }

    if (reloaded.isEmpty()) {
      System.err.println("[WatcherService] Reload returned empty — ignoring.");
      return;
    }

    processData.updateFromWatcher(reloaded);

    System.out.println("[WatcherService] Store updated from external change — "
        + reloaded.size() + " records.");
  }

}
