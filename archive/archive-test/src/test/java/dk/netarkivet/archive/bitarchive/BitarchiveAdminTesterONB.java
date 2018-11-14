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
package dk.netarkivet.archive.bitarchive;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * BitArchiveAdminData test class. Checking if directories for the bitarchive will be given back in the right order
 */

// FIXME: Move to ONB specific test area.
@Ignore("Only works for ONB")
public class BitarchiveAdminTesterONB {
    private BitarchiveAdmin ad;
    private static final String ARC_FILE_NAME = "testfile.arc";
    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() {
        rs.setUp();
    }

    @After
    public void tearDown() {
        if (ad != null) {
            ad.close();
        }
        rs.tearDown();
    }

    @Test
    public void testGetFirstBitarchiveDirectory01() throws IOException {
        final String[] TESTDIRS = {"/mnt/brz/brz_datasrc/wa001"};

        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();

        File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
        assertEquals("wa001", path.getParentFile().getParentFile().getName());
    }

    @Test
    public void testGetFirstBitarchiveDirectory02() throws IOException {
        final String[] TESTDIRS = {"/mnt/brz/brz_datasrc/wa001", "/mnt/brz/brz_datasrc/wa002"};

        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();

        File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
        assertEquals("wa001", path.getParentFile().getParentFile().getName());
    }

    @Test
    public void testGetFirstBitarchiveDirectory03() throws IOException {
        final String[] TESTDIRS = {"/mnt/brz/brz_datasrc/wa001", "/mnt/brz/brz_datasrc/wa002",
                "/mnt/brz/brz_datasrc/wa003"};

        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();

        File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
        assertEquals("wa001", path.getParentFile().getParentFile().getName());
    }

    @Test
    public void testGetFirstBitarchiveDirectory04() throws IOException {
        final String[] TESTDIRS = {"/mnt/brz/brz_datasrc/wa001", "/mnt/brz/brz_datasrc/wa002",
                "/mnt/brz/brz_datasrc/wa003", "/mnt/brz/brz_datasrc/wa004"};

        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();

        File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
        assertEquals("wa001", path.getParentFile().getParentFile().getName());
    }

    @Test
    public void testGetFirstBitarchiveDirectory05() throws IOException {
        final String[] TESTDIRS = {"/mnt/brz/brz_datasrc/wa001", "/mnt/brz/brz_datasrc/wa002",
                "/mnt/brz/brz_datasrc/wa003", "/mnt/brz/brz_datasrc/wa004", "/mnt/brz/brz_datasrc/wa005"};

        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();

        File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
        assertEquals("wa001", path.getParentFile().getParentFile().getName());
    }

    @Test
    public void testGetFirstBitarchiveDirectory06() throws IOException {
        final String[] TESTDIRS = {"/mnt/brz/brz_datasrc/wa001", "/mnt/brz/brz_datasrc/wa002",
                "/mnt/brz/brz_datasrc/wa003", "/mnt/brz/brz_datasrc/wa004", "/mnt/brz/brz_datasrc/wa005",
                "/mnt/brz/brz_datasrc/wa006"};

        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();

        File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
        assertEquals("wa001", path.getParentFile().getParentFile().getName());
    }

    @Test
    public void testGetFirstBitarchiveDirectory07() throws IOException {
        final String[] TESTDIRS = {"/mnt/brz/brz_datasrc/wa001", "/mnt/brz/brz_datasrc/wa002",
                "/mnt/brz/brz_datasrc/wa003", "/mnt/brz/brz_datasrc/wa004", "/mnt/brz/brz_datasrc/wa005",
                "/mnt/brz/brz_datasrc/wa006", "/mnt/brz/brz_datasrc/wa007"};

        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();

        File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
        assertEquals("wa001", path.getParentFile().getParentFile().getName());
    }

    @Test
    public void testGetFirstBitarchiveDirectory08() throws IOException {
        final String[] TESTDIRS = {"/mnt/brz/brz_datasrc/wa001", "/mnt/brz/brz_datasrc/wa002",
                "/mnt/brz/brz_datasrc/wa003", "/mnt/brz/brz_datasrc/wa004", "/mnt/brz/brz_datasrc/wa005",
                "/mnt/brz/brz_datasrc/wa006", "/mnt/brz/brz_datasrc/wa007", "/mnt/brz/brz_datasrc/wa008"};

        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();

        File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
        assertEquals("wa001", path.getParentFile().getParentFile().getName());
    }

    @Test
    public void testGetFirstBitarchiveDirectory09() throws IOException {
        final String[] TESTDIRS = {"/mnt/brz/brz_datasrc/wa002", "/mnt/brz/brz_datasrc/wa003",
                "/mnt/brz/brz_datasrc/wa004", "/mnt/brz/brz_datasrc/wa005", "/mnt/brz/brz_datasrc/wa006",
                "/mnt/brz/brz_datasrc/wa007", "/mnt/brz/brz_datasrc/wa008"};

        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();

        File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
        assertEquals("wa002", path.getParentFile().getParentFile().getName());
    }

    @Test
    public void testGetFirstBitarchiveDirectory10() throws IOException {
        final String[] TESTDIRS = {"/mnt/brz/brz_datasrc/wa003", "/mnt/brz/brz_datasrc/wa004",
                "/mnt/brz/brz_datasrc/wa005", "/mnt/brz/brz_datasrc/wa006", "/mnt/brz/brz_datasrc/wa007",
                "/mnt/brz/brz_datasrc/wa008"};

        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();

        File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
        assertEquals("wa003", path.getParentFile().getParentFile().getName());
    }

    @Test
    public void testGetFirstBitarchiveDirectory11() throws IOException {
        final String[] TESTDIRS = {"/mnt/brz/brz_datasrc/wa004", "/mnt/brz/brz_datasrc/wa005",
                "/mnt/brz/brz_datasrc/wa006", "/mnt/brz/brz_datasrc/wa007", "/mnt/brz/brz_datasrc/wa008"};

        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();

        File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
        assertEquals("wa004", path.getParentFile().getParentFile().getName());
    }

    @Test
    public void testGetFirstBitarchiveDirectory12() throws IOException {
        final String[] TESTDIRS = {"/mnt/brz/brz_datasrc/wa005", "/mnt/brz/brz_datasrc/wa006",
                "/mnt/brz/brz_datasrc/wa007", "/mnt/brz/brz_datasrc/wa008"};

        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();

        File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
        assertEquals("wa005", path.getParentFile().getParentFile().getName());
    }

    @Test
    public void testGetFirstBitarchiveDirectory13() throws IOException {
        final String[] TESTDIRS = {"/mnt/brz/brz_datasrc/wa006", "/mnt/brz/brz_datasrc/wa007",
                "/mnt/brz/brz_datasrc/wa008"};

        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();

        File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
        assertEquals("wa006", path.getParentFile().getParentFile().getName());
    }

    @Test
    public void testGetFirstBitarchiveDirectory14() throws IOException {
        final String[] TESTDIRS = {"/mnt/brz/brz_datasrc/wa007", "/mnt/brz/brz_datasrc/wa008"};

        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();

        File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
        assertEquals("wa007", path.getParentFile().getParentFile().getName());
    }

    @Test
    public void testGetFirstBitarchiveDirectory15() throws IOException {
        final String[] TESTDIRS = {"/mnt/brz/brz_datasrc/wa008"};

        Settings.set(ArchiveSettings.BITARCHIVE_SERVER_FILEDIR, TESTDIRS);
        ad = BitarchiveAdmin.getInstance();

        File path = ad.getTemporaryPath(ARC_FILE_NAME, 1L);
        assertEquals("wa008", path.getParentFile().getParentFile().getName());
    }
}
