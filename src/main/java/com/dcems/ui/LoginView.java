package com.dcems.ui;

import com.dcems.dao.ActivityDAO;
import com.dcems.dao.UserDAO;
import com.dcems.model.User;
import com.dcems.util.Session;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.util.Duration;

/** Split-screen login: brand panel on the left, sign-in card on the right. */
public class LoginView extends HBox {
    private final TextField username = new TextField();
    private final PasswordField password = new PasswordField();
    private final Label error = new Label();
    private final VBox card;

    public LoginView(Runnable onSuccess) {
        // ----- left brand panel -----
        Label hero = new Label("Digital Crime Evidence\nManagement System");
        hero.getStyleClass().add("login-hero");
        Label tag = new Label("Capture, protect and track digital evidence\nfrom crime scene to courtroom.");
        tag.getStyleClass().add("login-tag");
        VBox features = new VBox(12,
                feature("🔒  SHA-256 integrity verification"),
                feature("🔗  Tamper-proof chain of custody"),
                feature("📸  Drag & drop evidence capture"),
                feature("📊  Live case dashboard & reports"));
        Text icon = new Text("🛡");
        icon.setStyle("-fx-font-size: 54px;");
        VBox left = new VBox(22, icon, hero, tag, features);
        left.setAlignment(Pos.CENTER_LEFT);
        left.getStyleClass().add("login-left");
        HBox.setHgrow(left, Priority.ALWAYS);
        left.setPrefWidth(620);

        // ----- right sign-in card -----
        Label title = new Label("Welcome back");
        title.getStyleClass().add("login-title");
        Label sub = new Label("Sign in to access the evidence vault");
        sub.getStyleClass().add("page-sub");
        username.setPromptText("Username");
        password.setPromptText("Password");
        error.getStyleClass().add("login-error");
        error.setManaged(false);
        error.setVisible(false);
        Button login = new Button("Sign In  →");
        login.getStyleClass().addAll("btn", "btn-primary");
        login.setMaxWidth(Double.MAX_VALUE);
        login.setDefaultButton(true);
        login.setOnAction(e -> attempt(onSuccess));
        Label hint = new Label("Demo logins:  admin / admin123   •   investigator / invest123");
        hint.getStyleClass().add("login-hint");
        hint.setWrapText(true);

        card = new VBox(16, title, sub,
                com.dcems.util.UI.field("USERNAME", username),
                com.dcems.util.UI.field("PASSWORD", password),
                error, login, hint);
        card.getStyleClass().add("login-card");
        card.setMaxWidth(400);
        card.setMinWidth(400);

        StackPane right = new StackPane(card);
        right.getStyleClass().add("login-right");
        right.setPrefWidth(660);
        HBox.setHgrow(right, Priority.ALWAYS);

        getChildren().addAll(left, right);
        setFillHeight(true);
    }

    private Label feature(String s) {
        Label l = new Label(s);
        l.getStyleClass().add("login-feature");
        return l;
    }

    private void attempt(Runnable onSuccess) {
        String u = username.getText().trim();
        String p = password.getText();
        if (u.isEmpty() || p.isEmpty()) { fail("Enter username and password"); return; }
        User user = UserDAO.login(u, p);
        if (user == null) { fail("Invalid username or password"); return; }
        Session.user = user;
        ActivityDAO.log("LOGIN", user.fullName + " signed in");
        onSuccess.run();
    }

    /** Shows the error and shakes the card. */
    private void fail(String msg) {
        error.setText("⚠  " + msg);
        error.setManaged(true);
        error.setVisible(true);
        Timeline shake = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(card.translateXProperty(), 0)),
                new KeyFrame(Duration.millis(60), new KeyValue(card.translateXProperty(), -12)),
                new KeyFrame(Duration.millis(120), new KeyValue(card.translateXProperty(), 12)),
                new KeyFrame(Duration.millis(180), new KeyValue(card.translateXProperty(), -8)),
                new KeyFrame(Duration.millis(240), new KeyValue(card.translateXProperty(), 0)));
        shake.play();
    }
}
