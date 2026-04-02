package org.example.model;

/**
 * Konvertuje int u ordinalni rank (1st, 2nd, 3rd, 4th, itd.) i sadrzi rankove
 * za RAM i CPU metrike.
 * Ovaj record se koristi u ProcessDetailsView da bi prikazao rank procesa u
 * odnosu na ostale procese. Rankovi se racunaju u AnalyticsService.
 */

public record ProcessRanking(int ramRank, int cpuRank) {

  public static String toOrdinalRank(int rank) {
    if (rank <= 0)
      return "—";
    int mod100 = rank % 100;
    int mod10 = rank % 10;
    if (mod100 >= 11 && mod100 <= 13)
      return rank + "th";
    return switch (mod10) {
      case 1 -> rank + "st";
      case 2 -> rank + "nd";
      case 3 -> rank + "rd";
      default -> rank + "th";
    };
  }
}
