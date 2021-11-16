package org.mbari.vars.ui.mediaplayers.macos.avf;

import org.mbari.vars.ui.mediaplayers.NoopImageCaptureService;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Brian Schlining
 * @since 2018-04-25T11:02:00
 */
public class SelectableNoopImageCaptureService extends NoopImageCaptureService
    implements SelectableImageCaptureService{

    private static final SelectableImageCaptureService instance = new SelectableNoopImageCaptureService();

    @Override
    public Collection<String> listDevices() {
        return Collections.emptyList();
    }

    @Override
    public void setDevice(String device) {
        // Do nothing
    }

    public static SelectableImageCaptureService getInstance() {
        return instance;
    }
}
