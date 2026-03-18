package org.example.model;

import java.util.List;

public class ProcessItem {
  private final String processName;
  private String category;
  private final List<String> categoryOptions;

  public ProcessItem(String processName, String category, List<String> categoryOptions) {
    this.processName = processName;
    this.category = category;
    this.categoryOptions = List.copyOf(categoryOptions); // immutable snapshot;
  }

  // Read-only properties for the view
  public String getProcessName() {
    return processName;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String newCategory) {
    this.category = newCategory;
  }

  public List<String> getCategoryOptions() {
    return categoryOptions;
  }
}
