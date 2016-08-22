package dk.netarkivet.harvester.tools;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import dk.netarkivet.harvester.utils.CrawlertrapsUtils;

/**
 * Test all strings in the file argument for XML-wellformedness.
 */
public class CheckTrapsInFile {

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("Missing trapsfile argument");
			System.exit(1);
		}
		File trapsFile = new File(args[0]);
		if (!trapsFile.isFile()) {
			System.err.println("trapsfile argument '" +  trapsFile + "' does not exist");
			System.exit(1);
		}

		BufferedReader fr = null;
		String line=null;
		String trimmedLine=null;

		try {
			fr = new BufferedReader(new FileReader(trapsFile));
			while ((line = fr.readLine()) != null) {
				trimmedLine = line.trim();
				if (trimmedLine.isEmpty()) {
					continue;
				}
				if (CrawlertrapsUtils.isCrawlertrapsWellformedXML(trimmedLine)) {
					System.out.println("OK: crawlertrap '" + trimmedLine + "' is wellformed");
				}  else {
					System.out.println("BAD: crawlertrap '" + trimmedLine + "' is not wellformed");
				}
			}
		} finally {
			IOUtils.closeQuietly(fr);
		}
	}

}
