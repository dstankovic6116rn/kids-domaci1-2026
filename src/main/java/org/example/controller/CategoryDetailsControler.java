package org.example.controller;

public class CategoryDetailsControler {

  private final String categoryName;

  public CategoryDetailsControler(String categoryName) {
    this.categoryName = categoryName;
    // TODO: load data for this category from a service
    System.out.println("DetailController created for: " + categoryName);
  }

  public String getCategoryName() {
    return categoryName;
  }
}
