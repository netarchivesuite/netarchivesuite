/*$Id$
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
package dk.netarkivet.common.utils.cdx;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.Method;
import java.util.List;

import junit.framework.TestCase;

import dk.netarkivet.TestUtils;
import dk.netarkivet.common.utils.IteratorUtils;
import dk.netarkivet.testutils.ReflectUtils;

/**
 * Unit test for the BinSearch class.
 */
public class BinSearchTester extends TestCase {
    public BinSearchTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    /** Test that getLinesInFile(File, String) returns the expected
     * lines. Uses the locally defined wrapper method findLinesInFile.
     */
    public void testGetLinesInFile() throws IOException {
        // Test a failing search
        assertEquals("Should get no results for non-existing domain",
                0, findLinesInFile(TestInfo.CDX_FILE1, "http://fnord/").size());
        // Test a search beyond the end
        assertEquals("Should get no results for past-the-end domain",
                0, findLinesInFile(TestInfo.CDX_FILE1,
                        "http://xenophile.dk/").size());
        // Test a search beyond the start
        assertEquals("Should get no results for past-the-start domain",
                0, findLinesInFile(TestInfo.CDX_FILE1, "dns:101").size());
        // Test a simple search
        List<String> playerUrls = findLinesInFile(TestInfo.CDX_FILE1,
                "http://player.");
        assertEquals("Should get exactly 4 lines for player.",
                4, playerUrls.size());
        assertTrue("Should have right line first",
                playerUrls.get(0).startsWith(
                        "http://player.localeyes.tv/entry.asp"));
        assertTrue("Should have other line next",
                playerUrls.get(1).startsWith(
                        "http://player.localeyes.tv/graphics/"
                        + "copyright_m_logo.gif"));

        List<String> serverDkUrls = findLinesInFile(TestInfo.CDX_FILE1,
                "http://server-dk.");
        assertEquals("Should get exactly 5 lines for server-dk",
                5, serverDkUrls.size());
        assertTrue("Should have root first",
                serverDkUrls.get(0).startsWith(
                        "http://server-dk.imrworldwide.com/ "));
        assertTrue("Should have root second, too",
                serverDkUrls.get(1).startsWith(
                        "http://server-dk.imrworldwide.com/ "));
        assertTrue("Should have different thing third",
                serverDkUrls.get(2).startsWith(
                        "http://server-dk.imrworldwide.com/a1.js "));

        // Test that the Iterable can be reused
        Iterable<String> lines = BinSearch.getLinesInFile(TestInfo.CDX_FILE1,
                "http://server-dk.");
        int count1 = IteratorUtils.toList(lines.iterator()).size();
        int count2 = IteratorUtils.toList(lines.iterator()).size();
        assertEquals("Should still get 5 lines for server-dk", 5, count1);
        assertEquals("Should get the same amount second time around",
                count1, count2);
    }

    /**
     * Test the BinSearch.getLinesInFile(File, String) with
     * Danish letters.
     */
    public void testGetLinesInFileDanish() {
        // This test fails because RandomAccessFile doesn't support UniCode at
        // all (see its documentation:
        // http://java.sun.com/j2se/1.5.0/docs/api/java/io/RandomAccessFile.html
        // http://java.sun.com/javase/6/docs/api/java/io/RandomAccessFile.html).
        // It can sometimes look like it passes,
        // since it hits some bug in IDEA's JUnit framework.  It should
        // therefore not be included until a more robust way of random access
        // is found.
        // This is not really a problem, since our CDX files are all in
        // pure ASCII -- domain names are IDNA-encoded, and paths are %XX-
        // encoded.
        // See Netarchivesuite bug 1913 for this issue.
        if (!TestUtils.runningAs("SUN")) {
            return;
        }
        List<String> danish = findLinesInFile(TestInfo.CDX_FILE1,
                "http://download.");
        assertEquals("Should get exactly three lines for download.",
                3, danish.size());
        assertEquals("Should have read the danish line correctly",
                "http://download.macromedia.com/påb/",
                danish.get(1));
                //.startsWith("http://download.macromedia.com/påb/"));

        List<String> danishSearch = findLinesInFile(TestInfo.CDX_FILE1,
                "http://download.macromedia.com/påb/");
        assertEquals("Should get just the one line",
                1, danishSearch.size());
    }

    /** Wrapper around getLinesInFile that turns them into a List. 
     * @param file The file to search in 
     * @param find The string to look for in the file.
     * @return a List of lines that matches the String given by arg find.
     */
    private static List<String> findLinesInFile(File file, String find) {
        return IteratorUtils.toList(
                BinSearch.getLinesInFile(file, find).iterator());
    }

    /** Test that skipToLine goes to the expected place, and puts the file
     * pointer there.
     * @throws Exception
     */
    public void testSkipToLine() throws Exception {
        Method m = BinSearch.class.getDeclaredMethod("skipToLine",
                new Class[] {RandomAccessFile.class, Long.TYPE });
        m.setAccessible(true);
        RandomAccessFile f = new RandomAccessFile(
                TestInfo.SORTED_CDX_FILE, "r");
        // Test at start of line
        Long l = (Long) m.invoke(null, f, 24859); // 342-line
        assertEquals("Should have 343-line returned",
                25041, l.longValue());
        assertEquals("File should be at returned value",
                l.longValue(), f.getFilePointer());
        // Test just before start of line
        l = (Long) m.invoke(null, f, 24858);
        assertEquals("Should have 342-line returned",
                24859, l.longValue());
        assertEquals("File should be at returned value",
                l.longValue(), f.getFilePointer());
        // Test just after start of line
        l = (Long) m.invoke(null, f, 25042);
        assertEquals("Should have 344-line returned",
                25223, l.longValue());
        assertEquals("File should be at returned value",
                l.longValue(), f.getFilePointer());
    }

    /**
     * Test that findMiddleLine can actually find the right middle line.
     */
    public void testFindMiddleLine() throws Exception {
        Method m = ReflectUtils.getPrivateMethod(BinSearch.class,
                "findMiddleLine", RandomAccessFile.class, Long.TYPE, Long.TYPE);
        RandomAccessFile f = new RandomAccessFile(
                TestInfo.SORTED_CDX_FILE, "r");
        // This file has linestarts at 24859, 25041, 25223, 25406 consecutively
        Long l = (Long) m.invoke(null, f, 24859, 25223);
        assertEquals("Should have given line in middle", 25041, l.longValue());
        assertEquals("Should have file pointer at the right place",
                l.longValue(), f.getFilePointer());
        l = (Long) m.invoke(null, f, 24859, 25041);
        assertEquals("Should have given -1", -1, l.longValue());
        l = (Long) m.invoke(null, f, 24859, 25406);
        assertEquals("Should have found a line in between",
                25223, l.longValue());
        assertEquals("Should have file pointer at the right place",
                l.longValue(), f.getFilePointer());
        // This unit test is not complete.

    }
}
