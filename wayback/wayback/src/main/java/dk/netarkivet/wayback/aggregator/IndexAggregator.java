/* File:   $Id$
 * Revision: $Revision$
 * Author:   $Author$
 * Date:     $Date$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *  USA
 */

package dk.netarkivet.wayback.aggregator;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.wayback.WaybackSettings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Encapsulates the functionality for sorting and merging index files. Uses the
 * Unix sort cmd for optimized sorting and file merging. Operations in this
 * class are synchronized to avoid multiple jobs running at the same time (by
 * the same object at least).
 */
public class IndexAggregator {
    /** The logger for this class. */
    private Log log = LogFactory.getLog(getClass().getName());

    /**
     * Generates a sorted CDX index file based on the set of unsorted CDX input
     * files.<p> The operation will not run on a folder which already has a
     * process job running.
     *
     * @param files      A list of the files to aggregate
     * @param outputFile Name of the outputfile. In case of a empty filesNames
     *                   array no outputFiles will be generated
     */
    public void sortAndMergeFiles(File[] files, File outputFile) {
        processFiles(files, outputFile, null);
    }

    /**
     * Takes a list of sorted files and merges them.
     * @param files The files to merge.
     * @param outputFile The resulting file containing total sorted set of index lines found in all the provided index files                 
     */

    public void mergeFiles(File[] files, File outputFile) {
        List<String> args = new LinkedList<String>();
        args.add("-m");
        processFiles(files, outputFile, args);
    }

    /**
     * Calls the Unix sort command with the options <code>$filesNames -o
     * $outputfile -T WaybackSettings#WAYBACK_AGGREGATOR_TEMP_DIR.
     *
     * Sets the LC_ALL environment variable before making the call.
     *
     * @param files The files to merge and sort
     * @param outputFile The resulting sorted file
     * @param additionalArgs A list af extra arguments, which (if different from
     *                       null) are added to the sort call.<p> Note: If any
     *                       of the args contain a whitespace the call will
     *                       fail.
     */
    private void processFiles(File[] files, File outputFile,
                              List<String> additionalArgs) {
        if (files.length == 0) {
            return;
        } // Empty file list will cause sort to wait for further input, and the call will therefor never return

        Process p = null;

        try {
            List<String> inputFileList = new LinkedList<String>();
            for (int i = 0; i < files.length; i++) {
                if (files[i].exists() && files[i].isFile()) {
                    inputFileList.add(files[i].getCanonicalPath());
                } else {
                    log.warn("File "+files[i] +" doesn't exist or isn't a regular file, dropping from list of files to "
                             + "sort and merge");
                }
            }
            List<String> cmd = new LinkedList<String>();
            // Prepare to run the unix sort command, see sort manual page for details
            cmd.add("sort");
            cmd.addAll(inputFileList);
            cmd.add("-o");
            cmd.add(outputFile.getCanonicalPath());
            cmd.add("-T");
            cmd.add(Settings.get(WaybackSettings.WAYBACK_AGGREGATOR_TEMP_DIR));
            if (additionalArgs != null && !additionalArgs.isEmpty()) {
                for (String argument:additionalArgs) {
                    ArgumentNotValid.checkTrue(argument.indexOf(' ') == -1,
                                               "The argument '"+argument+"' contains spaces, this isn't allowed ");
                }
                cmd.addAll(additionalArgs);
            }
            ProcessBuilder pb = new ProcessBuilder(cmd);
            // Reset all locale definitions
            pb.environment().put("LC_ALL", "C");
            // Run the command in the user.dir directory
            pb.directory(new File(System.getProperty("user.dir")));
            p = pb.start();
            p.waitFor();
            if (p.exitValue() != 0) {
                log.error(
                        "Failed to sort index files, sort exited with return code " + p.exitValue());
            }
        } catch (Exception e) {
            log.error("Failed to aggregate indexes ", e);
        }
    }
}
