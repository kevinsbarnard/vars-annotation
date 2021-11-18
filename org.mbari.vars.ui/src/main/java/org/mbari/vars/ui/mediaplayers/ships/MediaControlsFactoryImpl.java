package org.mbari.vars.ui.mediaplayers.ships;

import javafx.scene.layout.Pane;
import org.mbari.vars.ui.Initializer;
import org.mbari.vars.ui.UIToolBox;
import org.mbari.vars.ui.mediaplayers.macos.avf.BMImageCaptureService;
import org.mbari.vars.ui.mediaplayers.macos.avf.MacImageCaptureServiceRef;
import org.mbari.vars.services.model.Annotation;
import org.mbari.vars.services.model.Media;
import org.mbari.vars.services.ImageCaptureService;
import org.mbari.vars.services.MediaService;
import mbarix4j.util.SystemUtilities;
import org.mbari.vars.ui.mediaplayers.*;
import org.mbari.vars.ui.mediaplayers.macos.bm.ImageCaptureServiceImpl;
import org.mbari.vcr4j.SimpleVideoError;
import org.mbari.vcr4j.VideoError;
import org.mbari.vcr4j.VideoState;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author Brian Schlining
 * @since 2017-12-28T16:03:00
 */
public class MediaControlsFactoryImpl implements MediaControlsFactory {

    private final UIToolBox toolBox = Initializer.getToolBox();
    private Pane controlPane;


    private Pane getControlPane() {
        if (controlPane == null) {
            controlPane = new ShipControlPane(toolBox.getI18nBundle());
        }
        return controlPane;
    }

    @Override
    public SettingsPane getSettingsPane() {
        return null;
    }

    @Override
    public boolean canOpen(Media media) {
        return media != null &&
                media.getUri() != null &&
                media.getUri().toString().startsWith(MediaParams.URI_PREFIX);
    }

    @Override
    public CompletableFuture<MediaControls<? extends VideoState, ? extends VideoError>> open(Media media) {
        CompletableFuture<MediaControls<? extends VideoState, ? extends VideoError>> cf =
                new CompletableFuture<>();

        ImageCaptureService imageCaptureService = new NoopImageCaptureService();
        if (SystemUtilities.isMacOS()) {
//            imageCaptureService = new MacImageCaptureServiceRef();
            imageCaptureService = ImageCaptureServiceImpl.newInstance();
        }


        ShipVideoIO io = new ShipVideoIO("Real-time for " + media.getCameraId());
        MediaPlayer<ShipVideoState, SimpleVideoError> mediaPlayer = new MediaPlayer<>(media,
                imageCaptureService,
                io,
                () -> {
                    // Set start and end date of a Video in the video asset manager
                    // based on the annotations
                    List<Annotation> annotations = new ArrayList<>(toolBox.getData().getAnnotations());
                    if (annotations.size() > 1) {
                        List<Annotation> sorted = annotations.stream()
                                .filter(Objects::nonNull)
                                .sorted(Comparator.comparing(Annotation::getRecordedTimestamp))
                                .collect(Collectors.toList());
                        if (!sorted.isEmpty()) {
                            Instant start = sorted.get(0).getRecordedTimestamp();
                            Instant end = sorted.get(sorted.size() - 1).getRecordedTimestamp();
                            Duration duration = Duration.between(start, end);
                            MediaService mediaService = toolBox.getServices().getMediaService();
                            mediaService.update(media.getVideoUuid(), start, duration);
                        }
                    }
                });

        MediaControls<ShipVideoState, SimpleVideoError> mediaControls =
                new MediaControls<>(mediaPlayer, getControlPane());

        cf.complete(mediaControls);
        return cf;
    }
}
