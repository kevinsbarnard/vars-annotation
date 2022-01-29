package org.mbari.vars.ui.javafx.imgfx;

import javafx.application.Platform;
import javafx.scene.image.Image;
import org.mbari.imgfx.imageview.editor.AnnotationPaneController;
import org.mbari.vars.ui.messages.ReloadServicesMsg;

public class IFXPaneController {

    private final IFXToolBox toolBox;
    private AnnotationPaneController annotationPaneController;

    public IFXPaneController(IFXToolBox toolBox) {
        this.toolBox = toolBox;
        init();
    }

    private void init() {
        annotationPaneController = new AnnotationPaneController(toolBox.getEventBus());
        loadConcepts();

        var appEventBus = toolBox.getUIToolBox()
                .getEventBus()
                .toObserverable();

        appEventBus.ofType(ReloadServicesMsg.class)
                .subscribe(msg -> loadConcepts());

        toolBox.getData()
                .selectedImageProperty()
                .addListener((obs, oldv, newv) -> setImage(newv));

    }

    private void loadConcepts() {
        toolBox.getUIToolBox()
                .getServices()
                .getConceptService()
                .findAllNames()
                .thenAccept(concepts -> Platform.runLater(() -> annotationPaneController.setConcepts(concepts)));

    }

    private void setImage(org.mbari.vars.services.model.Image image) {
        var jfxImage = image == null ? null : new Image(image.getUrl().toExternalForm());
        annotationPaneController.getAutoscalePaneController()
                .getView()
                .setImage(jfxImage);
    }

    public AnnotationPaneController getAnnotationPaneController() {
        return annotationPaneController;
    }
}
