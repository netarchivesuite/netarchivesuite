package dk.netarkivet.harvester.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.viewerproxy.webinterface.CrawlLogLinesMatchingRegexp;

/**
 * Find relevant crawllog lines for a specific domain in a specific metadata file 
 * args: domain metadatafile
 * 
 * Note: currently the regexp is embedded in the jsp page harvester/qa-gui/src/main/webapp/QA-searchcrawllog.jsp
 * but should probably be removed to the Reporting class ./harvester/harvester-core/src/main/java/dk/netarkivet/viewerproxy/webinterface/Reporting.java
 * 
 */
public class FindRelevantCrawllogLines {

	/** New regexp to fix NARK-1212 /NAS-2690 */
	public static String getRegexpToFindDomainLines(String domain) {
		return ".*(https?:\\/\\/(www\\.)?|dns:|ftp:\\/\\/)([\\w_-]+\\.)?([\\w_-]+\\.)?([\\w_-]+\\.)?" + domain.replaceAll("\\.", "\\\\.") +  "($|\\/|\\w|\\s).*"; 
	}
	
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.err.println("Too few or too many arguments. Two needed. You gave me " + args.length);
			System.exit(1);
		}
		String domain = args[0];
		File metadatafile = new File(args[1]);
		if (!metadatafile.isFile()) {
			System.err.println("The file given as argument does not exist or is a directory: " 
					+ metadatafile.getAbsolutePath());
			System.exit(1);
		}
		File resultFile1 = File.createTempFile("FindRelevant", "matchingLines", new File("/tmp"));
		
		String regexp = getRegexpToFindDomainLines(domain);
		File resultFile = resultFile1;	
		List<String> lines = findLines(metadatafile, regexp, resultFile);
		System.out.println("Found " + lines.size() + " matching lines for domain '" + domain + "' in file '" + metadatafile.getAbsolutePath() + "'");
		System.out.println("Resultfile is " + resultFile.getAbsolutePath());
		lines.clear();
		System.exit(0);
	}
	
	private static List<String> findLines(File metadatafile, String regexp, File resultFile) throws IOException {
		FileBatchJob job = new CrawlLogLinesMatchingRegexp(regexp);
		BatchLocalFiles batch = new BatchLocalFiles(new File[]{metadatafile});
		
		OutputStream os = new FileOutputStream(resultFile);
		batch.run(job, os);
		os.close();
		return org.apache.commons.io.FileUtils.readLines(resultFile);
	}
}
