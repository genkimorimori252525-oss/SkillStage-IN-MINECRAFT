package io.github.genkimorimori252525.skillstage.integration;

import io.github.genkimorimori252525.skillstage.kvfx.KvfxProject;

/** External authoring-system seam. Implementations may depend on Photon; this API never does. */
public interface AuthoringPort extends VfxPort {
    KvfxProject importArtifact(NativeArtifact artifact) throws VfxPortException;

    NativeArtifact exportProject(KvfxProject project) throws VfxPortException;
}