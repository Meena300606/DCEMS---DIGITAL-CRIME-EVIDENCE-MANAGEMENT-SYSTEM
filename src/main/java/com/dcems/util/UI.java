package com.dcems.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.function.Function;

/** Reusable UI building blocks so every screen looks consistent. */
public final class UI {
    private UI() {}

    public static final String CSS = UI.class.getResource("/css/style.css").toExternalForm();

    // ---------- text ----------
    public static Label title(String t) { Label l = new Label(t); l.getStyleClass().add("page-title"); return l; }
    public static Label subtitle(String t) { Label l = new Label(t); l.getStyleClass().add("page-sub"); return l; }
    public static Label section(String t) { Label l = new Label(t); l.getStyleClass().add("section-title"); return l; }

    public static VBox header(String title, String sub) {
        VBox v = new VBox(2, title(title), subtitle(sub));
        return v;
    }

    // ---------- buttons ----------
    public static Button button(String text, String style, Runnable action) {
        Button b = new Button(text);
        b.getStyleClass().addAll("btn", style);
        if (action != null) b.setOnAction(e -> action.run());
        return b;
    }
    public static Button primary(String t, Runnable a) { return button(t, "btn-primary", a); }
    public static Button danger(String t, Runnable a)  { return button(t, "btn-danger", a); }
    public static Button ghost(String t, Runnable a)   { return button(t, "btn-ghost", a); }

    // ---------- layout ----------
    public static VBox card(Node... children) {
        VBox v = new VBox(12, children);
        v.getStyleClass().add("card");
        v.setPadding(new Insets(18));
        return v;
    }

    public static VBox field(String label, Node input) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        VBox v = new VBox(5, l, input);
        if (input instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
        return v;
    }

    public static TextField text(String prompt) {
        TextField t = new TextField();
        t.setPromptText(prompt);
        return t;
    }

    public static Region spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    public static Label placeholder(String s) {
        Label l = new Label(s);
        l.getStyleClass().add("placeholder-text");
        return l;
    }

    // ---------- tables ----------
    public static <T> TableColumn<T, String> col(String title, Function<T, String> f, double width) {
        TableColumn<T, String> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(f.apply(d.getValue())));
        c.setPrefWidth(width);
        c.setMinWidth(width);
        c.setMaxWidth(width * 1.35);   // fixed-ish; use flex() for the column that should absorb extra space
        return c;
    }

    /** Lets a column absorb all the spare width of the table. */
    public static <T> TableColumn<T, String> flex(TableColumn<T, String> c) {
        c.setMinWidth(c.getPrefWidth() * 0.6);
        c.setMaxWidth(5000);
        return c;
    }

    public static <T> TableColumn<T, String> badgeCol(String title, Function<T, String> f, double width) {
        TableColumn<T, String> c = col(title, f, width);
        c.setCellFactory(tc -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null || s.isEmpty()) { setGraphic(null); setText(null); return; }
                Label b = new Label(s.replace('_', ' '));
                b.getStyleClass().addAll("badge", "badge-" + s.toLowerCase().replace('_', '-'));
                setGraphic(b);
                setText(null);
            }
        });
        return c;
    }

    public static <T> TableView<T> table() {
        TableView<T> t = new TableView<>();
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        t.setPlaceholder(placeholder("No records found"));
        VBox.setVgrow(t, Priority.ALWAYS);
        return t;
    }

    // ---------- dialogs ----------
    public static void style(Dialog<?> d) { d.getDialogPane().getStylesheets().add(CSS); }

    public static void info(String header, String msg) { alert(Alert.AlertType.INFORMATION, header, msg); }
    public static void error(String header, String msg) { alert(Alert.AlertType.ERROR, header, msg); }

    private static void alert(Alert.AlertType type, String header, String msg) {
        Alert a = new Alert(type);
        a.setTitle("DCEMS");
        a.setHeaderText(header);
        a.setContentText(msg);
        style(a);
        a.showAndWait();
    }

    public static boolean confirm(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setTitle("Please confirm");
        a.setHeaderText(null);
        style(a);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    public static HBox row(Node... n) {
        HBox h = new HBox(10, n);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }
}
