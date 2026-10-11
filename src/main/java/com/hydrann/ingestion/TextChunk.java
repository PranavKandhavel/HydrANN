package com.hydrann.ingestion;

public class TextChunk {

    private final String documentId;
    private final int pageNumber;
    private final int chunkIndex;
    private final String text;

    public TextChunk(
            String documentId,
            int pageNumber,
            int chunkIndex,
            String text) {

        this.documentId = documentId;
        this.pageNumber = pageNumber;
        this.chunkIndex = chunkIndex;
        this.text = text;
    }

    public String getDocumentId() {
        return documentId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getText() {
        return text;
    }

    @Override
    public String toString() {
        return "TextChunk{" +
                "documentId='" + documentId + '\'' +
                ", pageNumber=" + pageNumber +
                ", chunkIndex=" + chunkIndex +
                ", text='" + text + '\'' +
                '}';
    }
}