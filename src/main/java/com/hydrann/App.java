package com.hydrann;

import com.hydrann.embedding.MiniLMEmbedder;
import com.hydrann.ingestion.DocumentIngestionPipeline;
import com.hydrann.ingestion.ExtractedPage;
import com.hydrann.ingestion.PdfDirectoryLoader;
import com.hydrann.ingestion.PdfExtractor;
import com.hydrann.ingestion.SemanticChunker;
import com.hydrann.ingestion.TextChunk;

import java.nio.file.Path;
import java.util.List;

public class App {

    public static void main(String[] args) throws Exception {

        Path rawDocs =
                Path.of("data/raw-docs");

        PdfDirectoryLoader directoryLoader =
                new PdfDirectoryLoader();

        PdfExtractor pdfExtractor =
                new PdfExtractor();

        try (MiniLMEmbedder embedder =
                     new MiniLMEmbedder()) {

            SemanticChunker semanticChunker =
                    new SemanticChunker(
                            embedder,
                            0.45
                    );

            DocumentIngestionPipeline pipeline =
                    new DocumentIngestionPipeline(
                            directoryLoader,
                            pdfExtractor,
                            semanticChunker
                    );

            List<TextChunk> chunks =
                    pipeline.processDirectory(rawDocs);

            System.out.println();
            System.out.println("================================");
            System.out.println(
                    "TOTAL CHUNKS: " + chunks.size()
            );
            System.out.println("================================");

            for (TextChunk chunk : chunks) {

                System.out.println();
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
                        chunk.getText()
                );
            }
        }
    }
}