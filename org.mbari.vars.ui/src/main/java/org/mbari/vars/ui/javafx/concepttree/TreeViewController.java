package org.mbari.vars.ui.javafx.concepttree;

import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import org.mbari.vars.services.model.Concept;
import org.mbari.vars.services.model.ConceptMedia;
import org.mbari.vars.services.ConceptService;
import org.mbari.vars.ui.UIToolBox;
import org.mbari.vars.ui.javafx.shared.ImageStage;
import org.mbari.vars.ui.javafx.shared.FilterableTreeItem;
import org.mbari.vars.ui.messages.ReloadServicesMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Brian Schlining
 * @since 2017-06-01T16:21:00
 */
public class TreeViewController {

    private ContextMenu contextMenu;
    private ImageStage imageStage;
    private TreeView<Concept> treeView;
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final UIToolBox toolBox;

    public TreeViewController(UIToolBox toolBox) {
        this.toolBox = toolBox;
        toolBox.getEventBus()
                .toObserverable()
                .ofType(ReloadServicesMsg.class)
                .subscribe(evt -> loadRoot());
    }

    private FilterableTreeItem<Concept> buildTreeItem(Concept concept, FilterableTreeItem<Concept> parent) {
        FilterableTreeItem<Concept> item = new FilterableTreeItem<>(concept);
        if (parent != null) {
            parent.getSourceChildren().add(item);
        }
        concept.getChildren()
                .forEach(c -> buildTreeItem(c, item));
        return item;
    }

    public ImageStage getImageStage() {
        if (imageStage == null) {
            imageStage = new ImageStage();
            ImageStage imageStage = new ImageStage();
            BorderPane imageStageRoot = imageStage.getRoot();
            imageStageRoot.setStyle("-fx-background-color: black");
        }
        return imageStage;
    }

    private ContextMenu getContextMenu() {
        if (contextMenu == null) {
            contextMenu = new ContextMenu();
            MenuItem showImage = new MenuItem("Show image");
            contextMenu.getItems().addAll(showImage);
            showImage.setOnAction(event -> {
                TreeItem<Concept> item = getTreeView().getSelectionModel().getSelectedItem();
                Concept concept = item.getValue();
                if (concept != null &&
                        concept.getConceptDetails() != null &&
                        concept.getConceptDetails().getMedia() != null) {

                    concept.getConceptDetails()
                            .getMedia()
                            .stream()
                            .filter(ConceptMedia::isPrimary)
                            .findFirst()
                            .ifPresent(m -> {
                                Image image = new Image(m.getUrl().toExternalForm());
                                getImageStage().setImage(image);
                                getImageStage().show();
                            });
                }
            });
        }
        return contextMenu;
    }

    public TreeView<Concept> getTreeView() {
        if (treeView == null) {
            TreeCellFactory cellFactory = new TreeCellFactory(toolBox);
            treeView = new TreeView<>();
            treeView.getStyleClass().add("concepttree-treeview");
            treeView.setEditable(false);
            treeView.setCellFactory(tv -> cellFactory.build());
            treeView.setContextMenu(getContextMenu());
            loadRoot();
        }
        return treeView;
    }

    private void loadRoot() {
        toolBox.getServices()
                .getConceptService()
                .findRoot()
                .thenAccept(root -> {
                    Platform.runLater(() -> {
                        log.debug("Using root '" + root.getName() + "' to build tree");
                        TreeItem<Concept> rootItem = buildTreeItem(root, null);
                        treeView.setRoot(rootItem);
                    });
                });
    }
}
