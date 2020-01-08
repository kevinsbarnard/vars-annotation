package org.mbari.vars.ui.javafx.buttons;


import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.text.Text;
import org.mbari.vars.ui.UIToolBox;
import org.mbari.vars.ui.commands.DuplicateAnnotationsCmd;
import org.mbari.vars.ui.events.AnnotationsSelectedEvent;
import org.mbari.vars.ui.messages.DuplicateAnnotationMsg;
import org.mbari.vars.ui.javafx.Icons;
import org.mbari.vars.services.model.Annotation;
import org.mbari.vars.services.model.User;

/**
 * @author Brian Schlining
 * @since 2017-08-22T15:58:00
 */
public class DuplicateAnnotationBC extends AbstractBC {


    public DuplicateAnnotationBC(Button button, UIToolBox toolBox) {
        super(button, toolBox);
    }

    protected void init() {
        String tooltip = toolBox.getI18nBundle().getString("buttons.duplicate");
        Text icon = Icons.FLIP_TO_BACK.standardSize();
        initializeButton(tooltip, icon);

        toolBox.getEventBus()
                .toObserverable()
                .ofType(AnnotationsSelectedEvent.class)
                .subscribe(e -> {
                    User user = toolBox.getData().getUser();
                    boolean enabled = (user != null) && e.get().size() > 0;
                    button.setDisable(!enabled);
                });

        toolBox.getEventBus()
                .toObserverable()
                .ofType(DuplicateAnnotationMsg.class)
                .subscribe(m -> apply());
    }

    protected void apply() {
        ObservableList<Annotation> annotations = toolBox.getData().getSelectedAnnotations();
        User user = toolBox.getData().getUser();
        String activity = toolBox.getData().getActivity();
        toolBox.getEventBus()
                .send(new DuplicateAnnotationsCmd(user.getUsername(), activity, annotations, true));
    }

}
