package dk.netarkivet.harvester.heritrix3.report;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.utils.archive.ArchiveBatchJob;
import dk.netarkivet.common.utils.archive.ArchiveRecordBase;
import dk.netarkivet.common.utils.batch.ArchiveBatchFilter;

/**
 * Batchjobs that extract all outlinks in the heritrix metadata records. 
 * 
 */
public class ExtractOutlinksFromWarcMetadataBatchJob extends ArchiveBatchJob {
	
	private static final Logger log = LoggerFactory.getLogger(ExtractOutlinksFromWarcMetadataBatchJob.class);
	
	public ExtractOutlinksFromWarcMetadataBatchJob() {	
	}
	
	@Override
	public void processRecord(ArchiveRecordBase record, OutputStream os) {

		try {
			String line;
			BufferedReader bufferedReader = new BufferedReader( new InputStreamReader( record.getInputStream()) );
			while ( (line = bufferedReader.readLine()) != null ){ 
				if (line.startsWith("outlink")) {
					String [] parts = line.split("outlink: ");
					log.trace("Found outlink: {}", parts[1]);
					os.write(parts[1].getBytes(Charset.forName("UTF-8")));
					os.write("\n".getBytes(Charset.forName("UTF-8")));
				} else {
					log.trace("Skipping line: {}", line);
				}
			}  
		} catch (IOException e) {
			log.warn("Error: ", e );
		}
	}

	@Override
    public void initialize(OutputStream os) {
        
    }

	@Override
    public void finish(OutputStream os) {
        try {
	        os.flush();
        } catch (IOException e) {
	        e.printStackTrace();
        }
    }
	@Override
	public ArchiveBatchFilter getFilter() {
        return new ArchiveBatchFilter("OnlyMetadata") {
			
			@Override
			public boolean accept(ArchiveRecordBase record) {
				final String WARC_TYPE = "warc-type";
				final String METADATA_TYPE = "metadata";
				/*
				Set<String> hks = record.getHeader().getHeaderFieldKeys();
				for (String s: hks){
					//System.out.println(s);
				}*/
				return record.getHeader().getHeaderStringValue(WARC_TYPE).equals(METADATA_TYPE);
			}
		};
    }    
}

