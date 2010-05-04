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

    public UrlSearch(String arg1, String regex, String mimetype) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(regex, "String regex");
        ArgumentNotValid.checkNotNull(mimetype, "String mimetype");
        this.regex = regex;
        this.mimetype = mimetype;
    }

    @Override
    public void finish(OutputStream os) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void initialize(OutputStream os) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void processRecord(ARCRecord record, OutputStream os) {
        if(!record.getMetaData().getUrl().matches(regex)) {
            return;
        }
        if(!record.getMetaData().getMimetype().matches(mimetype)) {
            return;
        }
        try {
            os.write(new String(record.getMetaData().getUrl() 
                    + "\n").getBytes());
        } catch (IOException e) {
            throw new IOFailure("Unexpected problem when writing to output "
                    + "stream.", e);
        }
        // TODO Auto-generated method stub
        
    }
    

}
