/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.harvester.webinterface;

import java.io.File;

/**
 * Various constants used in testing the web interface support classes.
 */

public class TestInfo {
    public static final File BASE_DIR
           = new File("tests/dk/netarkivet/harvester/webinterface/data");    
    public static final File FUTURE_BASE_DIR
        = new File("tests/dk/netarkivet/harvester/data");  
    public static final File WORKING_DIR = new File(BASE_DIR, "working");
    public static final File ORIGINALS_DIR = new File(BASE_DIR, "originals");

    public static final File DBFILE = new File(BASE_DIR, "fullhddb.jar");
}
