package org.example.utils;

/**
 * Transformise ukupne sekunde u hh:mm:ss
 */
public class TimeFormatter {
  public TimeFormatter() {
  }

  public static String formatTime(long totalSeconds) {
    long hours = totalSeconds / 3600;
    long minutes = (totalSeconds % 3600) / 60;
    long seconds = totalSeconds % 60;
    return String.format("%02dh%02dm%02ds", hours, minutes, seconds);
  }

}
