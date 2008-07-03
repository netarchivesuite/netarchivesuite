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

package dk.netarkivet.common.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Utilities for interfacing with the (fairly low-level) java.util.zip
 * package
 *
 */
public class ZipUtils {
    private static Log log = LogFactory.getLog(ZipUtils.class.getName());
    /** The standard suffix for a gzipped file. */
    public static final String GZIP_SUFFIX = ".gz";

    /** Zip the contents of a directory into a file.
     *  Does *not* zip recursively.
     *
     * @param dir The directory to zip.
     * @param into The (zip) file to create.  The name should typically end
     * in .zip, but that is not required.
     */
    public static void zipDirectory(File dir, File into) {
        ArgumentNotValid.checkNotNull(dir, "File dir");
        ArgumentNotValid.checkNotNull(into, "File into");
        ArgumentNotValid.checkTrue(dir.isDirectory(),
                "directory '" + dir + "' to zip is not a directory");
        ArgumentNotValid.checkTrue
                (into.getAbsoluteFile().getParentFile().canWrite(),
                "cannot write to '" + into + "'");

        File[] files = dir.listFiles();
        FileOutputStream out;
        try {
            out = new FileOutputStream(into);
        } catch (IOException e) {
            throw new IOFailure("Error creating ZIP outfile file '"
                    + into + "'", e);
        }
        ZipOutputStream zipout = new ZipOutputStream(out);
        try {
            try {
                for (File f: files) {
                    if (f.isFile()) {
                        ZipEntry entry = new ZipEntry(f.getName());
                        zipout.putNextEntry(entry);
                        FileUtils.writeFileToStream(f, zipout);
                    } // Not doing directories yet.
                }
            } finally {
                zipout.close();
            }
        } catch (IOException e) {
            throw new IOFailure("Failed to zip directory '" + dir + "'", e);
        }
    }

    /** Unzip a zipFile into a directory.  This will create subdirectories
     * as needed.
     *
     * @param zipFile The file to unzip
     * @param toDir The directory to create the files under.  This directory
     * will be created if necessary.  Files in it will be overwritten if the
     * filenames match.
     */
    public static void unzip(File zipFile, File toDir) {
        ArgumentNotValid.checkNotNull(zipFile, "File zipFile");
        ArgumentNotValid.checkNotNull(toDir, "File toDir");
        ArgumentNotValid.checkTrue
                (toDir.getAbsoluteFile().getParentFile().canWrite(),
                "can't write to '" + toDir + "'");
        ArgumentNotValid.checkTrue(zipFile.canRead(),
                "can't read '" + zipFile + "'");
        InputStream inputStream = null;
        ZipFile unzipper = null;
        try {
            try {
                unzipper = new ZipFile(zipFile);
                Enumeration<? extends ZipEntry> entries = unzipper.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry ze = entries.nextElement();
                    File target = new File(toDir, ze.getName());
                    // Ensure that its dir exists
                    FileUtils.createDir
                            (target.getCanonicalFile().getParentFile());
                    if (ze.isDirectory()) {
                        target.mkdir();
                    } else {
                        inputStream = unzipper.getInputStream(ze);
                        FileUtils.writeStreamToFile(inputStream, target);
                        inputStream.close();
                    }
                }
            } finally {
                if (unzipper != null) {
                    unzipper.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Failed to unzip '" + zipFile + "'", e);
        }
    }

    /** GZip each of the files in fromDir, placing the result in toDir
     * (which will be created) with names having .gz appended.  All non-file
     * (directory, link, etc) entries in the source directory will be
     * skipped with a quiet little log message.
     *
     * @param fromDir An existing directory
     * @param toDir A directory where gzipped files will be placed.  This
     * directory must not previously exist.
     *   If the operation is not successfull, the directory will not be created.
     */
    public static void gzipFiles(File fromDir, File toDir) {
        ArgumentNotValid.checkNotNull(fromDir, "File fromDir");
        ArgumentNotValid.checkNotNull(toDir, "File toDir");
        ArgumentNotValid.checkTrue(fromDir.isDirectory(),
                "source '" + fromDir + "' must be an existing directory");
        ArgumentNotValid.checkTrue(!toDir.exists(),
                "destination directory '" + toDir + "' must not exist");

        File tmpDir = null;
        try {
            tmpDir = FileUtils.createUniqueTempDir(
                    toDir.getAbsoluteFile().getParentFile(),
                    toDir.getName());
            File[] fromFiles = fromDir.listFiles();
            for (File f : fromFiles) {
                if (f.isFile()) {
                    gzipFileInto(f, tmpDir);
                } else {
                    log.trace("Skipping non-file '" + f + "'");
                }
            }
            if (!tmpDir.renameTo(toDir)) {
                throw new IOFailure("Failed to rename temp dir '" + tmpDir
                        + "' to desired target '" + toDir + "'");
            }
        } finally {
            if (tmpDir != null) {
                try {
                    FileUtils.removeRecursively(tmpDir);
                } catch (IOFailure e) {
                    log.debug("Error removing temporary"
                            + " directory '" + tmpDir
                            + "' after gzipping of '" + toDir + "'", e);
                }
            }
        }
    }

    /** GZip a file into a given dir.  The resulting file will have .gz
     * appended.
     *
     * @param f A file to gzip.  This must be a real file, not a directory
     * or the like.
     * @param toDir The directory that the gzipped file will be placed in.
     */
    private static void gzipFileInto(File f, File toDir) {
        try {
            GZIPOutputStream out = null;
            try {
                File outF = new File(toDir, f.getName() + GZIP_SUFFIX);
                out = new GZIPOutputStream(new FileOutputStream(outF));
                FileUtils.writeFileToStream(f, out);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        // Not really a problem to not be able to close,
                        // so don't abort
                        log.debug(
                                "Error closing output file for '"
                                        + f + "'", e);
                    }
                }
            }
        } catch (IOException e) {
            throw new IOFailure("Error while gzipping file '" + f
                    + "'", e);
        }
    }

    /** Gunzip all .gz files in a given directory into another.  Files in
     * fromDir not ending in .gz or not real files will be skipped with a
     * log entry.
     *
     * @param fromDir The directory containing .gz files
     * @param toDir The directory to place the unzipped files in.  This
     * directory must not exist beforehand.
     * @throws IOFailure if there are problems creating the output directory
     * or gunzipping the files.
     */
    public static void gunzipFiles(File fromDir, File toDir) {
        ArgumentNotValid.checkNotNull(fromDir, "File fromDir");
        ArgumentNotValid.checkNotNull(toDir, "File toDir");
        ArgumentNotValid.checkTrue(fromDir.isDirectory(), "source directory '"
                + fromDir + "' must exist");
        ArgumentNotValid.checkTrue(!toDir.exists(), "destination directory '"
                + toDir + "' must not exist");
        File tempDir = FileUtils.createUniqueTempDir(
                toDir.getAbsoluteFile().getParentFile(),
                toDir.getName());
        try {
            File[] gzippedFiles = fromDir.listFiles();
            for (File f : gzippedFiles) {
                if (f.isFile() && f.getName().endsWith(GZIP_SUFFIX)) {
                    gunzipInto(f, tempDir);
                } else {
                    log.trace("Non-gzip file '" + f + "' found in gzip dir");
                }
            }
            if (!tempDir.renameTo(toDir)) {
                throw new IOFailure("Error renaming temporary directory '"
                        + tempDir + "' to target directory '" + toDir);
            }
        } finally {
            FileUtils.removeRecursively(tempDir);
        }
    }

    /** Gunzip a single file into a directory.  Unlike with the gzip()
     * command-line tool, the original file is not deleted.
     *
     * @param f The .gz file to unzip.
     * @param toDir The directory to gunzip into.  This directory must exist.
     * @throws IOFailure if there are any problems gunzipping.
     */
    private static void gunzipInto(File f, File toDir) {
        String fileName = f.getName();
        File outFile = new File(toDir,
                                fileName.substring(0, fileName.length()
                                                      - GZIP_SUFFIX.length()));
        gunzipFile(f, outFile);
    }

    /** Gunzip a single gzipped file into the given file. Unlike with the gzip()
     * command-line tool, the original file is not deleted.
     *
     * @param fromFile A gzipped file to unzip.
     * @param toFile The file that the contents of fromFile should be gunzipped
     * into.  This file must be in an existing directory.  Existing contents of
     * this file will be overwritten.
     */
    public static void gunzipFile(File fromFile, File toFile) {
        ArgumentNotValid.checkNotNull(fromFile, "File fromFile");
        ArgumentNotValid.checkTrue(fromFile.canRead(),
                                   "fromFile must be readable");
        ArgumentNotValid.checkNotNull(toFile, "File toFile");
        ArgumentNotValid.checkTrue(
                toFile.getAbsoluteFile().getParentFile().canWrite(),
                "toFile must be in a writeable dir");
        try {
            GZIPInputStream in = new LargeFileGZIPInputStream(
                    new FileInputStream(fromFile));
            FileUtils.writeStreamToFile(in, toFile);
        } catch (IOException e) {
            throw new IOFailure("Error ungzipping '"
                    + fromFile + "'", e);
        }
    }
}
