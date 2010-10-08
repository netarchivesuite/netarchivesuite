/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.archive.tools;


import java.io.File;

import dk.netarkivet.common.distribute.arcrepository.Replica;

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
   /** Warc file to test upload. */
    static final File WARC1 
        = new File(dk.netarkivet.archive.tools.TestInfo.WORKING_DIR, 
            "NAS-20100909163324-00000-mette.kb.dk.warc");
    
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
    
    static final String BATCH_ARG_ERROR_FILE_EXT = "ClassFile.error";
    static final String BATCH_C_ARG_NOREAD_FILE = "TestBatchClass.class";
    static final String BATCH_J_ARG_NOREAD_FILE = "TestBatchJar.jar";
    static final String BATCH_TEST_JAR_FILENAME = "testJar.jar";
    static final File BATCH_TEST_JAR_FILE = new File(WORKING_DIR, BATCH_TEST_JAR_FILENAME);     
    static final String BATCH_REPLICA_ERROR = "repErr";
    static final String BATCH_TEST_JAR_ERROR_CLASS = "error";
    static final String BATCH_TEST_JAR_GOOD_CLASS = "batch.PersonDataArcBatch";
    static final String BATCH_CS_REPLICA_NAME = Replica.getReplicaFromId("THREE").getName();
    
    static final File CACHE_DIR = new File(WORKING_DIR, "cache");
    static final File CACHE_TEMP_DIR = new File(WORKING_DIR, "tempCache");
    static final File CACHE_1_FILE = new File(CACHE_TEMP_DIR, "cache_file_1");
    static final File CACHE_OUTPUT_DIR = new File(WORKING_DIR, "outCache");
    static final File CACHE_ZIP_FILE = new File(CACHE_OUTPUT_DIR, "cache_file_1.gz");
    
    static final File DATABASE_DIR = new File(WORKING_DIR, "database");
    static final String DATABASE_URL = "jdbc:derby:" + DATABASE_DIR.getPath();
    static final File DATABASE_ADMIN_DATA_1 = new File(WORKING_DIR, "admin.data1");
    static final File DATABASE_ADMIN_DATA_2 = new File(WORKING_DIR, "admin.data2");
    static final File DATABASE_ADMIN_DATA_FALSE = new File(WORKING_DIR, "false.error");
}
