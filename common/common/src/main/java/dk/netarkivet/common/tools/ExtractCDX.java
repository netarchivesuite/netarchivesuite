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

package dk.netarkivet.common.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.batch.BatchLocalFiles;
import dk.netarkivet.common.utils.cdx.ExtractCDXJob;

/**
 *
 *
 * Command line tool for extracting CDX information from given ARC files.
 *
 * Usage:
 * java dk.netarkivet.common.tools.ExtractCDX file1.arc [file2.arc ...]
 *      > myindex.cdx
 *
 * Note: Does not depend on logging - communicates failures on stderr.
 */
public class ExtractCDX {
    /**
     * Main method. Extracts CDX from all given files and outputs the index
     * on stdout.
     * @param argv A list of (absolute paths to) files to index.
     */
    public static void main(String[] argv) {
        if (argv.length == 0) {
            System.err.println("Missing parameter: "
                    + "Must supply an ARC file to be indexed");
            dieWithUsage();
        }
        List<File> arcFiles = new ArrayList<File>();
        for (String arg : argv) {
           File f = toArcFile(arg);
            arcFiles.add(f);
        }
        File[] arcFileArray = arcFiles.toArray(new File[]{});
        BatchLocalFiles batchRunner = new BatchLocalFiles(arcFileArray);
        batchRunner.run(new ExtractCDXJob(), System.out);
    }

    /**
     * Verifies that the filename (absolute path) points to
     * an existing file and that it is an arc file.
     * @param filename The filename to verify.
     * @return The arc file, as a File.
     */
    private static File toArcFile(String filename) {
        File f;
        try {
            f = FileUtils.makeValidFileFromExisting(filename).getAbsoluteFile();
            if (!FileUtils.ARCS_FILTER.accept(f.getParentFile(), f.getName())) {
                dieWithError("Could not accept " + filename
                        + ": was not an arc file");
            }
            return f;
        } catch (IOFailure e) {
           dieWithError("Could not accept " + filename + ":" + e);
           return null; //Compiler does not recognize System.exit()
        }
    }

    /**
     * Prints out a message on stderr and exits with an error code.
     * @param msg The message to print.
     */
    private static void dieWithError(String msg) {
        System.err.println(msg);
        System.err.println("Exiting - output is not OK");
        System.exit(1);
    }

    /**
     * Prints out proper usage of this tool on stderr and exits
     * with an error code.
     */
    private static void dieWithUsage() {
        System.err.println("Usage: java " + ExtractCDX.class.getName()
                + " file1.arc [file2.arc ...]");
        System.exit(1);
    }
}
