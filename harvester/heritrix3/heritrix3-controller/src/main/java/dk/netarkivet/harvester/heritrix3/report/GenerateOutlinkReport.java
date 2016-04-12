package dk.netarkivet.harvester.heritrix3.report;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.utils.ApplicationUtils;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;

// run local warc-batchjob on the warc files produced by Heritrix3 that 
// looks for outlink lines in WARC-metadata records

public class GenerateOutlinkReport {

	private static final Logger log = LoggerFactory.getLogger(GenerateOutlinkReport.class);
	// Test-program
	public static void main(String[] args) {
		File warcsDir = new File("/home/svc/devel/webdanica-core");
		ApplicationUtils.dirMustExist(FileUtils.getTempDir());
		generateReport(warcsDir);
	}

	public static void generateReport(File warcFilesDir) {
		
		File[] warcFileArray = warcFilesDir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				//boolean matches = name.matches(FileUtils.WARC_PATTERN); // FIXME this pattern fails to find : 431-35-20160317083714655-00000-sb-test-har-001.statsbiblioteket.dk.warc
				boolean matches = name.contains("warc");
				
				log.debug(" '" + name + "' matches: " + matches);
				return matches;	
			}
		});
		log.debug("Found {} files matching the pattern for warcfiles",  warcFileArray.length);
		File outlinkreportFile = new File(FileUtils.getTempDir(), "outlinkreport-" +  System.currentTimeMillis() + ".txt");
		log.debug("Using outlinkreport file {} for outlinks written to metadata records", outlinkreportFile.getAbsolutePath());
		BatchLocalFiles batchRunner = new BatchLocalFiles(warcFileArray);
		OutputStream outlinkStream = null;
        try {
	        outlinkStream = new FileOutputStream(outlinkreportFile);
	        batchRunner.run(new ExtractOutlinksFromWarcMetadataBatchJob(), outlinkStream);
        } catch (FileNotFoundException e) {
        	log.debug("Error while creating outlinkreportfile{}", outlinkreportFile, e);
        } finally {
        	IOUtils.closeQuietly(outlinkStream);
        }
    }	
}
