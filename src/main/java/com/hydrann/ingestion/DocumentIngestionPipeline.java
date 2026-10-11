package com.hydrann.ingestion;

import com.hydrann.embedding.MiniLMEmbedder;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DocumentIngestionPipeline {

    private final PdfDirectoryLoader directoryLoader;
    private final PdfExtractor pdfExtractor;
    private final SemanticChunker semanticChunker;

    public DocumentIngestionPipeline(
            PdfDirectoryLoader directoryLoader,
            PdfExtractor pdfExtractor,
            SemanticChunker semanticChunker) {

        this.directoryLoader = directoryLoader;
        this.pdfExtractor = pdfExtractor;
        this.semanticChunker = semanticChunker;
    }

    /**
     * Processes every PDF inside the given directory.
     *
     * PDF directory
     *      ↓
     * PDF discovery
     *      ↓
     * PDF extraction
     *      ↓
     * Page extraction
     *      ↓
     * Semantic chunking
     *      ↓
     * TextChunk objects
     */
    public List<TextChunk> processDirectory(
            Path rawDocsDirectory) throws Exception {

        List<Path> pdfFiles =
                directoryLoader.load(rawDocsDirectory);

        List<TextChunk> allChunks =
                new ArrayList<>();

        for (Path pdfPath : pdfFiles) {

            System.out.println();
            System.out.println("================================");
            System.out.println(
                    "Processing: " +
                    pdfPath.getFileName()
            );
            System.out.println("================================");

            ExtractedPdf pdf =
                    pdfExtractor.extract(pdfPath);

            System.out.println(
                    "Document ID: " +
                    pdf.getDocumentId()
            );

            System.out.println(
                    "Pages: " +
                    pdf.getPageCount()
            );

            for (ExtractedPage page : pdf.getPages()) {

                String pageText =
                        page.getText();

                if (pageText == null ||
                        pageText.isBlank()) {

                    continue;
                }

                List<TextChunk> pageChunks =
                        semanticChunker.chunk(
                                pdf.getDocumentId(),
                                page.getPdfPageNumber(),
                                pageText
                        );

                allChunks.addAll(pageChunks);

                System.out.println(
                        "Page " +
                        page.getPdfPageNumber() +
                        " > " +
                        pageChunks.size() +
                        " chunks"
                );
            }
        }

        return allChunks;
    }
}