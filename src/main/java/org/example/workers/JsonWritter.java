package org.example.workers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import org.example.model.ProcessItem;

/**
 * Upisuje sve in-memory procese u JSON fajl
 * 
 * Da fajl ne bi zavrsio u parcijalnom stanju ako se upisivanje prekine
 * 1. Upisujemo prvo u .tmp
 * 2. Atomic move iz .tmp u target
 */
public class JsonWritter {

  public void write(List<ProcessItem> processes, String filePath) throws IOException {
    Path target = Paths.get(filePath);
    Path temp = Paths.get(filePath + ".tmp");

    // Prvo upisujemo u tmp file
    try (BufferedWriter writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
      writer.write(buildJson(processes));
    }

    // Atomically replace target with temp
    // REPLACE_EXISTING handles the case where the file already exists
    Files.move(temp, target,
        StandardCopyOption.REPLACE_EXISTING,
        StandardCopyOption.ATOMIC_MOVE);

    System.out.println("[ProcessJsonWriter] Saved " + processes.size()
        + " processes to " + target.toAbsolutePath());
  }

  /**
   * Generise json format iz liste procesa
   */
  private String buildJson(List<ProcessItem> processes) {
    StringBuilder sb = new StringBuilder();
    sb.append("{\n");
    sb.append("  \"processes\": [\n");

    for (int i = 0; i < processes.size(); i++) {
      ProcessItem p = processes.get(i);
      sb.append("    {\n");
      sb.append("      \"originalName\": ").append(toJsonString(p.getOriginalName())).append(",\n");
      sb.append("      \"aliasName\": ").append(toJsonString(p.getAliasName())).append(",\n");
      sb.append("      \"category\": ").append(toJsonString(p.getCategory())).append(",\n");
      sb.append("      \"isTrackingFreezed\": ").append(p.isTrackingFrozen()).append(",\n");
      sb.append("      \"totalTimeSeconds\": ").append(p.getUptimeSeconds()).append("\n");
      sb.append("    }");
      if (i < processes.size() - 1)
        sb.append(",");
      sb.append("\n");
    }

    sb.append("  ]\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Wrap-uje string u json quotes i escape-uje specijalne karaktere
   */
  private String toJsonString(String value) {
    if (value == null)
      return "null";
    return "\"" + value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        + "\"";
  }

}
