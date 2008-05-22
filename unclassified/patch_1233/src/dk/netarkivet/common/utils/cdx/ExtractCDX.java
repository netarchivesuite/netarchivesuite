/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.common.utils.cdx;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.ExceptionUtils;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.arc.BatchLocalFiles;

/**
 * Utility class for creating CDX-files.
 * The CDX-format is described here: http://www.archive.org/web/researcher/cdx_file_format.php
 *
 */

public class ExtractCDX {
    private static Log log = LogFactory.getLog(ExtractCDX.class.getName());

    /**
     * Add cdx info for a given ARC file to a given OutputStream.
     * Note, any exceptions are logged on level FINE but otherwise ignored.
     *
     * @param arcfile A file with arc records
     * @param cdxstream An output stream to add CDX lines to
     */
    public static void writeCDXInfo(File arcfile, OutputStream cdxstream) {
        ExtractCDXJob job = new ExtractCDXJob();
        BatchLocalFiles runner = new BatchLocalFiles(new File [] {arcfile});
        runner.run(job, cdxstream);
        log.trace("Created index for " + job.noOfRecordsProcessed()
                     + " records on file '" + arcfile + "'");
        Exception[] exceptions = job.getExceptions();
        if (exceptions.length > 0) {
            StringBuilder msg = new StringBuilder();
            for (Exception e : exceptions) {
                msg.append(ExceptionUtils.getStackTrace(e));
                msg.append('\n');
            }
            log.debug("Exceptions during generation of index on file '"
                        + arcfile + "': " + msg.toString());
        }
        log.debug("Created index of " + job.noOfRecordsProcessed()
                    + " records on file '" + arcfile + "'");

    }

    /**
     * Applies createCDXRecord() to all ARC files in a directory, creating
     * one CDX file per ARC file.
     * Note, any exceptions during index generation are logged at level FINE
     * but otherwise ignored.
     * Exceptions creating any cdx file are logged at level WARNING but
     * otherwise ignored.
     * CDX files are named as the arc files except ".arc" or ".arc.gz" is
     * replaced with ".cdx"
     *
     * @param arcFileDirectory A directory with arcfiles to generate index
     * for
     * @param cdxFileDirectory A directory to generate CDX files in
     * @throws ArgumentNotValid if any of directories are null or is not an
     * existing directory, or if cdxFileDirectory is not writable.
     */
    public static void generateCDX(File arcFileDirectory,
                                   File cdxFileDirectory)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(arcFileDirectory,
                                      "File arcFileDirectory");
        ArgumentNotValid.checkNotNull(cdxFileDirectory,
                                      "File cdxFileDirectory");
        if (!arcFileDirectory.isDirectory() || !arcFileDirectory.canRead()) {
            throw new ArgumentNotValid("The directory for arc files '"
                                       + arcFileDirectory
                                       + "' is not a readable directory");
        }
        if (!cdxFileDirectory.isDirectory() || !cdxFileDirectory.canWrite()) {
            throw new ArgumentNotValid("The directory for cdx files '"
                                       + arcFileDirectory
                                       + "' is not a writable directory");
        }
        Map<File, Exception> exceptions
                = new HashMap<File, Exception>();
        for (File arcfile : arcFileDirectory.listFiles(
                FileUtils.ARCS_FILTER)) {
            File cdxfile = new File(cdxFileDirectory, arcfile.getName()
                    .replaceFirst(FileUtils.ARC_PATTERN,
                                  FileUtils.CDX_EXTENSION));
            if (cdxfile.getName().equals(arcfile.getName())) {
                // If for some reason the file is not renamed (should never
                // happen), simply add the .cdx extension to avoid overwriting
                // existing file
                cdxfile = new File(cdxFileDirectory,
                                   cdxfile.getName() + FileUtils.CDX_EXTENSION);
            }
            try {
                OutputStream cdxstream = null;
                try {
                    cdxstream = new FileOutputStream(cdxfile);
                    writeCDXInfo(arcfile, cdxstream);
                } finally {
                    if (cdxstream != null) {
                        cdxstream.close();
                    }
                }
            } catch (Exception e) {
                exceptions.put(cdxfile, e);
            }
        }
        // Log any errors
        if (exceptions.size() > 0) {
            StringBuilder errorMsg = new StringBuilder(
                    "Exceptions during cdx file generation:\n");
            for (Map.Entry<File, Exception> fileException
                    : exceptions.entrySet()) {
                errorMsg.append("Could not create cdxfile '");
                errorMsg.append(fileException.getKey().getAbsolutePath());
                errorMsg.append("':\n");
                errorMsg.append(ExceptionUtils.getStackTrace(
                        fileException.getValue()));
                errorMsg.append('\n');
            }
            log.debug(errorMsg.toString());
        }
    }
}
