package com.hydrann.ingestion;

import com.hydrann.embedding.MiniLMEmbedder;

import java.util.ArrayList;
import java.util.List;

public class SemanticChunker {

    private final MiniLMEmbedder embedder;

    /*
     * If similarity between two adjacent paragraphs is
     * greater than or equal to this value, they belong
     * to the same semantic chunk.
     */
    private final double similarityThreshold;

    public SemanticChunker(
            MiniLMEmbedder embedder,
            double similarityThreshold) {

        this.embedder = embedder;
        this.similarityThreshold = similarityThreshold;
    }

    public List<TextChunk> chunk(
            String documentId,
            int pageNumber,
            String pageText) throws Exception {

        List<String> paragraphs = splitIntoParagraphs(pageText);

        List<TextChunk> chunks = new ArrayList<>();

        if (paragraphs.isEmpty()) {
            return chunks;
        }

        /*
         * Start the first chunk with the first paragraph.
         */
        StringBuilder currentChunk =
                new StringBuilder(paragraphs.get(0));

        /*
         * Embed the first paragraph.
         */
        float[] previousEmbedding =
                embedder.embed(paragraphs.get(0));

        int chunkIndex = 0;

        for (int i = 1; i < paragraphs.size(); i++) {

            String currentParagraph = paragraphs.get(i);

            /*
             * Embed current paragraph.
             */
            float[] currentEmbedding =
                    embedder.embed(currentParagraph);

            /*
             * Compare current paragraph with
             * the immediately previous paragraph.
             */
            double similarity =
                    cosineSimilarity(
                            previousEmbedding,
                            currentEmbedding
                    );

            System.out.println();
            System.out.println("--------------------------------");
            System.out.println(
                    "Paragraph " + i + " ↔ Paragraph " + (i + 1)
            );
            System.out.printf(
                    "Similarity: %.4f%n",
                    similarity
            );

            System.out.println("P" + i + ": " + paragraphs.get(i - 1));
            System.out.println("P" + (i + 1) + ": " + currentParagraph);
            if (similarity >= similarityThreshold) {

                /*
                 * Semantically related.
                 * Add paragraph to current chunk.
                 */
                currentChunk
                        .append("\n\n")
                        .append(currentParagraph);

            } else {

                /*
                 * Semantic boundary detected.
                 *
                 * Finish current chunk.
                 */
                chunks.add(
                        new TextChunk(
                                documentId,
                                pageNumber,
                                chunkIndex++,
                                currentChunk.toString()
                        )
                );

                /*
                 * Start a new chunk.
                 */
                currentChunk =
                        new StringBuilder(currentParagraph);
            }

            /*
             * Current paragraph becomes the previous
             * paragraph for the next comparison.
             */
            previousEmbedding = currentEmbedding;
        }

        /*
         * Add the final chunk.
         */
        chunks.add(
                new TextChunk(
                        documentId,
                        pageNumber,
                        chunkIndex,
                        currentChunk.toString()
                )
        );

        return chunks;
    }

    private List<String> splitIntoParagraphs(String text) {

        List<String> paragraphs = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return paragraphs;
        }

        /*
         * A paragraph is separated by one or more
         * blank lines.
         */
        String[] parts =
                text.split("\\r?\\n\\s*\\r?\\n");

        for (String part : parts) {

            String paragraph =
                    part.trim();

            if (!paragraph.isEmpty()) {
                paragraphs.add(paragraph);
            }
        }

        return paragraphs;
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