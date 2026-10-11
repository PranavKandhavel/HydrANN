package com.hydrann.ingestion;

import com.hydrann.embedding.MiniLMEmbedder;

import java.util.List;

public class SimilarityAnalyzerTest {

    public static void main(String[] args) throws Exception {

        List<String> paragraphs = List.of(

                // Machine Learning
                """
                Machine learning is a field of computer science that allows computers
                to learn patterns from data without being explicitly programmed.
                """,

                """
                Supervised learning trains a model using labeled examples where the
                correct output is already known.
                """,

                """
                Classification is a supervised learning task where the goal is to
                predict a discrete category or class.
                """,

                """
                Regression is another supervised learning task where the model predicts
                a continuous numerical value.
                """,

                // Neural Networks
                """
                Neural networks are computational models inspired by the structure of
                biological brains and are composed of interconnected layers of neurons.
                """,

                """
                An activation function determines whether a neuron should be activated
                and introduces non-linearity into a neural network.
                """,

                """
                Convolutional neural networks are specialized neural networks commonly
                used for processing images and spatial data.
                """,

                // Biology
                """
                The human heart is a muscular organ responsible for pumping blood
                throughout the body.
                """,

                """
                The heart contains four chambers: two atria and two ventricles, which
                work together to circulate blood through the body.
                """,

                """
                Blood is a connective tissue that transports oxygen, nutrients,
                hormones, and waste products throughout the body.
                """,

                // Physics
                """
                Earth is the third planet from the Sun and is the only known planet
                to support life.
                """,

                """
                Gravity is the force that attracts objects with mass toward one another.
                On Earth, gravity gives objects their weight.
                """,

                // Networking
                """
                Transmission Control Protocol, or TCP, is a transport-layer protocol
                that provides reliable and ordered delivery of data.
                """,

                """
                TCP uses mechanisms such as sequence numbers, acknowledgements, and
                retransmissions to ensure reliable communication between devices.
                """,

                """
                TCP congestion control regulates the amount of data sent into a network
                to prevent congestion and packet loss.
                """,

                // Databases
                """
                Database indexes improve query performance by providing a data structure
                that allows records to be located more efficiently.
                """,

                """
                A B-tree is a balanced tree data structure commonly used to implement
                database indexes because it supports efficient search and updates.
                """
        );

        try (MiniLMEmbedder embedder = new MiniLMEmbedder()) {

            SimilarityAnalyzer analyzer =
                    new SimilarityAnalyzer(embedder);

            List<Double> similarities =
                    analyzer.analyze(paragraphs);

            analyzer.printStatistics(similarities);
        }
    }
}