/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.archive.indexserver.distribute;

import java.io.File;

class TestInfo {
    private static final File BASE_DIR = new File("tests/dk/netarkivet/archive/indexserver/distribute");
    private static final File DATA_DIR = new File(BASE_DIR, "data");
    public static final File ORIGINALS_DIR = new File(DATA_DIR,"originals");
    public static final File WORKING_DIR = new File(DATA_DIR,"working");
    public static final File DUMMY_INDEX_FILE = new File(ORIGINALS_DIR,"dummy_index_file.txt");
    public static final File DUMMY_CACHEDIR = new File(ORIGINALS_DIR,"dummy_cachedir");
    public static final File DUMMY_CACHEFILE = new File(DUMMY_CACHEDIR,"dummy_index_file.txt.gz");
}
