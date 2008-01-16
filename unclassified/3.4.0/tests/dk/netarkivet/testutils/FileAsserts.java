/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

package dk.netarkivet.testutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import dk.netarkivet.common.utils.FileUtils;

/**
 * Utility functions for asserting statements about files.
 * Notice that using these may cause the files to be re-read several times.
 * This ought to be cheap, but may not be for big files.
 *
 */

public class FileAsserts {
    /** Assert that a given string exists in the file.  If it doesn't,
     * fail and print the file contents.  If the file couldn't be read,
     * fail and print the error message.
     *
     * @param msg An explanatory message.
     * @param str A string to find in the file.
     * @param file A file to scan.
     */
    public static void assertFileContains(String msg, String str, File file) {
        try {
            String contents = FileUtils.readFile(file);
            int index = contents.indexOf(str);
            if (index == -1) {
                System.out.println("Did not find string '" + str + "' in:\n"
                        + "==START FILE " + file + "==\n" + contents
                        + "\n==END FILE==");
                TestCase.fail(msg);
            }
        } catch (IOException e) {
            TestCase.fail("Should be able to read " + file + ", but got " + e);
        }
    }

    /** Assert that a given pattern has a match in the file.  If it doesn't,
     * fail and print the file contents.  If the file couldn't be read,
     * fail and print the error message.
     *
     * @param msg An explanatory message.
     * @param pattern A pattern to search for in the file.
     * @param file A file to scan.
     */
    public static void assertFileMatches(String msg, String pattern, File file) {
        try {
            String contents = FileUtils.readFile(file);
            if (!Pattern.compile(pattern, Pattern.MULTILINE).matcher(contents).find()) {
                System.out.println("Did not find pattern '" + pattern + "' in:\n"
                        + "==START FILE " + file + "==\n" + contents
                        + "\n==END FILE==");
                TestCase.fail(msg);
            }
        } catch (IOException e) {
            TestCase.fail("Should be able to read " + file + ", but got " + e);
        }
    }

    /** Assert that a given string exists in the file.  If it doesn't,
     * fail and print the file contents.  If the file couldn't be read,
     * fail and print the error message.
     *
     * @param msg An explanatory message.
     * @param file A file to scan.
     * @param str A string to find in the file.
     */
    public static void assertFileNotContains(String msg, File file, String str) {
        try {
            String contents = FileUtils.readFile(file);
            int index = contents.indexOf(str);
            if (index != -1) {
                System.out.println("Found string " + str + " in " + file
                        + " with contents:\n====\n" + contents + "\n====");
                TestCase.fail(msg);
            }
        } catch (IOException e) {
            TestCase.fail("Should be able to read " + file + ", but got " + e);
        }
    }

    /**
     * Assert that a given file has the expected number of lines If it doesn't,
     * fail and print the file contents.  If the file couldn't be read,
     * fail and print the error message.
     *
     * @param msg an explanatory message
     * @param file the File to check
     * @param n the expected number of lines
     */
    public static void assertFileNumberOfLines(String msg, File file, int n){
        try {
            BufferedReader r = new BufferedReader(new FileReader(file));
            int i = 0;
            String line = "";
            while (line != null){
                line = r.readLine();
                if (line != null) i++;
            }
            if (i != n) {
                TestCase.fail(msg + ": Expected " + n + " lines in " + file +
                        " but found only " + i);
            }
            r.close();
        } catch (IOException e) {
            TestCase.fail(msg + ": Couldn't read " + file + ", got " + e);
        }

    }

    /** Assert that a given file contains exactly the string given.  This will
     * read the given file's contents into a string.
     *
     * @param msg An explanatory message
     * @param toMatch The string that should be the full contents of the file
     * @param file The file that the string should be in.
     */
    public static void assertFileContainsExactly(String msg, String toMatch,
                                                 File file) {
        try {
            String contents = FileUtils.readFile(file);
            TestCase.assertEquals(msg, toMatch, contents);
        } catch (IOException e) {
            TestCase.fail("Should be able to read " + file + ", but got " + e);
        }
    }
}
