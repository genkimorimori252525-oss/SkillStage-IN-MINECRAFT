package io.github.genkimorimori252525.skillstage.integration;

import com.google.gson.JsonObject;
import io.github.genkimorimori252525.skillstage.kvfx.KvfxManifest;
import io.github.genkimorimori252525.skillstage.kvfx.KvfxProject;
import io.github.genkimorimori252525.skillstage.kvfx.KvfxSections;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PortContractTest {
    @Test
    void authoringPortCanPreserveNativeAndOpaqueDataWithoutPhotonDependencyInCore() throws Exception {
        byte[] nativeFx = new byte[] {1, 4, 1, 5, 9, 2, 6};
        AuthoringPort port = new SyntheticPhotonPort();

        KvfxProject project = port.importArtifact(NativeArtifact.of("demo.fxproj", nativeFx));

        assertArrayEquals(nativeFx, project.readEntry("authoring/photon2/original.fxproj").orElseThrow());
        assertArrayEquals(new byte[] {99, 98, 97}, project.readEntry("opaque/photon2/unknown-node.bin").orElseThrow());
        assertEquals("{\"kind\":\"synthetic-particle\"}\n",
                project.readUtf8("semantic/effect.json").orElseThrow());

        NativeArtifact exported = port.exportProject(project);
        assertArrayEquals(nativeFx, exported.primaryBytes());
        assertEquals(EditabilityLevel.FULL, port.assess(project).editability());
    }

    @Test
    void runtimePortReportsEmulationSeparatelyFromEditabilityAndDoesNotDestroyOpaqueData() throws Exception {
        AuthoringPort authoring = new SyntheticPhotonPort();
        RuntimeTargetPort runtime = new SyntheticForge1201Port();
        KvfxProject project = authoring.importArtifact(NativeArtifact.of("demo.fxproj", new byte[] {7, 7, 7}));

        TargetCompilation compilation = runtime.compile(project);

        assertEquals(TargetSupportLevel.EMULATED, compilation.report().targetSupport());
        assertEquals(EditabilityLevel.FULL, compilation.report().editability());
        assertTrue(new String(compilation.artifact().primaryBytes(), StandardCharsets.UTF_8)
                .contains("synthetic-particle"));
        assertArrayEquals(new byte[] {99, 98, 97}, project.readEntry("opaque/photon2/unknown-node.bin").orElseThrow());
    }

    @Test
    void capabilityIdsRemainOpenForFutureProviders() {
        VfxCapability future = new VfxCapability("photon.future-node");
        assertEquals("photon.future-node", future.id());
    }

    private static final class SyntheticPhotonPort implements AuthoringPort {
        private static final PortDescriptor DESCRIPTOR = new PortDescriptor(
                "synthetic-photon2",
                "Synthetic Photon 2 fixture",
                PortRole.AUTHORING,
                "Minecraft 1.21.1 / NeoForge",
                "Photon FXProject-like bytes",
                Set.of(VfxCapability.PARTICLE, VfxCapability.CURVE, VfxCapability.TIMELINE)
        );

        @Override
        public PortDescriptor descriptor() {
            return DESCRIPTOR;
        }

        @Override
        public KvfxProject importArtifact(NativeArtifact artifact) throws VfxPortException {
            try {
                JsonObject source = new JsonObject();
                source.addProperty("adapter", DESCRIPTOR.id());
                source.addProperty("nativeName", artifact.name());
                KvfxManifest manifest = KvfxManifest.create(
                        UUID.fromString("a8d1bc50-a6e9-4f12-a531-74bbab5671dc"),
                        "synthetic-photon-port",
                        "1"
                ).withExtension("source", source);

                return KvfxProject.builder(manifest)
                        .put(KvfxSections.path(KvfxSections.AUTHORING, "photon2/original.fxproj"), artifact.primaryBytes())
                        .putUtf8(KvfxSections.path(KvfxSections.SEMANTIC, "effect.json"), "{\"kind\":\"synthetic-particle\"}\n")
                        .put(KvfxSections.path(KvfxSections.OPAQUE, "photon2/unknown-node.bin"), new byte[] {99, 98, 97})
                        .putUtf8(KvfxSections.path(KvfxSections.EVIDENCE, "synthetic/import.txt"), "fixture\n")
                        .build();
            } catch (Exception e) {
                throw new VfxPortException("synthetic import failed", e);
            }
        }

        @Override
        public NativeArtifact exportProject(KvfxProject project) throws VfxPortException {
            byte[] bytes = project.readEntry("authoring/photon2/original.fxproj")
                    .orElseThrow(() -> new VfxPortException("native Photon payload is unavailable"));
            return NativeArtifact.of("export.fxproj", bytes);
        }

        @Override
        public FidelityReport assess(KvfxProject project) {
            return new FidelityReport(
                    RepresentationLevel.NATIVE,
                    EditabilityLevel.FULL,
                    ReplayLevel.EXACT,
                    TargetSupportLevel.SUPPORTED,
                    List.of()
            );
        }
    }

    private static final class SyntheticForge1201Port implements RuntimeTargetPort {
        private static final PortDescriptor DESCRIPTOR = new PortDescriptor(
                "synthetic-forge-1.20.1",
                "Synthetic Forge 1.20.1 target",
                PortRole.RUNTIME_TARGET,
                "Minecraft 1.20.1 / Forge",
                "synthetic runtime payload",
                Set.of(VfxCapability.PARTICLE)
        );

        @Override
        public PortDescriptor descriptor() {
            return DESCRIPTOR;
        }

        @Override
        public TargetCompilation compile(KvfxProject project) throws VfxPortException {
            String semantic = project.readUtf8("semantic/effect.json")
                    .orElseThrow(() -> new VfxPortException("semantic/effect.json is required by this synthetic target"));
            FidelityReport report = assess(project);
            return new TargetCompilation(
                    NativeArtifact.of("effect.kvfx1201", ("compiled=" + semantic).getBytes(StandardCharsets.UTF_8)),
                    report
            );
        }

        @Override
        public FidelityReport assess(KvfxProject project) {
            return new FidelityReport(
                    RepresentationLevel.RECONSTRUCTED,
                    EditabilityLevel.FULL,
                    ReplayLevel.EQUIVALENT,
                    TargetSupportLevel.EMULATED,
                    List.of("Synthetic target intentionally emulates rather than claiming exact Photon 2 replay")
            );
        }
    }
}