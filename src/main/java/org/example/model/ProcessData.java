package org.example.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory Store za sve pracene sistemske procese
 * 
 * ConcurrentHashMap ne zahteva rucno zakljucavanje tokom citanja od strane FX
 * thread-a dok se pozadinski scan() procesa odvija
 * Recikliranje PID-eva ne mora da se hendla ako je kljuc u mapi originalName
 * procesa
 * 
 * UptimeStore ce cuvati vrednost nakon sto se neki proces ugasi i azurirati se
 * kada se isti proces ponovo pokrene bez obzira da li ima novi PID
 */
public class ProcessData {

  private final ConcurrentHashMap<String, ProcessItem> processDataStore = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Long> uptimeStore = new ConcurrentHashMap<>();
  private volatile long lastMergeTimeMs = System.currentTimeMillis();

  private final Set<String> frozenProcesses = ConcurrentHashMap.newKeySet();

  /**
   * Merge-uje procese iz novog skeniranja u data store
   * 
   * Pravila merge-a:
   * 1. Ako originalName ne postoji, upisi novi proces
   * 2. Ako pid postoji azuriraj vrednosti ram i cpu iz poslednjeg skeniranja i
   * akumuliraj uptime vreme prethodnog procesa i procesa iz novog skeniranja
   * 
   * @param scannedProcesses sveza lista skeniranih procesa
   */
  public void merge(List<ProcessItem> scannedProcesses) {
    long now = System.currentTimeMillis();
    long elapsedTime = (now - lastMergeTimeMs) / 1000L;
    lastMergeTimeMs = now;

    Set<String> scannedKeys = new HashSet<>(scannedProcesses.size());

    for (ProcessItem incoming : scannedProcesses) {
      String key = incoming.getOriginalName();
      scannedKeys.add(key);

      ProcessItem storedProcess = processDataStore.get(key);

      if (storedProcess == null) {
        // Nov ili ponovno pokrenut proces, proverava da li je prethodno ostao u freeze,
        // ako jeste upisi poslednju vrednost u processDataStore
        long uptime = frozenProcesses.contains(key)
            ? uptimeStore.getOrDefault(key, 0L)
            : uptimeStore.merge(key, elapsedTime, Long::sum);

        incoming.setUptimeSeconds(uptime);
        incoming.setTrackingFrozen(frozenProcesses.contains(key));
        processDataStore.put(key, incoming);

      } else if (frozenProcesses.contains(key)) {
        // Postojeci proces u novom scan-u, zanemari uptime azuriranje
        storedProcess.setPid(incoming.getPid());
        storedProcess.setStartTime(incoming.getStartTime());
        storedProcess.setCpuUsage(incoming.getCpuUsage());
        storedProcess.setRamUsageMb(incoming.getRamUsageMb());
      } else {
        // Azuriraj sve
        long uptime = uptimeStore.merge(key, elapsedTime, Long::sum);
        storedProcess.setPid(incoming.getPid());
        storedProcess.setStartTime(incoming.getStartTime());
        storedProcess.setCpuUsage(incoming.getCpuUsage());
        storedProcess.setRamUsageMb(incoming.getRamUsageMb());
        storedProcess.setUptimeSeconds(uptime);
      }
    }

    // Izbaci procese koji vise ne postoje
    processDataStore.keySet().removeIf(k -> !scannedKeys.contains(k));
  }

  public long getLiveUptime(String originalName) {
    long banked = uptimeStore.getOrDefault(originalName, 0L);
    if (frozenProcesses.contains(originalName)) {
      return banked; // frozen — don't add elapsed
    }
    long elapsed = (System.currentTimeMillis() - lastMergeTimeMs) / 1000L;
    long raw = banked + elapsed;
    return raw;
  }

  public void freezeUptime(String originalName) {
    frozenProcesses.add(originalName);
    ProcessItem process = processDataStore.get(originalName);
    if (process != null)
      process.setTrackingFrozen(true);

  }

  public void unfreezeUptime(String originalName) {
    frozenProcesses.remove(originalName);
    ProcessItem process = processDataStore.get(originalName);
    if (process != null)
      process.setTrackingFrozen(false);
  }

  public boolean isFrozen(String originalName) {
    return frozenProcesses.contains(originalName);
  }

  /** Vraca snapshot cele uptime banke. */
  public ConcurrentHashMap<String, Long> getUptimeStore() {
    return uptimeStore;
  }

  public List<ProcessItem> getAll() {
    return new ArrayList<>(processDataStore.values());
  }

  public ProcessItem getByName(String name) {
    return processDataStore.get(name);
  }

  public int getSize() {
    return processDataStore.size();
  }

  public void remove(String originalName) {
    processDataStore.remove(originalName);
  }
}
