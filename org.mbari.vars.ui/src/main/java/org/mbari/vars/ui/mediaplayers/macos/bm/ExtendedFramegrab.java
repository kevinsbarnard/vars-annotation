package org.mbari.vars.ui.mediaplayers.macos.bm;

import org.mbari.vars.services.model.Framegrab;

import java.io.File;

public record ExtendedFramegrab(File file, Framegrab framegrab) {
}
