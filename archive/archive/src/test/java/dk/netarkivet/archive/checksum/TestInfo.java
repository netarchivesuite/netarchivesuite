/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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
package dk.netarkivet.archive.checksum;

import java.io.File;

public class TestInfo {

    static final File DATA_DIR = new File("tests/dk/netarkivet/archive/checksum/data");
    static final File WORKING_DIR = new File(DATA_DIR, "working"); 
    static final File TMP_DIR = new File(DATA_DIR, "tmp");
    static final File ORIGINAL_DIR = new File(DATA_DIR, "original");
    
    static final File UPLOAD_FILE_1 = new File(WORKING_DIR, "settings.xml");
    static final File UPLOAD_FILE_2 = new File(WORKING_DIR, "NetarchiveSuite-upload1.arc");

    static final File CHECKSUM_DIR = new File(WORKING_DIR, "cs");
    
    static final String TEST1_CHECKSUM = "c06df1852355f3017e480a20116b7376"; 
    static final String TEST2_CHECKSUM = "d87cc8068fa49f3a4926ce4d1cdf14e1"; 

}
