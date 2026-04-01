package org.example.workers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.example.model.AppConfig;

/**
 * Cita config.properties i kreira AppConfig
 * Bice Callable za Executor Service tako da se citanje fajla NE odvija na
 * FX Thread-u
 * 
 * Fallback vrednost za monitor.interval je 3000ms
 * 
 * https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html
 */

public class ConfigReader {

  private static final String CONFIG_PATH = "config.properties";
  private static final String KEY_SCAN_INTERVAL = "monitor.interval";
  private static final String KEY_MAPPING_FILE = "mapping.file";
  private static final String KEY_SNAPSHOT_INTERVAL = "snapshot.interval";
  private static final String KEY_FIXED_TIME_SNAPSHOT = "snapshot.fixed_time_";

  private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

  public AppConfig readConfig() {
    Properties properties = new Properties();
    File configFile = new File(CONFIG_PATH);

    try (InputStream is = new FileInputStream(configFile)) {

      properties.load(is);

    } catch (FileNotFoundException e) {
      System.err.println("Config File not found!" + e.getMessage());
    } catch (IOException e) {
      System.err.println("Config File not found!" + e.getMessage());
    } catch (Exception e) {
      System.err.println("ReadConfig error" + e.getMessage());
    }

    String rawScanInterval = properties.getProperty(KEY_SCAN_INTERVAL, "").trim();
    if (rawScanInterval.isEmpty()) {
      System.err.println("[ConfigReader] Missing key " + KEY_SCAN_INTERVAL);
    }

    long scanInterval;
    try {
      scanInterval = Long.parseLong(rawScanInterval);
    } catch (NumberFormatException e) {
      System.err.println("[ConfigReader] Invalid value '" + rawScanInterval + "' for key '"
          + KEY_SCAN_INTERVAL + "' — using default " + 3000);

      scanInterval = 3000;
    }

    String rawMappingFile = properties.getProperty(KEY_MAPPING_FILE, "").trim();
    if (rawMappingFile.isEmpty()) {
      System.err.println("[ConfigReader] Missing key " + KEY_MAPPING_FILE + "' — using default " + "process_info.json");
      rawMappingFile = "process_info.json";
    }

    String rawSnapshotInterval = properties.getProperty(KEY_SNAPSHOT_INTERVAL, "").trim();
    if (rawSnapshotInterval.isEmpty()) {
      System.err.println("[ConfigReader] Missing key " + KEY_SNAPSHOT_INTERVAL);
    }

    long snapshotInterval;
    try {
      snapshotInterval = Long.parseLong(rawSnapshotInterval);
    } catch (NumberFormatException e) {
      System.err.println("[ConfigReader] Invalid value '" + rawSnapshotInterval + "' for key '"
          + KEY_SNAPSHOT_INTERVAL + "' — using default " + 60);

      snapshotInterval = 60;
    }

    // Collect all snapshot.fixed_time_N entries in order
    List<LocalTime> fixedTimes = new ArrayList<>();
    int n = 1;
    while (true) {
      String key = KEY_FIXED_TIME_SNAPSHOT + n;
      String raw = properties.getProperty(key, "").trim();
      if (raw.isEmpty())
        break;
      try {
        fixedTimes.add(LocalTime.parse(raw, TIME_FORMAT));
        System.out.println("[ConfigReader] Fixed snapshot time " + n + ": " + raw);
      } catch (DateTimeParseException e) {
        System.err.println("[ConfigReader] Invalid time '" + raw
            + "' for key '" + key + "' — skipping. Expected HH:mm:ss");
      }
      n++;
    }

    AppConfig config = new AppConfig(scanInterval, rawMappingFile, snapshotInterval, fixedTimes);

    System.out.println("[ConfigReader] Loaded: " + config.toString());
    return config;
  }

}
