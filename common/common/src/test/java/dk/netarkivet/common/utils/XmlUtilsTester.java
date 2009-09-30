/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import junit.framework.TestCase;
import org.dom4j.Document;
import org.dom4j.Node;

import dk.netarkivet.testutils.preconfigured.MoveTestFiles;


/**
 * Unit tests for the class XmlUtils.
 */
public class XmlUtilsTester extends TestCase {
    private final MoveTestFiles mtf = new MoveTestFiles(TestInfo.DATADIR,
            TestInfo.TEMPDIR);

    public XmlUtilsTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        mtf.setUp();
    }

    public void tearDown() throws Exception {
        mtf.tearDown();
        super.tearDown();
    }

    public void testSetNode() throws Exception {
        Document doc = XmlUtils.getXmlDoc(TestInfo.XML_FILE_1);
        Node node = doc.selectSingleNode(TestInfo.XML_FILE_1_XPATH_1);
        assertEquals("Should have original value at start",
                "Should go away", node.getText());
        XmlUtils.setNode(doc, TestInfo.XML_FILE_1_XPATH_1, "newValue");
        assertEquals("Should have new value after setting it",
                "newValue", node.getText());
    }
}