/*
* File:     $Id$
* Revision: $Revision$
* Author:   $Author$
* Date:     $Date$
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
package dk.netarkivet.viewerproxy.webinterface;

import java.io.File;

import junit.framework.TestCase;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.TrivialArcRepositoryClient;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;

/**
 * Unit tests for Reporting class
 */
public class ReportingTester extends TestCase {
    private TrivialArcRepositoryClient tarc;
    private File dir;

    public void setUp() throws Exception {
        super.setUp();
        String[] values = new String[]{TrivialArcRepositoryClient.class.getName()};
        Settings.set(CommonSettings.ARC_REPOSITORY_CLIENT, values);
        ArcRepositoryClientFactory.getViewerInstance().close();
        tarc = (TrivialArcRepositoryClient) ArcRepositoryClientFactory
                .getViewerInstance();
        dir = (File) ReflectUtils.getPrivateField(
                TrivialArcRepositoryClient.class, "dir").get(tarc);
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, dir);
    }

    public void tearDown() throws Exception {
        super.tearDown();
        if (tarc != null) {
            tarc.close();
        }
        if (dir != null && dir.isDirectory()) {
            FileUtils.removeRecursively(dir);
        }
    }

    public void testGetFilesForJob() throws Exception {
        try {
            Reporting.getFilesForJob(-1);
            fail("Should fail on negative values");
        } catch (ArgumentNotValid e) {
            //Expectied
        }
        try {
            Reporting.getFilesForJob(0);
            fail("Should fail on zero");
        } catch (ArgumentNotValid e) {
            //Expectied
        }
        System.out.println(Reporting.getFilesForJob(2));
        System.out.println(Reporting.getFilesForJob(4));
    }

    public void testGetMetdataCDXRecordsForJob() throws Exception {
        try {
            Reporting.getMetdataCDXRecordsForJob(-1);
            fail("Should fail on negative values");
        } catch (ArgumentNotValid e) {
            //Expectied
        }
        try {
            Reporting.getMetdataCDXRecordsForJob(0);
            fail("Should fail on zero");
        } catch (ArgumentNotValid e) {
            //Expectied
        }
        System.out.println(Reporting.getMetdataCDXRecordsForJob(2));
        System.out.println(Reporting.getMetdataCDXRecordsForJob(4));
    }

    public void testGetCrawlLogForDomainInJob() throws Exception {
        try {
            Reporting.getCrawlLogForDomainInJob("test.dk", -1);
            fail("Should fail on negative values");
        } catch (ArgumentNotValid e) {
            //Expectied
        }
        try {
            Reporting.getCrawlLogForDomainInJob("test.dk", 0);
            fail("Should fail on zero");
        } catch (ArgumentNotValid e) {
            //Expectied
        }
        try {
            Reporting.getCrawlLogForDomainInJob("", 1);
            fail("Should fail on empty domain");
        } catch (ArgumentNotValid e) {
            //Expectied
        }
        try {
            Reporting.getCrawlLogForDomainInJob(null, 1);
            fail("Should fail on null domain");
        } catch (ArgumentNotValid e) {
            //Expectied
        }
        File file = Reporting.getCrawlLogForDomainInJob("netarkivet.dk", 2);
        System.out.println(file);
        System.out.println(FileUtils.readFile(file));
        file = Reporting.getCrawlLogForDomainInJob("kaarefc.dk", 2);
        System.out.println(file);
        System.out.println(FileUtils.readFile(file));
        file = Reporting.getCrawlLogForDomainInJob("doesnotexit.dk",
                                                         2);
        System.out.println(file);
        System.out.println(FileUtils.readFile(file));
        file = Reporting.getCrawlLogForDomainInJob("netarkivet.dk",
                                                         4);
        System.out.println(file);
        System.out.println(FileUtils.readFile(file));

    }

}
