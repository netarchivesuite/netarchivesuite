/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * Misc. handy file utilities.
 */
public class FileUtils {

    /** The logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(FileUtils.class);

    /** Extension used for CDX files, including separator . */
    public static final String CDX_EXTENSION = ".cdx";

    /** Extension used for ARC files, including separator . */
    public static final String ARC_EXTENSION = ".arc";

    /** Extension used for gzipped ARC files, including separator . */
    public static final String ARC_GZIPPED_EXTENSION = ".arc.gz";

    /** Extension used for WARC files, including separator . */
    public static final String WARC_EXTENSION = ".warc";

    /** Extension used for gzipped WARC files, including separator . */
    public static final String WARC_GZIPPED_EXTENSION = ".warc.gz";

    /**
     * Pattern matching ARC files, including separator. Note: (?i) means case insensitive, (\\.gz)? means .gz is
     * optionally matched, and $ means matches end-of-line. Thus this pattern will match file.arc.gz, file.ARC,
     * file.aRc.GZ, but not file.ARC.open
     */
    public static final String ARC_PATTERN = "(?i)\\.arc(\\.gz)?$";

    /**
     * Pattern matching open ARC files, including separator . Note: (?i) means case insensitive, (\\.gz)? means .gz is
     * optionally matched, and $ means matches end-of-line. Thus this pattern will match file.arc.gz.open,
     * file.ARC.open, file.arc.GZ.OpEn, but not file.ARC.open.txt
     */
    public static final String OPEN_ARC_PATTERN = "(?i)\\.arc(\\.gz)?\\.open$";

    /**
     * Pattern matching WARC files, including separator. Note: (?i) means case insensitive, (\\.gz)? means .gz is
     * optionally matched, and $ means matches end-of-line. Thus this pattern will match file.warc.gz, file.WARC,
     * file.WaRc.GZ, but not file.WARC.open
     */
    public static final String WARC_PATTERN = "(?i)\\.warc(\\.gz)?$";

    /**
     * Pattern matching open WARC files, including separator . Note: (?i) means case insensitive, (\\.gz)? means .gz is
     * optionally matched, and $ means matches end-of-line. Thus this pattern will match file.warc.gz.open,
     * file.WARC.open, file.warc.GZ.OpEn, but not file.wARC.open.txt
     */
    public static final String OPEN_WARC_PATTERN = "(?i)\\.warc(\\.gz)?\\.open$";

    /**
     * Pattern matching WARC and ARC files, including separator. Note: (?i) means case insensitive, (\\.gz)? means .gz
     * is optionally matched, and $ means matches end-of-line. Thus this pattern will match file.warc.gz, file.WARC,
     * file.WaRc.GZ, file.arc.gz, file.ARC, file.aRc.GZ but not file.WARC.open or file.ARC.open
     */
    public static final String WARC_ARC_PATTERN = "(?i)\\.(w)?arc(\\.gz)?$";

    /**
     * A FilenameFilter accepting a file if and only if its name (transformed to lower case) ends on ".cdx".
     */
    public static final FilenameFilter CDX_FILE_FILTER = new FilenameFilter() {
        public boolean accept(File directory, String filename) {
            return filename.toLowerCase().endsWith(CDX_EXTENSION);
        }
    };

    /**
     * A filter that matches files left open by a crashed Heritrix process. Don't work on these files while Heritrix is
     * still working on them.
     */
    public static final FilenameFilter OPEN_ARCS_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.matches(".*" + OPEN_ARC_PATTERN);
        }
    };

    /**
     * A filter that matches warcfiles left open by a crashed Heritrix process. Don't work on these files while Heritrix
     * is still working on them.
     */
    public static final FilenameFilter OPEN_WARCS_FILTER = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.matches(".*" + OPEN_WARC_PATTERN);
        }
    };

    /**
     * A filter that matches arc files, that is any file that ends on .arc or .arc.gz in any case.
     */
    public static final FilenameFilter ARCS_FILTER = new FilenameFilter() {
        public boolean accept(File directory, String filename) {
            return filename.toLowerCase().matches(".*" + ARC_PATTERN);
        }
    };

    /**
     * A filter that matches warc files, that is any file that ends on .warc or .warc.gz in any case.
     */
    public static final FilenameFilter WARCS_FILTER = new FilenameFilter() {
        public boolean accept(File directory, String filename) {
            return filename.toLowerCase().matches(".*" + WARC_PATTERN);
        }
    };

    /**
     * A filter that matches warc and arc files, that is any file that ends on .warc, .warc.gz, .arc or .arc.gz in any
     * case.
     */
    public static final FilenameFilter WARCS_ARCS_FILTER = new FilenameFilter() {
        public boolean accept(File directory, String filename) {
            return filename.toLowerCase().matches(".*" + WARC_ARC_PATTERN);
        }
    };

    /** How many times we will retry making a unique directory name. */
    private static final int MAX_RETRIES = 10;

    /** How many times we will retry making a directory. */
    private static final int CREATE_DIR_RETRIES = 3;
    /**
     * Maximum number of IDs we will put in a filename. Above this number, a checksum of the ids is generated instead.
     * This is done to protect us from getting filenames too long for the filesystem.
     */
    public static final int MAX_IDS_IN_FILENAME = 4;

    /**
     * Remove a file and any subfiles in case of directories.
     *
     * @param f A file to completely and utterly remove.
     * @return true if the file did exist, false otherwise.
     * @throws SecurityException If a security manager exists and its <code>{@link
     * java.lang.SecurityManager#checkDelete}</code> method denies delete access to the file
     */
    public static boolean removeRecursively(File f) {
        ArgumentNotValid.checkNotNull(f, "File f");
        if (!f.exists()) {
            return false;
        }

        // If the file is a directory, delete all files in this directory,
        // and its subdirectories
        if (f.isDirectory()) {
            File[] subfiles = f.listFiles();

            if (subfiles != null) { // Can be null in case of error
                for (File subfile : subfiles) {
                    removeRecursively(subfile);
                }
            }
        }
        if (!f.delete()) {
            boolean isDir = f.isDirectory();
            if (!isDir) {
                log.debug("Try once more deleting file '{}", f.getAbsolutePath());
                final boolean success = remove(f);
                if (!success) {
                    log.warn("Unable to remove file: '{}'", f.getAbsolutePath());
                    return false;
                }
            } else {
                log.warn("Problem with deletion of directory: '{}'.", f.getAbsolutePath());
                return false;
            }
        }

        return true;
    }

    /**
     * Remove a file.
     *
     * @param f A file to completely and utterly remove.
     * @return true if the file did exist, false otherwise.
     * @throws ArgumentNotValid if f is null.
     * @throws SecurityException If a security manager exists and its <code>{@link
     * java.lang.SecurityManager#checkDelete}</code> method denies delete access to the file
     */
    public static boolean remove(File f) {
        ArgumentNotValid.checkNotNull(f, "f");
        if (!f.exists()) {
            return false;
        }
        if (f.isDirectory()) {
            return false; // Do not attempt to delete a directory
        }
        if (!f.delete()) {
            // Hack to remove file on windows! Works only sometimes!
            File delFile = new File(f.getAbsolutePath());
            delFile.delete();
            if (delFile.exists()) {
                log.warn("Unable to remove file '{}'.", f.getAbsolutePath());
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a valid filename for most filesystems. Exchanges the following characters:
     * <p/>
     * " " -> "_" ":" -> "_" "+" -> "_"
     *
     * @param filename the filename to format correctly
     * @return a new formatted filename
     */
    public static String formatFilename(String filename) {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        String formattedFilename = filename;

        // remove spaces
        formattedFilename = formattedFilename.replace(' ', '_');

        // remove colons
        formattedFilename = formattedFilename.replace(':', '_');

        // remove add sign
        formattedFilename = formattedFilename.replace('+', '_');

        return formattedFilename;
    }

    /**
     * Retrieves all files whose names ends with 'type' from directory 'dir' and all its subdirectories.
     *
     * @param dir Path of base directory
     * @param files Initially, an empty list (e.g. an ArrayList)
     * @param type The extension/ending of the files to retrieve (e.g. ".xml", ".ARC")
     * @return A list of files from directory 'dir' and all its subdirectories
     */
    public static List<File> getFilesRecursively(String dir, List<File> files, String type) {
        ArgumentNotValid.checkNotNullOrEmpty(dir, "String dir");
        File theDirectory = new File(dir);
        ArgumentNotValid.checkTrue(theDirectory.isDirectory(), "File '" + theDirectory.getAbsolutePath()
                + "' does not represent a directory");
        ArgumentNotValid.checkNotNull(files, "files");
        ArgumentNotValid.checkNotNull(type, "type");

        File[] top = new File(dir).listFiles();
        for (File aTop : top) {
            if (aTop.isDirectory()) {
                getFilesRecursively(aTop.getAbsolutePath(), files, type);
            } else if (aTop.isFile() && aTop.getName().endsWith(type)) {
                files.add(aTop);
            }
        }

        return files;
    }

    /**
     * Load file content into text string.
     *
     * @param file The file to load
     * @return file content loaded into text string
     * @throws java.io.IOException If any IO trouble occurs while reading the file, or the file cannot be found.
     */
    public static String readFile(File file) throws IOException {
        ArgumentNotValid.checkNotNull(file, "File file");
        StringBuffer sb = new StringBuffer();

        BufferedReader br = new BufferedReader(new FileReader(file));

        try {
            int i;

            while ((i = br.read()) != -1) {
                sb.append((char) i);
            }
        } finally {
            br.close();
        }

        return sb.toString();
    }

    /**
     * Copy file from one location to another. Will silently overwrite an already existing file.
     *
     * @param from original to copy
     * @param to destination of copy
     * @throws IOFailure if an io error occurs while copying file, or the original file does not exist.
     */
    public static void copyFile(File from, File to) {
        ArgumentNotValid.checkNotNull(from, "File from");
        ArgumentNotValid.checkNotNull(to, "File to");
        if (!from.exists()) {
            String errMsg = "Original file '" + from.getAbsolutePath() + "' does not exist";
            log.warn(errMsg);
            throw new IOFailure(errMsg);
        }
        try {
            FileInputStream inStream = null;
            FileOutputStream outStream = null;
            FileChannel in = null;
            FileChannel out = null;
            try {
                inStream = new FileInputStream(from);
                outStream = new FileOutputStream(to);
                in = inStream.getChannel();
                out = outStream.getChannel();
                long bytesTransferred = 0;
                do {
                    // Note: in.size() is called every loop, because if it should
                    // change size, we might end up in an infinite loop trying to
                    // copy more bytes than are actually available.
                    bytesTransferred += in.transferTo(bytesTransferred,
                            Math.min(Constants.IO_CHUNK_SIZE, in.size() - bytesTransferred), out);
                } while (bytesTransferred < in.size());
            } finally {
                if (inStream != null) {
                    inStream.close();
                }
                if (outStream != null) {
                    outStream.close();
                }
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            }
        } catch (IOException e) {
            final String errMsg = "Error copying file '" + from.getAbsolutePath() + "' to '" + to.getAbsolutePath()
                    + "'";
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);
        }
    }

    /**
     * Copy an entire directory from one location to another. Note that this will silently overwrite old files, just
     * like copyFile().
     *
     * @param from Original directory (or file, for that matter) to copy.
     * @param to Destination directory, i.e. the 'new name' of the copy of the from directory.
     * @throws IOFailure On IO trouble copying files.
     */
    public static void copyDirectory(File from, File to) throws IOFailure {
        ArgumentNotValid.checkNotNull(from, "File from");
        ArgumentNotValid.checkNotNull(to, "File to");
        String errMsg;
        if (from.isFile()) {
            try {
                copyFile(from, to);
            } catch (Exception e) {
                errMsg = "Error copying from file '" + from.getAbsolutePath() + "' to file '" + to.getAbsolutePath()
                        + "'.";
                log.warn(errMsg, e);
                throw new IOFailure(errMsg, e);
            }
        } else {
            if (!from.exists()) {
                errMsg = "Can't find directory '" + from.getAbsolutePath() + "'.";
                log.warn(errMsg);
                throw new IOFailure(errMsg);
            }

            if (!from.isDirectory()) {
                errMsg = "File '" + from.getAbsolutePath() + "' is not a directory";
                log.warn(errMsg);
                throw new IOFailure(errMsg);
            }

            to.mkdir();

            if (!to.exists()) {
                errMsg = "Failed to create destination directory '" + to.getAbsolutePath() + "'.";
                log.warn(errMsg);
                throw new IOFailure(errMsg);
            }

            File[] subfiles = from.listFiles();

            for (File subfile : subfiles) {
                copyDirectory(subfile, new File(to, subfile.getName()));
            }
        }
    }

    /**
     * Read an entire file, byte by byte, into a byte array, ignoring any locale issues.
     *
     * @param file A file to be read.
     * @return A byte array with the contents of the file.
     * @throws IOFailure on IO trouble reading the file, or the file does not exist
     * @throws IndexOutOfBoundsException If the file is too large to be in an array.
     */
    public static byte[] readBinaryFile(File file) throws IOFailure, IndexOutOfBoundsException {
        ArgumentNotValid.checkNotNull(file, "File file");
        if (!file.exists()) {
            String errMsg = "File '" + file.getAbsolutePath() + "' does not exist";
            log.warn(errMsg);
            throw new IOFailure(errMsg);
        }

        String errMsg;
        if (file.length() > Integer.MAX_VALUE) {
            errMsg = "File '" + file.getAbsolutePath() + "' of size " + file.length()
                    + " (bytes) is too long to fit in an array";
            log.warn(errMsg);
            throw new IndexOutOfBoundsException(errMsg);
        }

        byte[] result = new byte[(int) file.length()];
        FileInputStream in = null;
        try {
            try {
                in = new FileInputStream(file);
                int bytesRead;
                for (int i = 0; i < result.length && (bytesRead = in.read(result, i, result.length - i)) != -1; i += bytesRead) {
                }
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            errMsg = "Error reading file '" + file.getAbsolutePath() + "'";
            log.warn(errMsg);
            throw new IOFailure(errMsg, e);
        }

        return result;
    }

    /**
     * Write an entire byte array to a file, ignoring any locale issues.
     *
     * @param file The file to write the data to
     * @param b The byte array to write to the file
     * @throws IOFailure If an exception occurs during the writing.
     */
    public static void writeBinaryFile(File file, byte[] b) {
        ArgumentNotValid.checkNotNull(file, "File file");
        ArgumentNotValid.checkNotNull(b, "byte[] b");
        FileOutputStream out = null;
        try {
            try {
                out = new FileOutputStream(file);
                out.write(b);
            } finally {
                if (out != null) {
                    out.close();
                }
            }
        } catch (Exception e) {
            final String errMsg = "writeBinaryFile exception";
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);
        }
    }

    /**
     * Return a filter that only accepts XML files (ending with .xml), irrespective of their location.
     *
     * @return A new filter for XML files.
     */
    public static FilenameFilter getXmlFilesFilter() {
        return new FilenameFilter() {
            /**
             * Tests if a specified file should be included in a file list.
             *
             * @param dir the directory in which the file was found. Unused in this implementation of accept.
             * @param name the name of the file.
             * @return <code>true</code> if and only if the name should be included in the file list; <code>false</code>
             * otherwise.
             * @see FilenameFilter#accept(java.io.File, java.lang.String)
             */
            public boolean accept(File dir, String name) {
                return name.endsWith(Constants.XML_EXTENSION);
            }
        };
    }

    /**
     * Read all lines from a file into a list of strings.
     *
     * @param file The file to read from.
     * @return The list of lines.
     * @throws IOFailure on trouble reading the file, or if the file does not exist
     */
    public static List<String> readListFromFile(File file) {
        ArgumentNotValid.checkNotNull(file, "File file");
        if (!file.exists()) {
            String errMsg = "File '" + file.getAbsolutePath() + "' does not exist";
            log.warn(errMsg);
            throw new IOFailure(errMsg);
        }
        List<String> lines = new ArrayList<String>();
        BufferedReader in = null;
        try {
            try {
                in = new BufferedReader(new FileReader(file));
                String line;
                while ((line = in.readLine()) != null) {
                    lines.add(line);
                }
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            String msg = "Could not read data from " + file.getAbsolutePath();
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
        return lines;
    }

    /**
     * Writes a collection of strings to a file, each string on one line.
     *
     * @param file A file to write to. The contents of this file will be overwritten.
     * @param collection The collection to write. The order it will be written in is unspecified.
     * @throws IOFailure if any error occurs writing to the file.
     * @throws ArgumentNotValid if file or collection is null.
     */
    public static void writeCollectionToFile(File file, Collection<String> collection) {
        ArgumentNotValid.checkNotNull(file, "file");
        ArgumentNotValid.checkNotNull(collection, "collection");
        try {
            PrintWriter writer = null;
            try {
                writer = new PrintWriter(new FileWriter(file));
                for (String fileName : collection) {
                    writer.println(fileName);
                }
                writer.flush();
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        } catch (IOException e) {
            String msg = "Error writing collection to file '" + file.getAbsolutePath() + "'";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
    }

    /**
     * Sort a file into another. The current implementation slurps all lines into memory. This will not scale forever.
     *
     * @param unsortedFile A file to sort
     * @param sortedOutput The file to sort into
     */
    public static void makeSortedFile(File unsortedFile, File sortedOutput) {
        ArgumentNotValid.checkNotNull(unsortedFile, "File unsortedFile");
        ArgumentNotValid.checkNotNull(sortedOutput, "File sortedOutput");
        List<String> lines;
        lines = readListFromFile(unsortedFile);
        Collections.sort(lines);
        writeCollectionToFile(sortedOutput, lines);
    }

    /**
     * Remove a line from a given file.
     *
     * @param line The full line to remove
     * @param file The file to remove the line from. This file will be rewritten in full, and the entire contents will
     * be kept in memory
     * @throws UnknownID If the file does not exist
     */
    public static void removeLineFromFile(String line, File file) {
        ArgumentNotValid.checkNotNull(line, "String line");
        ArgumentNotValid.checkNotNull(file, "File file");
        if (!file.exists()) {
            String errMsg = "The file '" + file.getAbsolutePath() + "' does not exist.";
            log.warn(errMsg);
            throw new UnknownID(errMsg);
        }

        List<String> lines = readListFromFile(file);
        lines.remove(line);
        writeCollectionToFile(file, lines);
    }

    /**
     * Check if the directory exists, and create it if needed. The complete path down to the directory is
     * created. If the directory creation fails a PermissionDenied exception is thrown.
     * If the directory is not writable, a warning is logged
     *
     * @param dir The directory to create
     * @return true if dir created.
     * @throws ArgumentNotValid If dir is null or its name is the empty string
     * @throws PermissionDenied If directory cannot be created for any reason
     */
    public static boolean createDir(File dir) throws PermissionDenied {
        ArgumentNotValid.checkNotNull(dir, "File dir");
        ArgumentNotValid.checkNotNullOrEmpty(dir.getName(), "File dir");
        boolean didCreate = false;
        if (!dir.exists()) {
            didCreate = true;
            int i = 0;
            // retrying creation due to sun bug (race condition)
            // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4742723
            while ((i++ < CREATE_DIR_RETRIES) && !(dir.isDirectory() && dir.canWrite())) {
                dir.mkdirs();
            }
            if (!(dir.isDirectory() && dir.canWrite())) {
                String msg = "Could not create directory '" + dir.getAbsolutePath() + "'";
                log.warn(msg);
                throw new PermissionDenied(msg);
            }
        } else {
            if (!dir.isDirectory()) {
                String msg = "Cannot make directory '" + dir.getAbsolutePath() + "' - a file is in the way";
                log.warn(msg);
                throw new PermissionDenied(msg);
            }
        }
        if (!dir.canWrite()) {
            String msg = "Cannot write to required directory '" + dir.getAbsolutePath() + "'";
            log.warn(msg);
        }
        return didCreate;
    }

    /**
     * Returns the number of bytes free on the file system calling the FreeSpaceProvider class defined by the setting
     * CommonSettings.FREESPACE_PROVIDER_CLASS (a.k.a. settings.common.freespaceprovider.class)
     *
     * @param f a given file
     * @return the number of bytes free defined in the settings.xml
     */
    public static long getBytesFree(File f) {
        return FreeSpaceProviderFactory.getInstance().getBytesFree(f);
    }

    /**
     * @param theFile A file to make relative
     * @param theDir A directory
     * @return the filepath of the theFile relative to theDir. null, if theFile is not relative to theDir. null, if
     * theDir is not a directory.
     */
    public static String relativeTo(File theFile, File theDir) {
        ArgumentNotValid.checkNotNull(theFile, "File theFile");
        ArgumentNotValid.checkNotNull(theDir, "File theDir");
        if (!theDir.isDirectory()) {
            log.trace("The File '{}' does not represent a directory. Null returned", theDir.getAbsolutePath());
            return null;
        }

        List<String> filePathList = new ArrayList<String>();
        List<String> theDirPath = new ArrayList<String>();
        File tempFile = theFile.getAbsoluteFile();

        filePathList.add(tempFile.getName());
        while ((tempFile = tempFile.getParentFile()) != null) {
            filePathList.add(tempFile.getName());
        }

        tempFile = theDir.getAbsoluteFile();
        theDirPath.add(tempFile.getName());
        while ((tempFile = tempFile.getParentFile()) != null) {
            theDirPath.add(tempFile.getName());
        }

        // check, at the path prefix is the same
        List<String> sublist = filePathList.subList(theDirPath.size() - 2, filePathList.size());
        if (!theDirPath.equals(sublist)) {
            log.trace("The file '{}' is not relative to the directory '{}'. Null returned", theFile.getAbsolutePath(),
                    theDir.getAbsolutePath());
            return null;
        }

        List<String> relativeList = filePathList.subList(0, theDirPath.size() - 2);

        StringBuffer sb = new StringBuffer();
        Collections.reverse(relativeList);
        for (String aRelativeList : relativeList) {
            sb.append(aRelativeList);
            sb.append(File.separatorChar);
        }
        sb.deleteCharAt(sb.length() - 1); // remove last separatorChar
        return sb.toString();
    }

    /**
     * Count the number of lines in a file.
     *
     * @param file the file to read
     * @return the number of lines in the file
     * @throws IOFailure If an error occurred while reading the file
     */
    public static long countLines(File file) {
        ArgumentNotValid.checkNotNull(file, "file");
        BufferedReader in = null;
        long count = 0;
        try {
            try {
                in = new BufferedReader(new FileReader(file));
                while (in.readLine() != null) {
                    ++count;
                }
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            String msg = "Could not check number of lines in '" + file.getAbsolutePath() + "'";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
        return count;
    }

    /**
     * Create an InputStream that reads from a file but removes the file when all data has been read.
     *
     * @param file A file to read. This file will be deleted when the inputstream is closed, finalized, reaches
     * end-of-file, or when the VM closes.
     * @return An InputStream containing the file's contents.
     * @throws IOFailure If an error occurs in creating the ephemeral input stream
     */
    public static InputStream getEphemeralInputStream(final File file) {
        ArgumentNotValid.checkNotNull(file, "file");
        // First make sure we remove the file if the VM dies
        file.deleteOnExit();
        try {
            // Then create an input stream that deletes the file upon exit.
            // Note that FileInputStream.finalize calls close().
            return new FileInputStream(file) {
                public void close() throws IOException {
                    super.close();
                    file.delete();
                }
            };
        } catch (IOException e) {
            String msg = "Error creating ephemeral input stream for " + file;
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
    }

    /**
     * Makes a valid file from filename passed in String. Ensures that the File object returned is not null, and that
     * isFile() returns true.
     *
     * @param filename The file to create the File object from
     * @return A valid, non-null File object.
     * @throws IOFailure if file cannot be created.
     */
    public static File makeValidFileFromExisting(String filename) throws IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");

        File res = new File(filename);
        if (!res.isFile()) {
            String errMsg = "Error: File object created from filename '" + filename
                    + "' is not a proper file, isFile() failed.";
            log.warn(errMsg);
            throw new IOFailure(errMsg);
        }
        return res;
    }

    /**
     * Write the entire contents of a file to a stream.
     *
     * @param f A file to write to the stream.
     * @param out The stream to write to.
     * @throws IOFailure If any error occurs while writing the file to a stream
     */
    public static void writeFileToStream(File f, OutputStream out) {
        ArgumentNotValid.checkNotNull(f, "File f");
        ArgumentNotValid.checkNotNull(out, "OutputStream out");

        byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
        try {
            FileInputStream in = new FileInputStream(f);
            try {
                int bytesRead;
                while ((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                in.close();
            }
        } catch (IOException e) {
            final String errMsg = "Error writing file '" + f.getAbsolutePath() + "' to stream";
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);
        }
    }

    /**
     * Write the contents of a stream into a file.
     *
     * @param in A stream to read from. This stream is not closed by this method.
     * @param f The file to write the stream contents into.
     * @throws IOFailure If any error occurs while writing the stream to a file
     */
    public static void writeStreamToFile(InputStream in, File f) {
        ArgumentNotValid.checkNotNull(f, "File f");
        ArgumentNotValid.checkNotNull(in, "InputStream in");

        byte[] buffer = new byte[Constants.IO_BUFFER_SIZE];
        try {
            FileOutputStream out = new FileOutputStream(f);
            try {
                int bytesRead;
                while ((bytesRead = in.read(buffer)) > 0) {
                    out.write(buffer, 0, bytesRead);
                }
            } finally {
                out.close();
            }
        } catch (IOException e) {
            final String errMsg = "Error writing stream to file '" + f.getAbsolutePath() + "'.";
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);

        }
    }

    /**
     * Get the location of the standard temporary directory. The existence of this directory should be ensure at the
     * start of every application.
     *
     * @return The directory that should be used for temporary files.
     */
    public static File getTempDir() {
        return new File(Settings.get(CommonSettings.DIR_COMMONTEMPDIR));
    }

    /**
     * Attempt to move a file using rename, and if that fails, move the file by copy-and-delete.
     *
     * @param fromFile The source
     * @param toFile The target
     */
    public static void moveFile(File fromFile, File toFile) {
        ArgumentNotValid.checkNotNull(fromFile, "File fromFile");
        ArgumentNotValid.checkNotNull(toFile, "File toFile");

        if (!fromFile.renameTo(toFile)) {
            copyFile(fromFile, toFile);
            remove(fromFile);
        }
    }

    /**
     * Given a set, generate a reasonable file name from the set.
     *
     * @param <T> The type of objects, that the Set IDs argument contains.
     * @param IDs A set of IDs.
     * @param suffix A suffix. May be empty string.
     * @return A reasonable file name.
     */
    public static <T extends Comparable<T>> String generateFileNameFromSet(Set<T> IDs, String suffix) {
        ArgumentNotValid.checkNotNull(IDs, "Set<T> IDs");
        ArgumentNotValid.checkNotNull(suffix, "String suffix");

        if (IDs.isEmpty()) {
            return "empty" + suffix;
        }

        List<T> sorted = new ArrayList<T>(IDs);
        Collections.sort(sorted);

        String allIDsString = StringUtils.conjoin("-", sorted);
        String fileName;
        if (sorted.size() > MAX_IDS_IN_FILENAME) {
            String firstNIDs = StringUtils.conjoin("-", sorted.subList(0, MAX_IDS_IN_FILENAME));
            fileName = firstNIDs + "-" + ChecksumCalculator.calculateMd5(allIDsString.getBytes()) + suffix;
        } else {
            fileName = allIDsString + suffix;
        }
        return fileName;
    }

    /**
     * Sort a crawl.log file according to the url.
     *
     * @param file The file containing the unsorted data.
     * @param toFile The file that the sorted data can be put into.
     * @throws IOFailure if there were errors running the sort process, or if the file does not exist.
     */
    public static void sortCrawlLog(File file, File toFile) {
        ArgumentNotValid.checkNotNull(file, "File file");
        ArgumentNotValid.checkNotNull(toFile, "File toFile");
        if (!file.exists()) {
            String errMsg = "The file '" + file.getAbsolutePath() + "' does not exist.";
            log.warn(errMsg);
            throw new IOFailure(errMsg);
        }

        File sortTempDir = null;
        if (Settings.getBoolean(CommonSettings.UNIX_SORT_USE_COMMON_TEMP_DIR)) {
            sortTempDir = FileUtils.getTempDir();
            if (!sortTempDir.isDirectory()) {
            	log.warn("We should be using commontempdir {} in the sort process, but the directory doesn't exist", 
            			sortTempDir.getAbsolutePath());
            	sortTempDir = null;
            }
        }
        boolean sortLikeCrawllog = true;
        int error = ProcessUtils.runUnixSort(file, toFile, sortTempDir, sortLikeCrawllog);
        if (error != 0) {
            final String errMsg = "Error code " + error + " sorting crawl log '" + file + "'";
            log.warn(errMsg);
            throw new IOFailure(errMsg);
        }
    }

    /**
     * Sort a crawl.log file according to the timestamp.
     *
     * @param file The file containing the unsorted data.
     * @param toFile The file that the sorted data can be put into.
     * @throws IOFailure if there were errors running the sort process, or if the file does not exist.
     */
    public static void sortCrawlLogOnTimestamp(File file, File toFile) {
        ArgumentNotValid.checkNotNull(file, "File file");
        ArgumentNotValid.checkNotNull(toFile, "File toFile");
        if (!file.exists()) {
            String errMsg = "The file '" + file.getAbsolutePath() + "' does not exist.";
            log.warn(errMsg);
            throw new IOFailure(errMsg);
        }

        File sortTempDir = null;
        if (Settings.getBoolean(CommonSettings.UNIX_SORT_USE_COMMON_TEMP_DIR)) {
            sortTempDir = FileUtils.getTempDir();
            if (!sortTempDir.isDirectory()) {
            	log.warn("We should be using commontempdir {} in the sort process, but the directory doesn't exist", 
            			sortTempDir.getAbsolutePath());
            	sortTempDir = null;
            }
        }
        boolean sortLikeCrawllog = false;
        int error = ProcessUtils.runUnixSort(file, toFile, sortTempDir, sortLikeCrawllog);
        if (error != 0) {
            final String errMsg = "Error code " + error + " sorting crawl log '" + file + "'";
            log.warn(errMsg);
            throw new IOFailure(errMsg);
        }
    }

    /**
     * Sort a CDX file according to our standard for CDX file sorting. This method depends on the Unix sort() command.
     *
     * @param file The raw unsorted CDX file.
     * @param toFile The file that the result will be put into.
     * @throws IOFailure If the file does not exist, or could not be sorted
     */
    public static void sortCDX(File file, File toFile) {
        ArgumentNotValid.checkNotNull(file, "File file");
        ArgumentNotValid.checkNotNull(toFile, "File toFile");
        if (!file.exists()) {
            String errMsg = "The file '" + file.getAbsolutePath() + "' does not exist.";
            log.warn(errMsg);
            throw new IOFailure(errMsg);
        }
        boolean sortLikeCrawllog = false;
        File sortTempDir = null;
        if (Settings.getBoolean(CommonSettings.UNIX_SORT_USE_COMMON_TEMP_DIR)) {
            sortTempDir = FileUtils.getTempDir();
            if (!sortTempDir.isDirectory()) {
            	log.warn("We should be using commontempdir {} in the sort process, but the directory doesn't exist", 
            			sortTempDir.getAbsolutePath());
            	sortTempDir = null;
            }

        }
        int error = ProcessUtils.runUnixSort(file, toFile, sortTempDir, sortLikeCrawllog);
        if (error != 0) {
            final String errMsg = "Error code " + error + " sorting cdx file '" + file.getAbsolutePath() + "'";
            log.warn(errMsg);
            throw new IOFailure(errMsg);
        }
    }

    /**
     * Sort a file using UNIX sort.
     *
     * @param file the file that you want to sort.
     * @param toFile The destination file.
     */
    public static void sortFile(File file, File toFile) {
        sortCDX(file, toFile);
    }

    /**
     * Creates a new temporary directory with a unique name. This directory will be deleted automatically at the end of
     * the VM (though behaviour if there are files in it is undefined). This method will try a limited number of times
     * to create a directory, using a randomly generated suffix, before giving up.
     *
     * @param inDir The directory where the temporary directory should be created.
     * @param prefix The prefix of the directory name, for identification purposes.
     * @return A newly created directory that no other calls to createUniqueDir returns.
     * @throws ArgumentNotValid if inDir is not an existing directory that can be written to.
     * @throws IOFailure if a free name couldn't be found within a reasonable number of tries.
     */
    public static File createUniqueTempDir(File inDir, String prefix) {
        ArgumentNotValid.checkNotNull(inDir, "File inDir");
        ArgumentNotValid.checkNotNullOrEmpty(prefix, "String prefix");
        ArgumentNotValid.checkTrue(inDir.isDirectory(), inDir + " must be a directory");
        ArgumentNotValid.checkTrue(inDir.canWrite(), inDir + " must be writeable");
        for (int tries = 0; tries < MAX_RETRIES; tries++) {
            File newDir;
            try {
                newDir = File.createTempFile(prefix, null, inDir);
            } catch (IOException e) {
                final String errMsg = "Couldn't create temporary file in '" + inDir.getAbsolutePath()
                        + "' with prefix '" + prefix + "'";
                log.warn(errMsg, e);
                throw new IOFailure(errMsg, e);
            }
            newDir.delete();
            if (newDir.mkdir()) {
                newDir.deleteOnExit();
                return newDir;
            }
        }
        final String errMsg = "Too many similar files around, cannot create " + "unique dir with prefix " + prefix
                + " in '" + inDir.getAbsolutePath() + "'.";
        log.warn(errMsg);
        throw new IOFailure(errMsg);
    }

    /**
     * Read the last line in a file. Note this method is not UTF-8 safe.
     *
     * @param file input file to read last line from.
     * @return The last line in the file (ending newline is irrelevant), returns an empty string if file is empty.
     * @throws ArgumentNotValid on null argument, or file is not a readable file.
     * @throws IOFailure on IO trouble reading file.
     */
    public static String readLastLine(File file) {
        ArgumentNotValid.checkNotNull(file, "File file");
        if (!file.isFile() || !file.canRead()) {
            final String errMsg = "File '" + file.getAbsolutePath() + "' is not a readable file.";
            log.warn(errMsg);
            throw new ArgumentNotValid(errMsg);
        }
        if (file.length() == 0) {
            return "";
        }
        RandomAccessFile rafile = null;
        try {
            rafile = new RandomAccessFile(file, "r");
            // seek to byte one before end of file (remember we know the file is
            // not empty) - this ensures that an ending newline is not read
            rafile.seek(rafile.length() - 2);
            // now search to the last linebreak, or beginning of file
            while (rafile.getFilePointer() != 0 && rafile.read() != '\n') {
                // search back two, because we just searched forward one to find
                // newline
                rafile.seek(rafile.getFilePointer() - 2);
            }
            return rafile.readLine();
        } catch (IOException e) {
            final String errMsg = "Unable to access file '" + file.getAbsolutePath() + "'";
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);
        } finally {
            try {
                if (rafile != null) {
                    rafile.close();
                }
            } catch (IOException e) {
                log.debug("Unable to close file '{}' after reading", file.getAbsolutePath(), e);
            }
        }
    }

    /**
     * Append the given lines to a file. Each lines is terminated by a newline.
     *
     * @param file A file to append to.
     * @param lines The lines to write.
     */
    public static void appendToFile(File file, String... lines) {
        ArgumentNotValid.checkNotNull(file, "File file");
        ArgumentNotValid.checkNotNull(lines, "String... lines");

        PrintWriter writer = null;
        int linesAppended = 0;
        try {
            boolean appendMode = true;
            writer = new PrintWriter(new FileWriter(file, appendMode));
            for (String line : lines) {
                writer.println(line);
                linesAppended++;
            }
        } catch (IOException e) {
            log.warn("Error appending {} lines to file '{}'. Only appended {} lines. ", lines.length,
                    file.getAbsolutePath(), linesAppended, e);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    /**
     * Loads an file from the class path (for retrieving a file from '.jar').
     *
     * @param filePath The path of the file.
     * @return The file from the class path.
     * @throws IOFailure If resource cannot be retrieved from the class path.
     */
    public static File getResourceFileFromClassPath(String filePath) throws IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(filePath, "String filePath");
        try {
            // retrieve the file as a stream from the classpath.
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);

            if (stream != null) {
                // Make stream into file, and return it.
                File tmpFile = File.createTempFile("tmp", "tmp");
                StreamUtils.copyInputStreamToOutputStream(stream, new FileOutputStream(tmpFile));
                return tmpFile;
            } else {
                String msg = "The resource was not retrieved correctly from the class path: '" + filePath + "'";
                log.trace(msg);
                throw new IOFailure(msg);
            }
        } catch (IOException e) {
            String msg = "Problems making stream of resource in class path into a file. Filepath: '" + filePath + "'";
            log.warn(msg, e);
            throw new IOFailure(msg, e);
        }
    }

    /**
     * Get a humanly readable representation of the file size. If the file is a directory, the size is the aggregate of
     * the files in the directory except that subdirectories are ignored. The number is given with 2 decimals.
     *
     * @param aFile a File object
     * @return a humanly readable representation of the file size (rounded)
     */
    public static String getHumanReadableFileSize(File aFile) {
        ArgumentNotValid.checkNotNull(aFile, "File aFile");
        final long bytesPerOneKilobyte = 1000L;
        final long bytesPerOneMegabyte = 1000000L;
        final long bytesPerOneGigabyte = 1000000000L;
        double filesize = 0L;
        if (aFile.isDirectory()) {
            for (File f : aFile.listFiles()) {
                if (f.isFile()) {
                    filesize = filesize + f.length();
                }
            }

        } else {
            filesize = aFile.length(); // normal file.
        }

        NumberFormat decFormat = new DecimalFormat("##.##");
        if (filesize < bytesPerOneKilobyte) {
            // represent size in bytes without the ".0"
            return (long) filesize + " bytes";
        } else if (filesize >= bytesPerOneKilobyte && filesize < bytesPerOneMegabyte) {
            // represent size in Kbytes
            return decFormat.format(filesize / bytesPerOneKilobyte) + " Kbytes";
        } else if (filesize >= bytesPerOneMegabyte && filesize < bytesPerOneGigabyte) {
            // represent size in Mbytes
            return decFormat.format(filesize / bytesPerOneMegabyte) + " Mbytes";
        } else {
            // represent in Gbytes
            return decFormat.format(filesize / bytesPerOneGigabyte) + " Gbytes";
        }
    }

    /**
     * @param aDir A directory
     * @return true, if the given directory contains files; else returns false
     */
    public static boolean hasFiles(File aDir) {
        ArgumentNotValid.checkExistsDirectory(aDir, "aDir");
        return (aDir.listFiles().length > 0);
    }

}
