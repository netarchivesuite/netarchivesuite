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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */

package dk.netarkivet.archive.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.HarvesterArcRepositoryClient;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;

/**
 * A tool to force upload of given arc or warc files into the ArcRepository 
 * found in settings.xml.
 * All successfully uploaded files are deleted locally.
 *
 * Usage: java dk.netarkivet.archive.tools.Upload file1 [file2 ...]
 *
 */
public class Upload {
    /**
     * Private constructor, to prevent instantiation of this tool.
     */
    private Upload() { }
    
    /**
     * Main method, uploads given arc files to the ArcRepository.
     * If some file does not exist or is not an arc file, the methods
     * prints out an error and calls System.exit().
     * Successfully uploaded files are deleted locally.
     * If some arc file cannot be uploaded, the method continues to
     * upload the rest of the files and does not delete that file.
     *
     * @param argv A list of absolute paths to the arc files that
     * should be uploaded.
     */
    public static void main(String[] argv) {
        if (argv.length == 0) {
            System.err.println("No files given to upload");
            dieWithUsage();
        }
        List<File> files = checkExistenceAndArcNess(argv);
        //Connect to ArcRepository
        HarvesterArcRepositoryClient arcrep = null;
        try{
            System.out.println("Connecting to ArcRepository");
            arcrep = ArcRepositoryClientFactory.getHarvesterInstance();
            //Upload each input file
            for (File f : files) {
                System.out.println("Uploading file '" + f + "'...");
                boolean success = uploadSingleFile(arcrep, f);
                if(success) {
                    System.out.println("Uploading file '" + f + "' succeeded");
                } else {
                    System.out.println("Uploading file '" + f + "' failed");

                }
            }
            System.out.println(
                    "All files processed, closing connection to ArcRepository");
        } finally {
            //Close connections
            if (arcrep != null) {
                arcrep.close();
            }
            JMSConnectionFactory.getInstance().cleanup();
        }
    }

    /**
     * Checks existence and arcness of all input files.
     * @param fileNames The input files as a String array
     * @return If all files existed and were arc or warc files,
     * a list of Files that is 1-1 with the input files.
     */
    private static List<File> checkExistenceAndArcNess(String[] fileNames) {
        List<File> files = new ArrayList<File>();
        for (String arg : fileNames) {
            try {
            File file = FileUtils.makeValidFileFromExisting(arg);
            if(!FileUtils.ARCS_FILTER.accept(
                    file.getParentFile(), file.getName()) 
              || !FileUtils.WARCS_FILTER.accept(
                      file.getParentFile(), file.getName())) {
                dieWithException("Error checking input file: ",
                        new IOFailure(file.getAbsolutePath()
                                + " is not an arc or warc file"));
            }
            files.add(file);
            } catch (IOFailure e) {
                dieWithException("Error concerning file '" + arg + "':", e);
            }
        }
        return files;
    }

    /**
     * Attempts to upload a given file.
     *
     * @param arcRep The repository to contact
     * @param f The file to upload. Should exist and be an arc file.
     * @return true if the upload succeeded, false otherwise.
     */
    private static boolean uploadSingleFile(
            HarvesterArcRepositoryClient arcRep, File f) {
        boolean success = false;
        try {
            arcRep.store(f);
            success = true;
        } catch (Exception e) {
            System.err.println("Error while storing file: " + e);
            e.printStackTrace(System.err);
        }
        return success;
    }

    /**
     * Output a message and a stack trace before exiting
     * with a failure code.
     * @param msg The message to output
     * @param e The Exception containing the relevant stack trace
     */
    private static void dieWithException(String msg, Exception e) {
        System.err.println(msg + e);
        e.printStackTrace(System.err);
        System.exit(1);
    }
    /**
     * Output proper way to call main() and exit with an error code.
     */
    private static void dieWithUsage() {
        System.err.println("Usage: java " + Upload.class.getName()
                + " files...");
        System.exit(1);
    }
}
