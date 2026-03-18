package org.example.services;

import java.util.List;

import org.example.model.ProcessItem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class DataService {
  public ObservableList<ProcessItem> loadItemPairs() {
    // Each ItemPair takes: label, initial value, list of 3 possible values.
    return FXCollections.observableArrayList(
        new ProcessItem("Apple", "$1.20", List.of("$1.20", "$1.50", "$1.80")),
        new ProcessItem("Banana", "$0.50", List.of("$0.50", "$0.75", "$1.00")),
        new ProcessItem("Cherry", "$3.80", List.of("$3.80", "$4.20", "$4.80")),
        new ProcessItem("Date", "$5.00", List.of("$5.00", "$5.50", "$6.00")),
        new ProcessItem("Elderberry", "$7.25", List.of("$7.25", "$7.75", "$8.25")),
        new ProcessItem("Fig", "$2.10", List.of("$2.10", "$2.40", "$2.70")),
        new ProcessItem("Grape", "$1.75", List.of("$1.75", "$2.00", "$2.25")),
        new ProcessItem("Honeydew", "$4.40", List.of("$4.40", "$4.80", "$5.20")),
        new ProcessItem("Kiwi", "$0.90", List.of("$0.90", "$1.10", "$1.30")),
        new ProcessItem("Lemon", "$0.60", List.of("$0.60", "$0.80", "$1.00")),
        new ProcessItem("Mango", "$2.30", List.of("$2.30", "$2.60", "$2.90")),
        new ProcessItem("Nectarine", "$1.95", List.of("$1.95", "$2.20", "$2.50")));
  }
}
