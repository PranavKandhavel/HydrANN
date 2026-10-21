
package com.hydrann.persistence;

import com.hydrann.embedding.MiniLMEmbedder;
import com.hydrann.ingestion.ChunkEmbedding;
import com.hydrann.embedding.ChunkEmbedder;
import com.hydrann.ingestion.ExtractedPdf;
import com.hydrann.ingestion.PdfDirectoryLoader;
import com.hydrann.ingestion.PdfExtractor;
import com.hydrann.ingestion.SemanticChunker;
import com.hydrann.ingestion.TextChunk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class IncrementalIngestionService {

    private final Path rawDocsDirectory;
    private final PdfDirectoryLoader directoryLoader;
    private final PdfExtractor pdfExtractor;
    private final SemanticChunker semanticChunker;
    private final ChunkEmbedder chunkEmbedder;
    private final PersistentVectorStore vectorStore;
    private final DocumentManifest manifest;

    public IncrementalIngestionService(
            Path rawDocsDirectory,
            PdfDirectoryLoader directoryLoader,
            PdfExtractor pdfExtractor,
            SemanticChunker semanticChunker,
            ChunkEmbedder chunkEmbedder,
            PersistentVectorStore vectorStore,
            DocumentManifest manifest) {

        if (rawDocsDirectory == null
                || directoryLoader == null
                || pdfExtractor == null
                || semanticChunker == null
                || chunkEmbedder == null
                || vectorStore == null
                || manifest == null) {
            throw new IllegalArgumentException(
                    "Incremental ingestion dependencies cannot be null."
            );
        }

        this.rawDocsDirectory = rawDocsDirectory
                .toAbsolutePath()
                .normalize();

        this.directoryLoader = directoryLoader;
        this.pdfExtractor = pdfExtractor;
        this.semanticChunker = semanticChunker;
        this.chunkEmbedder = chunkEmbedder;
        this.vectorStore = vectorStore;
        this.manifest = manifest;
    }

    public synchronized void synchronize() throws Exception {

        if (!Files.isDirectory(rawDocsDirectory)) {
            throw new IOException(
                    "Raw documents directory does not exist: "
                            + rawDocsDirectory
            );
        }

        // 1. Discover PDFs.
        List<Path> pdfPaths = directoryLoader.load(rawDocsDirectory);

        Set<String> currentPaths = new HashSet<>();

        int unchanged = 0;
        int processed = 0;

        for (Path pdfPath : pdfPaths) {

            // 2. Normalize the path so the manifest and vector metadata
            //    use exactly the same document identity.
            String documentKey = pdfPath
                    .toAbsolutePath()
                    .normalize()
                    .toString();

            currentPaths.add(documentKey);

            // 3. Calculate the current SHA-256 fingerprint.
            String fingerprint =
                    DocumentFingerprint.calculate(Path.of(documentKey));

            // 4. Skip every expensive stage when the file is unchanged.
            if (manifest.isUnchanged(documentKey, fingerprint)) {
                unchanged++;
                continue;
            }

            System.out.println("Ingesting: " + documentKey);

            // 5. Extract the changed PDF.
            ExtractedPdf extractedPdf =
                    pdfExtractor.extract(Path.of(documentKey));

            List<TextChunk> chunks = new ArrayList<>();

            // 6. Chunk each page independently.
            //    Use the normalized path as the stable document ID,
            //    rather than the extractor's newly generated UUID.
            for (var page : extractedPdf.getPages()) {

                List<TextChunk> pageChunks =
                        semanticChunker.chunk(
                                documentKey,
                                page.getPdfPageNumber(),
                                page.getText()
                        );

                chunks.addAll(pageChunks);
            }

            // 7. Embed only the chunks belonging to this changed PDF.
            List<ChunkEmbedding> embeddings =
                    chunkEmbedder.embedChunks(chunks);

            /*
             * 8. Replace the old document vectors.
             *
             * IMPORTANT:
             * deactivateDocument() deactivates every active vector with
             * this document path. Therefore, deactivate BEFORE appending
             * the replacement batch; doing it afterward would deactivate
             * the newly inserted vectors too.
             */
            vectorStore.deactivateDocument(documentKey);

            vectorStore.addDocumentBatch(
                    documentKey,
                    documentKey,
                    fingerprint,
                    embeddings
            );

            // 9. Update the manifest only after the vector-store calls
            //    complete successfully.
            manifest.markProcessed(documentKey, fingerprint);

            processed++;

            System.out.printf(
                    "Processed %s: %d chunks%n",
                    Path.of(documentKey).getFileName(),
                    embeddings.size()
            );
        }

        // 10. Find documents previously tracked but now absent.
        Set<String> deletedPaths =
                new HashSet<>(manifest.getKnownPaths());

        deletedPaths.removeAll(currentPaths);

        // 11. Deactivate deleted documents before removing them
        //     from the manifest.
        for (String deletedPath : deletedPaths) {
            vectorStore.deactivateDocument(deletedPath);

            System.out.println(
                    "Deactivated deleted document: " + deletedPath
            );
        }

        // 12. Persist the updated manifest after all deletions have
        //     been successfully deactivated.
        if (!deletedPaths.isEmpty()) {
            manifest.removeMissingDocuments(currentPaths);
        }

        System.out.println("\nIncremental ingestion complete.");
        System.out.println("Unchanged PDFs skipped: " + unchanged);
        System.out.println("New/changed PDFs processed: " + processed);
        System.out.println("Deleted PDFs deactivated: " + deletedPaths.size());
        System.out.println("Active vectors: " + vectorStore.size());
        System.out.println("Total stored vectors: "
                + vectorStore.totalStoredVectors());
    }
}
