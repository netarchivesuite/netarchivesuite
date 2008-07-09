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

package dk.netarkivet.archive.indexserver;

import java.io.File;

import junit.framework.TestCase;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * lc forgot to comment this!
 *
 */

public class CacheTestCase extends TestCase {
    private UseTestRemoteFile utrf = new UseTestRemoteFile();
    ReloadSettings rs = new ReloadSettings();

    public CacheTestCase(String string) {
        super(string);
    }

    public void setUp() throws Exception {
        super.setUp();
        rs.setUp();
        // This just is needed to allow an instance of CDXIndexCache to be made
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        utrf.setUp();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.WORKING_DIR);
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, new File(TestInfo.WORKING_DIR, "tmp").getAbsolutePath());
        Settings.set(CommonSettings.CACHE_DIR, new File(TestInfo.WORKING_DIR, "cache").getAbsolutePath());
        FileUtils.createDir(new File(TestInfo.WORKING_DIR, "tmp"));
    }

    public void tearDown() throws Exception {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        ArcRepositoryClientFactory.getViewerInstance().close();
        utrf.tearDown();
        super.tearDown();
        rs.tearDown();
    }
}
