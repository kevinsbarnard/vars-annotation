package org.mbari.m3.vars.annotation.commands;

import org.mbari.m3.vars.annotation.EventBus;
import org.mbari.m3.vars.annotation.UIToolBox;
import org.mbari.m3.vars.annotation.events.AnnotationsAddedEvent;
import org.mbari.m3.vars.annotation.events.AnnotationsRemovedEvent;
import org.mbari.m3.vars.annotation.events.AnnotationsSelectedEvent;
import org.mbari.m3.vars.annotation.model.Annotation;
import org.mbari.vcr4j.VideoIndex;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;


/**
 * @author Brian Schlining
 * @since 2017-07-26T11:21:00
 */
public class CreateAnnotationFromConceptCmd implements Command {

    private final String concept;
    private volatile Annotation annotation;

    public CreateAnnotationFromConceptCmd(String concept) {
        this.concept = concept;
    }

    @Override
    public void apply(UIToolBox toolBox) {
        toolBox.getVideoControlService()
                .requestVideoIndex()
                .thenAccept(videoIndex -> {
                    // TODO get group and activity setting
                    UUID videoReferenceUuid = toolBox.getData().getMedia().getVideoReferenceUuid();
                    String observer = toolBox.getData().getUser().getUsername();
                    Annotation a0 = new Annotation(concept, observer, videoIndex, videoReferenceUuid);
                    toolBox.getServices()
                            .getAnnotationService()
                            .createAnnotation(a0)
                            .thenAccept(a1 -> {
                                annotation = a1;
                                EventBus eventBus = toolBox.getEventBus();
                                eventBus.send(new AnnotationsAddedEvent(a1));
                                eventBus.send(new AnnotationsSelectedEvent(a1));
                            });
                });
    }

    @Override
    public void unapply(UIToolBox toolBox) {
        toolBox.getServices()
                .getAnnotationService()
                .deleteAnnotation(annotation.getObservationUuid())
                .thenAccept(a -> {
                    toolBox.getEventBus()
                            .send(new AnnotationsRemovedEvent(annotation));
                    annotation = null;
                });
    }

    @Override
    public String getDescription() {
        return "Create Annotation using " + concept;
    }
}
