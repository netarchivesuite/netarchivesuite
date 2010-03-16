/* File:     $Id$
 * Revision: $Revision$
 * Date:     $Date$
 * Author:   $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/** Performs a binary search through .cdx files for a given prefix string.
 * Currently only handles a single .cdx file.
 * */
public class BinSearch {
    /** The logger. */
    private static final Log log =
            LogFactory.getLog(BinSearch.class.getName());

    /** Our own comparison function.  Right now just does prefix match.
     *
     * @param line A line to find the prefix of
     * @param pattern The prefix to find.
     * @return A result equivalent to String.compareTo, but only for a prefix.
     */
    private static int compare(String line, String pattern) {
        String start = line.substring(0, Math.min(pattern.length(),
                line.length()));
        int cmp = start.compareTo(pattern);
        return cmp;
    }

    /** Given a file in sorted order and a prefix to search for, return a
     * an iterable that will return the lines in the files that start with
     * the prefix, in order.  They will be read lazily from the file.
     *
     * If no matches are found, it will still return an iterable with no
     * entries.
     *
     * @param file A CDX file to search in.
     * @param prefix The line prefix to search for.
     * @return An Iterable object that will return the lines
     * matching the prefix in the file.
     */
    public static Iterable<String> getLinesInFile(File file, String prefix) {
        try {
            RandomAccessFile in = null;
            try {
                in = new RandomAccessFile(file, "r");
                long matchingline = binSearch(in, prefix);
                if (matchingline == -1) {
                    // Simple empty Iterable
                    return Collections.emptyList();
                }
                long firstMatching = findFirstLine(in, prefix, matchingline);
                return new PrefixIterable(file, firstMatching, prefix);
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch (IOException e) {
            String message = "IOException reading file '" + file + "'";
            log.warn(message, e);
            throw new IOFailure(message, e);
        }
    }

    /** An implementation of Iterable that returns lines matching a given
     * prefix, starting at an offset.  We use compare() to determine if the
     * prefix matches.
     * */
    private static class PrefixIterable implements Iterable<String> {
        /** File we're reading from. */
        private final File file;
        /** The prefix of all lines we return. */
        private final String prefix;
        /** Where to start reading - seek to this without reading it. */
        private final long offset;

        /** Construct an Iterable from the given file, offset and prefix.
         *
         * @param file This file will be read when the iterator() is made.
         * The lines in this file must be sorted alphabetically.
         * @param offset The place where reading will start from.
         * @param prefix The prefix of all lines that will be read.
         */
        public PrefixIterable(File file, long offset, String prefix) {
            this.file = file;
            this.offset = offset;
            this.prefix = prefix;
        }

        /** Return a new iterator that stops (not skips) when the line
         * read no longer matches the prefix.
         * @return an iterator that stops (not skips) when the line
         * read no longer matches the prefix.  When the iterator ends, the
         * underlying file is closed.
         */
        public Iterator<String> iterator() {
            final RandomAccessFile infile;
            try {
                infile = new RandomAccessFile(file, "r");
                infile.seek(offset);
            } catch (IOException e) {
                String message = "IOException reading file '" + file + "'";
                log.warn(message, e);
                throw new IOFailure(message, e);
            }
            return new Iterator<String>() {
                String nextLine = null;
                boolean finished = false;

                /** Check whether there is a next element.
                 * Implementation note:  This method has the sideeffect of
                 * reading a line into its own buffer, if none is already read.
                 *
                 * @return True if there is a next element to be had.
                 * @throws IOFailure if there is an error reading the file.
                 */
                public boolean hasNext() {
                    if (nextLine != null) {
                        return true;
                    }
                    if (finished) {
                        return false;
                    }
                    String line;
                    try {
                        line = infile.readLine();
                    } catch (IOException e) {
                        String message = "IOException reading file '" 
                            + file + "'";
                        log.warn(message, e);
                        throw new IOFailure(message, e);
                    }
                    if (line == null || compare(line, prefix) != 0) {
                        finished = true;
                        cleanUp();
                        return false;
                    }
                    nextLine = line;
                    return true;
                }

                /** Close the input file and mark us to be done reading,
                 * so we don't try to read from a closed file.
                 */
                private void cleanUp() {
                    try {
                        infile.close();
                    } catch (IOException e) {
                        String message = "IOException closing file '"
                            + file + "'";
                        log.warn(message, e);
                        throw new IOFailure(message, e);
                    }
                }

                /** Return the next element, if any.
                 *
                 * @return Next element.
                 * @throws IOFailure if reading the underlying file causes
                 * errors.
                 */
                public String next() {
                    if (nextLine == null && !hasNext()) {
                        throw new NoSuchElementException();
                    }
                    String line = nextLine;
                    nextLine = null;
                    return line;
                }

                /** This iterator doesn't support remove.
                 * @throws UnsupportedOperationException*/
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                /** Ensures that the file pointer is really closed.
                 *
                 */
                public void finalize() {
                    cleanUp();
                }
            };
        }
    }

    /**
     * Return the index of the first line in the file to match 'find'. If the
     * lines in the file are roughly equal length, it reads
     * O(sqrt(n)) lines, where n is the distance from matchingline to the first
     * line.
     *
     * @param in
     *            The file to search in
     * @param find
     *            The string to match against the first line
     * @param matchingline
     *            The index to start searching from.  This index must be at
     *            the start of a line that matches 'find'
     * @return The offset into the file of the first line matching 'find'.
     *         Guaranteed to be <= matchingline.
     * @throws IOException If the matchingLine < 0 or some I/O error occurs.
     */
    private static long findFirstLine(RandomAccessFile in,
                                      String find, long matchingline)
            throws IOException {
        in.seek(matchingline);
        String line = in.readLine();
        if (line == null || compare(line, find) != 0) {
            final String msg = "Internal: Called findFirstLine without a "
                    + "matching line in '" + in + "' byte " + matchingline;
            log.warn(msg);
            throw new ArgumentNotValid(msg);
        }
        // Skip backwards in quadratically increasing steps.
        int linelength = line.length();
        long offset = linelength;
        for (int i = 1; matchingline - offset > 0;
             i++, offset = i * i * linelength) {
            skipToLine(in, matchingline - offset);
            line = in.readLine();
            if (line == null || compare(line, find) != 0) {
                break;
            }
        }
        // Either found start-of-file or a non-matching line
        long pos;
        if (matchingline - offset <= 0) {
            pos = 0;
            in.seek(0);
        } else {
            pos = in.getFilePointer();
        }
        // Seek forwards line by line until the first matching line.
        // This takes no more than sqrt(n) steps since we know there is
        // a matching line that far away by the way we skipped to here.
        while ((line = in.readLine()) != null) {
            if (compare(line, find) == 0) {
                return pos;
             }
            pos = in.getFilePointer();
        }

        return -1;
    }

    /** Skip to the next line after the given position by
     * reading a line.  Note that if the position is at the start
     * of a line, it will go to the next line.
     *
     * @param in A file to read from
     * @param pos The position to start at.
     * @return A new position in the file.  The file's pointer (as given by
     * getFilePointer()) is updated to match.
     * @throws IOException If some I/O error occurs
     */
    private static long skipToLine(RandomAccessFile in, long pos)
            throws IOException {
        in.seek(pos);
        in.readLine();
        return in.getFilePointer();
    }

    /** Perform a binary search for a string in a file.
     * Returns the position of a line that begins with 'find'.
     * Note that this may not be the first line, if there be duplicates.
     * @param in the RandomAccessFile
     * @param find The String to look for in the above file
     * @throws IOException If some I/O error occurs
     * @return The index of a line matching find, or -1 if none found.
     */
    private static long binSearch(RandomAccessFile in,
                                 String find) throws IOException {
        // The starting position for the binary search.  Always
        // at the start of a line that's < the wanted line.
        long startpos = 0;
        // Ensure that startpos isn't a match.
        in.seek(startpos);
        String line = in.readLine();
        if (line == null) {
            return -1;
        }
        if (compare(line, find) == 0) {
            return startpos;
        }
        // The ending position for the binary search.  Always
        // *after* a line that >= the wanted line (which also means
        // at the start of a line that's > the wanted line, or at EOF
        long endpos = in.length();

        // Set file pos to first line after middle.
        findMiddleLine(in, startpos, endpos);

        // When done searching, midpos points to a matching line, if any
        // Until the search is done, both endpos and startpos point
        // at non-matching lines (or EOF), and startpos < prevpos < endpos
        long prevpos = in.getFilePointer();
        do {
            line = in.readLine();
            if (line == null) {
                log.debug("Internal: Ran past end of file in '"
                          + in + "' at " + endpos);
                return -1;
            }
            int cmp = compare(line, find);
            if (cmp > 0) {
                endpos = prevpos;
            } else if (cmp < 0) {
                startpos = prevpos;
            } else {
                return prevpos;
            }
            if (startpos == endpos) {
                return -1;
            }
            prevpos = findMiddleLine(in, startpos, endpos);
            if (prevpos == -1) {
                return -1;
            }
        } while (true);
    }

    /** Returns the position of a line between startpos and endpos.
     * If no line other than the one starting at startpos can be found,
     * returns -1. Also sets the file pointer to the start of the line.
     * @param in The file to read from
     * @param startpos The lower bound for the position.  Must be the
     * start of a line.
     * @param endpos The upper bound for the position.  Must be the start of
     * a line or EOF.
     * @return The position of a line s.t. startpos < returnval < endpos,
     * or -1 if no such line can be found.
     * @throws IOException If some I/O error occurs
     */
    private static long findMiddleLine(RandomAccessFile in,
                                       long startpos, long endpos)
            throws IOException {
        // First check that there is a middle line at all.
        // If there is a line after startpos, but before endpos,
        // we remember it and as soon as we hit that line.
        long firstmiddleline = skipToLine(in, startpos);
        if (firstmiddleline == endpos) {
            return -1;
        }
        long newmidpos = endpos;
        int div = 1;
        while (newmidpos == endpos) {
            // Drat, newmidpos is not far enough back.
            // Divide back until we find a previous line.
            div *= 2;
            // Find an earlier point, half as far from startpos as the previous
            newmidpos = startpos + (endpos - startpos) / div;
            // If we get beyond the found line after the start line, just
            // return that.
            if (newmidpos < firstmiddleline) {
                newmidpos = firstmiddleline;
                in.seek(newmidpos);
                break;
            }
            // Now find the first line after the new middle
            newmidpos = skipToLine(in, newmidpos);
        }
        // Now the midpos should be != startpos && != endpos
        assert newmidpos != startpos
            : "Invariant violated: Newmidpos > startpos";
        return newmidpos;
    }
}
