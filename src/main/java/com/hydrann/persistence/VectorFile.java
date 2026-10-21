package com.hydrann.persistence;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class VectorFile implements AutoCloseable {

    private final FileChannel channel;
    private final int dimension;
    private long count;

    public VectorFile(Path path, int expectedDimension)
            throws IOException {

        Files.createDirectories(path.toAbsolutePath().getParent());

        boolean exists = Files.exists(path);

        channel = FileChannel.open(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE
        );

        try {
            if (!exists || channel.size() == 0) {
                dimension = expectedDimension;
                count = 0;
                writeHeader();
            } else {
                if (channel.size() < VectorFileHeader.HEADER_SIZE) {
                    throw new IOException("Truncated vectors.bin header");
                }

                ByteBuffer header = ByteBuffer
                        .allocate(VectorFileHeader.HEADER_SIZE)
                        .order(ByteOrder.BIG_ENDIAN);

                readFully(header, 0);
                header.flip();

                VectorFileHeader.HeaderData data =
                        VectorFileHeader.read(header);

                dimension = data.dimension();
                count = data.count();

                if (dimension != expectedDimension) {
                    throw new IOException(
                            "Vector dimension mismatch: stored=" +
                            dimension + ", expected=" + expectedDimension
                    );
                }

                long expectedSize = VectorFileHeader.HEADER_SIZE
                        + count * dimension * Float.BYTES;

                if (channel.size() < expectedSize) {
                    throw new IOException(
                            "vectors.bin is truncated: expected at least " +
                            expectedSize + " bytes, found " + channel.size()
                    );
                }
            }
        } catch (IOException | RuntimeException e) {
            channel.close();
            throw e;
        }
    }

    public synchronized long append(float[] vector)
            throws IOException {

        validateVector(vector);

        long vectorId = count;
        long offset = vectorOffset(vectorId);

        ByteBuffer buffer = ByteBuffer
                .allocate(Math.multiplyExact(dimension, Float.BYTES))
                .order(ByteOrder.BIG_ENDIAN);

        for (float value : vector) {
            buffer.putFloat(value);
        }

        buffer.flip();
        writeFully(buffer, offset);

        // Update the header only after the vector bytes are written.
        count++;
        writeHeader();

        return vectorId;
    }

    public synchronized float[] read(long vectorId)
            throws IOException {

        if (vectorId < 0 || vectorId >= count) {
            throw new IndexOutOfBoundsException(
                    "Invalid vector ID: " + vectorId
            );
        }

        ByteBuffer buffer = ByteBuffer
                .allocate(Math.multiplyExact(dimension, Float.BYTES))
                .order(ByteOrder.BIG_ENDIAN);

        readFully(buffer, vectorOffset(vectorId));
        buffer.flip();

        float[] vector = new float[dimension];

        for (int i = 0; i < dimension; i++) {
            vector[i] = buffer.getFloat();
        }

        return vector;
    }

    public int getDimension() {
        return dimension;
    }

    public synchronized long getCount() {
        return count;
    }

    private long vectorOffset(long vectorId) {
        return VectorFileHeader.HEADER_SIZE
                + vectorId * dimension * Float.BYTES;
    }

    private void validateVector(float[] vector) {
        if (vector == null || vector.length != dimension) {
            throw new IllegalArgumentException(
                    "Expected a vector of dimension " + dimension
            );
        }

        for (float value : vector) {
            if (!Float.isFinite(value)) {
                throw new IllegalArgumentException(
                        "Vector contains NaN or Infinity"
                );
            }
        }
    }

    private void writeHeader() throws IOException {
        ByteBuffer header =
                VectorFileHeader.create(dimension, count);

        writeFully(header, 0);
    }

    private void readFully(ByteBuffer buffer, long position)
            throws IOException {

        while (buffer.hasRemaining()) {
            int read = channel.read(buffer, position);

            if (read < 0) {
                throw new IOException("Unexpected end of vector file");
            }

            if (read == 0) {
                continue;
            }

            position += read;
        }
    }

    private void writeFully(ByteBuffer buffer, long position)
            throws IOException {

        while (buffer.hasRemaining()) {
            int written = channel.write(buffer, position);

            if (written <= 0) {
                throw new IOException("Could not write vector data");
            }

            position += written;
        }
    }

    @Override
    public void close() throws IOException {
        channel.close();
    }
}