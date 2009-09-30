/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.wayback;

import java.io.File;

/**
 * Defines test data and directories for the package
 * dk.netarkivet.archive.arcrepository.
 */
class TestInfo {
    static final File DATA_DIR
        = new File("tests/dk/netarkivet/wayback/data/");
    static final File ORIGINALS_DIR = new File(DATA_DIR, "originals");
    static final File WORKING_DIR = new File(DATA_DIR, "working");
    static final File FILE_DIR = new File(WORKING_DIR, "filedir");
    /**static final File CORRECT_ORIGINALS_DIR = new File(DATA_DIR,
            "correct/originals/");
    static final File CORRECT_WORKING_DIR = new File(DATA_DIR,
            "correct/working/");
    static final File TMP_FILE = new File(WORKING_DIR, "temp");*/
    static final File LOG_FILE = new File("tests/testlogs/netarkivtest.log");
    public static final long SHORT_TIMEOUT = 1000;
}
