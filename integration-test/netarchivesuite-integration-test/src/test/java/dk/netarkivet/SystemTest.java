/* File:            $Id$
 * Revision:        $Revision$
 * Author:          $Author$
 * Date:            $Date$
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
package dk.netarkivet;

import org.apache.log4j.Logger;
import org.jaccept.structure.ExtendedTestCase;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;

import com.thoughtworks.selenium.DefaultSelenium;
import com.thoughtworks.selenium.Selenium;

public class SystemTest extends ExtendedTestCase {

    protected Selenium selenium;

    protected final Logger log = TestLogger.getLogger(getClass());

    @BeforeClass
    @Parameters( { "selenium.host", "selenium.port", "selenium.browser",
            "selenium.url" })
    public void startSelenium(@Optional("localhost")
    String host, @Optional("4444")
    String port, @Optional("*firefox")
    String browser, @Optional("http://kb-test-adm-001.kb.dk")
    String url) {
        selenium = new DefaultSelenium(host, Integer.parseInt(port), browser,
                url + ":" + getPort() + "/");
        selenium.start();
        selenium.open("/HarvestDefinition/");
    }

    @AfterClass(alwaysRun = true)
    public void stopSelenium() {
        this.selenium.stop();
    }

    public String getPort() {
        return System.getProperty("systemtest.port", "8071");
    }
}
