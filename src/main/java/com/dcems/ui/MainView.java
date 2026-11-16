package com.dcems.ui;

import com.dcems.util.Session;
import com.dcems.util.Toast;
import com.dcems.util.UI;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Application shell: sidebar navigation + top bar + swappable content area. */
public class MainView extends BorderPane {
    private final StackPane content = new StackPane();
    private final Label crumb = new Label();
    private final ToggleGroup group = new ToggleGroup();
    private final Map<String, ToggleButton> buttons = new LinkedHashMap<>();
    private final Map<String, Supplier<Node>> pages = new LinkedHashMap<>();
    private final Map<String, String> titles = new LinkedHashMap<>();

    public MainView(StackPane host, Runnable onLogout) {
        Toast.install(host);

        register("dashboard", "🏠  Dashboard", "Dashboard", () -> new DashboardView(this::go));
        register("cases", "📁  Cases", "Case Management", CasesView::new);
        register("capture", "📸  Capture Evidence", "Evidence Capture", () -> new EvidenceCaptureView(this::go));
        register("vault", "🗄  Evidence Vault", "Evidence Vault", EvidenceVaultView::new);
        register("suspects", "🕵  Suspects", "Suspect Registry", SuspectsView::new);
        register("custody", "🔗  Chain of Custody", "Chain of Custody", CustodyView::new);
        register("reports", "📊  Reports", "Reports & Export", ReportsView::new);
        if (Session.isAdmin()) register("users", "👥  Users", "User Management", UsersView::new);

        setLeft(buildSidebar(onLogout));
        setTop(buildTopBar());
        setCenter(content);
        go("dashboard");
    }

    private void register(String key, String label, String title, Supplier<Node> page) {
        ToggleButton b = new ToggleButton(label);
        b.getStyleClass().add("nav-btn");
        b.setToggleGroup(group);
        b.setOnAction(e -> go(key));
        // never allow "no selection"
        b.selectedProperty().addListener((o, was, is) -> { if (!is && group.getSelectedToggle() == null) b.setSelected(true); });
        buttons.put(key, b);
        pages.put(key, page);
        titles.put(key, title);
    }

    /** Navigate to a page by key (also used by dashboard quick-actions). */
    public void go(String key) {
        buttons.get(key).setSelected(true);
        crumb.setText("DCEMS  /  " + titles.get(key));
        Node page = pages.get(key).get();
        content.getChildren().setAll(page);
        FadeTransition ft = new FadeTransition(Duration.millis(260), page);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    private VBox buildSidebar(Runnable onLogout) {
        Label icon = new Label("🛡");
        icon.getStyleClass().add("brand-icon-text");
        StackPane iconBox = new StackPane(icon);
        iconBox.getStyleClass().add("brand-icon");
        Label name = new Label("DCEMS");
        name.getStyleClass().add("brand-title");
        Label sub = new Label("EVIDENCE MANAGEMENT");
        sub.getStyleClass().add("brand-sub");
        HBox brand = new HBox(12, iconBox, new VBox(0, name, sub));
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.setPadding(new Insets(24, 0, 12, 22));

        VBox nav = new VBox(4);
        nav.setPadding(new Insets(0, 12, 0, 12));
        Label cap1 = new Label("OVERVIEW");
        cap1.getStyleClass().add("nav-caption");
        nav.getChildren().add(cap1);
        for (var e : buttons.entrySet()) {
            if (e.getKey().equals("cases")) { Label c = new Label("INVESTIGATION"); c.getStyleClass().add("nav-caption"); nav.getChildren().add(c); }
            if (e.getKey().equals("reports")) { Label c = new Label("ANALYSIS"); c.getStyleClass().add("nav-caption"); nav.getChildren().add(c); }
            if (e.getKey().equals("users")) { Label c = new Label("ADMIN"); c.getStyleClass().add("nav-caption"); nav.getChildren().add(c); }
            nav.getChildren().add(e.getValue());
        }

        // user chip
        String initial = Session.name().isEmpty() ? "?" : Session.name().substring(0, 1).toUpperCase();
        Label av = new Label(initial);
        av.getStyleClass().add("avatar-text");
        StackPane avatar = new StackPane(av);
        avatar.getStyleClass().add("avatar");
        Label un = new Label(Session.name());
        un.getStyleClass().add("user-name");
        Label ur = new Label(Session.user.role);
        ur.getStyleClass().add("user-role");
        Button logout = new Button("Sign out");
        logout.getStyleClass().add("logout-btn");
        logout.setOnAction(e -> onLogout.run());
        HBox chip = new HBox(10, avatar, new VBox(0, un, ur), UI.spacer(), logout);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("user-chip");
        VBox.setMargin(chip, new Insets(0, 12, 16, 12));

        VBox side = new VBox(brand, nav, UI.spacer(), chip);
        side.getStyleClass().add("sidebar");
        return side;
    }

    private HBox buildTopBar() {
        crumb.getStyleClass().add("topbar-title");
        Label clock = new Label();
        clock.getStyleClass().add("clock");
        DateTimeFormatter f = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy  •  HH:mm:ss");
        Timeline t = new Timeline(new KeyFrame(Duration.ZERO, e -> clock.setText(LocalDateTime.now().format(f))),
                new KeyFrame(Duration.seconds(1)));
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();
        Circle dot = new Circle(4);
        dot.getStyleClass().add("live-dot");
        Label db = new Label("Database connected");
        db.getStyleClass().add("topbar-title");
        HBox bar = new HBox(14, crumb, UI.spacer(), dot, db, new Label("|"), clock);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("topbar");
        return bar;
    }
}
