package org.mbari.m3.vars.annotation.ui;

import com.anchorage.docks.node.DockNode;
import com.anchorage.docks.stations.DockStation;
import com.anchorage.system.AnchorageSystem;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXComboBox;
import de.jensd.fx.glyphs.GlyphsFactory;
import de.jensd.fx.glyphs.materialicons.MaterialIcon;
import de.jensd.fx.glyphs.materialicons.utils.MaterialIconFactory;
import io.reactivex.Observable;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.StatusBar;
import org.mbari.m3.vars.annotation.EventBus;
import org.mbari.m3.vars.annotation.UIToolBox;
import org.mbari.m3.vars.annotation.events.AnnotationsSelectedEvent;
import org.mbari.m3.vars.annotation.events.MediaChangedEvent;
import org.mbari.m3.vars.annotation.events.UserAddedEvent;
import org.mbari.m3.vars.annotation.events.UserChangedEvent;
import org.mbari.m3.vars.annotation.messages.*;
import org.mbari.m3.vars.annotation.model.Annotation;
import org.mbari.m3.vars.annotation.model.Media;
import org.mbari.m3.vars.annotation.model.User;
import org.mbari.m3.vars.annotation.services.UserService;
import org.mbari.m3.vars.annotation.ui.annotable.AnnotationTableController;
import org.mbari.m3.vars.annotation.ui.cbpanel.ConceptButtonPanesController;
import org.mbari.m3.vars.annotation.ui.concepttree.SearchTreePaneController;
import org.mbari.m3.vars.annotation.ui.mediadialog.MediaPaneController;
import org.mbari.m3.vars.annotation.ui.mediadialog.SelectMediaDialog;
import org.mbari.m3.vars.annotation.ui.prefs.PreferencesDialogController;

import java.util.*;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

/**
 * @author Brian Schlining
 * @since 2017-08-28T16:04:00
 */
public class AppPaneController {

    private BorderPane root;
    private DockStation dockStation;
    private SplitPane masterPane;
    private SplitPane topPane;
    private SplitPane bottomPane;
    private TabPane tabPane;
    private final AnnotationTableController annotationTableController;
    private ToolBar toolBar;
    private final UIToolBox toolBox;
    private ComboBox<String> usersComboBox;
    private PopOver openPopOver;
    private StatusBar utilityPane;
    private final ImageViewController imageViewController;
    private final PreferencesDialogController preferencesDialogController;
    private final SelectMediaDialog selectMediaDialog;
    private ControlsPaneController controlsPaneController;
    private MediaPaneController mediaPaneController;
    private BulkEditorPaneController bulkEditorPaneController;
    private AncillaryDataPaneController ancillaryDataPaneController;
    private static final String masterPaneKey =  "master-split-pane";
    private static final String topPaneKey = "top-split-pane";
    private static final String bottomPaneKey = "bottom-split-pane";



    public AppPaneController(UIToolBox toolBox) {
        this.toolBox = toolBox;
        selectMediaDialog = new SelectMediaDialog(toolBox.getServices().getMediaService(),
                toolBox.getI18nBundle());
        selectMediaDialog.getDialogPane().getStylesheets().addAll(toolBox.getStylesheets());
        annotationTableController = new AnnotationTableController(toolBox);
        preferencesDialogController = new PreferencesDialogController(toolBox);
        imageViewController = new ImageViewController();
        controlsPaneController = new ControlsPaneController(toolBox);
        mediaPaneController = MediaPaneController.newInstance();
        bulkEditorPaneController = BulkEditorPaneController.newInstance(toolBox);
        ancillaryDataPaneController = new AncillaryDataPaneController(toolBox);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saveDividerPositions(masterPaneKey, getMasterPane());
            saveDividerPositions(bottomPaneKey, getBottomPane());
            saveDividerPositions(topPaneKey, getTopPane());
        }));


    }

    public BorderPane getRoot() {
        if (root == null) {
            root = new BorderPane(getMasterPane());
            root.setTop(getToolBar());
            root.setBottom(getUtilityPane());
        }
        return root;
    }

    private void saveDividerPositions(String name, SplitPane pane) {
        // Pref path is UserNode/Class/name/0 (or 1 or 2)
        Preferences p0 = Preferences.userNodeForPackage(getClass());
        Preferences p1 = p0.node(name);
        double[] pos = pane.getDividerPositions();
        for (int i = 0; i < pos.length; i++) {
            p1.putDouble(i+ "", pos[i]);
        }
    }

    private void loadDividerPositions(String name, SplitPane pane) {
        Preferences p0 = Preferences.userNodeForPackage(getClass());
        Preferences p1 = p0.node(name);
        double[] positions = pane.getDividerPositions();
        for (int i = 0; i < positions.length; i++) {
            try {
                double v = p1.getDouble(i + "", positions[i]);
                pane.setDividerPosition(i, v);
            }
            catch (Exception e) {
                // TODO log it
            }
        }
    }


    public SplitPane getMasterPane() {
        if (masterPane == null) {
            masterPane = new SplitPane(getTopPane(), getBottomPane());
            masterPane.setOrientation(Orientation.VERTICAL);
            loadDividerPositions(masterPaneKey, masterPane);
        }
        return masterPane;

    }

    public SplitPane getTopPane() {
        if (topPane == null) {
            topPane = new SplitPane(annotationTableController.getTableView(),
                   getTabPane());
            imageViewController.getImageView()
                    .fitWidthProperty()
                    .bind(Bindings.subtract(1, topPane.getDividers().
                            get(0).positionProperty()).multiply(topPane.widthProperty()));
            loadDividerPositions(topPaneKey, topPane);
        }
        return topPane;
    }

    public SplitPane getBottomPane() {
        if (bottomPane == null) {
            ConceptButtonPanesController panesController = new ConceptButtonPanesController(toolBox);
            bottomPane = new SplitPane(controlsPaneController.getRoot(),
                    panesController.getRoot());
            bottomPane.setOrientation(Orientation.VERTICAL);
            loadDividerPositions(bottomPaneKey, bottomPane);
        }
        return bottomPane;
    }

    public TabPane getTabPane() {
        if (tabPane == null) {
            tabPane = new TabPane();

            Observable<Object> observable = toolBox.getEventBus().toObserverable();

            Tab imageTab = new Tab("Images", imageViewController.getRoot());
            imageTab.setClosable(false);
            observable.ofType(AnnotationsSelectedEvent.class)
                    .subscribe(evt -> {
                        Collection<Annotation> annotations = evt.get();
                        if (annotations == null || annotations.size() != 1) {
                            imageViewController.setAnnotation(null);
                        }
                        else {
                            imageViewController.setAnnotation(annotations.iterator().next());
                        }
                    });

            Tab bulkEditTab = new Tab("Bulk Editor", bulkEditorPaneController.getRoot());
            bulkEditTab.setClosable(false);

            Tab mediaTab = new Tab("Media", mediaPaneController.getRoot());
            mediaTab.setClosable(false);
            observable.ofType(AnnotationsSelectedEvent.class)
                    .subscribe(e -> showMediaOfSelectedRow(e.get()));

            Tab dataTab = new Tab("Data", ancillaryDataPaneController.getRoot());
            dataTab.setClosable(false);
            observable.ofType(AnnotationsSelectedEvent.class)
                    .subscribe(e -> showAncillaryData(e.get()));

            SearchTreePaneController treeController = new SearchTreePaneController(toolBox.getServices().getConceptService(),
                    toolBox.getI18nBundle());
            observable.ofType(ShowConceptInTreeViewMsg.class)
                    .subscribe(msg -> treeController.setSearchText(msg.getName()));
            Tab treeTab = new Tab("Knowledgebase", treeController.getRoot());
            treeTab.setClosable(false);

            tabPane.getTabs().addAll(imageTab, bulkEditTab, treeTab, dataTab, mediaTab);

        }
        return tabPane;
    }


    public ToolBar getToolBar() {
        if (toolBar == null) {
            ResourceBundle bundle = toolBox.getI18nBundle();
            GlyphsFactory gf = MaterialIconFactory.get();

            Text openIcon = gf.createIcon(MaterialIcon.VIDEO_LIBRARY, "30px");
            Button openButton = new JFXButton();
            openButton.setGraphic(openIcon);
            openButton.setOnAction(e -> getOpenPopOver().show(openButton));
            openButton.setTooltip(new Tooltip(bundle.getString("apppane.toolbar.button.open")));

            Text undoIcon = gf.createIcon(MaterialIcon.UNDO, "30px");
            Button undoButton = new JFXButton();
            undoButton.setGraphic(undoIcon);
            undoButton.setOnAction(e -> toolBox.getEventBus().send(new UndoMsg()));
            undoButton.setTooltip(new Tooltip(bundle.getString("apppane.toolbar.button.undo")));

            Text redoIcon = gf.createIcon(MaterialIcon.REDO, "30px");
            Button redoButton = new JFXButton();
            redoButton.setGraphic(redoIcon);
            redoButton.setOnAction(e -> toolBox.getEventBus().send(new RedoMsg()));
            redoButton.setTooltip(new Tooltip(bundle.getString("apppane.toolbar.button.redo")));

            Text refreshIcon = gf.createIcon(MaterialIcon.CACHED, "30px");
            Button refreshButton = new JFXButton();
            refreshButton.setGraphic(refreshIcon);
            refreshButton.setOnAction(e -> toolBox.getEventBus().send(new ClearCacheMsg()));
            refreshButton.setTooltip(new Tooltip(bundle.getString("apppane.toolbar.button.refresh")));

            Text prefsIcon = gf.createIcon(MaterialIcon.SETTINGS, "30px");
            Button prefsButton = new JFXButton();
            prefsButton.setGraphic(prefsIcon);
            prefsButton.setOnAction(e -> preferencesDialogController.show());
            prefsButton.setTooltip(new Tooltip(bundle.getString("apppane.toolbar.button.prefs")));

            Label videoLabel = new Label(toolBox.getI18nBundle().getString("apppane.label.media"));
            Label mediaLabel = new Label();
            toolBox.getEventBus()
                    .toObserverable()
                    .ofType(MediaChangedEvent.class)
                    .subscribe(e -> {
                        mediaLabel.setText(null);
                        Media media = e.get();
                        if (media != null) {
                            String uri = media.getUri().toString();
                            if (uri.length() > 70) {
                                int n = uri.length();
                                uri = uri.substring(n - 70, n);
                            }
                            mediaLabel.setText(media.getVideoName() + " [" + uri + "]");
                        }
                    });


            toolBar = new ToolBar(openButton,
                    undoButton,
                    redoButton,
                    refreshButton,
                    prefsButton,
                    new Label(bundle.getString("apppane.label.user")),
                    getUsersComboBox(),
                    videoLabel,
                    mediaLabel);
        }
        return toolBar;
    }

    public PopOver getOpenPopOver() {
        if (openPopOver == null) {

            GlyphsFactory gf = MaterialIconFactory.get();
            ResourceBundle i18n = toolBox.getI18nBundle();

            Text remoteIcon = gf.createIcon(MaterialIcon.VIDEO_LIBRARY, "30px");
            Button remoteButton = new JFXButton(null, remoteIcon);
            remoteButton.setTooltip(new Tooltip(i18n.getString("apppane.button.open.remote")));
            remoteButton.setOnAction(e -> {
                Optional<Media> media = selectMediaDialog.showAndWait();
                media.ifPresent(m -> toolBox.getEventBus().send(new MediaChangedEvent(null, m)));
            });

            Text localIcon = gf.createIcon(MaterialIcon.QUEUE, "30px");
            Button localButton = new JFXButton(null, localIcon);
            localButton.setTooltip(new Tooltip(i18n.getString("apppane.button.open.local")));

            Text tapeIcon = gf.createIcon(MaterialIcon.PERM_MEDIA, "30px");
            Button tapeButton = new JFXButton(null, tapeIcon);
            tapeButton.setTooltip(new Tooltip(i18n.getString("apppane.button.open.tape")));

            Text realtimeIcon = gf.createIcon(MaterialIcon.TIMER, "30px");
            Button realtimeButton = new JFXButton(null, realtimeIcon);
            realtimeButton.setTooltip(new Tooltip(i18n.getString("apppane.button.open.realtime")));

            VBox vbox = new VBox(remoteButton, localButton, tapeButton, realtimeButton);

            openPopOver = new PopOver(vbox);

        }
        return openPopOver;
    }

    public ComboBox<String> getUsersComboBox() {
        if (usersComboBox == null) {

            UserService userService = toolBox.getServices().getUserService();
            usersComboBox = new JFXComboBox<>();
            Comparator<String> sorter = Comparator.comparing(String::toString, String.CASE_INSENSITIVE_ORDER);

            // Listen to UserAddedEvent and add it to the combobox
            EventBus eventBus = toolBox.getEventBus();
            eventBus.toObserverable()
                    .ofType(UserAddedEvent.class)
                    .subscribe(event -> {
                        User user = event.get();
                        usersComboBox.getItems().add(user.getUsername());
                        FXCollections.sort(usersComboBox.getItems(), sorter);
                        usersComboBox.getSelectionModel().select(user.getUsername());
                    });

            // When a username is selected send a change event
            JavaFxObservable.valuesOf(usersComboBox.getSelectionModel().selectedItemProperty())
                    .subscribe(s -> {
                        userService.findAllUsers()
                                .thenAccept(users -> {
                                    Optional<User> opt = users.stream()
                                            .filter(u -> u.getUsername().equals(s))
                                            .findFirst();
                                    opt.ifPresent(user -> eventBus.send(new UserChangedEvent(user)));
                                });
                    });

            // Populate the combobox and select the user form the OS
            userService.findAllUsers()
                    .thenAccept(users -> {
                        List<String> usernames = users.stream()
                                .map(User::getUsername)
                                .sorted(sorter)
                                .collect(Collectors.toList());
                        usersComboBox.setItems(FXCollections.observableList(usernames));
                        String defaultUser = System.getProperty("user.name");
                        usersComboBox.getSelectionModel().select(defaultUser);
                    });

        }
        return usersComboBox;
    }

    public StatusBar getUtilityPane() {
        if (utilityPane == null) {
            utilityPane = new StatusBar();
            utilityPane.setText(null);

            // Listen for progress bar notifications
            Observable<Object> observable = toolBox.getEventBus().toObserverable();
            observable.ofType(ShowProgress.class)
                    .subscribe(s -> Platform.runLater(() -> utilityPane.setProgress(0.00001)));
            observable.ofType(SetProgress.class)
                    .subscribe(s -> Platform.runLater(() -> utilityPane.setProgress(s.getProgress())));
            observable.ofType(HideProgress.class)
                    .subscribe(s -> Platform.runLater(() -> utilityPane.setProgress(0.0)));
            observable.ofType(SetStatusBarMsg.class)
                    .subscribe(s -> Platform.runLater(() -> utilityPane.setText(s.getMsg())));

            Label groupLabel = new Label(toolBox.getI18nBundle()
                    .getString("apppane.statusbar.label.group"));
            ComboBox<String> groupCombobox = new JFXComboBox<>();
            groupCombobox.setEditable(true);
            toolBox.getServices()
                    .getAnnotationService()
                    .findGroups()
                    .thenAccept(groups -> {
                        Platform.runLater(() -> {
                            groupCombobox.getItems().addAll(groups);
                        });
                    });
            groupCombobox.getSelectionModel()
                    .selectedItemProperty()
                    .addListener((obs, oldv, newv) -> toolBox.getData().setGroup(newv));
            groupCombobox.valueProperty().addListener((obs, oldv, newv) ->
                    toolBox.getData().setGroup(newv));

            Pane spacer0 = new Pane();
            spacer0.setPrefSize(20, 5);

            Label activityLabel = new Label(toolBox.getI18nBundle()
                    .getString("apppane.statusbar.label.activity"));
            ComboBox<String> activityCombobox = new JFXComboBox<>();
            activityCombobox.setEditable(true);
            toolBox.getServices()
                    .getAnnotationService()
                    .findActivities()
                    .thenAccept(activities -> {
                        Platform.runLater(() -> {
                            activityCombobox.getItems().addAll(activities);
                        });
                    });
            activityCombobox.getSelectionModel()
                    .selectedItemProperty()
                    .addListener((obs, oldv, newv) -> toolBox.getData().setActivity(newv));
            activityCombobox.valueProperty().addListener((obs, oldv, newv) ->
                    toolBox.getData().setActivity(newv));

            Pane spacer1 = new Pane();
            spacer1.setPrefSize(20, 5);

            CheckBox checkBox = new JFXCheckBox(toolBox.getI18nBundle()
                    .getString("apppane.statusbar.label.concurrent"));
            checkBox.selectedProperty()
                    .addListener((obs, oldv, newv) ->
                            toolBox.getEventBus().send(new ShowConcurrentAnnotationsMsg(newv)));
            // When the media is changed unselect the check box
            toolBox.getData()
                    .mediaProperty()
                    .addListener((obs, oldv, newv) -> checkBox.setSelected(false));

            utilityPane.getLeftItems()
                    .addAll(groupLabel,
                            groupCombobox,
                            spacer0,
                            activityLabel,
                            activityCombobox,
                            spacer1,
                            checkBox);


        }
        return utilityPane;
    }

    public AnnotationTableController getAnnotationTableController() {
        return annotationTableController;
    }

    private void showMediaOfSelectedRow(Collection<Annotation> annotations) {
        if (annotations == null || annotations.size() != 1) {
            mediaPaneController.setMedia(null);
        }
        else {
            Annotation annotation = annotations.iterator().next();
            Media media = toolBox.getData().getMedia();
            if (media != null &&
                    annotation.getVideoReferenceUuid().equals(media.getVideoReferenceUuid())) {
                mediaPaneController.setMedia(media);
            }
            else {
                toolBox.getServices()
                        .getMediaService()
                        .findByUuid(annotation.getVideoReferenceUuid())
                        .thenAccept(m -> mediaPaneController.setMedia(m));
            }
        }
    }

    private void showAncillaryData(Collection<Annotation> annotations) {
        if (annotations == null || annotations.size() != 1) {
            ancillaryDataPaneController.setAncillaryData(null);
        }
        else {
            Annotation first = annotations.iterator().next();
            ancillaryDataPaneController.setAncillaryData(first.getObservationUuid());
        }
    }
}
