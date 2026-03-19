module org.example {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires com.github.oshi;

    opens org.example to javafx.fxml;

    exports org.example;
}
