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
import java.util.List;

import junit.framework.TestCase;

import org.archive.crawler.deciderules.DecidingScope;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Node;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.XmlUtils;

/** Testclass for class HeritrixTemplate. */
public class HeritrixTemplateTester extends TestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Simple test that construction works as intended with or without
     * verification.
     */
    public void testHeritrixTemplate() {
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
            fail("ArgumentNotValid exception not expected with verify set to false: "
                 + e);
        }

        File f = new File(TestInfo.TOPDATADIR, "default_orderxml.xml");
        doc = XmlUtils.getXmlDoc(f);

        try {
            new HeritrixTemplate(doc, true);
        } catch (ArgumentNotValid e) {
            fail("ArgumentNotValid exception not expected:" + e);
        }

        // Test failing cases
        doc = XmlUtils.getXmlDoc(f);
        List<Node> nodes =
                doc.selectNodes(HeritrixTemplate.QUEUE_TOTAL_BUDGET_XPATH);
        for (Node n : nodes) {
            n.detach();
            doc.remove(n);
        }
        try {
            new HeritrixTemplate(doc, true);
            fail("Should have checked QueueTotalBudgetXpath");
        } catch (ArgumentNotValid e) {
            // expected
        }

        // Check validation of QueueTotalBudgetXpath
        doc = XmlUtils.getXmlDoc(f);

        checkLegalValues("The value should be legal for QueueTotalBudgetXpath",
                         doc, HeritrixTemplate.QUEUE_TOTAL_BUDGET_XPATH,
                         "10", "-1", "  30\n ", "1234567890");
        checkIllegalValues("The value should be illegal for QueueTotalBudgetXpath",
                           doc, HeritrixTemplate.QUEUE_TOTAL_BUDGET_XPATH,
                           "1+1", "  ", "+1", "abc", "0x40");

        // Check validation of GroupMaxAllKbXpath
        doc = XmlUtils.getXmlDoc(f);

        checkLegalValues("The value should be legal for GroupMaxAllKbXpath",
                         doc, HeritrixTemplate.GROUP_MAX_ALL_KB_XPATH,
                         "10", "-1", "  30\n ", "1234567890");
        checkIllegalValues("The value should be illegal for GroupMaxAllKbXpath",
                           doc, HeritrixTemplate.GROUP_MAX_ALL_KB_XPATH,
                           "1+1", "  ", "+1", "abc", "0x40");

        // Check validation of DECIDERULES_MAP_XPATH
        doc = XmlUtils.getXmlDoc(f);

        checkLegalValues("The value should be legal for DecideRulesMapXpath",
                         doc, HeritrixTemplate.DECIDERULES_MAP_XPATH,
                        "", "<map><foo/></map>", "bl<af=10\"<</</>");

        // Check validation of heritrixUserAgentXpath
        doc = XmlUtils.getXmlDoc(f);

        checkLegalValues("The value should be legal for heritrixUserAgentXpath",
                         doc, HeritrixTemplate.HERITRIX_USER_AGENT_XPATH,
                         "fnord (from +http://foo.bar/baz ) harvester",
                         "fali(dah) (+https://127.0.0.1/)  ",
                         "fnord qux (not http://foo.bar, but +http://baz.com instead) :)",
                         "Mozilla/5.0 (compatible; heritrix/1.12.1 +http://my_website.com/my_infopage.html)\n"
                         + "            ");
        checkIllegalValues("The value should be illegal for heritrixUserAgentXpath",
                           doc, HeritrixTemplate.HERITRIX_USER_AGENT_XPATH,
                           "http://foo.bar", "(+https://foo.bar)",
                           "fnord (+netarchive.dk)", " (+http://foo.bar)",
                           "(+http://a. b)", "foo (http://foo.bar)");

        // Check validation of heritrixFromXpath
        doc = XmlUtils.getXmlDoc(f);

        checkLegalValues("The value should be legal for heritrixFromXpath",
                         doc, HeritrixTemplate.HERITRIX_FROM_XPATH,
                         "foo@bar.baz", " foo@bar.com", "x+2@\"+3.@fkd-10.00");
        checkIllegalValues("The value should be illegal for heritrixFromXpath",
                           doc, HeritrixTemplate.HERITRIX_FROM_XPATH,
                           "@bar.com", "foO@bar", "bar.com");
        
        // Check validation of HeritrixTemplate.ARCHIVER_PATH_XPATH
        // Make sure that Heritrix writes the arcfiles to the correct dir.
        doc = XmlUtils.getXmlDoc(f);
        checkLegalValues("The value should be legal for ARCHIVER_PATH_XPATH",
                doc, HeritrixTemplate.ARCHIVER_PATH_XPATH,
                dk.netarkivet.common.Constants.ARCDIRECTORY_NAME);
        
        checkIllegalValues("The value should be illegal for ARCHIVER_PATH_XPATH",
                doc, HeritrixTemplate.ARCHIVER_PATH_XPATH,
                "*", "", "bar.com");
   }

    private void checkIllegalValues(String msg, Document doc,
                                    String xpath, String... values) {
        for (String value : values) {
            try {
                XmlUtils.setNode(doc, xpath, value);
                new HeritrixTemplate(doc, true);
                fail(msg + ": '" + value + "' should not be legal");
            } catch (ArgumentNotValid e) {
                // expected
            }
        }
    }

    /** Check that the given values are legal in the xpath given */
    private void checkLegalValues(String msg, Document doc,
                                  String xpath, String... values) {
        for (String value : values) {
            try {
                XmlUtils.setNode(doc, xpath, value);
                    new HeritrixTemplate(doc, true);
            } catch (ArgumentNotValid e) {
                fail(msg + ": '" + value + "' should be legal: " + e);
            }
        }
    }

    /** Test methods getTemplate(), isVerified(). */
    public void testIsVerified() {
        File f = new File(TestInfo.TOPDATADIR, "default_orderxml.xml");
        Document doc = XmlUtils.getXmlDoc(f);
        HeritrixTemplate ht = new HeritrixTemplate(doc);
        assertTrue("Should be true", ht.isVerified());
        ht = new HeritrixTemplate(doc, true);
        assertTrue("Should be true", ht.isVerified());
        ht = new HeritrixTemplate(doc, false);
        assertFalse("Should be false", ht.isVerified());
    }

    public void testGetTemplate() {
        File f = new File(TestInfo.TOPDATADIR, "default_orderxml.xml");
        Document doc = XmlUtils.getXmlDoc(f);
        HeritrixTemplate ht = new HeritrixTemplate(doc);
        Document doc1 = ht.getTemplate();
        assertEquals("should have equal contents", doc1.asXML(), doc.asXML());
        String templateAsXML = ht.getXML();
        assertEquals("should have equal contents", templateAsXML, doc.asXML());
    }
    
    public void testForDecidingScope() {
        
        File f = new File(TestInfo.TOPDATADIR, "default_orderxml.xml");
        Document doc = XmlUtils.getXmlDoc(f);        
        String xpath = "/crawl-order/controller/newObject[@name='scope']"
            + "[@class='" + DecidingScope.class.getName()
            + "']";
        Node node = doc.selectSingleNode(xpath);
        assertTrue("DecidingScope not found in order.xml", node != null);
        HeritrixTemplate ht = new HeritrixTemplate(doc);
        assertTrue("Order not verified", ht.isVerified());
    }
}
