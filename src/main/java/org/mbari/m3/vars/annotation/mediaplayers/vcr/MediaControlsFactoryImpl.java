package org.mbari.m3.vars.annotation.mediaplayers.vcr;

import org.mbari.m3.vars.annotation.Initializer;
import org.mbari.m3.vars.annotation.UIToolBox;
import org.mbari.m3.vars.annotation.mediaplayers.*;
import org.mbari.m3.vars.annotation.mediaplayers.macos.MacImageCaptureServiceRef;
import org.mbari.m3.vars.annotation.messages.ShowInfoAlert;
import org.mbari.m3.vars.annotation.messages.ShowNonfatalErrorAlert;
import org.mbari.m3.vars.annotation.model.Annotation;
import org.mbari.m3.vars.annotation.model.Media;
import org.mbari.m3.vars.annotation.services.ImageCaptureService;
import org.mbari.m3.vars.annotation.services.MediaService;
import org.mbari.util.SystemUtilities;
import org.mbari.vcr4j.*;
import org.mbari.vcr4j.commands.VideoCommands;
import org.mbari.vcr4j.decorators.StatusDecorator;
import org.mbari.vcr4j.decorators.VCRSyncDecorator;
import org.mbari.vcr4j.jserialcomm.SerialCommVideoIO;
import org.mbari.vcr4j.rs422.RS422Error;
import org.mbari.vcr4j.rs422.RS422State;
import org.mbari.vcr4j.rs422.commands.RS422VideoCommands;
import org.mbari.vcr4j.rs422.decorators.UserbitsAsTimeDecorator;

import org.mbari.vcr4j.ui.javafx.VcrControlPaneController;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * @author Brian Schlining
 * @since 2018-03-26T10:52:00
 */
public class MediaControlsFactoryImpl implements MediaControlsFactory {


    private final UIToolBox toolBox = Initializer.getToolBox();
    public static Class PREF_NODE_KEY = MediaControlsFactoryImpl.class;
    public static final String PREF_SERIALPORT_KEY = "serial-port";
    private final Preferences prefs = Preferences.userNodeForPackage(PREF_NODE_KEY);
    private final VcrControlPaneController vcrController = VcrControlPaneController.newInstance();

    public MediaControlsFactoryImpl() {
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
        return openMediaPlayer(media).thenApply(mediaPlayer -> {
            VideoIO<RS422State, RS422Error> io = mediaPlayer.getVideoIO();
            VideoController<RS422State, RS422Error> videoController = new VideoController<>(io);
            vcrController.setVideoController(videoController);
            return new MediaControls<>(mediaPlayer, vcrController.getRoot());
        });
    }

    private CompletableFuture<MediaPlayer<RS422State, RS422Error>> openMediaPlayer(Media media) {
        CompletableFuture<MediaPlayer<RS422State, RS422Error>> cf =
                new CompletableFuture<>();

        // We need to close the old media player before a new one is created. Otherwise the
        // serial port may already be in use
        MediaPlayer<? extends VideoState, ? extends VideoError> oldMediaPlayer = toolBox.getMediaPlayer();
        if (oldMediaPlayer != null) {
            oldMediaPlayer.close();
        }

        Runnable runnable = () -> {

            ImageCaptureService imageCaptureService = new NoopImageCaptureService();
            if (SystemUtilities.isMacOS()) {
                imageCaptureService = new MacImageCaptureServiceRef();
            }

            Optional<String> opt = getSelectedSerialPort();
            if (opt.isPresent()) {
                String serialPort = opt.get();
                SerialCommVideoIO io = SerialCommVideoIO.open(serialPort);
                VCRSyncDecorator<RS422State, RS422Error> syncDecorator = new VCRSyncDecorator<>(io);
                StatusDecorator<RS422State, RS422Error> statusDecorator = new StatusDecorator<>(io);
                UserbitsAsTimeDecorator timeDecorator = new UserbitsAsTimeDecorator(io);
                VideoIO<RS422State, RS422Error> simpleIo = new SimpleVideoIO<>(io.getConnectionID(),
                        io.getCommandSubject(),
                        io.getStateObservable(),
                        io.getErrorObservable(),
                        timeDecorator.getIndexObservable());
                simpleIo.send(VideoCommands.REQUEST_INDEX);
                MediaPlayer<RS422State, RS422Error> mediaPlayer = new MediaPlayer<>(media,
                        imageCaptureService,
                        simpleIo,
                        () -> {
                            // Set start and end date of a Video in the video asset manager
                            // based on the annotations
                            List<Annotation> annotations = new ArrayList<>(toolBox.getData().getAnnotations());
                            if (annotations.size() > 1) {
                                List<Annotation> sorted = annotations.stream()
                                        .filter(Objects::nonNull)
                                        .filter(a -> a.getRecordedTimestamp() != null && a.getRecordedTimestamp().isAfter(Instant.EPOCH))
                                        .sorted(Comparator.comparing(Annotation::getRecordedTimestamp))
                                        .collect(Collectors.toList());
                                Instant start = sorted.get(0).getRecordedTimestamp();
                                Instant end = sorted.get(sorted.size() - 1).getRecordedTimestamp();
                                Duration duration = Duration.between(start, end);
                                MediaService mediaService = toolBox.getServices().getMediaService();
                                mediaService.update(media.getVideoReferenceUuid(), start, duration);
                            }
                            syncDecorator.unsubscribe();
                            statusDecorator.unsubscribe();
                            timeDecorator.unsubscribe();
                        });
                cf.complete(mediaPlayer);
            } else {
                // TODO send alert to pop up dialog
                ResourceBundle i18n = Initializer.getToolBox().getI18nBundle();
                String title = i18n.getString("mediaplayer.vcr.open.missing.title");
                String header = i18n.getString("mediaplayer.vcr.open.missing.header");
                String content = i18n.getString("mediaplayer.vcr.open.missing.content");
                ShowInfoAlert alert = new ShowInfoAlert(title, header, content);
                Initializer.getToolBox()
                        .getEventBus()
                        .send(alert);
            }
        };
        Thread thread = new Thread(runnable,
                getClass().getSimpleName() + "-" + Instant.now());
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler((th, ex) -> {
            ResourceBundle i18n = Initializer.getToolBox().getI18nBundle();
            String title = i18n.getString("mediaplayer.vcr.open.error.title");
            String header = i18n.getString("mediaplayer.vcr.open.error.header");
            String content = i18n.getString("mediaplayer.vcr.open.error.content") + " " +
                     MediaControlsFactoryImpl.getSelectedSerialPort().orElse("<undefined>");
            ShowNonfatalErrorAlert alert = new ShowNonfatalErrorAlert(title, header, content, new RuntimeException(ex));
            Initializer.getToolBox()
                    .getEventBus()
                    .send(alert);
        });
        thread.start();

        return cf;
    }

    /**
     *
     * @return All serial ports
     */
    public static List<String> getSerialPorts() {
        return SerialCommVideoIO.getSerialPorts();
    }

    /**
     * Retrieves the serialPort value from local preferences
     * @return The current or most recently used serial port
     */
    public static Optional<String> getSelectedSerialPort() {
        Preferences prefs = Preferences.userNodeForPackage(PREF_NODE_KEY);
        String serialPort = prefs.get(PREF_SERIALPORT_KEY, null);
        if (serialPort == null || serialPort.isEmpty()) {
            return Optional.empty();
        }
        else {
            return Optional.of(serialPort);
        }
    }


    /**
     * Stores the serialPort value to local preferences
     * @param serialPort
     */
    public static void setSelectedSerialPort(String serialPort) {
        Preferences prefs = Preferences.userNodeForPackage(MediaControlsFactoryImpl.PREF_NODE_KEY);
        prefs.put(MediaControlsFactoryImpl.PREF_SERIALPORT_KEY, serialPort);
    }
}
