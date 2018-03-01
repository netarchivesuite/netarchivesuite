/*
 * #%L
 * Netarchivesuite - common - test
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
package dk.netarkivet.common.tools;

import java.io.File;

public class TestInfo {
    static final File WORKING_DIR = new File("./tests/dk/netarkivet/common/tools/working/");
    static final File DATA_DIR = new File("./tests/dk/netarkivet/common/tools/data/originals");

    static final File METADATA_DIR = new File(DATA_DIR, "admindatadir");

    // Three ARC files with one record in each:
    static final File ARC1 = new File(WORKING_DIR, "test1.arc");
    static final File ARC2 = new File(WORKING_DIR, "test2.arc");
    static final File ARC3 = new File(WORKING_DIR, "test3.arc");
    // For each of the above, description of the contained record:
    static final String ARC1_CONTENT = "First test content.";
    static final String ARC2_CONTENT = "Second test content.";
    static final String ARC3_CONTENT = "Third test content.";
    static final String ARC1_MIME = "text/plain";
    static final String ARC2_MIME = "text/plain";
    static final String ARC3_MIME = "application/x-text";
    static final String ARC1_URI = "testdata://netarkivet.dk/test/foo/1";
    static final String ARC2_URI = "testdata://netarkivet.dk/test/foo/2";
    static final String ARC3_URI = "testdata://netarkivet.dk/test/bar/1";
    // A CDX file:
    static final File INDEX_FILE = new File(WORKING_DIR, "2-3-cache.zip");
}
