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
package dk.netarkivet.harvester.harvesting;
import java.io.File;

import junit.framework.TestCase;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.testutils.preconfigured.MockupIndexServer;
import dk.netarkivet.testutils.preconfigured.MockupJMS;

/**
 * Unittests for the class HeritrixFiles.
 */
public class HeritrixFilesTester extends TestCase {

    private MockupJMS mjms = new MockupJMS();
    private File resultFile = new File(TestInfo.HERITRIX_TEMP_DIR, "result");
    private MockupIndexServer mis = new MockupIndexServer(resultFile);

    public HeritrixFilesTester(String s) {
        super(s);
    }

    public void setUp() {
       TestInfo.WORKING_DIR.mkdirs();
       mjms.setUp();
       mis.setUp();
    }

    public void tearDown() {
        mis.tearDown();
        mjms.tearDown();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
    }

    /**
     * Test correct behaviour of the HeritrixFiles contructor.
     *
     */
    public void testConstructor() {
        try {
            new HeritrixFiles(null, 0, 0);
            fail("Invalid arguments should throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        HeritrixFiles hf = null;
        TestInfo.HERITRIX_TEMP_DIR.mkdir();
        hf = new HeritrixFiles(TestInfo.HERITRIX_TEMP_DIR, 42, 42);

        // check, that crawlDir is correctly set
        assertEquals("crawlDir should be set up correctly.",
                     TestInfo.HERITRIX_TEMP_DIR.getAbsolutePath(),
                     hf.getCrawlDir().getAbsolutePath());

        // check, that arcFilePrefix is correctly set
        assertEquals("arcFilePrefix should contain job id and harvest id",
                     "42-42", hf.getArcFilePrefix());
    }

    /**
     * Test, that writeOrderXml fails correctly with bad arguments:
     * - null argument
     * - Document object with no contents.
     *
     * Bug 871 caused this test to be written.
     */
    public void testWriteOrderXml(){
        TestInfo.HERITRIX_TEMP_DIR.mkdir();
        HeritrixFiles hf =
            new HeritrixFiles(TestInfo.HERITRIX_TEMP_DIR, 42, 42);
        try {
            hf.writeOrderXml(null);
            fail("ArgumentNotValid exception with null Document");
        } catch (ArgumentNotValid e) {
            //Expected
        }
        DocumentFactory docFactory = DocumentFactory.getInstance();
        try {
            hf.writeOrderXml(docFactory.createDocument());
            fail("ArgumentNotValid exception with Document with no contents");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        // test, that order xml is written, if argument is valid

        Document doc = XmlUtils.getXmlDoc(TestInfo.ORDER_FILE);
        try {
            hf.writeOrderXml(doc);
        } catch (Exception e) {
            fail("Exception not expected: " + e);
        }
    }
    /**
     * Test, that writeSeedsTxt fails correctly with bad arguments:
     * - null argument
     * - empty String
     *
     * Bug 871 caused this test to be written.
     */
    public void testWriteSeedsTxt() {
        TestInfo.HERITRIX_TEMP_DIR.mkdir();
        HeritrixFiles hf =
            new HeritrixFiles(TestInfo.HERITRIX_TEMP_DIR, 42, 42);
        try {
            hf.writeSeedsTxt(null);
            fail("ArgumentNotValid exception with null seeds");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            hf.writeSeedsTxt("");
            fail("ArgumentNotValid exception with seeds equal to empty string");
        } catch (ArgumentNotValid e) {
            //Expected
        }

        try {
            hf.writeSeedsTxt("www.netarkivet.dk\nwww.sulnudu.dk");
        } catch (Exception e) {
            fail("Exception not expected with seeds equal to valid non-empty String object" + e);
        }
    }
    
    /** Check, that the getArcsDir method works.*/
    public void testGetArcsDir() {
        TestInfo.HERITRIX_TEMP_DIR.mkdir();
        HeritrixFiles hf =
            new HeritrixFiles(TestInfo.HERITRIX_TEMP_DIR, 42, 42);
        File arcsdir = hf.getArcsDir();
        assertEquals("Wrong arcsdir", new File(TestInfo.HERITRIX_TEMP_DIR,
                        dk.netarkivet.common.Constants.ARCDIRECTORY_NAME),
                        arcsdir);
    }
}