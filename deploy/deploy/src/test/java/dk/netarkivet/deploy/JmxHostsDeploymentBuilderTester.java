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
package dk.netarkivet.deploy;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import junit.framework.TestCase;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

public class JmxHostsDeploymentBuilderTester extends TestCase {
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
            TestInfo.WORKING_DIR);

    public JmxHostsDeploymentBuilderTester(String s) {
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

    public void testBuilder() throws DocumentException {
        StringBuilder result = new StringBuilder();
        DeploymentBuilder builder = new JmxHostsDeploymentBuilder(result);
        
        ItConfigParser itconfigHandler = new ItConfigParser(builder);
        //Create parser
        SAXParserFactory factory = SAXParserFactory.newInstance();
        try {
            SAXParser saxParser = factory.newSAXParser();
            //Proxy all the beans
            saxParser.parse(TestInfo.IT_CONF_FILE, itconfigHandler);
        } catch (Exception e) {
            throw new IOFailure("Failed to parse it-config", e);
        }
        // DeploymentBuilder cleanup and more (??)
        // is done in the done() method.
        builder.done();
        Document d = DocumentHelper.parseText(result.toString());
        Document d2 = DocumentHelper.parseText("<settings xmlns=\"http://www.netarkivet.dk/schemas/monitor_settings\"><monitor><jmxMonitorRolePassword>test</jmxMonitorRolePassword></monitor></settings>");
        d.normalize();
        d2.normalize();
        assertEquals("Should get correct result when parsing itconfig to get "
                     + "monitor settings", d.asXML(), d2.asXML());
    }
    
    
}
