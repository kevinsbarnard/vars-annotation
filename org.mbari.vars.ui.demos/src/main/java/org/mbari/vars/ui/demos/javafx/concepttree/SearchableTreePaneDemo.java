package org.mbari.vars.ui.demos.javafx.concepttree;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.mbari.vars.ui.Initializer;
import org.mbari.vars.ui.demos.javafx.DemoConstants;
import org.mbari.vars.services.ConceptService;
import org.mbari.vars.ui.javafx.concepttree.SearchTreePaneController;


import java.util.ResourceBundle;

/**
 * @author Brian Schlining
 * @since 2017-05-16T12:48:00
 */
public class SearchableTreePaneDemo extends Application {

    private static ConceptService conceptService = DemoConstants.newConceptService();
    private static ResourceBundle uiBundle = DemoConstants.UI_BUNDLE;

    public static void main(String[] args) throws InterruptedException {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Platform.setImplicitExit(true);
        SearchTreePaneController controller = new SearchTreePaneController(DemoConstants.getToolBox(), uiBundle);
        BorderPane node = controller.getRoot();
        Scene scene = new Scene(node, 800, 800);
        scene.getStylesheets().addAll(Initializer.getToolBox().getStylesheets());
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
    }
}
