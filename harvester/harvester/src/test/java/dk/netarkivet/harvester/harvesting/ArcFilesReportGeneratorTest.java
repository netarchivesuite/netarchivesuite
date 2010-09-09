/**
 * 
 */
package dk.netarkivet.harvester.harvesting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.ParseException;

import junit.framework.TestCase;

/**
 * @author ngiraud
 *
 */
public class ArcFilesReportGeneratorTest extends TestCase {
	
	public final void testPatterns() throws ParseException {
		
		Object[] params = ArcFilesReportGenerator.ARC_OPEN_FORMAT.parse(
				"2010-07-20 16:12:53.698 INFO thread-14 org.archive.io.WriterPoolMember.createFile() Opened /somepath/jobs/current/high/5_1279642368951/arcs/5-1-20100720161253-00000.arc.gz.open"
		);
		assertEquals(
				"2010-07-20 16:12:53.698", 
				(String) params[0]);			
		assertEquals(
				"14", 
				(String) params[1]);
		assertEquals(
				"/somepath/jobs/current/high/5_1279642368951/arcs/5-1-20100720161253-00000.arc.gz", 
				(String) params[2]);
		
		params = ArcFilesReportGenerator.ARC_CLOSE_FORMAT.parse(
				"2010-07-20 16:14:31.792 INFO thread-29 org.archive.io.WriterPoolMember.close() Closed /somepath/jobs/current/high/5_1279642368951/arcs/5-1-20100720161253-00000-bnf_test.arc.gz, size 162928");
		assertEquals(
				"2010-07-20 16:14:31.792", 
				(String) params[0]);	
		assertEquals(
				"29", 
				(String) params[1]);
		assertEquals(
				"/somepath/jobs/current/high/5_1279642368951/arcs/5-1-20100720161253-00000-bnf_test.arc.gz", 
				(String) params[2]);			
		assertEquals(
				162928L, 
				Long.parseLong((String) params[3]));
	}
	
	public final void testReportGeneration() throws IOException {
		
		File actualReport = null;		
		try {
			File crawlDir = new File(
					TestInfo.BASEDIR, 
					"arcFilesReport" + File.separator + "crawldir");
			ArcFilesReportGenerator gen = new ArcFilesReportGenerator(crawlDir);

			File expectedReport = new File(TestInfo.BASEDIR, 
					"arcFilesReport" + File.separator 
					+ "expected.arcfiles-report.txt");
			

			actualReport = gen.generateReport();
			
			assertEquals(
					toString(expectedReport), 
					toString(actualReport));
			
		} finally {
			if ((actualReport != null) && actualReport.exists()) {
				if (! actualReport.delete()) {
					actualReport.deleteOnExit();
				}
			}
		}
	}
	
	private static String toString(File f) throws IOException {
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		BufferedReader br = 
			new BufferedReader(new FileReader(f));
		String line = null;
		while ((line = br.readLine()) != null) {
			pw.println(line);
		}
		
		String fileContents = sw.toString();
		
		pw.close();
		br.close();
		
		return fileContents;
	}

}
