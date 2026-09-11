package io.github.genkimorimori252525.skillstage.photon2.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public final class NbtBinary {
    public static final long MAX_DECOMPRESSED_BYTES = 64L * 1024L * 1024L;
    public static final int MAX_DEPTH = 128;
    public static final int MAX_COLLECTION_LENGTH = 1_000_000;

    private NbtBinary() {}

    public static Nbt.Document read(byte[] bytes, boolean gzip) throws IOException {
        InputStream source = new ByteArrayInputStream(bytes);
        if (gzip) source = new GZIPInputStream(source);
        try (DataInputStream input = new DataInputStream(new LimitedInputStream(source, MAX_DECOMPRESSED_BYTES))) {
            int type = input.readUnsignedByte();
            if (type != 10) throw new IOException("NBT root must be TAG_Compound, got " + type);
            String rootName = input.readUTF();
            Nbt.Tag root = readPayload(input, type, 0);
            if (!(root instanceof Nbt.CompoundTag compound)) throw new IOException("NBT root payload is not compound");
            return new Nbt.Document(rootName, compound);
        } catch (EOFException e) {
            throw new IOException("Truncated NBT document", e);
        }
    }

    public static byte[] write(Nbt.Document document, boolean gzip) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        if (gzip) {
            try (GZIPOutputStream gz = new GZIPOutputStream(bytes);
                 DataOutputStream output = new DataOutputStream(gz)) {
                writeDocument(output, document);
            }
        } else {
            try (DataOutputStream output = new DataOutputStream(bytes)) {
                writeDocument(output, document);
            }
        }
        return bytes.toByteArray();
    }

    private static void writeDocument(DataOutputStream output, Nbt.Document document) throws IOException {
        output.writeByte(10);
        output.writeUTF(document.rootName());
        writePayload(output, document.root());
    }

    private static Nbt.Tag readPayload(DataInputStream input, int type, int depth) throws IOException {
        if (depth > MAX_DEPTH) throw new IOException("NBT depth exceeds " + MAX_DEPTH);
        return switch (type) {
            case 1 -> new Nbt.ByteTag(input.readByte());
            case 2 -> new Nbt.ShortTag(input.readShort());
            case 3 -> new Nbt.IntTag(input.readInt());
            case 4 -> new Nbt.LongTag(input.readLong());
            case 5 -> new Nbt.FloatTag(input.readFloat());
            case 6 -> new Nbt.DoubleTag(input.readDouble());
            case 7 -> {
                int length = checkedLength(input.readInt());
                byte[] value = new byte[length];
                input.readFully(value);
                yield new Nbt.ByteArrayTag(value);
            }
            case 8 -> new Nbt.StringTag(input.readUTF());
            case 9 -> {
                int elementType = input.readUnsignedByte();
                int length = checkedLength(input.readInt());
                if (length > 0 && elementType == 0) throw new IOException("Non-empty TAG_List has TAG_End element type");
                if (elementType > 12) throw new IOException("Invalid TAG_List element type " + elementType);
                ArrayList<Nbt.Tag> list = new ArrayList<>(Math.min(length, 4096));
                for (int i = 0; i < length; i++) list.add(readPayload(input, elementType, depth + 1));
                yield new Nbt.ListTag(elementType, list);
            }
            case 10 -> {
                LinkedHashMap<String, Nbt.Tag> map = new LinkedHashMap<>();
                int count = 0;
                while (true) {
                    int childType = input.readUnsignedByte();
                    if (childType == 0) break;
                    if (childType > 12) throw new IOException("Invalid NBT tag type " + childType);
                    if (++count > MAX_COLLECTION_LENGTH) throw new IOException("NBT compound has too many entries");
                    String name = input.readUTF();
                    map.put(name, readPayload(input, childType, depth + 1));
                }
                yield new Nbt.CompoundTag(map);
            }
            case 11 -> {
                int length = checkedLength(input.readInt());
                int[] value = new int[length];
                for (int i = 0; i < length; i++) value[i] = input.readInt();
                yield new Nbt.IntArrayTag(value);
            }
            case 12 -> {
                int length = checkedLength(input.readInt());
                long[] value = new long[length];
                for (int i = 0; i < length; i++) value[i] = input.readLong();
                yield new Nbt.LongArrayTag(value);
            }
            default -> throw new IOException("Unsupported NBT tag type " + type);
        };
    }

    private static int checkedLength(int length) throws IOException {
        if (length < 0 || length > MAX_COLLECTION_LENGTH) {
            throw new IOException("Invalid NBT collection length " + length);
        }
        return length;
    }

    private static void writePayload(DataOutputStream output, Nbt.Tag tag) throws IOException {
        if (tag instanceof Nbt.ByteTag value) output.writeByte(value.value());
        else if (tag instanceof Nbt.ShortTag value) output.writeShort(value.value());
        else if (tag instanceof Nbt.IntTag value) output.writeInt(value.value());
        else if (tag instanceof Nbt.LongTag value) output.writeLong(value.value());
        else if (tag instanceof Nbt.FloatTag value) output.writeFloat(value.value());
        else if (tag instanceof Nbt.DoubleTag value) output.writeDouble(value.value());
        else if (tag instanceof Nbt.ByteArrayTag value) {
            byte[] array = value.value();
            output.writeInt(array.length);
            output.write(array);
        } else if (tag instanceof Nbt.StringTag value) output.writeUTF(value.value());
        else if (tag instanceof Nbt.ListTag value) {
            output.writeByte(value.elementType());
            output.writeInt(value.values().size());
            for (Nbt.Tag child : value.values()) writePayload(output, child);
        } else if (tag instanceof Nbt.CompoundTag value) {
            for (var entry : value.values().entrySet()) {
                output.writeByte(entry.getValue().typeId());
                output.writeUTF(entry.getKey());
                writePayload(output, entry.getValue());
            }
            output.writeByte(0);
        } else if (tag instanceof Nbt.IntArrayTag value) {
            int[] array = value.value();
            output.writeInt(array.length);
            for (int element : array) output.writeInt(element);
        } else if (tag instanceof Nbt.LongArrayTag value) {
            long[] array = value.value();
            output.writeInt(array.length);
            for (long element : array) output.writeLong(element);
        } else {
            throw new IOException("Unsupported NBT tag implementation " + tag.getClass().getName());
        }
    }

    private static final class LimitedInputStream extends FilterInputStream {
        private final long limit;
        private long count;

        private LimitedInputStream(InputStream in, long limit) {
            super(in);
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            int value = super.read();
            if (value >= 0) add(1);
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = super.read(b, off, len);
            if (read > 0) add(read);
            return read;
        }

        private void add(long amount) throws IOException {
            count += amount;
            if (count > limit) throw new IOException("NBT exceeds decompressed byte limit " + limit);
        }
    }
}