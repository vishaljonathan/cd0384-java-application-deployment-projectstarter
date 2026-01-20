module security.service {
    requires javafx.controls;
    requires javafx.fxml;
    requires image.service;

    // Allow unit tests to access domain classes
    exports com.udacity.catpoint.data;
    exports com.udacity.catpoint.service;

    // Required for JavaFX reflection
    opens com.udacity.catpoint.application to javafx.fxml;
}
