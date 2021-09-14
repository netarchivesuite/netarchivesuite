package dk.netarkivet.wayback.hadoop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.hadoop.util.Progressable;

import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.wayback.batch.DeduplicationCDXExtractionBatchJob;

public class DedupIndexer implements Indexer {

    @Override public List<String> indexFile(File file, Progressable progressable) throws IOException {
        FileBatchJob fileBatchJob = new DeduplicationCDXExtractionBatchJob();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        fileBatchJob.initialize(baos);
        //TODO this could fail on timeout as the progressable is not updated
        fileBatchJob.processFile(file, baos);
        fileBatchJob.finish(baos);
        baos.flush();
        return Arrays.asList(baos.toString().split("\\n"));
    }

}
