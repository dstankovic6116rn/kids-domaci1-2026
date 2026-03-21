package org.example.model;

import java.util.List;

public class ProcessItem {
  public static final String DEFAULT_CATEGORY = "No Category";
  public static final List<String> CATEGORY_OPTIONS = List.of(DEFAULT_CATEGORY, "Work", "Fun", "Other");

  private volatile long pid;
  private volatile long startTime;
  private final String originalName;

  private volatile String aliasName;
  private volatile String category;

  private volatile double cpuUsage;
  private volatile double ramUsageMb;
  private volatile long uptimeSeconds;

  public ProcessItem(long pid, long startTime, String originalName) {
    this.pid = pid;
    this.startTime = startTime;
    this.originalName = originalName;
    this.aliasName = originalName;
    this.category = DEFAULT_CATEGORY;
  }

  public long getPid() {
    return pid;
  }

  public void setPid(long pid) {
    this.pid = pid;
  }

  public long getStartTime() {
    return startTime;
  }

  public void setStartTime(long time) {
    this.startTime = time;
  }

  public String getOriginalName() {
    return originalName;
  }

  public String getAliasName() {
    return aliasName;
  }

  public void setAliasName(String name) {
    this.aliasName = name;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String cat) {
    this.category = cat;
  }

  public double getCpuUsage() {
    return cpuUsage;
  }

  public void setCpuUsage(double cpu) {
    this.cpuUsage = cpu;
  }

  public double getRamUsageMb() {
    return ramUsageMb;
  }

  public void setRamUsageMb(double ram) {
    this.ramUsageMb = ram;
  }

  public long getUptimeSeconds() {
    return uptimeSeconds;
  }

  public void setUptimeSeconds(long uptime) {
    this.uptimeSeconds = uptime;
  }

  public String getDisplayName() {
    return (aliasName != null && !aliasName.isBlank()) ? aliasName : originalName;
  }

}
