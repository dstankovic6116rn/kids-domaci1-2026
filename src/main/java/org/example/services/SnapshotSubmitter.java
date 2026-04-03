package org.example.services;

/**
 * Ovo je posebno korisno zbog circular dependency-ja između AnalyticsService i
 * FileExecutorService
 * AnalyticsService treba da okida snapshotove u FileExecutorService ali ne
 * treba da zna detalje o tome kako FileExecutorService radi svoje zakazivanje i
 * izvršavanje.
 */
public interface SnapshotSubmitter {
  void submitFixedTimeSnapshot();
}
