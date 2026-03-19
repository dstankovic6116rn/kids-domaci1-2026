module org.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;

    opens org.example to javafx.fxml;

    exports org.example;
}
