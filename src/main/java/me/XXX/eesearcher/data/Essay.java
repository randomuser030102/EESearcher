package me.XXX.eesearcher.data;

import org.apache.pdfbox.io.RandomAccessBuffer;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Represents an Essay which is effectively immutable and thread-safe.
 */
public class Essay implements AutoCloseable {

    private IndexData indexData;
    private volatile boolean closed;
    private PDDocument document;

    public Essay(final IndexData indexData, final byte[] rawPDF) {
        this.indexData = indexData;
        try {
            PDFParser parser = new PDFParser(new RandomAccessBuffer(rawPDF));
            parser.parse();
            this.document = parser.getPDDocument();
        } catch (IOException ex) {
            if (this.document != null) {
                try {
                    this.document.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            // Should never happen!
            // FIXME log error
            throw new RuntimeException(ex);
        }
    }

    public Essay(@NotNull final IndexData indexData, @NotNull final PDDocument document) {
        if (document.getDocument().isClosed()) {
            throw new IllegalArgumentException("PDDocument is already closed!");
        }
        this.indexData = indexData;
        this.document = document;
    }

    public @NotNull IndexData getIndexData() {
        return this.indexData;
    }

    public @NotNull PDDocument getDocument() {
        if (this.closed) {
            throw new IllegalStateException("Essay closed!");
        }
        return this.document;
    }

    @Override
    public synchronized void close() throws IOException {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.document.close();
        this.document = null;
    }
}
