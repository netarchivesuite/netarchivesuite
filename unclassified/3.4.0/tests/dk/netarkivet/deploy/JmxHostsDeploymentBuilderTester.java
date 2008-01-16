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

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.deploy.DeploymentBuilder;
import dk.netarkivet.deploy.ItConfigParser;
import dk.netarkivet.deploy.JmxHostsDeploymentBuilder;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;

import junit.framework.TestCase;

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

    public void testBuilder() {
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
    }
    
    
}
