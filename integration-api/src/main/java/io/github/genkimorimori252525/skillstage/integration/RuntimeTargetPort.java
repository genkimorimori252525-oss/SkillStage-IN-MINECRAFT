package io.github.genkimorimori252525.skillstage.integration;

import io.github.genkimorimori252525.skillstage.kvfx.KvfxProject;

/** Runtime/compiler seam for a concrete Minecraft target such as Forge 1.20.1. */
public interface RuntimeTargetPort extends VfxPort {
    TargetCompilation compile(KvfxProject project) throws VfxPortException;
}