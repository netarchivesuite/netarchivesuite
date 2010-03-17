/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package dk.netarkivet.wayback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import junit.framework.TestCase;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestFileUtils;

/**
 * csr forgot to comment this!
 *
 */

public class DeduplicateToCDXApplicationTester extends TestCase {

    PrintStream orig_std_out;
    OutputStream new_std_out;
    PrintStream orig_std_err;
    OutputStream new_std_err;

    public void setUp() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        orig_std_out = System.out;
        orig_std_err = System.err;
        new_std_out = new ByteArrayOutputStream();
        new_std_err = new ByteArrayOutputStream();
        System.setErr(new PrintStream(new_std_err));
        System.setOut(new PrintStream(new_std_out));
    }


    public void tearDown() {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        FileUtils.remove(TestInfo.LOG_FILE);
        System.setOut(orig_std_out);
        System.setOut(orig_std_err);
    }

    public void testGenerateCDX() throws IOException {
        File file1 = new File(TestInfo.WORKING_DIR, "dedup_crawl_log.txt");
        File file2 = new File(TestInfo.WORKING_DIR, "dedup_crawl_log2.txt");
        String[] files = new String[] {file1.getAbsolutePath(), file2.getAbsolutePath()};
        DeduplicateToCDXApplication app = new DeduplicateToCDXApplication();
        app.generateCDX(files);
        String output = ((ByteArrayOutputStream) new_std_out).toString();
        String error = ((ByteArrayOutputStream) new_std_err).toString();
        assertTrue("Error string should be empty", error.equals(""));
        assertTrue("Expect plenty of cdx results, not '" + output + "'",
                   output.split("\n").length > 20);
    }

}
