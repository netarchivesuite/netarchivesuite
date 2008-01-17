/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.netarkivet.common.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import dk.netarkivet.common.exceptions.IOFailure;


/**
 *
 */
public class TestInfo {
    public static final File BASEDIR = new File("./tests/dk/netarkivet/common/utils");
    public static final File TEMPDIR = new File(BASEDIR, "working");
    public static final File DATADIR = new File(BASEDIR, "data");
    public static final File TESTXML = new File(TEMPDIR, "test.xml");
    public static final File NEWXML = new File(TEMPDIR, "savedtest.xml");
    public static final File INVALIDXML = new File(TEMPDIR, "invalid.xml");
    public static final String SETTINGSFILENAME =
            new File(TEMPDIR, "settings.xml").getAbsolutePath();
    public static final String DEFAULTSEEDLIST
            = "settings.harvester.datamodel.domain.defaultSeedlist";
    public static final String DEFAULTSEEDLIST_VALUE = "defaultseeds";
    public static final String PORT = "settings.common.jms.port";
    public static final String PORTVALUE = "7676";
    public static final String TIMEOUT = "settings.common.arcrepositoryClient.timeout";
    public static final String FIVE_HUNDRED_MEGA_FILE_ZIPPED = "500-mega.zip";
    public static final String FIVE_HUNDRED_MEGA_FILE_UNZIPPED = "500-mega";
    public static final File MD5_EMPTY_FILE = new File(TEMPDIR, "MD5-empty-file");
    public static final String UNUSED_PROPERTY = "settings.never.ever.define.this.property";

    static final File FILEUTILS_DATADIR = new File(BASEDIR, "fileutils_data");
    static final File CDX_FILTER_TEST_DATA_DIR = new File(FILEUTILS_DATADIR,"cdx-filter");
    static final Set<String> CDX_FILTER_TEST_CDX_FILES =
        new HashSet<String>(java.util.Arrays.asList(new String[]{
                "Reader1.cdx",
                "Reader2.cdx"
        }));
    public static final File XML_FILE_1 = new File(TestInfo.TEMPDIR, "test1.xml");
    public static final File XML_FILE_2 = new File(TestInfo.TEMPDIR, "test2.xml");
    public static final String XML_FILE_1_XPATH_1 = "/test/file/attachHere";
    public static final String LAST_LINE_TEXT = "last line";
    public static final File FILE_WITH_NEWLINE_AT_END = new File(TEMPDIR, "newlinedfile.txt");
    public static final File FILE_WITH_NO_NEWLINE_AT_END = new File(TEMPDIR, "nonewlinedfile.txt");
    public static final File FILE_WITH_ONE_LINE = new File(TEMPDIR, "onelinedfile.txt");
    public static final File EMPTY_FILE = new File(TEMPDIR, "emptyfile.txt");
    public static File LOG_FILE = new File("tests/testlogs/netarkivtest.log");
    public static final File ZIPDIR = new File(TEMPDIR, "zipdir");
    public static final File ZIPPED_DIR = new File(TestInfo.ZIPDIR, "zippedDir.zip");

    public static final File NON_EXISTING_FILE = new File("/no/such/file");
    public static final File SETTINGS_FILE = new File(TEMPDIR, "settings.xml");


    /**
     * This method unzips the
     * @param aZipFile
     * @param destinationDirectory
     * @return true (if successfull)
     */
    public static boolean unzipTo(File aZipFile, File destinationDirectory) {
        try {
            final int BUFFER = 2048;
            BufferedOutputStream dest = null;
            FileInputStream fis = new FileInputStream(aZipFile);
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {

                int count;
                byte[] data = new byte[BUFFER];

                // write the files to directory destinationDirectory
                File unzippedFile = new File(destinationDirectory,
                                             entry.getName());
                FileOutputStream fos = new FileOutputStream(unzippedFile);

                dest = new BufferedOutputStream(fos, BUFFER);

                while ((count = zis.read(data, 0, BUFFER)) != -1) {
                    dest.write(data, 0, count);
                }

                dest.flush();
                dest.close();
            }

            zis.close();
        } catch (Exception e) {
            throw new IOFailure("Unzipping of file " +
                                aZipFile.getAbsolutePath() + " to directory " +
                                destinationDirectory + " failed!", e.getCause());
        }

        return true;
    }
}
