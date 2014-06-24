package dk.netarkivet.archive.bitarchive.distribute;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.arc.ARCBatchJob;
import dk.netarkivet.common.utils.batch.ARCBatchFilter;

import java.io.IOException;
import java.io.OutputStream;

import org.archive.io.arc.ARCRecord;

@SuppressWarnings({ "serial"})
public class TestBatchJobRuns extends ARCBatchJob {
    boolean initialized;
    public int records_processed;
    boolean finished;

    public ARCBatchFilter getFilter() {
        return ARCBatchFilter.NO_FILTER;
    }

    public void initialize(OutputStream os) {
        initialized = true;
    }

    public void processRecord(ARCRecord record, OutputStream os) {
        records_processed++;
    }

    public void finish(OutputStream os) {
        try {
            os.write(("Records Processed = " + records_processed).getBytes());
        } catch (IOException e) {
            throw new IOFailure ("Error writing to output file: ", e);
        }
        finished = true;
    }
}
