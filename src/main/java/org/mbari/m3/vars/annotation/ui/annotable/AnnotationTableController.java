package org.mbari.m3.vars.annotation.ui.annotable;

import com.sun.javafx.scene.control.skin.TableViewSkin;
import com.sun.javafx.scene.control.skin.VirtualFlow;
import io.reactivex.Observable;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.mbari.m3.vars.annotation.Data;
import org.mbari.m3.vars.annotation.EventBus;
import org.mbari.m3.vars.annotation.events.AnnotationsAddedEvent;
import org.mbari.m3.vars.annotation.events.AnnotationsChangedEvent;
import org.mbari.m3.vars.annotation.events.AnnotationsRemovedEvent;
import org.mbari.m3.vars.annotation.events.AnnotationsSelectedEvent;
import org.mbari.m3.vars.annotation.messages.SeekMsg;
import org.mbari.m3.vars.annotation.model.Media;
import org.mbari.m3.vars.annotation.util.FormatUtils;
import org.mbari.m3.vars.annotation.UIToolBox;
import org.mbari.m3.vars.annotation.model.Annotation;
import org.mbari.m3.vars.annotation.model.Association;
import org.mbari.vcr4j.time.Timecode;

import javax.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

/**
 * @author Brian Schlining
 * @since 2017-05-10T10:04:00
 *
 * TODO all strings need to be put in i18n
 */
public class AnnotationTableController {

    private TableView<Annotation> tableView;
    private final ResourceBundle i18n;
    private final EventBus eventBus;
    private final Data data;


    @Inject
    public AnnotationTableController(UIToolBox toolBox) {
        this.i18n = toolBox.getI18nBundle();
        this.eventBus = toolBox.getEventBus();
        this.data = toolBox.getData();

        Observable<Object> observable = eventBus.toObserverable();

        observable.ofType(AnnotationsAddedEvent.class)
                .subscribe(e -> {
                    getTableView().getItems().addAll(e.get());
                    getTableView().sort();
                });

        observable.ofType(AnnotationsRemovedEvent.class)
                .subscribe(e -> getTableView().getItems().removeAll(e.get()));

        // Listen for external selection events, but ignore ones generated by this controlle
        observable.ofType(AnnotationsSelectedEvent.class)
                .filter(e -> e.getEventSource() != AnnotationTableController.this)
                .subscribe(e -> select(e.get()));

        observable.ofType(AnnotationsChangedEvent.class)
                .subscribe(e -> {
                    Collection<Annotation> annotations = e.get();
                    ObservableList<Annotation> items = getTableView().getItems();
                    for (Annotation a : annotations) {
                        int idx = items.indexOf(a);
                        items.remove(idx);
                        items.add(idx, a);
                    }
                    tableView.refresh();
                    tableView.sort();
                    eventBus.send(new AnnotationsSelectedEvent(annotations));
                });


        // Load the column visibility and width
        Preferences prefs = Preferences.userNodeForPackage(getClass());
        Preferences columnPrefs = prefs.node("table-columns");
        getTableView().getColumns()
                .forEach(tc -> {
                    Preferences p = columnPrefs.node(tc.getId());
                    String s = p.get("visible", "true");
                    boolean isVisible = s.equals("true");
                    String w = p.get("width", "100");
                    double width = Double.parseDouble(w);
                    tc.setVisible(isVisible);
                    tc.setPrefWidth(width);
                });


        // Save column visibility and width
        Runtime.getRuntime()
                .addShutdownHook(new Thread(() -> {
                    getTableView().getColumns()
                            .forEach(tc -> {
                                Preferences p = columnPrefs.node(tc.getId());
                                p.put("visible", "false");
                                p.put("width", tc.getWidth() + "");
                            });
                    getTableView().getVisibleLeafColumns()
                            .forEach(tc -> {
                                Preferences p = columnPrefs.node(tc.getId());
                                p.put("visible", "true");
                            });
                }));

        // TODO this should save column widths to prefs

//        ChangeListener<Boolean> visibleListener = (obs, oldVis, newVis) -> {
//          data.getPreferencesService().findByNameAndKey()
//        };
//
//        data.preferencesServiceProperty().addListener((obs, oldPref, newPref) -> {
//            getTableView().getColumns()
//                    .forEach(c -> {
//                        c.visibleProperty().
//                    });
//        });
//        tableView.getColumns()
//                .forEach(c -> {
//                    c.visibleProperty().addListener((obj, oldVal, newVal) -> {
//                        data.getPreferencesService()
//                    });
//                });
    }

    private void select(Collection<Annotation> annotations) {
        Platform.runLater(() -> {
            TableView.TableViewSelectionModel<Annotation> selectionModel = getTableView().getSelectionModel();
            selectionModel.clearSelection();
            annotations.forEach(selectionModel::select);
        });

    }


    public TableView<Annotation> getTableView() {
        if (tableView == null) {
            tableView = new TableView<>();
            tableView.setTableMenuButtonVisible(true);

            tableView.getSelectionModel()
                    .selectedItemProperty()
                    .addListener((obs, oldv, newv) -> {
                        int[] visibleRows = getVisibleRows();
                        int i = tableView.getItems().indexOf(newv);
                        if (i < visibleRows[0] || i > visibleRows[1]) {
                            tableView.scrollTo(newv);
                        }
                    });

            // --- Add all columns
            TableColumn<Annotation, Instant> timestampCol = new TableColumn<>(i18n.getString("annotable.col.timestamp"));
            timestampCol.setCellValueFactory(new PropertyValueFactory<>("recordedTimestamp"));
            timestampCol.setId("recordedTimestamp");

            TableColumn<Annotation, Timecode> timecodeCol= new TableColumn<>(i18n.getString("annotable.col.timecode"));
            timecodeCol.setCellValueFactory(new PropertyValueFactory<>("timecode"));
            timecodeCol.setId("timecode");

            TableColumn<Annotation, Duration> elapsedTimeCol = new TableColumn<>(i18n.getString("annotable.col.elapsedtime"));
            elapsedTimeCol.setCellValueFactory(new PropertyValueFactory<>("elapsedTime"));
            elapsedTimeCol.setCellFactory( c -> new TableCell<Annotation, Duration>() {
                @Override
                protected void updateItem(Duration item, boolean empty) {
                    super.updateItem(item, empty);
                    if (item == null || empty) {
                        setText(null);
                    }
                    else {
                        setText(FormatUtils.formatDuration(item));
                    }
                }
            });
            elapsedTimeCol.setId("elapsedTime");

            TableColumn<Annotation, String> obsCol =
                    new TableColumn<>(i18n.getString("annotable.col.concept"));
            obsCol.setCellValueFactory(new PropertyValueFactory<>("concept"));
            obsCol.setId("concept");

            TableColumn<Annotation, List<Association>> assCol =
                    new TableColumn<>(i18n.getString("annotable.col.association"));
            assCol.setCellValueFactory(new PropertyValueFactory<>("associations"));
            assCol.setSortable(false);
            assCol.setCellFactory(c -> new AssociationsTableCell());
            assCol.setId("associations");

            TableColumn<Annotation, FGSValue> fgsCol =
                    new TableColumn<>(i18n.getString("annotable.col.framegrab"));
            fgsCol.setCellValueFactory(param ->
                    new SimpleObjectProperty<>(new FGSValue(param.getValue())));
            fgsCol.setSortable(false);
            fgsCol.setCellFactory(c -> new FGSTableCell());
            fgsCol.setId("fgs");

            TableColumn<Annotation, String> obvCol
                    = new TableColumn<>(i18n.getString("annotable.col.observer"));
            obvCol.setCellValueFactory(new PropertyValueFactory<>("observer"));
            obvCol.setId("observer");

            TableColumn<Annotation, String> actCol
                    = new TableColumn<>(i18n.getString("annotable.col.activity"));
            actCol.setCellValueFactory(new PropertyValueFactory<>("activity"));
            actCol.setId("activity");

            TableColumn<Annotation, String> grpCol
                    = new TableColumn<>(i18n.getString("annotable.col.group"));
            grpCol.setCellValueFactory(new PropertyValueFactory<>("group"));
            grpCol.setId("group");

            // TODO get column order from preferences
            tableView.getColumns().addAll(timecodeCol, elapsedTimeCol, timestampCol,
                    obsCol, assCol, fgsCol, obvCol, actCol, grpCol);


            TableView.TableViewSelectionModel<Annotation> selectionModel = tableView.getSelectionModel();

            selectionModel.setSelectionMode(SelectionMode.MULTIPLE);

            selectionModel.selectedItemProperty()
                    .addListener((obs, oldv, newv) -> {
                        eventBus.send(new AnnotationsSelectedEvent(AnnotationTableController.this,
                                selectionModel.getSelectedItems()));
                    });

            tableView.setRowFactory(param -> {
                TableRow<Annotation> row = new TableRow<>();
                ContextMenu menu = new ContextMenu();
                MenuItem seekItem = new MenuItem(i18n.getString("annotable.ctxmenu.seek"));
                seekItem.setOnAction(evt -> {
                    Annotation a = row.getItem();
                    if (a.getTimecode() != null) {
                        eventBus.send(new SeekMsg<>(a.getTimecode()));
                    }
                    else if (a.getElapsedTime() != null) {
                        eventBus.send(new SeekMsg<>(a.getElapsedTime()));
                    }
                });
                menu.getItems().add(seekItem);

                // Set context menu on row, but use a binding to make it only show for non-empty rows:
                row.contextMenuProperty()
                        .bind(Bindings.when(row.emptyProperty())
                            .then((ContextMenu) null)
                            .otherwise(menu));
                return row;
            });



        }
        return tableView;
    }


    /**
     * THis is a total hack. We need it as scrollTo jumps the selected row to the top
     * of the table. Jarring if the row is already visible. As a workaround, we only
     * scroll if the row isn't already visible
     * @return A 2 element ray with the start and end index of visible rows
     */
    private int[] getVisibleRows() {
        TableView<Annotation> tableView = getTableView();
        TableViewSkin<?> skin = (TableViewSkin<?>) tableView.getSkin();
        if (skin == null) return new int[] {0, 0};
        VirtualFlow<?> flow = (VirtualFlow<?>) skin.getChildren().get(1);
        int idxFirst;
        int idxLast;
        if (flow != null &&
                flow.getFirstVisibleCellWithinViewPort() != null &&
                flow.getLastVisibleCellWithinViewPort() != null) {
            idxFirst = flow.getFirstVisibleCellWithinViewPort().getIndex();
            if (idxFirst > tableView.getItems().size()) {
                idxFirst = tableView.getItems().size() - 1;
            }
            idxLast = flow.getLastVisibleCellWithinViewPort().getIndex();
            if (idxLast > tableView.getItems().size()) {
                idxLast = tableView.getItems().size() - 1;
            }
        }
        else {
            idxFirst = 0;
            idxLast = 0;
        }
        return new int[]{idxFirst, idxLast};
    }

}
