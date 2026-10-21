package com.hydrann;

import com.hydrann.annheads.ANNHead;
import com.hydrann.annheads.BruteForceHead;
import com.hydrann.annheads.LshHead;
import com.hydrann.annheads.Neighbor;
import com.hydrann.controller.IndexManager;
import com.hydrann.embedding.ChunkEmbedder;
import com.hydrann.embedding.MiniLMEmbedder;
import com.hydrann.ingestion.PdfDirectoryLoader;
import com.hydrann.ingestion.PdfExtractor;
import com.hydrann.ingestion.SemanticChunker;
import com.hydrann.ingestion.TextChunk;
import com.hydrann.persistence.DocumentManifest;
import com.hydrann.persistence.IncrementalIngestionService;
import com.hydrann.persistence.MetadataFile;
import com.hydrann.persistence.PersistentVectorStore;
import com.hydrann.vectorstore.SearchResult;

import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;

public class SearchTest {

    private static final int DIM = 384;
    private static final int K = 5;

    /**
     * Usage:
     *   (no args)  -> serve queries from the LSH head, print recall vs brute force
     *   brute      -> serve queries from the brute-force head (refactor check)
     */
    public static void main(String[] args) throws Exception {

        boolean useLsh =
                !(args.length > 0 && args[0].equalsIgnoreCase("brute"));

        Path rawDocs = Path.of("data/raw-docs");
        Path persistenceDir = Path.of("data/persisted-index");

        try (
                MiniLMEmbedder embedder = new MiniLMEmbedder();
                PersistentVectorStore vectorStore =
                        new PersistentVectorStore(
                                persistenceDir,
                                DIM,
                                new MetadataFile()
                        );
                Scanner scanner = new Scanner(System.in)
        ) {

            // -----------------------------------------
            // 1. Initialize ingestion components
            // -----------------------------------------

            SemanticChunker chunker =
                    new SemanticChunker(embedder, 0.45);

            PdfDirectoryLoader loader =
                    new PdfDirectoryLoader();

            PdfExtractor extractor =
                    new PdfExtractor();

            ChunkEmbedder chunkEmbedder =
                    new ChunkEmbedder(embedder);

            // -----------------------------------------
            // 2. Load document manifest
            // -----------------------------------------

            DocumentManifest manifest =
                    new DocumentManifest(
                            persistenceDir.resolve("documents.json")
                    );

            // -----------------------------------------
            // 3. Synchronize PDFs with persistent index
            // -----------------------------------------

            IncrementalIngestionService ingestionService =
                    new IncrementalIngestionService(
                            rawDocs,
                            loader,
                            extractor,
                            chunker,
                            chunkEmbedder,
                            vectorStore,
                            manifest
                    );

            ingestionService.synchronize();

            // -----------------------------------------
            // 4. Build the ANN head(s)
            //
            // BruteForceHead is always the ground truth.
            // The active head serves the queries.
            // -----------------------------------------

            BruteForceHead truthHead = new BruteForceHead(vectorStore);

            LshHead lshHead = null;
            ANNHead activeHead = truthHead;

            if (useLsh) {
                // 8 tables, 10 bits per table, multi-probe on, fixed seed
                lshHead = new LshHead(vectorStore, DIM, 8, 10, true, 42L);
                activeHead = lshHead;
            }

            long buildStart = System.nanoTime();
            IndexManager index = new IndexManager(vectorStore, activeHead);
            long buildMs = (System.nanoTime() - buildStart) / 1_000_000;

            System.out.printf(
                    "%nActive head: %s | indexed in %d ms | est. memory: %d KB%n",
                    activeHead.name(),
                    buildMs,
                    activeHead.memoryBytes() / 1024
            );

            // -----------------------------------------
            // 5. Interactive semantic search
            // -----------------------------------------

            while (true) {

                System.out.println();
                System.out.print("Search (type 'exit' to quit): ");

                if (!scanner.hasNextLine()) {
                    break;
                }

                String query = scanner.nextLine().trim();

                if (query.equalsIgnoreCase("exit")) {
                    break;
                }

                if (query.isBlank()) {
                    continue;
                }

                // Embed the query in the same 384D vector space.
                float[] queryVector = embedder.embed(query);

                // Retrieve the top-K active chunks via the active head.
                List<SearchResult> results =
                        index.search(queryVector, K);

                System.out.println();
                System.out.println("========== RESULTS ==========");

                if (results.isEmpty()) {
                    System.out.println("No matching chunks found.");
                    continue;
                }

                for (int i = 0; i < results.size(); i++) {

                    SearchResult result = results.get(i);

                    TextChunk chunk =
                            result.getChunkEmbedding().getChunk();

                    System.out.println();
                    System.out.println("#" + (i + 1));

                    System.out.printf(
                            "Score: %.4f%n",
                            result.getScore()
                    );

                    System.out.println(
                            "Document: " + chunk.getDocumentId()
                    );

                    System.out.println(
                            "Page: " + chunk.getPageNumber()
                    );

                    System.out.println(
                            "Chunk: " + chunk.getChunkIndex()
                    );

                    System.out.println("Text:");
                    System.out.println(chunk.getText());
                }

                // -----------------------------------------
                // 6. Recall / latency check (LSH mode only)
                // -----------------------------------------

                if (lshHead != null) {

                    long t0 = System.nanoTime();
                    List<Neighbor> truth = truthHead.search(queryVector, K);
                    long bruteMicros = (System.nanoTime() - t0) / 1_000;

                    long t1 = System.nanoTime();
                    List<Neighbor> approx = lshHead.search(queryVector, K);
                    long lshMicros = (System.nanoTime() - t1) / 1_000;

                    Set<Integer> truthIds = truth.stream()
                            .map(Neighbor::id)
                            .collect(Collectors.toSet());

                    long hits = approx.stream()
                            .filter(n -> truthIds.contains(n.id()))
                            .count();

                    System.out.println();
                    System.out.printf(
                            "[LSH] recall@%d = %d/%d | LSH %d us | brute force %d us%n",
                            K, hits, truthIds.size(), lshMicros, bruteMicros
                    );
                }
            }
        }
    }
}