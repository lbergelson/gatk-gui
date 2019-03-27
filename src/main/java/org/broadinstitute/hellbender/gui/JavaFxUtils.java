package org.broadinstitute.hellbender.gui;

import javafx.scene.Node;
import javafx.scene.control.TabPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Paint;

public final class JavaFxUtils {

    public static void colorBackground(TabPane tabs, String color) {
        tabs.setBackground(new Background(new BackgroundFill(Paint.valueOf(color), null, null)));
    }

    public static void expandToFillParent(Node node) {
        VBox.setVgrow(node, Priority.ALWAYS);
        HBox.setHgrow(node, Priority.ALWAYS);
    }
}
