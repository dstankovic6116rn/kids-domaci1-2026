package org.example.controller;

import org.example.view.ToolbarView;

public class ToolbarController {

  private final ToolbarView toolbarView;

  public ToolbarController(ToolbarView toolbarView) {
    this.toolbarView = toolbarView;
    bindActions();
  }

  private void bindActions() {
    toolbarView.setOnSave(this::onSave);
    toolbarView.setOnLoad(this::onLoad);
    toolbarView.setOnShutdown(this::onShutdown);
  }

  private void onSave() {
    System.out.println("Save clicked");
  }

  private void onLoad() {
    System.out.println("Load clicked");
  }

  private void onShutdown() {
    System.out.println("Shutdown clicked");
  }

}
