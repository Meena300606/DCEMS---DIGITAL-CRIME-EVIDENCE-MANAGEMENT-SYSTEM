package com.dcems.ui;

import com.dcems.dao.ActivityDAO;
import com.dcems.dao.EvidenceDAO;
import com.dcems.dao.StatsDAO;
import com.dcems.model.Activity;
import com.dcems.model.Evidence;
import com.dcems.util.Fmt;
import com.dcems.util.Session;
import com.dcems.util.UI;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.util.Map;
import java.util.function.Consumer;

/** Landing page: live statistics, quick actions, charts, recent evidence and activity feed. */
public class DashboardView extends ScrollPane {

    public DashboardView(Consumer<String> nav) {
        VBox page = new VBox(20);
        page.getStyleClass().add("page");

        // ---- header ----
        page.getChildren().add(UI.header("Welcome back, " + firstName(Session.name()) + " 👋",
                "Here is what is happening across your investigations today."));

        // ---- stat cards ----
        long total = StatsDAO.totalCases(), open = StatsDAO.openCases();
        long ev = StatsDAO.totalEvidence(), sus = StatsDAO.totalSuspects(), ver = StatsDAO.verifiedEvidence();
        HBox stats = new HBox(18,
                stat("📁", "ic-blue", "Total Cases", total),
                stat("🔥", "ic-orange", "Active Cases", open),
                stat("🗄", "ic-purple", "Evidence", ev),
                stat("✅", "ic-green", "Verified", ver),
                stat("🕵", "ic-blue", "Suspects", sus));
        stats.getChildren().forEach(n -> HBox.setHgrow(n, Priority.ALWAYS));
        page.getChildren().add(stats);

        // ---- quick actions ----
        HBox quick = new HBox(12,
                quick("➕  New Case", () -> nav.accept("cases")),
                quick("📸  Capture Evidence", () -> nav.accept("capture")),
                quick("🔍  Search Vault", () -> nav.accept("vault")),
                quick("🔗  Chain of Custody", () -> nav.accept("custody")),
                quick("📊  Generate Report", () -> nav.accept("reports")));
        page.getChildren().add(quick);

        // ---- charts ----
        PieChart pie = new PieChart();
        for (Map.Entry<String, Long> e : StatsDAO.evidenceByType().entrySet())
            pie.getData().add(new PieChart.Data(e.getKey() + " (" + e.getValue() + ")", e.getValue()));
        pie.setLegendVisible(false);
        pie.setLabelsVisible(true);
        pie.setPrefHeight(300);
        pie.setAnimated(true);

        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis();
        y.setMinorTickVisible(false);
        y.setTickLabelFormatter(new NumberAxis.DefaultFormatter(y) {
            @Override public String toString(Number n) {
                return n.doubleValue() == Math.floor(n.doubleValue()) ? String.valueOf(n.intValue()) : "";
            }
        });
        BarChart<String, Number> bar = new BarChart<>(x, y);
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        for (Map.Entry<String, Long> e : StatsDAO.casesByStatus().entrySet())
            s.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        bar.getData().add(s);
        bar.setLegendVisible(false);
        bar.setPrefHeight(300);
        bar.setBarGap(6);
        bar.setCategoryGap(40);

        VBox pieCard = UI.card(UI.section("Evidence by Type"), pie);
        VBox barCard = UI.card(UI.section("Cases by Status"), bar);
        HBox charts = new HBox(18, pieCard, barCard);
        HBox.setHgrow(pieCard, Priority.ALWAYS);
        HBox.setHgrow(barCard, Priority.ALWAYS);
        pieCard.setPrefWidth(450);
        barCard.setPrefWidth(450);
        page.getChildren().add(charts);

        // ---- recent evidence + activity ----
        TableView<Evidence> table = UI.table();
        table.getColumns().add(UI.col("Evidence No", (Evidence e) -> e.evidenceNo, 105));
        table.getColumns().add(UI.col("Case", (Evidence e) -> e.caseNo, 105));
        table.getColumns().add(UI.flex(UI.col("Type", (Evidence e) -> e.type, 100)));
        table.getColumns().add(UI.badgeCol("Status", (Evidence e) -> e.status, 115));
        table.setItems(FXCollections.observableArrayList(EvidenceDAO.recent(6)));
        table.setPrefHeight(300);
        table.setMinHeight(300);
        VBox recent = UI.card(UI.section("Recently Captured Evidence"), table);

        ListView<Activity> feed = new ListView<>(FXCollections.observableArrayList(ActivityDAO.recent(8)));
        feed.setPlaceholder(UI.placeholder("No activity yet"));
        feed.setPrefHeight(300);
        feed.setMinHeight(300);
        feed.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Activity a, boolean empty) {
                super.updateItem(a, empty);
                if (empty || a == null) { setGraphic(null); return; }
                Label act = new Label(a.action);
                act.getStyleClass().addAll("badge", "badge-collected");
                Label d = new Label(a.detail);
                d.setWrapText(true);
                d.setMaxWidth(260);
                Label meta = new Label(a.username + " • " + Fmt.dt(a.createdAt));
                meta.getStyleClass().add("page-sub");
                setGraphic(new VBox(3, act, d, meta));
            }
        });
        VBox act = UI.card(UI.section("Activity Feed"), feed);
        act.setMinWidth(340);
        act.setMaxWidth(340);
        HBox bottom = new HBox(18, recent, act);
        HBox.setHgrow(recent, Priority.ALWAYS);
        page.getChildren().add(bottom);

        setContent(page);
        setFitToWidth(true);
    }

    private static String firstName(String n) { return n.contains(" ") ? n.substring(0, n.indexOf(' ')) : n; }

    private static HBox stat(String icon, String colour, String label, long value) {
        Label ic = new Label(icon);
        ic.getStyleClass().add("stat-icon-text");
        StackPane iconBox = new StackPane(ic);
        iconBox.getStyleClass().addAll("stat-icon", colour);

        IntegerProperty v = new SimpleIntegerProperty(0);
        Label val = new Label("0");
        val.getStyleClass().add("stat-value");
        val.textProperty().bind(v.asString());
        new Timeline(new KeyFrame(Duration.millis(900), new KeyValue(v, (int) value, Interpolator.EASE_OUT))).play();
        Label lab = new Label(label);
        lab.getStyleClass().add("stat-label");
        lab.setMinWidth(Region.USE_PREF_SIZE);
        val.setMinWidth(Region.USE_PREF_SIZE);

        HBox box = new HBox(12, iconBox, new VBox(0, val, lab));
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("stat-card");
        box.setMaxWidth(Double.MAX_VALUE);
        return box;
    }

    private static Button quick(String text, Runnable r) {
        Button b = new Button(text);
        b.getStyleClass().add("quick");
        b.setOnAction(e -> r.run());
        return b;
    }
}
