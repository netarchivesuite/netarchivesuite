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

package dk.netarkivet.testutils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsNot;
import org.junit.Assert;

import dk.netarkivet.common.utils.FileUtils;
import junit.framework.TestCase;

/**
 * Utility functions for asserting statements about files. Notice that using these may cause the files to be re-read
 * several times. This ought to be cheap, but may not be for big files.
 */

public class FileAsserts {
    /**
     * Assert that a given string exists in the file. If it doesn't, fail and print the file contents. If the file
     * couldn't be read, fail and print the error message.
     *
     * @param msg An explanatory message.
     * @param str A string to find in the file.
     * @param file A file to scan.
     */
    public static void assertFileContains(String msg, String str, File file) {
        try {
            String contents = FileUtils.readFile(file);
            // http://stackoverflow.com/a/1092241/53897
            Assert.assertThat(contents, CoreMatchers.containsString(str));
        } catch (IOException e) {
            TestCase.fail("Should be able to read " + file + ", but got " + e);
        }
    }

    /**
     * Assert that a given pattern has a match in the file. If it doesn't, fail and print the file contents. If the file
     * couldn't be read, fail and print the error message.
     *
     * @param msg An explanatory message.
     * @param regexp A pattern to search for in the file.
     * @param file A file to scan.
     */
    public static void assertFileMatches(String msg, String regexp, File file) {
        try {
            String contents = FileUtils.readFile(file);
            Pattern pattern = Pattern.compile(regexp, Pattern.MULTILINE);
            // https://github.com/derari/cthul/wiki/Matchers#string-matchers
            Assert.assertThat(contents, org.cthul.matchers.CthulMatchers.containsPattern(pattern));
        } catch (IOException e) {
            TestCase.fail("Should be able to read " + file + ", but got " + e);
        }
    }

    /**
     * Assert that a given string exists in the file. If it doesn't, fail and print the file contents. If the file
     * couldn't be read, fail and print the error message.
     *
     * @param msg An explanatory message.
     * @param file A file to scan.
     * @param str A string to find in the file.
     */
    public static void assertFileNotContains(String msg, File file, String str) {
        try {
            String contents = FileUtils.readFile(file);
            // http://stackoverflow.com/a/1092241/53897
            Assert.assertThat(contents, IsNot.not(CoreMatchers.containsString(str)));
        } catch (IOException e) {
            TestCase.fail("Should be able to read " + file + ", but got " + e);
        }
    }

    /**
     * Assert that a given file has the expected number of lines If it doesn't, fail and print the file contents. If the
     * file couldn't be read, fail and print the error message.
     *
     * @param msg an explanatory message
     * @param file the File to check
     * @param n the expected number of lines
     */
    public static void assertFileNumberOfLines(String msg, File file, int n) {
        try {
            BufferedReader r = new BufferedReader(new FileReader(file));
            int i = 0;
            String line = "";
            while (line != null) {
                line = r.readLine();
                if (line != null) {
                    i++;
                }
            }
            if (i != n) {
                TestCase.fail(msg + ": Expected " + n + " lines in " + file + " but found only " + i);
            }
            r.close();
        } catch (IOException e) {
            TestCase.fail(msg + ": Couldn't read " + file + ", got " + e);
        }

    }

    /**
     * Assert that a given file contains exactly the string given. This will read the given file's contents into a
     * string.
     *
     * @param msg An explanatory message
     * @param toMatch The string that should be the full contents of the file
     * @param file The file that the string should be in.
     */
    public static void assertFileContainsExactly(String msg, String toMatch, File file) {
        try {
            String contents = FileUtils.readFile(file);
            TestCase.assertEquals(msg, toMatch, contents);
        } catch (IOException e) {
            TestCase.fail("Should be able to read " + file + ", but got " + e);
        }
    }
}
