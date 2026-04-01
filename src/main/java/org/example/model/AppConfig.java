package org.example.model;

import java.time.LocalTime;
import java.util.List;

/**
 * Cuva parsiranu konfiguraciju iz config.properties
 * Nepromenljivo nakon kreiranja
 */

public class AppConfig {

  private final long scanIntervalMS;
  private final String mappingFile;
  private final long snapshotIntervalSec;
  private final List<LocalTime> fixedSnapshotTimes;

  public AppConfig(long scanIntervalMS, String mappingFile, long snapshotIntervalSec,
      List<LocalTime> fixedSnapshotTimes) {
    this.scanIntervalMS = scanIntervalMS;
    this.mappingFile = mappingFile;
    this.snapshotIntervalSec = snapshotIntervalSec;
    this.fixedSnapshotTimes = fixedSnapshotTimes;
  }

  public long getScanIntervalMS() {
    return scanIntervalMS;
  }

  public String getMappingFile() {
    return mappingFile;
  }

  public long getSnapshotIntervalSec() {
    return snapshotIntervalSec;
  }

  public List<LocalTime> getFixedSnapshotTimes() {
    return fixedSnapshotTimes;
  }

}
