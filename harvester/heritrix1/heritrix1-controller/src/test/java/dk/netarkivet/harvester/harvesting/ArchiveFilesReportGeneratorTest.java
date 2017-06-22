/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2017 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.harvester.harvesting;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestResourceUtils;

public class ArchiveFilesReportGeneratorTest {
    @Rule public TestName test = new TestName();
    private File WORKING_DIR;

    @Before
    public void setUp() throws Exception {
        WORKING_DIR = new File(TestResourceUtils.OUTPUT_DIR, getClass().getSimpleName() + "/" + test.getMethodName());
        FileUtils.removeRecursively(WORKING_DIR);
        FileUtils.createDir(WORKING_DIR);

        File ARC_FILES_REPORT_DIR = new File("src/test/resources/arcFilesReport");
        FileUtils.copyDirectory(ARC_FILES_REPORT_DIR, WORKING_DIR);
    }

    @Test
    public final void testPatterns() throws ParseException {

        Object[] params = ArchiveFilesReportGenerator.FILE_OPEN_FORMAT
                .parse("2010-07-20 16:12:53.698 INFO thread-14 org.archive.io.WriterPoolMember.createFile() Opened /somepath/jobs/current/high/5_1279642368951/arcs/5-1-20100720161253-00000.arc.gz.open");
        assertEquals("2010-07-20 16:12:53.698", params[0]);
        assertEquals("14", params[1]);
        assertEquals("/somepath/jobs/current/high/5_1279642368951/arcs/5-1-20100720161253-00000.arc.gz",
                params[2]);

        params = ArchiveFilesReportGenerator.FILE_CLOSE_FORMAT
                .parse("2010-07-20 16:14:31.792 INFO thread-29 org.archive.io.WriterPoolMember.close() Closed /somepath/jobs/current/high/5_1279642368951/arcs/5-1-20100720161253-00000-bnf_test.arc.gz, size 162928");
        assertEquals("2010-07-20 16:14:31.792", params[0]);
        assertEquals("29", params[1]);
        assertEquals("/somepath/jobs/current/high/5_1279642368951/arcs/5-1-20100720161253-00000-bnf_test.arc.gz",
                params[2]);
        assertEquals(162928L, Long.parseLong((String) params[3]));
    }

    @Test
    public final void testReportGeneration() throws IOException {
        File crawlDir = new File(WORKING_DIR, "crawldir");
        ArchiveFilesReportGenerator gen = new ArchiveFilesReportGenerator(crawlDir);
        File expectedReport = new File(WORKING_DIR, "expected.arcfiles-report.txt");

        File actualReport = gen.generateReport();

        assertEquals(FileUtils.readFile(expectedReport), FileUtils.readFile(actualReport));
    }
}
