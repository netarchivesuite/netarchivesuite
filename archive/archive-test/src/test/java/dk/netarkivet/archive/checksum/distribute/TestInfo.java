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
package dk.netarkivet.archive.checksum.distribute;

import java.io.File;

public class TestInfo {

    static final File ORIGINAL_DIR = new File("tests/dk/netarkivet/archive/checksum/distribute/data/original");
    static final File WORK_DIR = new File("tests/dk/netarkivet/archive/checksum/distribute/data/working");

    static final File UPLOADMESSAGE_TESTFILE_1 = new File(WORK_DIR, "NetarchiveSuite-test1.arc");

    static final File BASE_FILE_DIR = new File(TestInfo.WORK_DIR, "basefiledir");

    static final File CORRECTMESSAGE_TESTFILE_1 = new File(BASE_FILE_DIR, "NetarchiveSuite-test1.arc");
    static final File CORRECTMESSAGE_TESTFILE_2 = new File(BASE_FILE_DIR, "NetarchiveSuite-correct2.arc");

    static final File CHECKSUM_FILE = new File(BASE_FILE_DIR, "checksum_THREE.md5");

    static final String UPLOADFILE_1_CHECKSUM = "d87cc8068fa49f3a4926ce4d1cdf14e1";
    static final String CORRECTFILE_1_CHECKSUM = "98ed62d461697085c802011f6fd7716d";
}
