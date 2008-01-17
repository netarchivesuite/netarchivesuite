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

package dk.netarkivet.archive.webinterface;

/**
 * Constants for the bitarchive webinterface
 *
 */

public class Constants {
    /** Parameter name for the bitarchive to perform operation on. */
    public static final String BITARCHIVE_NAME_PARAM = "bitarchive";
    /** Parameter name for the action of running a batch job for missing files.
     *  */
    public static final String FIND_MISSING_FILES_PARAM = "findmissingfiles";
    /** Parameter name for the action of running a checksum batch job. */
    public static final String CHECKSUM_PARAM = "checksum";
    /** Parameter name for the file to perform checksum operations on. */
    public static final String FILENAME_PARAM = "file";
    /** Parameter name for request to fix checksum in admin data. */
    public static final String FIX_ADMIN_CHECKSUM_PARAM = "fixadminchecksum";
    /** Parameter name for credentials for removing a file with wrong checksum.
     *  */
    public static final String CREDENTIALS_PARAM = "credentials";

    /** BitPreservation main Java server page that contains status information
     * about the bitarchives. */
    public static final String FILESTATUS_PAGE
            = "Bitpreservation-filestatus.jsp";
    /** BitPreservation page that checks if any files are missing in one of
     *  the bitarchives */
    public static final String FILESTATUS_MISSING_PAGE
            = "Bitpreservation-filestatus-missing.jsp";
    /** BitPreservation page that checks files in archive for wrong checksum */
    public static final String FILESTATUS_CHECKSUM_PAGE
            = "Bitpreservation-filestatus-checksum.jsp";
    /** Maximum number of files to toogle on one go. */
    public static final int MAX_TOGGLE_AMOUNT = 100;

}
