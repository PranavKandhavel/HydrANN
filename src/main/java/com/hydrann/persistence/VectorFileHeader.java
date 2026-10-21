package com.hydrann.persistence;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class VectorFileHeader {

    public static final long MAGIC = 0x48594452414E4E31L; // HYDRANN1
    public static final int VERSION = 1;
    public static final int HEADER_SIZE = 24;
    public static final int DEFAULT_DIMENSION = 384;

    private VectorFileHeader() {}

    public static ByteBuffer create(int dimension, long count) {
        ByteBuffer buffer = ByteBuffer
                .allocate(HEADER_SIZE)
                .order(ByteOrder.BIG_ENDIAN);

        buffer.putLong(MAGIC);
        buffer.putInt(VERSION);
        buffer.putInt(dimension);
        buffer.putLong(count);
        buffer.flip();

        return buffer;
    }

    public static HeaderData read(ByteBuffer buffer) {
        buffer.order(ByteOrder.BIG_ENDIAN);

        long magic = buffer.getLong();
        int version = buffer.getInt();
        int dimension = buffer.getInt();
        long count = buffer.getLong();

        if (magic != MAGIC) {
            throw new IllegalStateException(
                    "Invalid vectors.bin magic number"
            );
        }

        if (version != VERSION) {
            throw new IllegalStateException(
                    "Unsupported vector format version: " + version
            );
        }

        if (dimension <= 0 || count < 0) {
            throw new IllegalStateException(
                    "Invalid vector dimension or count"
            );
        }

        return new HeaderData(dimension, count);
    }

    public record HeaderData(int dimension, long count) {}
}