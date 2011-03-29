/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;

import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.testutils.StringAsserts;


/**
 * Unit tests for the class TemplateDAO.
 *
 */
public class TemplateDAOTester extends DataModelTestCase {
    public TemplateDAOTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetAll() throws Exception {
        TemplateDAO dao = TemplateDAO.getInstance();
        Iterator<String> i = dao.getAll();
        //File[] order_files = TestInfo.BASE_DIR_ORDER_XML_TEMPLATES.listFiles(FileUtils.getXmlFilesFilter());
        StringBuffer sb = new StringBuffer();
        while (i.hasNext()) {
            String templateName = i.next();
            sb.append(templateName + ",");
        }
        assertEquals("More or less templates found ",
                "FullSite-order,Max_20_2-order,OneLevel-order,default_orderxml,",
                sb.toString());
    }

    public void testCreate() throws DocumentException {
        TemplateDAO dao = TemplateDAO.getInstance();
        String defaultOrderXmlName = Settings.get(
                HarvesterSettings.DOMAIN_DEFAULT_ORDERXML);
        assertTrue("The default orderxml should exist",
                dao.exists(defaultOrderXmlName));
        HeritrixTemplate doc = dao.read(defaultOrderXmlName);
        String doc1String = doc.getTemplate().asXML();
        final String newOrderXmlName = "newTemplate";
        dao.create(newOrderXmlName, doc);
        assertTrue("The new orderxml should exist",
                dao.exists(newOrderXmlName));
        HeritrixTemplate newDoc = dao.read(newOrderXmlName);
        assertEquals("The XML for the new template should be the same",
                doc1String, newDoc.getTemplate().asXML());

        doc = dao.read(defaultOrderXmlName);
        File f = new File("tests/dk/netarkivet/harvester/datamodel/data/default_orderxml.xml");
        Document doc2 = XmlUtils.getXmlDoc(f);
        dao.update(defaultOrderXmlName, new HeritrixTemplate(doc2));
        // Check, that when you read again the DOMAIN_DEFAULT_ORDERXML
        // You get the same content as you saved to the DB.
        Document doc3 = dao.read(defaultOrderXmlName).getTemplate();
        doc2.normalize();
        doc3.normalize();
        assertEquals( "Text of doc2 and doc3 is equal", doc2.asXML(), doc3.asXML() );
    }

    public void testUpdate() throws Exception {
        TemplateDAO dao = TemplateDAO.getInstance();
        String defaultOrderXmlName = Settings.get(
                HarvesterSettings.DOMAIN_DEFAULT_ORDERXML);
        assertTrue("The default orderxml should exist",
                dao.exists(defaultOrderXmlName));

        Document doc = dao.read(defaultOrderXmlName).getTemplate();
        
        assertNull("Template should have no foo element",
                doc.getRootElement().attribute("foo"));

        doc.getRootElement().addAttribute("foo", "bar");
        dao.update(defaultOrderXmlName, new HeritrixTemplate(doc));
        Document doc2 = dao.read(defaultOrderXmlName).getTemplate();
        assertNotNull("Template should now have foo element",
                doc2.getRootElement().attribute("foo"));
        assertEquals("Foo element should be bar",
                "bar", doc2.getRootElement().attribute("foo").getStringValue());
    }

    public void testDelete() {
        TemplateDAO dao = TemplateDAO.getInstance();
        String defaultOrderXmlName = Settings.get(
                HarvesterSettings.DOMAIN_DEFAULT_ORDERXML);
        assertTrue("The default orderxml should exist",
                dao.exists(defaultOrderXmlName));
        dao.delete(defaultOrderXmlName);
        assertFalse("The default orderxml should not exist anymore",
                dao.exists(defaultOrderXmlName));

        // Test that you cannot delete a template in use.
        try {
            dao.delete("FullSite-order");
            fail("Should not be allowed to delete template in use");
        } catch (PermissionDenied e) {
            // Expected
            for (String domainName : new String[] {
                "dr.dk", "kb.dk", "netarkivet.dk", "statsbiblioteket.dk"
            }) {
                StringAsserts.assertStringContains("Should mention that "
                        + domainName + " uses the template",
                        domainName, e.getMessage());
            }
        }
    }

    /**
     * Reset the template DAO singleton.  Only for use from tests!
     */
    public static void resetTemplateDAO() {
        TemplateDAO.resetSingleton();
    }


}
