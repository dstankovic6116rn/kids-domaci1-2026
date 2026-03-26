package org.example.model;

/**
 * Prosledjuje se ProcessDetailsView sa ProcessItem kako view ne bi morao da
 * zove DataService da racuna ranking
 */
public class ProcessRanking {

  private final int ramRank;
  private final int cpuRank;

  public ProcessRanking(int ramRank, int cpuRank) {
    this.ramRank = ramRank;
    this.cpuRank = cpuRank;
  }

  public int getRamRank() {
    return ramRank;
  }

  public int getCpuRank() {
    return cpuRank;
  }

  /**
   * Konvertuje int rank u string 1st, 2nd, 3rd itd..
   * 
   * @param rank
   */
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
