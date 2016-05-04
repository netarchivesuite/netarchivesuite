package dk.netarkivet.harvester.heritrix3.report;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.ApplicationUtils;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;

// run local warc-batchjob on the warc files produced by Heritrix3 that 
// looks for outlink lines in WARC-metadata records

public class GenerateOutlinkReport {

	private static final Logger log = LoggerFactory.getLogger(GenerateOutlinkReport.class);
	
	
	public static void main(String[] args) {
		File warcsDir = new File("/home/svc/devel/webdanica-core");
		ApplicationUtils.dirMustExist(FileUtils.getTempDir());
		File finalFile = generateReport(warcsDir);
		System.out.println("File '" + finalFile.getAbsolutePath() + "' has length: " + finalFile.length());
	}

	/**
	 * Run local warc-batchjob on the warc files produced by Heritrix3 that 
	 * looks for outlink lines in WARC-metadata records.
	 * @param warcFilesDir directory containing warc-files.
	 * @return a textfile of outlinks in WARC-metadata records.
	 */
	public static File generateReport(File warcFilesDir) {

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
			return removeDuplicateOutlinks(outlinkreportFile);
		} catch (FileNotFoundException e) {
			log.debug("Error while creating outlinkreportfile{}", outlinkreportFile, e);
			return null;
		} finally {
			IOUtils.closeQuietly(outlinkStream);
		}
	}
	
	private static File removeDuplicateOutlinks(File inputfile) {
		ArgumentNotValid.checkExistsNormalFile(inputfile, "File inputfile");
		String line;
		Set<String> links = new TreeSet<String>();
		BufferedReader bufferedReader = null;
		int linesRead=0;
        try {
	        bufferedReader = new BufferedReader( new FileReader(inputfile));
	        while ( (line = bufferedReader.readLine()) != null ){
	        	links.add(line);
	        	linesRead++;
	        }
        } catch (FileNotFoundException e) {
	        log.warn("Error ", e);
        } catch (IOException e1) {
        	log.warn("Error ", e1);
        } finally {
        	IOUtils.closeQuietly(bufferedReader);
        }
        
        log.info("Read {} lines: Found {} unique outlinks", linesRead, links.size());
        File outputFile = new File(FileUtils.getTempDir(), "outlink-reportfile-final-" 
        		+ System.currentTimeMillis() + ".txt" );
        PrintWriter writer = null;
        
	        try {
	            writer = new PrintWriter(outputFile, "UTF-8");
	            for (String link: links) {
	            	writer.println(link);
	            }
            } catch (FileNotFoundException e) {
	            return null;
            } catch (UnsupportedEncodingException e) {
            	return null;
            } finally {
            	IOUtils.closeQuietly(writer);
            }
        
		return outputFile;
	}
}
