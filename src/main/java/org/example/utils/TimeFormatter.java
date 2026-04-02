package org.example.utils;

/**
 * Transformise ukupne sekunde u hh:mm:ss
 */
public class TimeFormatter {
  public TimeFormatter() {
  }

  public static String formatTime(long totalSeconds) {
    // Zastita od negativnog vremena — moze se desiti ako je store korumpiran
    long safe = Math.max(0L, totalSeconds);
    long hours = safe / 3600;
    long minutes = (safe % 3600) / 60;
    long seconds = safe % 60;
    return String.format("%02dh%02dm%02ds", hours, minutes, seconds);
  }

}
