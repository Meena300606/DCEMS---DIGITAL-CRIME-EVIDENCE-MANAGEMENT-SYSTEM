package com.dcems;

import com.dcems.db.Database;
import com.dcems.ui.LoginView;
import com.dcems.ui.MainView;
import com.dcems.util.Session;
import com.dcems.util.UI;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/** Application entry point (run with: mvn javafx:run). */
public class App extends Application {
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        // Any unexpected error inside an event handler is shown nicely instead of silently failing.
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            e.printStackTrace();
            Platform.runLater(() -> UI.error("Something went wrong", String.valueOf(e.getMessage())));
        });

        try {
            Database.init();
        } catch (Exception e) {
            UI.error("Cannot connect to MySQL",
                    "Please start MySQL and check src/main/resources/db.properties (host, port, user, password).\n\n" + e.getMessage());
            Platform.exit();
            return;
        }

        stage.setTitle("DCEMS – Digital Crime Evidence Management System");
        stage.setMinWidth(1200);
        stage.setMinHeight(720);
        showLogin();
        stage.show();
    }

    private void showLogin() {
        Session.user = null;
        Scene scene = new Scene(new LoginView(this::showMain), 1360, 820);
        scene.getStylesheets().add(UI.CSS);
        stage.setScene(scene);
    }

    private void showMain() {
        StackPane root = new StackPane();
        root.getChildren().add(new MainView(root, this::showLogin));
        Scene scene = new Scene(root, 1360, 820);
        scene.getStylesheets().add(UI.CSS);
        stage.setScene(scene);
    }

    public static void main(String[] args) { launch(args); }
}
