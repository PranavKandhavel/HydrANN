package com.hydrann;

import com.hydrann.embedding.ChunkEmbedder;
import com.hydrann.embedding.MiniLMEmbedder;
import com.hydrann.ingestion.ChunkEmbedding;
import com.hydrann.ingestion.DocumentIngestionPipeline;
import com.hydrann.ingestion.PdfDirectoryLoader;
import com.hydrann.ingestion.PdfExtractor;
import com.hydrann.ingestion.SemanticChunker;
import com.hydrann.ingestion.TextChunk;
import com.hydrann.vectorstore.InMemoryVectorStore;
import com.hydrann.vectorstore.SearchResult;

import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class SearchTest {

    public static void main(String[] args) throws Exception {

        Path rawDocs =
                Path.of("data/raw-docs");

        /*
         * -----------------------------------------
         * 1. Create embedding model
         * -----------------------------------------
         */

        try (MiniLMEmbedder embedder =
                     new MiniLMEmbedder()) {

            /*
             * -----------------------------------------
             * 2. Create semantic chunker
             * -----------------------------------------
             */

            SemanticChunker chunker =
                    new SemanticChunker(
                            embedder,
                            0.45
                    );

            /*
             * -----------------------------------------
             * 3. Discover and extract PDFs
             * -----------------------------------------
             */

            PdfDirectoryLoader directoryLoader =
                    new PdfDirectoryLoader();

            PdfExtractor pdfExtractor =
                    new PdfExtractor();

            DocumentIngestionPipeline pipeline =
                    new DocumentIngestionPipeline(
                            directoryLoader,
                            pdfExtractor,
                            chunker
                    );

            /*
             * -----------------------------------------
             * 4. Get all TextChunks
             * -----------------------------------------
             */

            List<TextChunk> chunks =
                    pipeline.processDirectory(rawDocs);

            System.out.println();
            System.out.println(
                    "Total chunks: " + chunks.size()
            );

            /*
             * -----------------------------------------
             * 5. Embed all chunks
             * -----------------------------------------
             */

            ChunkEmbedder chunkEmbedder =
                    new ChunkEmbedder(embedder);

            List<ChunkEmbedding> embeddings =
                    chunkEmbedder.embedChunks(chunks);

            System.out.println(
                    "Total embeddings: " +
                    embeddings.size()
            );

            /*
             * -----------------------------------------
             * 6. Build vector store
             * -----------------------------------------
             */

            InMemoryVectorStore vectorStore =
                    new InMemoryVectorStore();

            vectorStore.addAll(embeddings);

            System.out.println(
                    "Vectors in store: " +
                    vectorStore.size()
            );

            /*
             * -----------------------------------------
             * 7. Interactive search
             * -----------------------------------------
             */

            Scanner scanner =
                    new Scanner(System.in);

            while (true) {

                System.out.println();
                System.out.print(
                        "Search (type 'exit' to quit): "
                );

                String query =
                        scanner.nextLine();

                if (query.equalsIgnoreCase("exit")) {
                    break;
                }

                if (query.isBlank()) {
                    continue;
                }

                /*
                 * Convert query text into the
                 * same 384-dimensional embedding
                 * space as the stored chunks.
                 */
                float[] queryVector =
                        embedder.embed(query);

                /*
                 * Search for top 5 nearest chunks.
                 */
                List<SearchResult> results =
                        vectorStore.search(
                                queryVector,
                                5
                        );

                System.out.println();
                System.out.println(
                        "========== RESULTS =========="
                );

                for (int i = 0;
                     i < results.size();
                     i++) {

                    SearchResult result =
                            results.get(i);

                    TextChunk chunk =
                            result
                                    .getChunkEmbedding()
                                    .getChunk();

                    System.out.println();
                    System.out.println(
                            "#" + (i + 1)
                    );

                    System.out.printf(
                            "Score: %.4f%n",
                            result.getScore()
                    );

                    System.out.println(
                            "Document: " +
                            chunk.getDocumentId()
                    );

                    System.out.println(
                            "Page: " +
                            chunk.getPageNumber()
                    );

                    System.out.println(
                            "Chunk: " +
                            chunk.getChunkIndex()
                    );

                    System.out.println(
                            "Text:"
                    );

                    System.out.println(
                            chunk.getText()
                    );
                }
            }
        }
    }
}