package org.example.workers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.example.model.ProcessItem;

public class JsonReader {

  public List<ProcessItem> read(String filePath) {
    Path path = Paths.get(filePath);

    if (!Files.exists(path)) {
      System.out.println("[ProcessJsonReader] No existing file at "
          + path.toAbsolutePath() + " — starting fresh.");
      return List.of();
    }

    String json;
    try {
      json = Files.readString(path, StandardCharsets.UTF_8);
    } catch (IOException e) {
      System.err.println("[ProcessJsonReader] Failed to read "
          + path.toAbsolutePath() + ": " + e.getMessage());
      return List.of();
    }

    try {
      List<ProcessItem> records = parse(json);
      System.out.println("[ProcessJsonReader] Loaded " + records.size()
          + " historic records from " + path.toAbsolutePath());
      return records;
    } catch (Exception e) {
      System.err.println("[ProcessJsonReader] Failed to parse "
          + path.toAbsolutePath() + ": " + e.getMessage());
      return List.of();
    }
  }

  private List<ProcessItem> parse(String json) {
    List<ProcessItem> result = new ArrayList<>();

    // Split into object blocks — each { ... } is one process entry
    String[] blocks = json.split("\\{");

    for (String block : blocks) {
      // Skip blocks that don't look like process entries
      if (!block.contains("originalName"))
        continue;

      String originalName = extractString(block, "originalName");
      String aliasName = extractString(block, "aliasName");
      String category = extractString(block, "category");
      boolean isFreezed = extractBoolean(block, "isTrackingFreezed");
      long totalTimeSeconds = extractLong(block, "totalTimeSeconds");

      if (originalName == null || originalName.isBlank())
        continue;

      // Construct with placeholder pid=0 and startTime=0 —
      // these will be overwritten by the first real scan merge()
      ProcessItem sp = new ProcessItem(0L, 0L, originalName);
      sp.setAliasName(aliasName != null ? aliasName : originalName);
      sp.setCategory(category != null ? category : ProcessItem.DEFAULT_CATEGORY);
      sp.setUptimeSeconds(totalTimeSeconds);
      sp.setTrackingFrozen(isFreezed);

      result.add(sp);
    }

    return result;
  }

  private String extractString(String block, String key) {
    String search = "\"" + key + "\"";
    int keyIdx = block.indexOf(search);
    if (keyIdx == -1)
      return null;

    int colonIdx = block.indexOf(':', keyIdx + search.length());
    if (colonIdx == -1)
      return null;

    // Find the value after the colon, skipping whitespace
    int valueStart = colonIdx + 1;
    while (valueStart < block.length()
        && Character.isWhitespace(block.charAt(valueStart))) {
      valueStart++;
    }

    if (valueStart >= block.length())
      return null;

    // Check for null
    if (block.startsWith("null", valueStart))
      return null;

    // Expect a quoted string
    if (block.charAt(valueStart) != '"')
      return null;

    int strStart = valueStart + 1;
    int strEnd = strStart;
    while (strEnd < block.length()) {
      char c = block.charAt(strEnd);
      if (c == '"' && block.charAt(strEnd - 1) != '\\')
        break;
      strEnd++;
    }

    return block.substring(strStart, strEnd)
        .replace("\\\"", "\"")
        .replace("\\\\", "\\")
        .replace("\\n", "\n")
        .replace("\\r", "\r")
        .replace("\\t", "\t");
  }

  private boolean extractBoolean(String block, String key) {
    String search = "\"" + key + "\"";
    int keyIdx = block.indexOf(search);
    if (keyIdx == -1)
      return false;

    int colonIdx = block.indexOf(':', keyIdx + search.length());
    if (colonIdx == -1)
      return false;

    String remainder = block.substring(colonIdx + 1).stripLeading();
    return remainder.startsWith("true");
  }

  private long extractLong(String block, String key) {
    String search = "\"" + key + "\"";
    int keyIdx = block.indexOf(search);
    if (keyIdx == -1)
      return 0L;

    int colonIdx = block.indexOf(':', keyIdx + search.length());
    if (colonIdx == -1)
      return 0L;

    String remainder = block.substring(colonIdx + 1).stripLeading();
    StringBuilder digits = new StringBuilder();
    for (char c : remainder.toCharArray()) {
      if (Character.isDigit(c))
        digits.append(c);
      else if (!digits.isEmpty())
        break;
    }

    if (digits.isEmpty())
      return 0L;
    try {
      return Long.parseLong(digits.toString());
    } catch (NumberFormatException e) {
      return 0L;
    }
  }

}
