/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
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
