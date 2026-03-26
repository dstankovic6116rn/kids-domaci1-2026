package org.example.services;

import java.io.File;
import java.io.FileInputStream;
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
  private static final String KEY_SCAN_INTERVAL = "monitor.interval";

  public AppConfig readConfig() {
    Properties properties = new Properties();
    File configFile = new File("app.properties");

    try (InputStream is = new FileInputStream(configFile)) {
      properties.load(is);
    } catch (IOException e) {
      System.err.println("Config File not found!" + e.getMessage());
    } catch (Exception e) {
      System.err.println("readConfig error" + e.getMessage());
    }

    String raw = properties.getProperty(KEY_SCAN_INTERVAL, "").trim();
    if (raw.isEmpty()) {
      System.err.println("[ConfigReader] Missing key '" + KEY_SCAN_INTERVAL);
    }

    long scanInterval;
    try {
      scanInterval = Long.parseLong(raw);

    } catch (NumberFormatException e) {
      System.err.println("[ConfigReader] Invalid value '" + raw + "' for key '"
          + KEY_SCAN_INTERVAL + "' — using default " + 3000);

      scanInterval = 3000;
    }

    AppConfig config = new AppConfig(scanInterval);
    System.out.println("[ConfigReader] Loaded: " + config);
    return config;
  }

}
