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
package dk.netarkivet.harvester.datamodel;

import java.io.File;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.XmlUtils;

import junit.framework.TestCase;

/**
 * Testclass for class HeritrixTemplate.
 *
 */
public class HeritrixTemplateTester extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    /** Simple test that construction works as intended with
     *  or without verification.
     * */
    public void testContruction() {
        Document doc = null;
        try {
            new HeritrixTemplate(doc);
            fail("ArgumentNotValid exception expected with null argument");
        } catch (ArgumentNotValid e) {
            //Expected
        }        
        doc = DocumentFactory.getInstance().createDocument();
        try {
            new HeritrixTemplate(doc);
            fail("ArgumentNotValid exception expected with empty document");
        } catch (ArgumentNotValid e) {
            //Expected
        }
       
        try {
            new HeritrixTemplate(doc, false);
        } catch (ArgumentNotValid e) {
            fail("ArgumentNotValid exception not expected with verify set to false: " + e);
        }
        
        File f = new File(TestInfo.TOPDATADIR, "default_orderxml.xml");
        doc = XmlUtils.getXmlDoc(f);
              
        try {
            new HeritrixTemplate(doc, true);
        } catch (ArgumentNotValid e) {
            fail("ArgumentNotValid exception not expected:" + e);
        }
    }
    
    /** 
     * Test methods getTemplate(), isVerified().
     */
    public void testGetters() {
        File f = new File(TestInfo.TOPDATADIR, "default_orderxml.xml");
        Document doc = XmlUtils.getXmlDoc(f);
        HeritrixTemplate ht = new HeritrixTemplate(doc);
        assertTrue("Should be true", ht.isVerified());
        ht = new HeritrixTemplate(doc, true);
        assertTrue("Should be true", ht.isVerified());
        ht = new HeritrixTemplate(doc, false);
        assertFalse("Should be false", ht.isVerified());
        Document doc1 = ht.getTemplate();
        assertEquals("should have equal contents", doc1.asXML(), doc.asXML());
        String templateAsXML = ht.getXML();
        assertEquals("should have equal contents", templateAsXML, doc.asXML());
    }   
}
