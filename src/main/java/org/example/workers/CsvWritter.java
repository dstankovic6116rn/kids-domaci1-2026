package org.example.workers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.example.model.ProcessItem;

public class CsvWritter {

  private static final DateTimeFormatter FILE_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss_SSS");

  private static final DateTimeFormatter ROW_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

  private static final String HEADER = "timestamp,pid,process_name,cpu_usage,ram_usage,category,alias_name";

  public void write(List<ProcessItem> processes) {
    // Vreme se belezi u trenutku poziva funkcije a ne u trenutku pokretanja od
    // strane executor-a
    ZonedDateTime zdt = Instant.now().atZone(ZoneOffset.UTC);

    String filename = "snapshot_" + FILE_TIMESTAMP_FORMAT.format(zdt) + ".csv";
    String rowTime = ROW_TIMESTAMP_FORMAT.format(zdt);

    Path target = Paths.get(filename);

    try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8)) {
      writer.write(HEADER);
      writer.newLine();

      for (ProcessItem p : processes) {
        writer.write(buildRow(p, rowTime));
        writer.newLine();
      }
    } catch (IOException e) {
      System.err.println("[ProcessCsvWriter] Error writting CSV: " + e.getMessage());
      e.printStackTrace();
    }

    System.out.println("[ProcessCsvWriter] Wrote to: " + target.toAbsolutePath());
  }

  private String buildRow(ProcessItem p, String timestamp) {
    String cpu = Double.isNaN(p.getCpuUsage())
        ? "N/A"
        : String.format("%.2f", p.getCpuUsage());

    String ram = p.getRamUsageMb() == 0.0
        ? "0"
        : String.format("%.2f", p.getRamUsageMb());

    return String.join(",",
        timestamp,
        String.valueOf(p.getPid()),
        escapeCsv(p.getOriginalName()),
        cpu,
        ram,
        escapeCsv(p.getCategory()),
        escapeCsv(p.getAliasName()));
  }

  /**
   * Escapes a CSV field — wraps in quotes if it contains comma, quote or newline.
   * Internal quotes are doubled per RFC 4180.
   *
   * RFC 4180: https://www.rfc-editor.org/rfc/rfc4180
   */
  private String escapeCsv(String value) {
    if (value == null)
      return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }
}
