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
  private long lastMergeTimeMs = System.currentTimeMillis();

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
    System.out.println("elsapsed Time" + elapsedTime);
    lastMergeTimeMs = now;

    Set<String> scannedKeys = new HashSet<>(scannedProcesses.size());

    for (ProcessItem incoming : scannedProcesses) {
      String key = incoming.getOriginalName();
      scannedKeys.add(key);

      // merge() ConcurrentHashMap.merge atomically inserts or accumulates
      long uptime = uptimeStore.merge(key, elapsedTime, Long::sum);

      ProcessItem storedProcess = processDataStore.get(key);

      if (storedProcess == null) {
        incoming.setUptimeSeconds(uptime);
        processDataStore.put(key, incoming);
      } else {
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

  public List<ProcessItem> getAll() {
    return new ArrayList<>(processDataStore.values());
  }

  public ProcessItem getByName(String name) {
    return processDataStore.get(name);
  }

  public int getSize() {
    return processDataStore.size();
  }
}
