package org.mbari.vars.ui.javafx.raziel;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.mbari.vars.services.model.EndpointStatus;
import org.mbari.vars.ui.javafx.Icons;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class EndpointStatusPaneController {
    private final ObjectProperty<EndpointStatus> endpointStatuses = new SimpleObjectProperty<>();
    private final HBox root = new HBox();
    private final Label nameLabel = new Label();
    private final Label statusLabel = new Label();
    private final Tooltip urlTooltip = new Tooltip();
    private final Text okIcon = Icons.CHECK.standardSize();
    private final Text failIcon = Icons.CLEAR.standardSize();

    public EndpointStatusPaneController() {
        init();
    }

    public HBox getRoot() {
        return root;
    }

    private void init() {
        nameLabel.setTooltip(urlTooltip);
        root.getChildren().addAll(statusLabel, nameLabel);
        okIcon.setStroke(Color.GREEN);
        failIcon.setStroke(Color.RED);
        endpointStatuses.addListener(((observable, oldValue, newValue) -> update(newValue)));
    }

    public void update(EndpointStatus es) {
        if (es.isHealthy()) {
            updateAsOK(es);
        }
        else {
            updateAsFail(es);
        }
    }

    private void updateAsOK(EndpointStatus es) {
        var healthStatus = es.getHealthStatusCheck().getHealthStatus();
        statusLabel.setGraphic(okIcon);
        var s = String.format("%s v%s on JDK %s",
                es.getEndpointConfig().getName(),
                healthStatus.getVersion(),
                healthStatus.getJdkVersion());
        nameLabel.setText(s);
        urlTooltip.setText(es.getEndpointConfig().getUrl().toExternalForm());
    }

    private void updateAsFail(EndpointStatus es) {
        statusLabel.setGraphic(failIcon);
        nameLabel.setText(es.getEndpointConfig().getName());
        urlTooltip.setText(null);
    }


    public EndpointStatus getEndpointStatuses() {
        return endpointStatuses.get();
    }

    public ObjectProperty<EndpointStatus> endpointStatusesProperty() {
        return endpointStatuses;
    }

    public void setEndpointStatuses(EndpointStatus endpointStatuses) {
        this.endpointStatuses.set(endpointStatuses);
    }

    public static List<EndpointStatusPaneController> from(Set<EndpointStatus> statuses) {
        return statuses.stream()
                .map(es -> {
                    var pane = new EndpointStatusPaneController();
                    Platform.runLater(() -> pane.update(es));
                    return pane;
                })
                .collect(Collectors.toList());
    }
}
