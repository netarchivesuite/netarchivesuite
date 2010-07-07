package dk.netarkivet.common.utils.batch;

import java.io.IOException;
import java.io.OutputStream;

import javax.annotation.Resource;
import javax.annotation.Resources;

import org.archive.io.arc.ARCRecord;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.arc.ARCBatchJob;

/**
 * The batchjob checks each record whether it has a specific URL and/or 
 * specific mimetype (both in the shape of a regular expression).
 * The URLs of the digital objects which matches these constrains are returned.
 */
@SuppressWarnings("serial")
@Resources(value = {
        @Resource(name="regex", description="The regular expression for the "
            + "urls.", type=java.lang.String.class), 
        @Resource(name="mimetype", type=java.lang.String.class),
        @Resource(description="Batchjob for finding URLs which matches a given"
            + " regular expression and has a mimetype which matches another"
            + " regular expression.", 
                type=dk.netarkivet.common.utils.batch.UrlSearch.class)})
public class UrlSearch extends ARCBatchJob {
    private String regex;
    private String mimetype;
    private long urlCount = 0L;
    private long mimeCount = 0L;
    private long totalCount = 0L;
    private long bothCount = 0L;

    public UrlSearch(String arg1, String regex, String mimetype) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(regex, "String regex");
        ArgumentNotValid.checkNotNull(mimetype, "String mimetype");
        this.regex = regex;
        this.mimetype = mimetype;
    }

    @Override
    public void finish(OutputStream os) {
        // TODO Auto-generated method stub

        try {
            os.write(new String("\nResults:\n").getBytes());
            os.write(new String("Urls matched = " + urlCount 
                    + "\n").getBytes());
            os.write(new String("Mimetypes matched = " + mimeCount 
                    + "\n").getBytes());
            os.write(new String("Url and Mimetype matches = " + bothCount 
                    + "\n").getBytes());
        } catch (IOException e) {
            throw new IOFailure("Unexpected problem when writing to output "
                    + "stream.", e);
        }
    }

    @Override
    public void initialize(OutputStream os) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void processRecord(ARCRecord record, OutputStream os) {
        totalCount++;
        boolean valid = true;
        if(record.getMetaData().getUrl().matches(regex)) {
            urlCount++;
        } else {
            valid = false;
        }
        if(record.getMetaData().getMimetype().matches(mimetype)) {
            mimeCount++;
        } else {
            valid = false;
        }
        
        if(valid) {
            bothCount++;
            try {
            os.write(new String(record.getMetaData().getUrl() + " : " 
                    + record.getMetaData().getMimetype() + "\n").getBytes());
            } catch (IOException e) {
                // unexpected!
                throw new IOFailure("Cannot print to os!", e);
            }
        }
    }
    

}
