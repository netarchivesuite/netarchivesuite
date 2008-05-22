/*$Id$
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
 * Created by IntelliJ IDEA.
 * User: lc
 * Date: Nov 4, 2004
 * Time: 11:26:36 AM
 * To change this template use File | Settings | File Templates.
 */
public class ARCReaderTester extends TestCase {
    public static final String ARCHIVE_DIR =
            "tests/dk/netarkivet/common/utils/arc/data/input/";
    public void testARCReaderClose() {
        try {
            FileUtils.copyFile(new File(ARCHIVE_DIR + "fyensdk.arc"),
                    new File(ARCHIVE_DIR + "working.arc"));
            ARCReader reader = ARCReaderFactory.get(new File(ARCHIVE_DIR + "working.arc"));
            ARCRecord record = (ARCRecord) reader.get(0);
            BitarchiveRecord rec =
                    new BitarchiveRecord(record);
            record.close();
            reader.close();
            new File(ARCHIVE_DIR + "working.arc").delete();
        } catch (IOException e) {
            fail("Should not throw IOException " + e);
        }

    }
}
