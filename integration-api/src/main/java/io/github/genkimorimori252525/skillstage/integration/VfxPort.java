package io.github.genkimorimori252525.skillstage.integration;

import io.github.genkimorimori252525.skillstage.kvfx.KvfxProject;

public interface VfxPort {
    PortDescriptor descriptor();

    FidelityReport assess(KvfxProject project);
}