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

package dk.netarkivet.archive.tools;


import java.io.File;

public class TestInfo {
    static final File WORKING_DIR 
        = new File("./tests/dk/netarkivet/archive/tools/working/");
    static final File DATA_DIR 
        = new File("./tests/dk/netarkivet/archive/tools/data/originals");

    //Three ARC files with one record in each:
    static final File ARC1 
        = new File(dk.netarkivet.archive.tools.TestInfo.WORKING_DIR, 
              "test1.arc");
    static final File ARC2 
        = new File(dk.netarkivet.archive.tools.TestInfo.WORKING_DIR, 
                "test2.arc");
    static final File ARC3 
        = new File(dk.netarkivet.archive.tools.TestInfo.WORKING_DIR, 
                "test3.arc");
    //An index cache file:
    static final File INDEX_DIR 
        = new File(dk.netarkivet.archive.tools.TestInfo.WORKING_DIR, 
                "2-3-cache");
    //A record listed in the index file:
    static final String TEST_ENTRY_URI
            = "http://www.kaarefc.dk/style.css";

    static final String TEST_ENTRY_FILENAME 
        = "2-2-20060731110420-00000-sb-test-har-001.statsbiblioteket.dk.arc";
    static final long TEST_ENTRY_OFFSET = 8459;
}
