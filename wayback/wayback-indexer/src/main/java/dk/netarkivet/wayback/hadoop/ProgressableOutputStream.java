package dk.netarkivet.wayback.hadoop;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.hadoop.util.Progressable;

/**
 * An OutputStream Wrapper that calls Progressable.progress() for each operation
 * This is a hack, in order for us to feed progressable into the BatchJob interface
 * @see DedupIndexer
 * @see dk.netarkivet.wayback.batch.DeduplicationCDXExtractionBatchJob
 *
 */
public class ProgressableOutputStream extends OutputStream {
    private final OutputStream os;
    private final Progressable progressable;

    public ProgressableOutputStream(OutputStream os, Progressable progressable) {
        this.os = os;
        this.progressable = progressable;
    }

    @Override public void write(int b) throws IOException {
        os.write(b);
        progressable.progress();
    }

    @Override public void write(byte[] b) throws IOException {
        os.write(b);
        progressable.progress();
    }

    @Override public void write(byte[] b, int off, int len) throws IOException {
        os.write(b, off, len);
        progressable.progress();
    }

    @Override public void flush() throws IOException {
        os.flush();
        progressable.progress();
    }

    @Override public void close() throws IOException {
        os.close();
        progressable.progress();
    }
}
