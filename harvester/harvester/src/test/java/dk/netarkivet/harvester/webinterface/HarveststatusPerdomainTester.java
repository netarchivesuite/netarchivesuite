/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.harvester.webinterface;

import javax.servlet.ServletException;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.jasper.JasperException;
import org.xml.sax.SAXException;

import dk.netarkivet.testutils.StringAsserts;


/**
 * lc forgot to comment this!
 *
 */

public class HarveststatusPerdomainTester extends JspTestCase {
    private static final String webPage = "/History/Harveststatus-perdomain.jsp";

    // Experimental!  Try to compile a JSP page and invoke the result.
    protected void setUp() throws MalformedURLException,
            IllegalAccessException, ParserConfigurationException,
            JasperException, ClassNotFoundException, InstantiationException {
        super.setUp(webPage);
    }

    public void testStandardSetup() throws IOException, ServletException,
            SAXException, ParserConfigurationException {
        request.setupAddParameter("domainName", (String)null);
        runPage();
//        assertValidXHTML();
    }

    public void testWildcardDomain() throws IOException, ServletException {
        request.setupAddParameter("domainName", "*");
        runPage();
        String contents = super.output.toString();
        StringAsserts.assertStringContains("Should have right title",
                "Domæner Som Matcher *", contents);
        StringAsserts.assertStringMatches("Should contain four domains",
                "(\\?domainName=.*){4}", contents);
    }
}
