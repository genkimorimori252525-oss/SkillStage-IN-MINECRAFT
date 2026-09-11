package io.github.genkimorimori252525.skillstage.photon2;

import io.github.genkimorimori252525.skillstage.integration.NativeArtifact;
import io.github.genkimorimori252525.skillstage.integration.VfxPortException;
import io.github.genkimorimori252525.skillstage.photon2.internal.Nbt;
import io.github.genkimorimori252525.skillstage.photon2.internal.NbtBinary;

import java.io.IOException;
import java.util.ArrayList;

public final class Photon2NativeCodec {
    public static final int CURRENT_PROJECT_VERSION = 5;
    public static final int MAX_NATIVE_BYTES = 64 * 1024 * 1024;

    public Photon2Document parse(NativeArtifact artifact) throws VfxPortException {
        byte[] bytes = artifact.primaryBytes();
        if (bytes.length == 0) throw new VfxPortException("Photon artifact is empty");
        if (bytes.length > MAX_NATIVE_BYTES) {
            throw new VfxPortException("Photon artifact exceeds native byte limit " + MAX_NATIVE_BYTES);
        }

        final Photon2NativeKind kind;
        try {
            kind = Photon2NativeKind.fromName(artifact.name());
        } catch (IllegalArgumentException e) {
            throw new VfxPortException(e.getMessage(), e);
        }

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
        int i = 0;
        for (Nbt.Tag tag : objects.values()) {
            if (!(tag instanceof Nbt.CompoundTag wrapper)) {
                throw new VfxPortException("Photon fxObjects[" + i + "] is not a compound");
            }
            String type = wrapper.string("type").orElse("<unknown>");
            Nbt.CompoundTag objectData = wrapper.compound("data").orElse(null);
            int version = objectData == null ? -1 : objectData.intValue("version").orElse(-1);
            index.add(new Photon2ObjectIndex(i, type, version, objectData != null));
            i++;
        }

        return new Photon2Document(kind, projectVersion, fxData.compound("timeline").isPresent(), index, bytes);
    }

    private static Nbt.CompoundTag requireCompound(Nbt.CompoundTag parent, String key, String path)
            throws VfxPortException {
        return parent.compound(key).orElseThrow(() -> new VfxPortException("Photon NBT is missing compound " + path));
    }
}