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
  // Popunjava se iz JSON-a na startu aplikacije
  private final ConcurrentHashMap<String, ProcessItem> historicData = new ConcurrentHashMap<>();

  private volatile long lastMergeTimeMs = System.currentTimeMillis();

  /**
   * Ucitava istorijske podatke iz JSON-a u historicData mapu i uptimeStore, kao i
   * set frozenProcesses
   * Ove informacije ce se koristiti za merge-ovanje sa novim skenovima i za
   * odrzavanje kontinuiteta u pracenju procesa prilikom ponovnog pokretanja
   * aplikacije.
   * 
   * @param records
   */
  public void loadFromHistory(List<ProcessItem> records) {
    for (ProcessItem item : records) {
      String name = item.getOriginalName();

      uptimeStore.put(name, item.getUptimeSeconds());

      historicData.put(name, item);
    }
    System.out.println("[ProcessStore] Loaded " + records.size()
        + " historic records into bank.");
  }

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
        /**
         * returns the previous value associated with key, or null if there was no
         * mapping for key and removes the key from the map. This is atomic.
         */
        ProcessItem historyProcess = historicData.remove(key);

        // Nov ili ponovno pokrenut proces, proverava da li je prethodno ostao u freeze,
        // ako jeste upisi poslednju vrednost u processDataStore
        boolean frozen = historyProcess != null && historyProcess.isTrackingFrozen();
        long uptime = frozen
            ? uptimeStore.getOrDefault(key, 0L)
            : uptimeStore.merge(key, elapsedTime, Long::sum);

        incoming.setUptimeSeconds(uptime);
        incoming.setTrackingFrozen(frozen);

        // Ako postoji istorijski zapis, prenesi aliasName i category u novi proces
        if (historyProcess != null) {
          incoming.setAliasName(historyProcess.getAliasName());
          incoming.setCategory(historyProcess.getCategory());
        }

        processDataStore.put(key, incoming);

      } else if (storedProcess.isTrackingFrozen()) {
        // Postojeci proces u novom scan-u je frozen, zanemari uptime azuriranje
        storedProcess.setPid(incoming.getPid());
        storedProcess.setStartTime(incoming.getStartTime());
        storedProcess.setCpuUsage(incoming.getCpuUsage());
        storedProcess.setRamUsageMb(incoming.getRamUsageMb());
        storedProcess.setTrackingFrozen(true);
      } else {
        // Azuriraj sve
        long uptime = uptimeStore.merge(key, elapsedTime, Long::sum);
        storedProcess.setPid(incoming.getPid());
        storedProcess.setStartTime(incoming.getStartTime());
        storedProcess.setCpuUsage(incoming.getCpuUsage());
        storedProcess.setRamUsageMb(incoming.getRamUsageMb());
        storedProcess.setUptimeSeconds(uptime);
        storedProcess.setTrackingFrozen(false);
      }
    }

    // Izbaci procese koji vise ne postoje
    processDataStore.keySet().removeIf(k -> !scannedKeys.contains(k));
  }

  public long getLiveUptime(String originalName) {
    long banked = uptimeStore.getOrDefault(originalName, 0L);
    ProcessItem liveProcess = processDataStore.get(originalName);
    if (liveProcess != null && liveProcess.isTrackingFrozen()) {
      return banked; // frozen — don't add elapsed
    }

    // Fall back to historicData for processes not running this session
    ProcessItem historicProcess = historicData.get(originalName);
    if (historicProcess != null && historicProcess.isTrackingFrozen()) {
      return banked; // frozen — don't add elapsed
    }

    long elapsed = (System.currentTimeMillis() - lastMergeTimeMs) / 1000L;
    long raw = banked + elapsed;

    return raw;
  }

  public void freezeUptime(String originalName) {
    ProcessItem process = processDataStore.get(originalName);
    if (process != null)
      process.setTrackingFrozen(true);

  }

  public void unfreezeUptime(String originalName) {
    ProcessItem process = processDataStore.get(originalName);
    if (process != null)
      process.setTrackingFrozen(false);
  }

  public boolean isFrozen(String originalName) {
    ProcessItem process = processDataStore.get(originalName);
    if (process != null)
      return process.isTrackingFrozen();

    ProcessItem historicProcess = historicData.get(originalName);
    return historicProcess != null && historicProcess.isTrackingFrozen();
  }

  /**
   * Merge-uje trenutne podatke o procesima sa onima iz istorije koji se nisu
   * pokretali u trenutnoj sesiji kako bi se svi upisali u json i istorijski
   * podaci sacuvali
   */
  public List<ProcessItem> getAllWithHistory() {
    List<ProcessItem> result = new ArrayList<>(processDataStore.values());

    for (ProcessItem historicProcess : historicData.values()) {
      String name = historicProcess.getOriginalName();
      long storedUptime = uptimeStore.getOrDefault(name, historicProcess.getUptimeSeconds());

      historicProcess.setUptimeSeconds(storedUptime);
      result.add(historicProcess);
    }
    return result;
  }

  public List<ProcessItem> getAll() {
    return new ArrayList<>(processDataStore.values());
  }

  public ProcessItem getByName(String name) {
    return processDataStore.get(name);
  }

  public void remove(String originalName) {
    processDataStore.remove(originalName);
  }
}
