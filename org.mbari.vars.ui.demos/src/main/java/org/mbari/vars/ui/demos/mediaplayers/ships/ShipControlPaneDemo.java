package org.mbari.vars.ui.demos.mediaplayers.ships;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.mbari.vars.ui.Initializer;
import org.mbari.vars.ui.UIToolBox;
import org.mbari.vars.ui.mediaplayers.ships.ShipControlPane;

/**
 * @author Brian Schlining
 * @since 2018-01-02T17:11:00
 */
public class ShipControlPaneDemo extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        UIToolBox toolBox = Initializer.getToolBox();
        ShipControlPane pane = new ShipControlPane(toolBox.getI18nBundle());
        Scene scene = new Scene(pane);
        scene.getStylesheets().addAll(toolBox.getStylesheets());
        primaryStage.setScene(scene);

        primaryStage.show();
    }
}
