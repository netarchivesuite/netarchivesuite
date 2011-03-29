/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package dk.netarkivet.archive;

/**
 * Constants for the Archive module.
 */
public class Constants {
    /**
     * Internationalisation resource bundle.
     */
    public static final String TRANSLATIONS_BUNDLE =
            "dk.netarkivet.archive.Translations";
    
    /**
     * The name of the directory in which files are stored.
     */
    public static final String FILE_DIRECTORY_NAME = "filedir";
    
    /**
     * Temporary directory used during upload, where partial files exist, until
     * moved into directory FILE_DIRECTORY_NAME.
     */
    public static final String TEMPORARY_DIRECTORY_NAME = "tempdir";

    /**
     * Directory where "deleted" files are placed".
     */
    public static final String ATTIC_DIRECTORY_NAME = "atticdir";
}
