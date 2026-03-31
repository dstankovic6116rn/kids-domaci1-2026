package org.example.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.example.model.AppConfig;

/**
 * Cita config.properties i kreira AppConfig
 * Bice Callable za File Executor Service tako da se citanje fajla NE odvija na
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

    String rawMappingFile = properties.getProperty(KEY_MAPPING_FILE, "").trim();
    if (rawMappingFile.isEmpty()) {
      System.err.println("[ConfigReader] Missing key " + KEY_MAPPING_FILE + "' — using default " + "process_info.json");
      rawMappingFile = "process_info.json";
    }

    long scanInterval;
    try {
      scanInterval = Long.parseLong(rawScanInterval);
    } catch (NumberFormatException e) {
      System.err.println("[ConfigReader] Invalid value '" + rawScanInterval + "' for key '"
          + KEY_SCAN_INTERVAL + "' — using default " + 3000);

      scanInterval = 3000;
    }

    AppConfig config = new AppConfig(scanInterval, rawMappingFile);
    System.out.println("[ConfigReader] Loaded: " + config);
    return config;
  }

}
