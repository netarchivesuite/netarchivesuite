/*
 * #%L
 * Netarchivesuite - archive - test
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
package dk.netarkivet.archive.bitarchive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.testutils.LogbackRecorder;

public class BitarchiveTesterGetFile extends BitarchiveTestCase {
    //private static final File ORIGINALS_DIR = new File(new File(TestInfo.DATA_DIR, "getFile"), "originals");
    protected File getOriginalsDir() {
    	return new File(new File(TestInfo.DATA_DIR, "getFile"), "originals");
        //return ORIGINALS_DIR;
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testGetFile_Failure() throws Exception {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        String arcFileID = "test";
        File result = archive.getFile(arcFileID);
        assertNull("Non-existing file should give null result", result);
        lr.assertLogContains("Log should mention non-success", arcFileID + "' not found");
        lr.stopRecorder();
    }

    @Test
    public void testGetFile_Success() throws IOException {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        String arcFileID = "Upload1.ARC";
        File result = archive.getFile(arcFileID);
        assertEquals("Result should be the expected file", new File(TestInfo.FILE_DIR, arcFileID).getCanonicalPath(),
                result.getCanonicalPath());
        lr.assertLogContains("Log should mention start", "Get file '" + arcFileID);
        lr.assertLogContains("Log should mention success", "Getting file '" + result);
        lr.stopRecorder();
    }

}
