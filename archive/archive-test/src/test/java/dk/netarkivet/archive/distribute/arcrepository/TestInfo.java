/*
 * #%L
 * Netarchivesuite - archive - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
package dk.netarkivet.archive.distribute.arcrepository;

import java.io.File;
import java.net.URI;

import dk.netarkivet.common.utils.arc.ARCKey;

/**
 * Constants for shared use by viewerproxy unit tests.
 */
public class TestInfo {

    private static final File BASE_DIR = new File("tests/dk/netarkivet/archive/distribute/arcrepository/data");
    public static final File WORKING_DIR = new File(BASE_DIR, "working");
    public static final File ORIGINALS_DIR = new File(BASE_DIR, "originals");
    /**
     * An archive directory to work on.
     */
    public static final File ARCHIVE_DIR = new File(WORKING_DIR, "bitarchive1");
    public static final File INDEX_DIR_2_3 = new File(TestInfo.WORKING_DIR, "2-3-cache");
    public static final File INDEX_DIR_2_4_3_5 = new File(TestInfo.WORKING_DIR, "2-4-3-5-cache");
    public static URI GIF_URL;
    public static final File LOG_PATH = new File(WORKING_DIR, "tmp");
    /** The key listed for GIF_URL. */
    public static final ARCKey GIF_URL_KEY = new ARCKey(
            "2-2-20060731110420-00000-sb-test-har-001.statsbiblioteket.dk.arc", 73269);
    public static final File SAMPLE_FILE = new File(WORKING_DIR, "testFile.txt");
    public static final File SAMPLE_FILE_COPY = new File(WORKING_DIR, "testCopy.txt");
    public static final File EMPTY_FILE = new File(WORKING_DIR, "zeroByteFile");

}
