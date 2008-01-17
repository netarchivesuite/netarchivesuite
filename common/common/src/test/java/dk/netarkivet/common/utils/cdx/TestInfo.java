/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
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

package dk.netarkivet.common.utils.cdx;

import java.io.File;

/**
 * Constants for the tests in this package
 *
 */

public class TestInfo {
    //Reference to test input file:
    static final File ARC_DIR = new File("tests/dk/netarkivet/common/utils/cdx/data/input/");
    static final File ARC_FILE1 = new File(ARC_DIR, "fyensdk.arc");
    static final File ARC_FILE2 = new File(ARC_DIR, "input-2.arc");
    static final File ARC_FILE3 = new File(ARC_DIR, "input-3.arc");
    // This file may or may not reflect the ARC_FILE
    static final File SORTED_CDX_FILE = new File(ARC_DIR, "fyensdk.cdx");
    //Reference to test output file:
    static final File CDX_DIR = new File("tests/dk/netarkivet/common/utils/cdx/data/cdxoutput/");
    static final File CDX_FILE = new File(CDX_DIR, "fyensdk.cdx");
    //Output Stream for batch jobs
    static final int NUM_RECORDS = 11;
    static final File CDX_FILE1 = new File(ARC_DIR, "Reader1.cdx");
    static final File CDX_FILE2 = new File("tests/dk/netarkivet/common/utils/cdx/data/input/Reader2.cdx");
    static final File CDX_FILE3 = new File("tests/dk/netarkivet/common/utils/cdx/data/input/Reader3.cdx");
    static final File CDX_FILE4 = new File("tests/dk/netarkivet/common/utils/cdx/data/input/Reader4.cdx");
    static final File MISSING_FILE = new File("tests/dk/netarkivet/common/utils/cdx/data/input/Missing.cdx");
    public static final File TEMP_FILE = new File(CDX_DIR, "tmp");
    public static final File CORRECT_CDX_FILE = new File(ARC_DIR, "fyensdk.cdx-correct");

    private TestInfo() {}
}
