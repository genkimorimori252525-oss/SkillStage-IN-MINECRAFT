package io.github.genkimorimori252525.skillstage.photon2;

import io.github.genkimorimori252525.skillstage.integration.NativeArtifact;
import io.github.genkimorimori252525.skillstage.integration.VfxPortException;
import io.github.genkimorimori252525.skillstage.photon2.internal.Nbt;
import io.github.genkimorimori252525.skillstage.photon2.internal.NbtBinary;

import java.io.IOException;
import java.util.ArrayList;

public final class Photon2NativeCodec {
    public static final int CURRENT_PROJECT_VERSION = 5;
    public static final int CURRENT_PARTICLE_OBJECT_VERSION = 2;
    public static final int MAX_NATIVE_BYTES = 64 * 1024 * 1024;

    public Photon2Document parse(NativeArtifact artifact) throws VfxPortException {
        byte[] bytes = artifact.primaryBytes();
        validateNativeEnvelope(artifact.name(), bytes);
        final Photon2NativeKind kind = nativeKind(artifact.name());

        final Nbt.Document nbt;
        try {
            nbt = NbtBinary.read(bytes, kind.gzip());
        } catch (IOException e) {
            throw new VfxPortException("Invalid Photon " + kind + " NBT: " + e.getMessage(), e);
        }

        Nbt.CompoundTag fxData;
        int projectVersion;
        if (kind == Photon2NativeKind.FX_PROJECT) {
            Nbt.CompoundTag meta = requireCompound(nbt.root(), "meta", "root.meta");
            projectVersion = meta.intValue("version_num").orElse(-1);
            Nbt.CompoundTag data = requireCompound(nbt.root(), "data", "root.data");
            Nbt.CompoundTag fx = requireCompound(data, "fx", "root.data.fx");
            fxData = requireCompound(fx, "fxData", "root.data.fx.fxData");
        } else {
            projectVersion = nbt.root().intValue("version").orElse(-1);
            fxData = requireCompound(nbt.root(), "fxData", "root.fxData");
        }

        Nbt.ListTag objects = fxData.list("fxObjects")
                .orElseThrow(() -> new VfxPortException("Photon FX is missing fxObjects list"));
        if (objects.elementType() != 10 && !objects.values().isEmpty()) {
            throw new VfxPortException("Photon fxObjects must be a list of compounds");
        }

        ArrayList<Photon2ObjectIndex> index = new ArrayList<>();
        ArrayList<Photon2ParticleSemantic> particles = new ArrayList<>();
        int i = 0;
        for (Nbt.Tag tag : objects.values()) {
            if (!(tag instanceof Nbt.CompoundTag wrapper)) {
                throw new VfxPortException("Photon fxObjects[" + i + "] is not a compound");
            }
            String type = wrapper.string("type").orElse("<unknown>");
            Nbt.CompoundTag objectData = wrapper.compound("data").orElse(null);
            int version = objectData == null ? -1 : objectData.intValue("version").orElse(-1);
            Photon2ObjectIndex objectIndex = new Photon2ObjectIndex(i, type, version, objectData != null);
            index.add(objectIndex);
            if (projectVersion == CURRENT_PROJECT_VERSION
                    && objectIndex.isParticleEmitter()
                    && version == CURRENT_PARTICLE_OBJECT_VERSION
                    && objectData != null) {
                particles.add(extractParticle(i, objectData));
            }
            i++;
        }

        return new Photon2Document(kind, projectVersion, fxData.compound("timeline").isPresent(), index, particles, bytes);
    }

    public void validateNativeEnvelope(String name, byte[] bytes) throws VfxPortException {
        nativeKind(name);
        if (bytes.length == 0) throw new VfxPortException("Photon artifact is empty");
        if (bytes.length > MAX_NATIVE_BYTES) {
            throw new VfxPortException("Photon artifact exceeds native byte limit " + MAX_NATIVE_BYTES);
        }
    }

    public Photon2NativeKind nativeKind(String name) throws VfxPortException {
        try {
            return Photon2NativeKind.fromName(name);
        } catch (IllegalArgumentException e) {
            throw new VfxPortException(e.getMessage(), e);
        }
    }

    private static Photon2ParticleSemantic extractParticle(int index, Nbt.CompoundTag objectData) {
        Nbt.CompoundTag config = objectData.compound("config").orElse(new Nbt.CompoundTag(java.util.Map.of()));
        return new Photon2ParticleSemantic(
                index,
                objectData.string("name").orElse("particle_emitter"),
                objectData.intValue("version").orElse(-1),
                config.intValue("duration").orElse(null),
                config.booleanValue("looping").orElse(null),
                config.intValue("prewarm").orElse(null),
                config.intValue("maxParticles").orElse(null),
                config.booleanValue("parallelUpdate").orElse(null),
                constantNumber(config, "startDelay"),
                constantNumber(config, "startLifetime"),
                constantNumber(config, "startSpeed")
        );
    }

    private static Double constantNumber(Nbt.CompoundTag config, String key) {
        Nbt.CompoundTag wrapper = config.compound(key).orElse(null);
        if (wrapper == null || !"constant".equals(wrapper.string("type").orElse(null))) return null;
        Nbt.CompoundTag data = wrapper.compound("data").orElse(null);
        return data == null ? null : data.numberValue("number").orElse(null);
    }

    private static Nbt.CompoundTag requireCompound(Nbt.CompoundTag parent, String key, String path)
            throws VfxPortException {
        return parent.compound(key).orElseThrow(() -> new VfxPortException("Photon NBT is missing compound " + path));
    }
}