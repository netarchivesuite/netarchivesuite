/*
 * #%L
 * Netarchivesuite - common - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
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

package dk.netarkivet.common.utils.batch;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.utils.arc.TestInfo;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

public class DatedFileListJobTester {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);

    @Before
    public void setUp() throws Exception {
        mtf.setUp();
    }

    @After
    public void tearDown() throws Exception {
        mtf.tearDown();
    }

    @Test
    public void testProcess() throws IOException, InterruptedException {
        FileListJob job = new FileListJob();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        processOnDirectory(job, TestInfo.WORKING_DIR, os);
        int totalFiles = countLines(os);
        int filesToBeIgnored = 4;
        int filesTouched = 0;
        for (File file : TestInfo.WORKING_DIR.listFiles()) {
            if (filesTouched < filesToBeIgnored) {
                file.setLastModified(new Date().getTime() - 3600000L);
                filesTouched++;
            } else {
                file.setLastModified(new Date().getTime());
            }
        }
        // Now there are four files with timestamp one hour ago and the rest with timestamp "now".
        DatedFileListJob job2 = new DatedFileListJob(new Date(new Date().getTime() - 1800000));
        os = new ByteArrayOutputStream();
        processOnDirectory(job2, TestInfo.WORKING_DIR, os);
        assertEquals("Expected the number of files found to exclude those files with timestamp set to one hour ago.",
                totalFiles - filesToBeIgnored, countLines(os));
    }

    private static void processOnDirectory(FileBatchJob job, File dir, OutputStream os) {
        for (File file : dir.listFiles()) {
            job.processFile(file, os);
        }
    }

    private static int countLines(ByteArrayOutputStream os) throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader(os.toString()));
        int lines = 0;
        while (reader.readLine() != null) {
            lines++;
        }
        return lines;
    }

}
