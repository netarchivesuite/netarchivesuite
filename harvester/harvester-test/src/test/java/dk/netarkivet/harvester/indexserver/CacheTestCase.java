/*
 * #%L
 * Netarchivesuite - harvester - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

package dk.netarkivet.harvester.indexserver;

import java.io.File;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * Base class that is used to test the cache system.
 */
public abstract class CacheTestCase {
    private UseTestRemoteFile utrf = new UseTestRemoteFile();
    ReloadSettings rs = new ReloadSettings();

    public void setUp() throws Exception {
        rs.setUp();
        // This just is needed to allow an instance of CDXIndexCache to be made
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        utrf.setUp();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        Settings.set(CommonSettings.DIR_COMMONTEMPDIR, new File(TestInfo.WORKING_DIR, "tmp").getAbsolutePath());
        Settings.set(CommonSettings.CACHE_DIR, new File(TestInfo.WORKING_DIR, "cache").getAbsolutePath());
        FileUtils.createDir(new File(TestInfo.WORKING_DIR, "tmp"));
    }

    public void tearDown() throws Exception {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        ArcRepositoryClientFactory.getViewerInstance().close();
        utrf.tearDown();
        rs.tearDown();
    }
}