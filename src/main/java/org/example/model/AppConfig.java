package org.example.model;

/**
 * Cuva parsiranu konfiguraciju iz config.properties
 */

public class AppConfig {

  private final long scanIntervalMS;
  private final String mappingFile;

  public AppConfig(long scanIntervalMS, String mappingFile) {
    this.scanIntervalMS = scanIntervalMS;
    this.mappingFile = mappingFile;
  }

  public long getScanIntervalMS() {
    return scanIntervalMS;
  }

  public String getMappingFile() {
    return mappingFile;
  }

}
