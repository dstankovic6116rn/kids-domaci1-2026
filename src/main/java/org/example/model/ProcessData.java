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
 * 
 * Historic Data mapa se puni iz JSON-a na startu aplikacije i koristi se za
 * merge-ovanje sa novim skenovima i za odrzavanje kontinuiteta u pracenju
 * procesa prilikom ponovnog pokretanja.
 * 
 * 
 */
public class ProcessData {

  private final ConcurrentHashMap<String, ProcessItem> processDataStore = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Long> uptimeStore = new ConcurrentHashMap<>();
  // Popunjava se iz JSON-a na startu aplikacije
  private final ConcurrentHashMap<String, ProcessItem> historicData = new ConcurrentHashMap<>();

  private volatile long lastMergeTimeMs = System.currentTimeMillis();

  /**
   * Cuva tacno uptime vreme prilikom prvog ucitavanja procesa iz JSON-a da bi se
   * uptime mogao tacno agregirati sa uptime-om iz trenutne sesije u slucaju da se
   * proces ponovo pokrene a pocetna vrenost je rucno promenjena u jsonu.
   */
  private final ConcurrentHashMap<String, Long> sessionStartStore = new ConcurrentHashMap<>();

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

      // Popuni sessionStartStore tako da se uptime iz JSON-a moze tacno agregirati sa
      // uptime-om iz trenutne sesije u slucaju da se proces ponovo pokrene
      sessionStartStore.put(name, item.getUptimeSeconds());
    }
    System.out.println("[ProcessStore] Loaded " + records.size()
        + " historic records into store.");
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

        sessionStartStore.putIfAbsent(key, uptime);
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
    processDataStore.entrySet().removeIf(k -> {
      if (scannedKeys.contains(k.getKey())) {
        return false;
      } else {
        // Ovi procesi su verovatno obrisani ili ugaseni od strane korisnika,
        // prebaci ih u historicData mapu
        historicData.put(k.getKey(), k.getValue());
        return true;
      }
    });
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

  // Freeze/Unfreeze processes
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

  public void updateFromWatcher(List<ProcessItem> reloaded) {

    Set<String> reloadedNames = new HashSet<>();

    for (ProcessItem incoming : reloaded) {
      String name = incoming.getOriginalName();
      reloadedNames.add(name);

      ProcessItem live = processDataStore.get(name);

      if (live != null) {
        live.setAliasName(incoming.getAliasName());
        live.setCategory(incoming.getCategory());
        live.setTrackingFrozen(incoming.isTrackingFrozen());

        long currentBank = uptimeStore.getOrDefault(name, 0L);
        long sessionStart = sessionStartStore.getOrDefault(name, currentBank);
        long sessionContribution = Math.max(0L, currentBank - sessionStart);
        long newBank = incoming.getUptimeSeconds() + sessionContribution;

        uptimeStore.put(name, newBank);
        sessionStartStore.put(name, incoming.getUptimeSeconds());
        live.setUptimeSeconds(newBank);

      } else {
        uptimeStore.put(name, incoming.getUptimeSeconds());
        historicData.put(name, incoming);
      }
    }

    // Izbrisi iz istorije procese koji se ne nalaze u reloaded listi — ovi procesi
    // su verovatno obrisani ili preimenovani od strane korisnika
    historicData.keySet().removeIf(name -> !reloadedNames.contains(name));

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
