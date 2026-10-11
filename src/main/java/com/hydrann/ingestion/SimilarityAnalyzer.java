package com.hydrann.ingestion;

import com.hydrann.embedding.MiniLMEmbedder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimilarityAnalyzer {

    private final MiniLMEmbedder embedder;

    public SimilarityAnalyzer(MiniLMEmbedder embedder) {
        this.embedder = embedder;
    }

    public List<Double> analyze(
            List<String> paragraphs) throws Exception {

        List<Double> similarities = new ArrayList<>();

        if (paragraphs.size() < 2) {
            return similarities;
        }

        float[] previousEmbedding =
                embedder.embed(paragraphs.get(0));

        for (int i = 1; i < paragraphs.size(); i++) {

            float[] currentEmbedding =
                    embedder.embed(paragraphs.get(i));

            double similarity =
                    cosineSimilarity(
                            previousEmbedding,
                            currentEmbedding
                    );

            similarities.add(similarity);

            System.out.printf(
                    "P%d ↔ P%d : %.4f%n",
                    i,
                    i + 1,
                    similarity
            );

            previousEmbedding = currentEmbedding;
        }

        return similarities;
    }

    public void printStatistics(
            List<Double> similarities) {

        if (similarities.isEmpty()) {
            System.out.println(
                    "Not enough paragraphs to analyze."
            );
            return;
        }

        List<Double> sorted =
                new ArrayList<>(similarities);

        Collections.sort(sorted);

        double min = sorted.get(0);

        double max =
                sorted.get(sorted.size() - 1);

        double mean =
                sorted.stream()
                        .mapToDouble(Double::doubleValue)
                        .average()
                        .orElse(0.0);

        double median =
                calculateMedian(sorted);

        double standardDeviation =
                calculateStandardDeviation(
                        similarities,
                        mean
                );

        System.out.println();
        System.out.println("==============================");
        System.out.println("SIMILARITY STATISTICS");
        System.out.println("==============================");

        System.out.printf(
                "Count              : %d%n",
                similarities.size()
        );

        System.out.printf(
                "Minimum            : %.4f%n",
                min
        );

        System.out.printf(
                "Maximum            : %.4f%n",
                max
        );

        System.out.printf(
                "Mean               : %.4f%n",
                mean
        );

        System.out.printf(
                "Median             : %.4f%n",
                median
        );

        System.out.printf(
                "Standard deviation : %.4f%n",
                standardDeviation
        );

        System.out.println();

        printPercentiles(sorted);
    }

    private double calculateMedian(
            List<Double> sorted) {

        int size = sorted.size();

        if (size % 2 == 1) {

            return sorted.get(size / 2);

        } else {

            return (
                    sorted.get(size / 2 - 1)
                            +
                    sorted.get(size / 2)
            ) / 2.0;
        }
    }

    private double calculateStandardDeviation(
            List<Double> values,
            double mean) {

        double sum = 0.0;

        for (double value : values) {

            double difference =
                    value - mean;

            sum += difference * difference;
        }

        double variance =
                sum / values.size();

        return Math.sqrt(variance);
    }

    private void printPercentiles(
            List<Double> sorted) {

        System.out.printf(
                "25th percentile    : %.4f%n",
                percentile(sorted, 0.25)
        );

        System.out.printf(
                "50th percentile    : %.4f%n",
                percentile(sorted, 0.50)
        );

        System.out.printf(
                "75th percentile    : %.4f%n",
                percentile(sorted, 0.75)
        );

        System.out.printf(
                "90th percentile    : %.4f%n",
                percentile(sorted, 0.90)
        );
    }

    private double percentile(
            List<Double> sorted,
            double percentile) {

        if (sorted.size() == 1) {
            return sorted.get(0);
        }

        double index =
                percentile * (sorted.size() - 1);

        int lower =
                (int) Math.floor(index);

        int upper =
                (int) Math.ceil(index);

        if (lower == upper) {
            return sorted.get(lower);
        }

        double weight =
                index - lower;

        return sorted.get(lower)
                * (1.0 - weight)
                +
                sorted.get(upper)
                * weight;
    }

    private double cosineSimilarity(
            float[] a,
            float[] b) {

        double dotProduct = 0.0;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
        }

        return dotProduct;
    }
}