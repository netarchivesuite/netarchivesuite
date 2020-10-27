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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SlowTest;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.HarvesterSettings;

/**
 * Unit tests for the class TemplateDAO.
 */
public class TemplateDAOTester extends DataModelTestCase {
    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Category(SlowTest.class)
    @Test
    public void testGetAll() throws Exception {
        TemplateDAO dao = TemplateDAO.getInstance();
        Iterator<String> i = dao.getAll();
        // File[] order_files = TestInfo.BASE_DIR_ORDER_XML_TEMPLATES.listFiles(FileUtils.getXmlFilesFilter());
        StringBuffer sb = new StringBuffer();
        String templateName = null;
        int total = 0;
        while (i.hasNext()) {
            templateName = i.next();
            sb.append(templateName + ",");
            total++;
        }
        assertEquals("More or less templates found ", "FullSite-order,Max_20_2-order,OneLevel-order,default_orderxml,",
                sb.toString());
        HeritrixTemplate heritrixTemplate = dao.read(templateName);
        heritrixTemplate.setIsActive(!heritrixTemplate.isActive());
        dao.update(templateName, heritrixTemplate);
        assertEquals("Expect 1 inactive template now.", 1, getCount(dao, false));

        assertEquals("Expect number of active templates to have decreased by 1.", total-1, getCount(dao, true));
    }

    private int getCount(TemplateDAO dao, boolean isActive) {
        final Iterator<String> all = dao.getAll(isActive);
        int count = 0;
        while (all.hasNext()) {
            all.next();
            count++;
        }
        return count;
    }

    @Category(SlowTest.class)
    @Test
    public void testGetAllWithArg() throws Exception {
        TemplateDAO dao = TemplateDAO.getInstance();
        Iterator<String> i = dao.getAll(true);
        // File[] order_files = TestInfo.BASE_DIR_ORDER_XML_TEMPLATES.listFiles(FileUtils.getXmlFilesFilter());
        StringBuffer sb = new StringBuffer();
        while (i.hasNext()) {
            String templateName = i.next();
            sb.append(templateName + ",");
        }
        assertEquals("More or less templates found ", "FullSite-order,Max_20_2-order,OneLevel-order,default_orderxml,",
                sb.toString());
    }


    @Category(SlowTest.class)
    @Test
    public void testCreate() throws DocumentException {
        TemplateDAO dao = TemplateDAO.getInstance();
        String defaultOrderXmlName = Settings.get(HarvesterSettings.DOMAIN_DEFAULT_ORDERXML);
        assertTrue("The default orderxml should exist", dao.exists(defaultOrderXmlName));
        HeritrixTemplate doc = dao.read(defaultOrderXmlName);
        H1HeritrixTemplate docH1 = (H1HeritrixTemplate)doc;
        String doc1String = docH1.getTemplate().asXML();
        final String newOrderXmlName = "newTemplate";
        dao.create(newOrderXmlName, doc);
        assertTrue("The new orderxml should exist", dao.exists(newOrderXmlName));
        HeritrixTemplate newDoc = dao.read(newOrderXmlName);
        H1HeritrixTemplate newDocH1 = (H1HeritrixTemplate)newDoc;
        assertEquals("The XML for the new template should be the same", doc1String, 
        		newDocH1.getTemplate().asXML());

        doc = dao.read(defaultOrderXmlName);
        File f = new File("tests/dk/netarkivet/harvester/datamodel/data/default_orderxml.xml");
        Document doc2 = XmlUtils.getXmlDoc(f);
        dao.update(defaultOrderXmlName, new H1HeritrixTemplate(doc2));
        // Check, that when you read again the DOMAIN_DEFAULT_ORDERXML
        // You get the same content as you saved to the DB.
        
        HeritrixTemplate doc3H = dao.read(defaultOrderXmlName);
        H1HeritrixTemplate doc3H1 = (H1HeritrixTemplate) doc3H;
        Document doc3 = doc3H1.getTemplate();
        
        doc2.normalize();
        doc3.normalize();
        assertEquals("Text of doc2 and doc3 is equal", doc2.asXML(), doc3.asXML());
    }

    @Category(SlowTest.class)
    @Test
    public void testUpdate() throws Exception {
        TemplateDAO dao = TemplateDAO.getInstance();
        String defaultOrderXmlName = Settings.get(HarvesterSettings.DOMAIN_DEFAULT_ORDERXML);
        assertTrue("The default orderxml should exist", dao.exists(defaultOrderXmlName));

        // FIXME H1 template test ONLY
        Document doc = ((H1HeritrixTemplate)dao.read(defaultOrderXmlName)).getTemplate();

        assertNull("Template should have no foo element", doc.getRootElement().attribute("foo"));

        doc.getRootElement().addAttribute("foo", "bar");
        HeritrixTemplate temp = new H1HeritrixTemplate(doc);
        temp.setIsActive(false);
        dao.update(defaultOrderXmlName, temp);
        HeritrixTemplate readTemplate = dao.read(defaultOrderXmlName);
        Document doc2 = ((H1HeritrixTemplate) readTemplate).getTemplate();
        assertNotNull("Template should now have foo element", doc2.getRootElement().attribute("foo"));
        assertEquals("Foo element should be bar", "bar", doc2.getRootElement().attribute("foo").getStringValue());
        assertFalse("New version of template object should be inactive.", readTemplate.isActive());
    }

    /**
     * Reset the template DAO singleton. Only for use from tests!
     */
    public static void resetTemplateDAO() {
        TemplateDAO.resetSingleton();
    }

}
