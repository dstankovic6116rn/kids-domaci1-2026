package org.example.controller;

import java.util.List;

import org.example.model.ProcessItem;
import org.example.services.DataService;
import org.example.view.CategoryDetailsView;
import org.example.view.MainView;

/**
 * Dohvata sve procese koji pripadaju datoj kategoriji iz DataService-a i
 * prosledjuje CategoryDetailsView
 */
public class CategoryDetailsControler {

  private final CategoryDetailsView categoryDetailsView;

  public CategoryDetailsControler(MainView mainView, DataService dataService, String categoryName) {

    List<ProcessItem> processes = dataService.getProcessesByCategoryName(categoryName);
    categoryDetailsView = new CategoryDetailsView(categoryName, processes);
    categoryDetailsView.setOnBackRequested(mainView::showMain);
    mainView.showCategoryDetails(categoryDetailsView);
  }
}
