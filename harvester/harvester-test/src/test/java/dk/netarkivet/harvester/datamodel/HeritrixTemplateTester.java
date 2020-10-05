/*
 * #%L
 * Netarchivesuite - harvester - test
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
package dk.netarkivet.harvester.datamodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Node;
import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.harvesting.report.Heritrix1Constants;

/** Testclass for class HeritrixTemplate. */
@SuppressWarnings({"unchecked"})
public class HeritrixTemplateTester {

    /**
     * Simple test that construction works as intended with or without verification.
     */
    @Test
    public void testHeritrixTemplate() {
        Document doc = null;
        try {
            new H1HeritrixTemplate(doc);
            fail("ArgumentNotValid exception expected with null argument");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        doc = DocumentFactory.getInstance().createDocument();
        try {
            new H1HeritrixTemplate(doc);
            fail("ArgumentNotValid exception expected with empty document");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        try {
            new H1HeritrixTemplate(doc, false);
        } catch (ArgumentNotValid e) {
            fail("ArgumentNotValid exception not expected with verify set to false: " + e);
        }

        Object[][] orderXmls = { {"default_orderxml.xml", new Callback() {
            @Override
            public void check(File f) {
                checkArcValues(f);
            }
        }}, {"default_orderxml_warc.xml", new Callback() {
            @Override
            public void check(File f) {
                checkWarcValues(f);
            }
        }}, {"default_orderxml_arc_warc.xml", new Callback() {
            @Override
            public void check(File f) {
                checkArcValues(f);
                checkWarcValues(f);
            }
        }}};
        for (int i = 0; i < orderXmls.length; ++i) {
            checkHeritrixTemplate((String) orderXmls[i][0], (Callback) orderXmls[i][1]);
        }

        File f = new File(TestInfo.TOPDATADIR, "default_orderxml_nowriter.xml");
        doc = XmlUtils.getXmlDoc(f);
        try {
            H1HeritrixTemplate ht = new H1HeritrixTemplate(doc, true);
            assertFalse("HeritrixTemplate should be missing a write processor", ht.isVerified());
        } catch (ArgumentNotValid e) {
            // expected
        }
    }

    private interface Callback {
        public void check(File f);
    }

    private void checkHeritrixTemplate(String orderXmlFilename, Callback callback) {
        Document doc;
        File f = new File(TestInfo.TOPDATADIR, orderXmlFilename);
        doc = XmlUtils.getXmlDoc(f);

        try {
            new H1HeritrixTemplate(doc, true);
        } catch (ArgumentNotValid e) {
            fail("ArgumentNotValid exception not expected:" + e);
        }

        // Test failing cases
        doc = XmlUtils.getXmlDoc(f);
        List<Node> nodes = doc.selectNodes(H1HeritrixTemplate.GROUP_MAX_FETCH_SUCCESS_XPATH);
        for (Node n : nodes) {
            n.detach();
            doc.remove(n);
        }
        try {
            new H1HeritrixTemplate(doc, true);
            fail("Should have checked GroupMaxFetchSuccessXpath");
        } catch (ArgumentNotValid e) {
            // expected
        }

        // Check validation of GroupMaxFetchSuccessXpath
        doc = XmlUtils.getXmlDoc(f);

        checkLegalValues("The value should be legal for GroupMaxFetchSuccessXpath", doc,
                H1HeritrixTemplate.GROUP_MAX_FETCH_SUCCESS_XPATH, "10", "-1", "  30\n ", "1234567890");
        checkIllegalValues("The value should be illegal for GroupMaxFetchSuccessXpath", doc,
                H1HeritrixTemplate.GROUP_MAX_FETCH_SUCCESS_XPATH, "1+1", "  ", "+1", "abc", "0x40");

        // Check validation of GroupMaxAllKbXpath
        doc = XmlUtils.getXmlDoc(f);

        checkLegalValues("The value should be legal for GroupMaxAllKbXpath", doc,
                H1HeritrixTemplate.GROUP_MAX_ALL_KB_XPATH, "10", "-1", "  30\n ", "1234567890");
        checkIllegalValues("The value should be illegal for GroupMaxAllKbXpath", doc,
                H1HeritrixTemplate.GROUP_MAX_ALL_KB_XPATH, "1+1", "  ", "+1", "abc", "0x40");

        // Check validation of DECIDERULES_MAP_XPATH
        doc = XmlUtils.getXmlDoc(f);

        checkLegalValues("The value should be legal for DecideRulesMapXpath", doc,
                H1HeritrixTemplate.DECIDERULES_MAP_XPATH, "", "<map><foo/></map>", "bl<af=10\"<</</>");

        // Check validation of heritrixUserAgentXpath
        doc = XmlUtils.getXmlDoc(f);

        checkLegalValues("The value should be legal for heritrixUserAgentXpath", doc,
                H1HeritrixTemplate.HERITRIX_USER_AGENT_XPATH, "fnord (from +http://foo.bar/baz ) harvester",
                "fali(dah) (+https://127.0.0.1/)  ", "fnord qux (not http://foo.bar, but +http://baz.com instead) :)",
                "Mozilla/5.0 (compatible; heritrix/1.12.1 +http://my_website.com/my_infopage.html)\n" + "            ");
        checkIllegalValues("The value should be illegal for heritrixUserAgentXpath", doc,
                H1HeritrixTemplate.HERITRIX_USER_AGENT_XPATH, "http://foo.bar", "(+https://foo.bar)",
                "fnord (+netarchive.dk)", " (+http://foo.bar)", "(+http://a. b)", "foo (http://foo.bar)");

        // Check validation of heritrixFromXpath
        doc = XmlUtils.getXmlDoc(f);

        checkLegalValues("The value should be legal for heritrixFromXpath", doc, H1HeritrixTemplate.HERITRIX_FROM_XPATH,
                "foo@bar.baz", " foo@bar.com", "x+2@\"+3.@fkd-10.00");
        checkIllegalValues("The value should be illegal for heritrixFromXpath", doc,
                H1HeritrixTemplate.HERITRIX_FROM_XPATH, "@bar.com", "foO@bar", "bar.com");

        // Check validation of HeritrixTemplate.ARCHIVER_PATH_XPATH
        // Make sure that Heritrix writes the arcfiles to the correct dir.
        callback.check(f);
    }

    private void checkArcValues(File f) {
        Document doc = XmlUtils.getXmlDoc(f);
        checkLegalValues("The value should be legal for ARC_ARCHIVER_PATH_XPATH", doc,
                H1HeritrixTemplate.ARC_ARCHIVER_PATH_XPATH, dk.netarkivet.common.Constants.ARCDIRECTORY_NAME);
        checkIllegalValues("The value should be illegal for ARC_ARCHIVER_PATH_XPATH", doc,
                H1HeritrixTemplate.ARC_ARCHIVER_PATH_XPATH, "*", "", "bar.com");
    }

    private void checkWarcValues(File f) {
        Document doc = XmlUtils.getXmlDoc(f);
        checkLegalValues("The value should be legal for WARC_ARCHIVER_PATH_XPATH", doc,
                H1HeritrixTemplate.WARC_ARCHIVER_PATH_XPATH, dk.netarkivet.common.Constants.WARCDIRECTORY_NAME);
        checkIllegalValues("The value should be illegal for WARC_ARCHIVER_PATH_XPATH", doc,
                H1HeritrixTemplate.WARC_ARCHIVER_PATH_XPATH, "*", "", "bar.com");
    }

    private void checkIllegalValues(String msg, Document doc, String xpath, String... values) {
        for (String value : values) {
            try {
                XmlUtils.setNode(doc, xpath, value);
                H1HeritrixTemplate ht = new H1HeritrixTemplate(doc, true);
                assertFalse(msg + ": '" + value + "' should not be legal", ht.isVerified());
            } catch (ArgumentNotValid e) {
                // expected
            }
        }
    }

    /** Check that the given values are legal in the xpath given. */
    private void checkLegalValues(String msg, Document doc, String xpath, String... values) {
        for (String value : values) {
            try {
                XmlUtils.setNode(doc, xpath, value);
                H1HeritrixTemplate ht = new H1HeritrixTemplate(doc, true);
                assertTrue(msg + ": '" + value + "' should be legal", ht.isVerified());
            } catch (ArgumentNotValid e) {
                fail(msg + ": '" + value + "' should be legal: " + e);
            }
        }
    }

    /** Test methods getTemplate(), isVerified(). */
    @Test
    public void testIsVerified() {
        File f = new File(TestInfo.TOPDATADIR, "default_orderxml.xml");
        Document doc = XmlUtils.getXmlDoc(f);
        H1HeritrixTemplate ht = new H1HeritrixTemplate(doc);
        assertTrue("Should be true", ht.isVerified());
        ht = new H1HeritrixTemplate(doc, true);
        assertTrue("Should be true", ht.isVerified());
        ht = new H1HeritrixTemplate(doc, false);
        assertFalse("Should be false", ht.isVerified());
    }

    @Test
    public void testGetTemplate() {
        File f = new File(TestInfo.TOPDATADIR, "default_orderxml.xml");
        Document doc = XmlUtils.getXmlDoc(f);
        H1HeritrixTemplate ht = new H1HeritrixTemplate(doc);
        Document doc1 = ht.getTemplate();
        assertEquals("should have equal contents", doc1.asXML(), doc.asXML());
        String templateAsXML = ht.getXML();
        assertEquals("should have equal contents", templateAsXML, doc.asXML());
    }

    @Test
    public void testForDecidingScope() {
        File f = new File(TestInfo.TOPDATADIR, "default_orderxml.xml");
        Document doc = XmlUtils.getXmlDoc(f);
        String xpath = "/crawl-order/controller/newObject[@name='scope']"
        		+ "[@class='" + Heritrix1Constants.DECIDINGSCOPE_CLASSNAME + "']";
        Node node = doc.selectSingleNode(xpath);
        assertTrue("DecidingScope not found in order.xml", node != null);
        H1HeritrixTemplate ht = new H1HeritrixTemplate(doc);
        assertTrue("Order not verified", ht.isVerified());
    }

    @Test
    public void testEditOrderXML_ArchiveFormat() {
        File f = new File(TestInfo.TOPDATADIR, "default_orderxml.xml");
        HeritrixTemplate ht = HeritrixTemplate.read(f);
        ht.setArchiveFormat("arc");
        File fwarc = new File(TestInfo.TOPDATADIR, "default_orderxml_warc.xml");
        ht = HeritrixTemplate.read(fwarc);
        ht.setArchiveFormat("warc");
    }
}
