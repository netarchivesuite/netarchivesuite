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
package dk.netarkivet.archive.checksum;

import java.io.File;

public class TestInfo {

    static final File DATA_DIR = new File("tests/dk/netarkivet/archive/checksum/data");
    static final File WORKING_DIR = new File(DATA_DIR, "working");
    static final File TMP_DIR = new File(DATA_DIR, "tmp");
    static final File ORIGINAL_DIR = new File(DATA_DIR, "original");

    static final File UPLOAD_FILE_1 = new File(WORKING_DIR, "settings.xml");
    static final File UPLOAD_FILE_2 = new File(WORKING_DIR, "NetarchiveSuite-upload1.arc");

    static final File CHECKSUM_DIR = new File(WORKING_DIR, "cs");

    static final String TEST1_CHECKSUM = "616fdef40001383b80991b1b4d582a69";

    static final String TEST2_CHECKSUM = "d87cc8068fa49f3a4926ce4d1cdf14e1";
}
