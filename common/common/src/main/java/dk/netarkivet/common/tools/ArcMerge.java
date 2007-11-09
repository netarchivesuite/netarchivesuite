/* $Id$
 * $Date$
 * $Revision$
 * $Author$
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
package dk.netarkivet.common.tools;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.arc.ARCUtils;

import org.archive.io.arc.ARCWriter;

import java.io.File;
import java.io.IOException;


/**
 * Command line tool for merging several ARC files into a single ARC file.
 *
 * Usage:
 * java dk.netarkivet.common.tools.ArcMerge file1 [file2] ... > myarchive.arc
 *
 * Note: Does not depend on logging - communicates failure on stderr
 */
public class ArcMerge extends ToolRunnerBase {

    /**
     * Main method. Reads all ARC files specified (as arguments)
     * and outputs a merged ARC file on stdout.
     *
     * @param args The command line arguments should be a list of ARC files
     * to be merged.
     * At least one input ARC file should be given.
     */
    public static void main(String[] args) {
        ArcMerge instance = new ArcMerge();
        instance.runTheTool(args);
    }

    protected SimpleCmdlineTool makeMyTool() {
        return new ArcMergeTool();
    }

    private static class ArcMergeTool implements SimpleCmdlineTool {

        /**
         * This instance is declared outside of run method to ensure reliable
         * teardown in ase of exceptions during execution.
         */
        private ARCWriter aw;

        /**
         * Accept only at least one parameter.
         * @param args the arguments
         * @return false, if length of args is zero; returns true otherwise
         */
        public boolean checkArgs(String... args) {
            return args.length > 0;
        }

        /**
         * Create the ARCWriter instance here for reliable execution of
         * close method in teardown.
         * @param args the arguments (presently not used)
         */
        public void setUp(String... args) {
            try {
                // The name "dummy.arc" is used for the first (file metadata)
                // record
                aw = ARCUtils.getToolsARCWriter(System.out,
                        new File("dummy.arc"));
            } catch (IOException e) {
                throw new IOFailure(e.getMessage());
            }
        }

        /**
         * Ensure reliable execution of the ARCWriter.close() method.
         * Remember to check if aw was actually created.
         */
        public void tearDown() {
            try {
                if (aw != null) {
                    aw.close();
                }
            } catch (IOException e) {
                throw new IOFailure(e.getMessage());
            }
        }

        /**
         * Perform the actual work. Iterate over the input files passed in args
         * (from command line), inert in file and close. Creating and closing
         * the ARCWriter is done in setup and teardown methods.
         * @param args the input files (represented as a String array)
         */
        public void run(String... args) {
            for (String s : args) {
                ARCUtils.insertARCFile(
                        FileUtils.makeValidFileFromExisting(s),
                        aw);
            }
        }

        /**
         * Return the list of parameters accepted by the ArcMergeTool class.
         * @return the list of parameters accepted.
         */
        public String listParameters() {
            return "file1 [file2] ...";
        }
    }
}
