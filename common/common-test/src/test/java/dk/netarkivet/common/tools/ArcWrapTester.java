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
package dk.netarkivet.common.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Iterator;

import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.ARCTestUtils;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;

/**
 * Unit test for the ArcWrap tool.
 */
public class ArcWrapTester {
    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams();
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.DATA_DIR, TestInfo.WORKING_DIR);

    @Before
    public void setUp() {
        mtf.setUp();
        pss.setUp();
        pse.setUp();
    }

    @After
    public void tearDown() {
        pse.tearDown();
        pss.tearDown();
        mtf.tearDown();
    }

    @Test
    public void testMain() throws IOException {
        String pathname = "tests/dk/netarkivet/common/tools/ArcWrapTester.txt";
        File storeFile = new File(pathname);
        assertTrue(pathname + " exists()?", storeFile.exists());
        String mime = "text/plain";
        String arcUri = "testdata://netarkivet.dk/test/code/ArcWrapTester.java";
        File arcFile = new File(TestInfo.WORKING_DIR, "test.arc");
        OutputStream myOut = new FileOutputStream(arcFile);
        System.setOut(new PrintStream(myOut));
        try {
            ArcWrap.main(new String[] {storeFile.getAbsolutePath(), arcUri, mime});
        } catch (SecurityException e) {
            assertEquals("Should have exited normally", 0, pse.getExitValue());
        }
        myOut.close();
        // Put an ARCReader on top of the file.
        ARCReader r = ARCReaderFactory.get(arcFile);
        Iterator<ArchiveRecord> it = r.iterator();
        Assert.assertTrue(it.hasNext());
        Assert.assertNotNull(it.next()); // Skip ARC file header
        // Read the record, checking mime-type, uri and content.
        Assert.assertTrue(it.hasNext());
        ARCRecord record = (ARCRecord) it.next();
        ARCRecordMetaData meta = record.getMetaData();
        assertEquals("Should record the object under the given URI", arcUri, meta.getUrl());
        assertEquals("Should indicate the intended MIME type", mime, meta.getMimetype());
        String foundContent = ARCTestUtils.readARCRecord(record);
        assertEquals("Should store content unchanged", FileUtils.readFile(storeFile), foundContent);
        Assert.assertFalse(it.hasNext());
    }
}
