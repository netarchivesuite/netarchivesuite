/*
 * #%L
 * Netarchivesuite - archive - test
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
package dk.netarkivet.archive.arcrepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.arcrepository.TrivialArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.utils.ChecksumCalculator;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileListJob;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * Unit-tests for the class TrivialArcRepositoryClient
 */
public class TrivialArcRepositoryClientTester {
    MoveTestFiles mtf = new MoveTestFiles(
    		TestInfo.DISTRIBUTE_ARCREPOSITORY_ORIGINALS_DIR,
    		TestInfo.DISTRIBUTE_ARCREPOSITORY_WORKING_DIR);
    ReloadSettings rs = new ReloadSettings();
    UseTestRemoteFile utrf = new UseTestRemoteFile();

    @Before
    public void setUp() {
        rs.setUp();
        utrf.setUp();

        Settings.set(CommonSettings.DIR_COMMONTEMPDIR,
        		TestInfo.DISTRIBUTE_ARCREPOSITORY_WORKING_DIR.getAbsolutePath());
        mtf.setUp();
    }

    @After
    public void tearDown() {
        mtf.tearDown();
        utrf.tearDown();
        rs.tearDown();
    }

    @Test
    public void testStore() throws Exception {
        ArcRepositoryClient arcrep = new TrivialArcRepositoryClient();

        BatchStatus status = arcrep.batch(new FileListJob(), "BA");
        assertEquals("Should have no files processed at outset", 0, status.getNoOfFilesProcessed());

        FileUtils.copyFile(TestInfo.DISTRIBUTE_ARCREPOSITORY_SAMPLE_FILE,
        		TestInfo.DISTRIBUTE_ARCREPOSITORY_SAMPLE_FILE_COPY);
        arcrep.store(TestInfo.DISTRIBUTE_ARCREPOSITORY_SAMPLE_FILE);
        assertFalse("Should have deleted file after upload", TestInfo.DISTRIBUTE_ARCREPOSITORY_SAMPLE_FILE.exists());
        status = arcrep.batch(new FileListJob(), "BA");
        assertEquals("Should have 1 files processed after store", 1, status.getNoOfFilesProcessed());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        status.getResultFile().appendTo(out);
        assertEquals("Should list the one file",
        		TestInfo.DISTRIBUTE_ARCREPOSITORY_SAMPLE_FILE.getName() + "\n", out.toString());
        File f = File.createTempFile("foo", "bar", FileUtils.getTempDir());
        arcrep.getFile(TestInfo.DISTRIBUTE_ARCREPOSITORY_SAMPLE_FILE.getName(),
                Replica.getReplicaFromId("TWO"), f);
        assertEquals("Should have expected contents back",
                ChecksumCalculator
                        .calculateMd5(TestInfo.DISTRIBUTE_ARCREPOSITORY_SAMPLE_FILE_COPY),
                ChecksumCalculator.calculateMd5(f));
    }
}
