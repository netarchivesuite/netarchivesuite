/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.netarkivet.common.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.mockobjects.servlet.MockJspWriter;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * 
 * Unit tests for the StreamUtils class.
 *
 */
public class StreamUtilsTester extends TestCase {
    
    private UseTestRemoteFile rf = new UseTestRemoteFile();
    ReloadSettings rs = new ReloadSettings();

    private static final File BASE_DIR = new File("tests/dk/netarkivet/common/utils");
    private static final File ORIGINALS = new File(BASE_DIR, "fileutils_data");
    private static final File WORKING = new File(BASE_DIR, "working");
    private static final File TESTFILE = new File(WORKING, "streamutilstestfile.txt");
    
    public void setUp() {
        rs.setUp();
        FileUtils.removeRecursively(WORKING);
        TestFileUtils.copyDirectoryNonCVS(ORIGINALS, WORKING);
        rf.setUp();
    }

    public void tearDown() {
        FileUtils.removeRecursively(WORKING);
        rf.tearDown();
        rs.tearDown();
    }
  
    public void testCopyInputStreamToOutputStream() throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream("foobar\n".getBytes());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        StreamUtils.copyInputStreamToOutputStream(in, out);
        assertEquals("Input should have been transferred",
                     "foobar\n", out.toString());
        assertEquals("In stream should be at EOF",
                     -1, in.read());

        in = new ByteArrayInputStream("hash".getBytes());
        StreamUtils.copyInputStreamToOutputStream(in, out);
        assertEquals("Extra input should have been transferred",
                     "foobar\nhash", out.toString());

        StreamUtils.copyInputStreamToOutputStream(in, out);
        assertEquals("Re-reading input should give no error and no change",
                     "foobar\nhash", out.toString());

        in = new ByteArrayInputStream("".getBytes());
        StreamUtils.copyInputStreamToOutputStream(in, out);
        assertEquals("Zero input should have been transferred",
                     "foobar\nhash", out.toString());

        try {
            StreamUtils.copyInputStreamToOutputStream(null, out);
            fail("Null in should cause error");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            StreamUtils.copyInputStreamToOutputStream(in, null);
            fail("Null out should cause error");
        } catch (ArgumentNotValid e) {
            // expected
        }

        // TODO: Test with streams that cause errors if closed.
    }
    
    /** test that method copyInputStreamToJspWriter works. */
    public void testCopyInputStreamToJspWriter() throws Exception {
        StringBuffer buf = new StringBuffer();
        MyMockJspWriter writer = new MyMockJspWriter(buf);
        String testfileAsString = FileUtils.readFile(TESTFILE);
        StreamUtils.copyInputStreamToJspWriter(new FileInputStream(TESTFILE), writer);
        
        assertEquals(testfileAsString, buf.toString());
    }
    
    private class MyMockJspWriter extends MockJspWriter {
        private StringBuffer buf;
        
        public MyMockJspWriter(StringBuffer buf) {
            this.buf = buf;
        }
        
        public void write(String str, int off, int len) {
            buf.append(str.substring(off, len));
        }
      }
}
