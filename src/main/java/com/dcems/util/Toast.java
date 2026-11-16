package com.dcems.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/** Small pop-up notifications that fade in and out. */
public final class Toast {
    private static StackPane host;
    private Toast() {}

    public static void install(StackPane root) { host = root; }
    public static void success(String m) { show(m, "success"); }
    public static void error(String m) { show(m, "error"); }
    public static void info(String m) { show(m, "info"); }

    private static void show(String msg, String type) {
        if (host == null) return;
        Label l = new Label(msg);
        l.getStyleClass().addAll("toast", "toast-" + type);
        l.setMouseTransparent(true);
        l.setOpacity(0);
        StackPane.setAlignment(l, Pos.BOTTOM_RIGHT);
        StackPane.setMargin(l, new Insets(0, 26, 26, 0));
        host.getChildren().add(l);
        FadeTransition in = new FadeTransition(Duration.millis(220), l);
        in.setToValue(1);
        PauseTransition wait = new PauseTransition(Duration.seconds(2.8));
        FadeTransition out = new FadeTransition(Duration.millis(450), l);
        out.setToValue(0);
        out.setOnFinished(e -> host.getChildren().remove(l));
        new SequentialTransition(in, wait, out).play();
    }
}
