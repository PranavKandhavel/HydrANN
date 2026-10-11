package com.hydrann.ingestion;

import com.hydrann.embedding.MiniLMEmbedder;

import java.util.List;

public class SemanticChunkerTest {

    public static void main(String[] args) throws Exception {

        try (MiniLMEmbedder embedder =
                     new MiniLMEmbedder()) {

            SemanticChunker chunker =
                    new SemanticChunker(
                            embedder,
                            0.45
                    );

            String text = """
            Machine learning is a branch of artificial intelligence
            that allows computers to learn patterns from data without
            being explicitly programmed for every task.

            Supervised learning algorithms learn from labeled examples,
            where each training example contains both an input and its
            corresponding expected output.

            Classification is a supervised learning problem in which
            the goal is to assign an input to one of several discrete
            categories.

            Regression is another supervised learning problem, but
            instead of predicting a category, the model predicts a
            continuous numerical value.

            Neural networks are composed of interconnected layers of
            artificial neurons that transform input data into useful
            representations.

            The activation function determines how strongly a neuron
            responds to its input and introduces non-linearity into the
            network.

            Convolutional neural networks are particularly effective
            for processing images because convolutional layers can
            detect local patterns such as edges and textures.

            The human heart is a muscular organ responsible for pumping
            blood throughout the body.

            The heart contains four chambers, including two atria and
            two ventricles, which work together to circulate blood.

            Blood carries oxygen and nutrients to tissues while also
            transporting carbon dioxide and other waste products away
            from cells.

            The Earth revolves around the Sun and completes one orbit
            approximately every 365 days.

            Gravity is the force that keeps planets and other celestial
            objects in their orbits around larger bodies.

            In computer networks, the Transmission Control Protocol
            provides reliable and ordered delivery of data between
            applications.

            TCP uses acknowledgements, sequence numbers, and
            retransmission mechanisms to recover from lost or corrupted
            packets.

            Congestion control allows TCP to adjust its transmission
            rate when the network becomes overloaded.

            Database indexes improve query performance by allowing the
            database engine to locate records without scanning every
            row in a table.

            A B-tree is a balanced tree structure commonly used for
            database indexes because it keeps keys ordered and supports
            efficient searches.
            """;

            List<TextChunk> chunks =
                    chunker.chunk(
                            "test-document",
                            1,
                            text
                    );

            System.out.println();
            System.out.println("==============================");
            System.out.println("GENERATED CHUNKS");
            System.out.println("==============================");

            for (TextChunk chunk : chunks) {

                System.out.println();
                System.out.println(
                        "Chunk " + chunk.getChunkIndex()
                );

                System.out.println(
                        "Page: " + chunk.getPageNumber()
                );

                System.out.println(
                        chunk.getText()
                );
            }
        }
    }
}