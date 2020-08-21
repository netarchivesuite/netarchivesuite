package dk.netarkivet.wayback.hadoop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.wayback.batch.DeduplicationCDXExtractionBatchJob;

public class DedupIndexer implements Indexer {

    @Override public List<String> indexFile(File file) throws IOException {
        FileBatchJob fileBatchJob = new DeduplicationCDXExtractionBatchJob();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        fileBatchJob.initialize(baos);
        fileBatchJob.processFile(file, baos);
        fileBatchJob.finish(baos);
        return Arrays.asList(baos.toString().split("\\n"));
    }

}
