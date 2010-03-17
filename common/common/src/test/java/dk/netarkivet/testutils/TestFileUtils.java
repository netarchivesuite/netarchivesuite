/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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

package dk.netarkivet.testutils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;

/**
 * File utilities specific to the test classes.
 */
public class TestFileUtils {
    public static final FilenameFilter NON_CVS_DIRS_FILTER =
            new FilenameFilter() {
                public boolean accept(File directory, String filename) {
                    return !((filename.equals("CVS") &&
                             new File(directory, filename).isDirectory()
                    || (filename.equals(".svn") &&
                             new File(directory, filename).isDirectory())));
                }
            };
    public static final FileFilter DIRS_ONLY_FILTER = new FileFilter() {
        public boolean accept(File dir) {
            return dir.isDirectory();
        }
    };

    /**
     * Copy an entire directory from one location to another, skipping CVS
     * directories. Note that this will silently overwrite old files, just like
     * copyFile().
     *
     * @param from Original directory (or file, for that matter) to copy.
     * @param to   Destination directory, i.e. the 'new name' of the copy of the
     *             from directory.
     * @throws IOFailure
     */
    public static final void copyDirectoryNonCVS(File from, File to)
            throws IOFailure {
        if (from.isFile()) {
            try {
                FileUtils.copyFile(from, to);
            } catch (Exception e) {
                throw new IOFailure("Error copying from "
                                    + from.getAbsolutePath() + " to "
                                    + to.getAbsolutePath(), e);
            }
        } else {
            if (from.getName().equals("CVS")) {
                return;
            }
            if (from.getName().equals(".svn")) {
                return;
            }
            if (!from.exists()) {
                throw new IOFailure("Can't find directory " + from);
            }

            if (!from.isDirectory()) {
                throw new IOFailure("File is not a directory: " + from);
            }

            to.mkdir();

            if (!to.exists()) {
                throw new IOFailure("Failed to create directory " + to);
            }

            File[] subfiles = from.listFiles();

            for (File subfile : subfiles) {
                copyDirectoryNonCVS(subfile,
                                    new File(to, subfile.getName()));
            }
        }
    }

    /**
     * Compares the content of two directories and report all differences in the
     * returned text string. If no difference are located, an empty string ("") is
     * returned. All files located in the directories are treated as text files,
     * and a text comparison is done on a line by line basis. This function will
     * not work if the dirs contain binary files. No attempt is made to recover
     * from errors.
     *
     * @param fstDir The directory to compare with sndDir
     * @param sndDir The directory to compare with fstDir
     * @return A text string describing the differences between the two dirs. Empty
     *         if no differences are found.
     * @throws IOFailure if there are problems reading the content of the dirs.
     */
    public static String compareDirsText(File fstDir, File sndDir)
            throws IOFailure {
        String result = "";

        // retrieve lists of all files in the two directories
        List<File> fstFiles = new ArrayList<File>();
        List<File> sndFiles = new ArrayList<File>();

        FileUtils.getFilesRecursively(fstDir.getPath(), fstFiles, "");
        FileUtils.getFilesRecursively(sndDir.getPath(), sndFiles, "");

        Map<String, File> fstFilesMap = new HashMap<String, File>();
        for (File f : fstFiles) {
            fstFilesMap.put(removePrefixDir(fstDir, f), f);
        }

        Map<String, File> sndFilesMap = new HashMap<String, File>();
        for (File f : sndFiles) {
            sndFilesMap.put(removePrefixDir(sndDir, f), f);
        }

        // The two dirs should contain the same files
        for (String s : fstFilesMap.keySet()) {
            if (!sndFilesMap.containsKey(s)) {
                result += "Result file not found in second dir:" + s + "\n";
            }
        }

        // The two dirs should contain the same files
        for (String s : sndFilesMap.keySet()) {
            if (!fstFilesMap.containsKey(s)) {
                result += "Target file not found in result set:" + s + "\n";
            }
        }

        // No reason to continue when sets of files do not match
        if (result.length() > 0) {
            return result;
        }

        // The files in each dir should be identical
        try {
            for (String s : fstFilesMap.keySet()) {
                //Remove all carriage returns to make the comparison work on both Windows and Linux:
                String fst = FileUtils.readFile(fstFilesMap.get(s)).replaceAll(
                        "\r", "");
                String snd = FileUtils.readFile(sndFilesMap.get(s)).replaceAll(
                        "\r", "");
                if (!fst.equals(snd)) {
                    result += "Target and result differs for:" + s + "\n";
                    result += getDifferences(fst, snd) + "\n";
                }
            }
        } catch (FileNotFoundException e) {
            throw new IOFailure(
                    "While comparing the files in " + fstFilesMap.keySet(), e);
        } catch (IOException e) {
            throw new IOFailure(
                    "While comparing the files in " + fstFilesMap.keySet(), e);
        }

        return result;
    }

    /**
     * Strips a path prefix from a file name.
     *
     * @param dir The path prefix to remove from the given file's name.
     * @param f   The file to remove the path prefix from.
     * @return The name of the file without the specified path prefix.
     */
    private static String removePrefixDir(File dir, File f) {
        return f.getAbsolutePath().replaceAll(dir.getAbsolutePath(), "");
    }


    /**
     * Return textual description of the differences between two strings.
     * @param s1 strings to compare
     * @param s2 strings to compare
     * @return first line of text that differs
     */
    public static String getDifferences(String s1, String s2) {
        String[] startStrings = s1.split("\n");
        String[] endStrings = s2.split("\n");
        List<Difference> differences =
                new Diff(startStrings, endStrings).diff();
        StringBuilder res = new StringBuilder();

        for (Difference d : differences) {
            // Would like to do this as a unitfied diff (diff -u) instead,
            // but that's a little more complex.
            if (d.getAddedEnd() == -1) {
                // Deletion
                res.append("Deleted " + getDifferenceLines(d, false) + "\n");
                for (int i = d.getDeletedStart(); i <= d.getDeletedEnd(); i++) {
                    res.append("< " + startStrings[i] + "\n");
                }
            } else if (d.getDeletedEnd() == -1) {
                // Addition
                res.append("Added " + getDifferenceLines(d, true) + "\n");
                for (int i = d.getAddedStart(); i <= d.getAddedEnd(); i++) {
                    res.append("> " + endStrings[i] + "\n");
                }
            } else {
                // Modification
                res.append("Modified " + getDifferenceLines(d, false) + " into "
                           + getDifferenceLines(d, true) + "\n");
                for (int i = d.getDeletedStart(); i <= d.getDeletedEnd(); i++) {
                    res.append("< " + startStrings[i] + "\n");
                }
                res.append("---\n");
                for (int i = d.getAddedStart(); i <= d.getAddedEnd(); i++) {
                    res.append("> " + endStrings[i] + "\n");
                }
            }
        }
        return res.toString();
    }

    private static String getDifferenceLines(Difference d, boolean added) {
        int startLine;
        int endLine;
        if (added) {
            startLine = d.getAddedStart();
            endLine = d.getAddedEnd();
        } else {
            startLine = d.getDeletedStart();
            endLine = d.getDeletedEnd();
        }
        if (startLine == endLine) {
            return "line " + (startLine + 1);
        } else {
            return "lines " + (startLine + 1) + "-" + (endLine + 1);
        }
    }

    /**
     * Make a temporary directory using File.createTempFile.
     *
     * @param prefix
     * @param suffix
     * @return a temporary directory using File.createTempFile
     * @throws IOException
     */
    public static File createTempDir(String prefix, String suffix)
            throws IOException {
        File temp = File.createTempFile(prefix, suffix);
        temp.delete();
        temp.mkdir();
        return temp;
    }

    /**
     * Make a temporary directory using File.createTempFile.
     *
     * @param prefix
     * @param suffix
     * @param directory
     * @return a temporary directory using File.createTempFile
     * @throws IOException
     */
    public static File createTempDir(String prefix, String suffix,
                                     File directory) throws IOException {
        File temp = File.createTempFile(prefix, suffix, directory);
        temp.delete();
       temp.mkdir();
       return temp;
    }


    /** Find files recursively that match the given filter.
     *
     * @param start The directory (or file) to start at.
     * @param filter Filter of files to include.  All files (including
     * directories) are passed to this filter and are included if
     * filter.accept() returns true.  Subdirectories are scanned whether or
     * not filter.accept() returns true for them.
     * @return List of files (in no particular order) that match the filter.
     * and reside under start.
     */
    public static List<File> findFiles(File start, FileFilter filter) {
        List<File> results = new ArrayList<File>();
        File[] filesThisLevel = start.listFiles();
        for (File f : filesThisLevel) {
            if (filter.accept(f)) {
                results.add(f);
            }
            if (f.isDirectory()) {
                results.addAll(findFiles(f, filter));
            }
        }
        return results;
    }


}
