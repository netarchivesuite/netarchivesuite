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
package dk.netarkivet.common.tools;

import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public class CreateharvestMappingsFromAdminDataTester extends TestCase {

    private PreventSystemExit pse = new PreventSystemExit();
    private File admindatadir = new File(TestInfo.WORKING_DIR, "admindatadir");
    private File admindataFile = new File(TestInfo.METADATA_DIR, "admin.data.reduced.zip");
    private File admindataReducedFile = new File(admindatadir, "admin.data.reduced");
    private File admindataFileUnzipped = new File(admindatadir, "admin.data");
    private File jobidHarvestidFile = new File("jobid-harvestid.txt");


    public void setUp() throws Exception {

        FileUtils.createDir(admindatadir);
        assertTrue("original admindata should exist: " + admindataFile.getAbsolutePath(), admindataFile.exists());
        //System.out.println("destination: " + admindataFileUnzipped.getAbsolutePath());
        dk.netarkivet.common.utils.TestInfo.unzipTo(admindataFile, admindatadir);
        FileUtils.copyFile(admindataReducedFile,
                admindataFileUnzipped);
        assertTrue("admin.data should exist here: " + admindatadir.getAbsolutePath(), admindataFileUnzipped.exists());
        pse.setUp();
    }

    /**
     * Test dk.netarkivet.common.tools.CreateHarvestMappingsFromAdminData.main.
     * @throws IOException
     */
    public void testCreateMapping() throws IOException {
        ByteArrayOutputStream baosOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baosOut));
        ByteArrayOutputStream baosErr = new ByteArrayOutputStream();
        System.setErr(new PrintStream(baosErr));
        try {
            CreateHarvestMappingsFromAdminData.main(new String[]{});
            fail("Should System.exit(1) on no args");
        } catch (SecurityException e) {
            System.err.flush();
            StringAsserts.assertStringMatches("Should have usage in stderr",
                    "Usage: CreateHarvestMappingsFromAdminData <admindata-dir>", baosErr.toString());

            System.out.flush();
            assertEquals("Should not output anything to stdout",
                    "",
                    baosOut.toString());
            baosOut.reset();
            baosErr.reset();
        }
        assertFalse("The file jobid-harvestid.txt should not exist now", jobidHarvestidFile.exists());


        // test output, if metatadir contains no admin.data, or dir does not exist
        /* Create false metadatadir */
        try {
            //new File(TestInfo.WORKING_DIR

            CreateHarvestMappingsFromAdminData.main(new String[]{});
            fail("Should System.exit(1) on no args");
        } catch (SecurityException e) {
        }



        // Test, if CreateHarvestMappingsFromAdminData.main creates
        // a proper jobid-harvestid.txt file.
        try {
            CreateHarvestMappingsFromAdminData.main(new String[]{admindatadir.getAbsolutePath()});
        } catch (SecurityException e) {
            fail("SecurityException not expected with valid arguments");
        }
        //
        assertTrue("The file jobid-harvestid.txt should exist now", jobidHarvestidFile.exists());
        System.err.flush();
        System.out.flush();
        baosOut.reset();
        baosErr.reset();
    }


    public void tearDown() throws Exception {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        pse.tearDown();
        FileUtils.remove(jobidHarvestidFile);
    }


}
