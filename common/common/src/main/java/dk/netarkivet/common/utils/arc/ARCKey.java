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
package dk.netarkivet.common.utils.arc;

import java.io.File;

/**
 * Represents a location key in the ARC format.
 */
public class ARCKey {
    /** The ARC file that we will be reading from. */
    private File arcFile;
    /** The offset that the entry starts at in the file. */
    private long offset;
    /** Extension used by gzipped arc-files. */
    private static final String GZIPPED_ARC_FILE_EXTENSION = ".arc.gz";
    /** Extension used Alexa dat files. */
    private static final String ALEXA_DAT_FILE_EXTENSION = ".dat";

    /** Constructor for ARCKey.
     * Note that if the filename ends in .dat (it's an Alexa-style
     * DAT file), we assume that the file we actually want is a
     * .arc.gz file as produced by Alexa tools.  That is because the
     * Alexa cdx generator does not put the correct filename in there.
     *
     * @param archiveFileName The name of the archive found in the cdx file
     * @param offset The offset in the arc file of this entry.
     */
    public ARCKey(String archiveFileName, long offset) {
        String arcgz;
        if (archiveFileName.toLowerCase().endsWith(ALEXA_DAT_FILE_EXTENSION)) {
            arcgz = archiveFileName.substring(0,
                    archiveFileName.length() - ALEXA_DAT_FILE_EXTENSION.length()
                    ) + GZIPPED_ARC_FILE_EXTENSION;
        } else {
            arcgz = archiveFileName;
        }
        arcFile = new File(arcgz);
        this.offset = offset;
    }

    /** Getter for offset.
     * @return The offset into the ARC file used for this key */
    public long getOffset() {
        return offset;
    }

    /** Getter for arcFile.
     * @return The ARC file that this entry can be found in */
    public File getFile() {
        return arcFile;
    }

    /** @return a textual representation of filename and offset */
    public String toString() {
        return getFile() + " offset: " + getOffset();
    }
}

