package io.github.genkimorimori252525.skillstage.photon2.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Nbt {
    private Nbt() {}

    public interface Tag {
        int typeId();
    }

    public record ByteTag(byte value) implements Tag { public int typeId() { return 1; } }
    public record ShortTag(short value) implements Tag { public int typeId() { return 2; } }
    public record IntTag(int value) implements Tag { public int typeId() { return 3; } }
    public record LongTag(long value) implements Tag { public int typeId() { return 4; } }
    public record FloatTag(float value) implements Tag { public int typeId() { return 5; } }
    public record DoubleTag(double value) implements Tag { public int typeId() { return 6; } }
    public record StringTag(String value) implements Tag {
        public StringTag {
            if (value == null) throw new IllegalArgumentException("value must not be null");
        }
        public int typeId() { return 8; }
    }

    public static final class ByteArrayTag implements Tag {
        private final byte[] value;
        public ByteArrayTag(byte[] value) { this.value = value.clone(); }
        public byte[] value() { return value.clone(); }
        public int typeId() { return 7; }
    }

    public static final class IntArrayTag implements Tag {
        private final int[] value;
        public IntArrayTag(int[] value) { this.value = value.clone(); }
        public int[] value() { return value.clone(); }
        public int typeId() { return 11; }
    }

    public static final class LongArrayTag implements Tag {
        private final long[] value;
        public LongArrayTag(long[] value) { this.value = value.clone(); }
        public long[] value() { return value.clone(); }
        public int typeId() { return 12; }
    }

    public static final class ListTag implements Tag {
        private final int elementType;
        private final List<Tag> values;

        public ListTag(int elementType, List<Tag> values) {
            if (elementType < 0 || elementType > 12) throw new IllegalArgumentException("invalid elementType");
            List<Tag> copy = new ArrayList<>(values);
            for (Tag value : copy) {
                if (value == null) throw new IllegalArgumentException("list tag must not contain null");
                if (value.typeId() != elementType) throw new IllegalArgumentException("mixed NBT list types");
            }
            if (!copy.isEmpty() && elementType == 0) throw new IllegalArgumentException("non-empty TAG_End list");
            this.elementType = elementType;
            this.values = Collections.unmodifiableList(copy);
        }

        public int elementType() { return elementType; }
        public List<Tag> values() { return values; }
        public int typeId() { return 9; }
    }

    public static final class CompoundTag implements Tag {
        private final Map<String, Tag> values;

        public CompoundTag(Map<String, Tag> values) {
            LinkedHashMap<String, Tag> copy = new LinkedHashMap<>();
            values.forEach((key, value) -> {
                if (key == null || value == null) throw new IllegalArgumentException("compound entry must not be null");
                copy.put(key, value);
            });
            this.values = Collections.unmodifiableMap(copy);
        }

        public Map<String, Tag> values() { return values; }
        public Optional<Tag> get(String key) { return Optional.ofNullable(values.get(key)); }
        public Optional<CompoundTag> compound(String key) {
            Tag value = values.get(key);
            return value instanceof CompoundTag c ? Optional.of(c) : Optional.empty();
        }
        public Optional<ListTag> list(String key) {
            Tag value = values.get(key);
            return value instanceof ListTag l ? Optional.of(l) : Optional.empty();
        }
        public Optional<String> string(String key) {
            Tag value = values.get(key);
            return value instanceof StringTag s ? Optional.of(s.value()) : Optional.empty();
        }
        public Optional<Integer> intValue(String key) {
            Tag value = values.get(key);
            return value instanceof IntTag i ? Optional.of(i.value()) : Optional.empty();
        }
        public int typeId() { return 10; }
    }

    public record Document(String rootName, CompoundTag root) {
        public Document {
            if (rootName == null) throw new IllegalArgumentException("rootName must not be null");
            if (root == null) throw new IllegalArgumentException("root must not be null");
        }
    }
}