/*
 * #%L
 * Netarchivesuite - common - test
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
package dk.netarkivet.common.utils.arc;

import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import junit.framework.TestCase;

import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.util.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 * A simple test of the ARCREADER that is bundled with Heritrix 1.14.4. 
 */
@SuppressWarnings({ "unused"})
public class ARCReaderTester extends TestCase {
    public static final String ARCHIVE_DIR =
            "tests/dk/netarkivet/common/utils/arc/data/input/";
    public static final String testFileName = "working.arc";
    
    public void testARCReaderClose() {
        try {
            final File testfile = new File(ARCHIVE_DIR + testFileName);
            FileUtils.copyFile(new File(ARCHIVE_DIR + "fyensdk.arc"),
                    testfile);
            
            ARCReader reader = ARCReaderFactory.get(testfile);
            ARCRecord record = (ARCRecord) reader.get(0);
            BitarchiveRecord rec =
                    new BitarchiveRecord(record, testFileName);
            record.close();
            reader.close();
            testfile.delete();
        } catch (IOException e) {
            fail("Should not throw IOException " + e);
        }

    }
}
