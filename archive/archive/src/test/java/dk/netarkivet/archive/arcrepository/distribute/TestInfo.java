/* File:    $Id: UnitTesterSuite.java 1338 2010-03-17 15:27:53Z svc $
 * Version: $Revision: 1338 $
 * Date:    $Date: 2010-03-17 16:27:53 +0100 (Wed, 17 Mar 2010) $
 * Author:  $Author: svc $
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

package dk.netarkivet.archive.arcrepository.distribute;

import java.io.File;

public class TestInfo {
    public static final File BASEDIR =
        new File("tests/dk/netarkivet/archive/arcrepository/distribute/data");

    public static final File ORIGINALS = new File(BASEDIR, "originals");

    public static final File WORKING = new File(BASEDIR, "working");

    public static final File ARCDIR = new File(WORKING, "local_files");
    public static final File ARCFILE = new File(ARCDIR, "Upload2.ARC");


}
