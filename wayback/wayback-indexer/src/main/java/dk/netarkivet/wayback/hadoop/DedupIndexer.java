package dk.netarkivet.wayback.hadoop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.util.Progressable;

import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.wayback.batch.DeduplicationCDXExtractionBatchJob;

public class DedupIndexer implements Indexer {

    @Override public List<String> indexFile(File file, Progressable progressable) throws IOException {
        FileBatchJob fileBatchJob = new DeduplicationCDXExtractionBatchJob();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                OutputStream progressableOutputStream = new ProgressableOutputStream(baos, progressable);) {
            fileBatchJob.initialize(progressableOutputStream);
            //TODO This can timeout if no dedup records are written to the OS for 300 seconds
            // The right way is to ditch the BatchJob interface and thread progressable through the code
            fileBatchJob.processFile(file, progressableOutputStream);
            fileBatchJob.finish(progressableOutputStream);
            progressableOutputStream.flush();
            return Arrays.asList(baos.toString().split("\\n"));
        }
    }

}
