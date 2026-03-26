package org.example.model;

/**
 * Cuva parsiranu konfiguraciju iz config.properties
 */

public class AppConfig {

  private final long scanIntervalMS;

  public AppConfig(long scanIntervalMS) {
    this.scanIntervalMS = scanIntervalMS;
  }

  public long getScanIntervalMS() {
    return scanIntervalMS;
  }

}
