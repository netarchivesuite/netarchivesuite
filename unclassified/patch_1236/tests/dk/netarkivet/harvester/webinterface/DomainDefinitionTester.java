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

import javax.servlet.jsp.PageContext;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.harvester.datamodel.Domain;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.DomainDAO;
import dk.netarkivet.harvester.datamodel.SeedList;

/**
 * Tests for class DomainDefinition associated with JSP-page
 * Definitions-edit-domain.jsp.
 */
public class DomainDefinitionTester extends WebinterfaceTestCase {
    public DomainDefinitionTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Test that we can change the default configuration of a domain.
     */
    public void testChangeDefaultConfiguration() {
        DomainDAO ddao = DomainDAO.getInstance();
        Domain domain = ddao.read("netarkivet.dk");
        long edition = domain.getEdition();
        String defaultConfig = domain.getDefaultConfiguration().getName();
        assertEquals("Default configuration should be the default",
                     "defaultconfig", defaultConfig);
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        TestServletRequest request = new TestServletRequest();
        parameterMap.put(Constants.DOMAIN_PARAM, new String[]{"netarkivet.dk"});
        parameterMap.put(Constants.DEFAULT_PARAM, new String[]{"conf2"});
        parameterMap.put(Constants.UPDATE_PARAM, new String[]{"1"});
        parameterMap.put(Constants.EDITION_PARAM, new String[]{edition + ""});
        parameterMap.put(Constants.CRAWLERTRAPS_PARAM, new String[]{""});
        parameterMap.put(Constants.COMMENTS_PARAM, new String[]{""});
        parameterMap.put(Constants.ALIAS_PARAM, new String[]{""});
        request.setParameterMap(parameterMap);
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new TestPageContext(request);
        DomainDefinition.processRequest(pageContext, I18N);
        defaultConfig = ddao.read(
                "netarkivet.dk").getDefaultConfiguration().getName();
        assertEquals("Default configuration should have been changed", "conf2",
                     defaultConfig);
    }

    /**
     * Test that we can add a new domain configuration.
     */
    public void testAddDomainConfiguration() {
        DomainDAO ddao = DomainDAO.getInstance();
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        parameterMap.put(Constants.UPDATE_PARAM, new String[]{"1"});
        parameterMap.put(Constants.DOMAIN_PARAM, new String[]{"netarkivet.dk"});
        parameterMap.put(Constants.DEFAULT_PARAM, new String[]{"conf2"});
        TestServletRequest request = new TestServletRequest();
        parameterMap.put("configName", new String[]{"new_config"});
        parameterMap.put("order_xml", new String[]{"OneLevel-order"});
        parameterMap.put("urlListList", new String[]{"seeds", "defaultseeds"});
        parameterMap.put(Constants.EDITION_PARAM, new String[]{1l + ""});
        parameterMap.put(Constants.CRAWLERTRAPS_PARAM, new String[]{""});
        parameterMap.put(Constants.COMMENTS_PARAM, new String[]{""});
        parameterMap.put(Constants.ALIAS_PARAM, new String[]{""});
        request.setParameterMap(parameterMap);
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new TestPageContext(request);
        DomainConfigurationDefinition.processRequest(pageContext, I18N);
        //
        // Check that the new configuration is created with all the right properties
        //
        DomainConfiguration newConf = ddao.read(
                "netarkivet.dk").getConfiguration("new_config");
        assertEquals("Name of configuration should be new_config", "new_config",
                     newConf.getName());
        assertEquals("Order template shoudl be one-level", "OneLevel-order",
                     newConf.getOrderXmlName());
        assertEquals("Load should be 60", 60, newConf.getMaxRequestRate());
        assertEquals("Max objects should be -1", -1, newConf.getMaxObjects());
        Iterator<SeedList> slIt = newConf.getSeedLists();
        // Iterator should return exactly two seedlists with names "seeds" and "defaultseeds"
        int slCount = 0;
        while (slIt.hasNext()) {
            String slName = slIt.next().getName();
            assertTrue("Seedlist should be in given list, not " + slName,
                       slName.equals("defaultseeds") || slName.equals("seeds"));
            slCount++;
        }
        assertEquals("Should be exactly two seedlists", 2, slCount);
    }

    /**
     * Test that we can change properties of a configuration.
     */
    public static void testUpdateConfiguration() {
        DomainDAO ddao = DomainDAO.getInstance();
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        parameterMap.put(Constants.UPDATE_PARAM, new String[]{"1"});
        parameterMap.put(Constants.DOMAIN_PARAM, new String[]{"netarkivet.dk"});
        parameterMap.put(Constants.DEFAULT_PARAM, new String[]{"conf2"});
        TestServletRequest request = new TestServletRequest();
        DomainConfiguration conf = ddao.read("netarkivet.dk").getConfiguration(
                "conf2");
        parameterMap.put("configName", new String[]{conf.getName()});
        parameterMap.put("order_xml", new String[]{conf.getOrderXmlName()});
        parameterMap.put("maxRate", new String[]{"20"});
        parameterMap.put("maxObjects", new String[]{"10"});
        parameterMap.put("urlListList", new String[]{"seeds"});
        parameterMap.put(Constants.EDITION_PARAM, new String[]{1l + ""});
        request.setParameterMap(parameterMap);
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new TestPageContext(request);
        DomainConfigurationDefinition.processRequest(pageContext, I18N);
        //
        // and check that values have been updated
        //
        DomainConfiguration newConf = ddao.read(
                "netarkivet.dk").getConfiguration("conf2");
        assertEquals("Name of configuration should be conf2", "conf2",
                     newConf.getName());
        assertEquals("Order template shoudl be FullSite", "FullSite-order",
                     newConf.getOrderXmlName());
        assertEquals("Load should be 20", 20, newConf.getMaxRequestRate());
        assertEquals("Max objects should be 10", 10, newConf.getMaxObjects());
        Iterator<SeedList> slIt = newConf.getSeedLists();
        // Iterator should return exactly one seedlists with names "seeds"
        int slCount = 0;
        while (slIt.hasNext()) {
            String slName = slIt.next().getName();
            assertTrue("Seedlist should be in given list, not " + slName,
                       slName.equals("seeds"));
            slCount++;
        }
        assertEquals("Should be exactly one seedlist", 1, slCount);
    }

    /**
     * Test that we can update the seeds associated with a given domain.
     */
    public void testUpdateUrlList() {
        DomainDAO ddao = DomainDAO.getInstance();
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        parameterMap.put(Constants.UPDATE_PARAM, new String[]{"1"});
        parameterMap.put(Constants.DOMAIN_PARAM, new String[]{"netarkivet.dk"});
        parameterMap.put(Constants.DEFAULT_PARAM, new String[]{"conf2"});
        TestServletRequest request = new TestServletRequest();
        parameterMap.put("urlListName", new String[]{"seeds"});
        parameterMap.put("seedList",
                         new String[]{"www.netarkivet.dk\nwww.netarchive.dk"});
        parameterMap.put(Constants.EDITION_PARAM, new String[]{1l + ""});
        request.setParameterMap(parameterMap);
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new TestPageContext(request);
        DomainSeedsDefinition.processRequest(pageContext, I18N);
        //
        // and check that values have been updated
        //
        SeedList sl = ddao.read("netarkivet.dk").getSeedList("seeds");
        List<String> seedList = sl.getSeeds();
        assertTrue("Seedlist should contain www.netarkivet.dk",
                   seedList.contains("www.netarkivet.dk"));
        assertTrue("Seedlist should contain www.netarchive.dk",
                   seedList.contains("www.netarchive.dk"));
        assertEquals("Seedlist should have two elements", 2, seedList.size());
    }

    /**
     * Test that we can update the crawlertraps associated
     * with a domain.
     */
    public void testSetCrawlertraps() {
        DomainDAO ddao = DomainDAO.getInstance();
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        parameterMap.put(Constants.UPDATE_PARAM, new String[]{"1"});
        parameterMap.put(Constants.DOMAIN_PARAM, new String[]{"netarkivet.dk"});
        parameterMap.put(Constants.DEFAULT_PARAM, new String[]{"conf2"});
        TestServletRequest request = new TestServletRequest();
        parameterMap.put(Constants.CRAWLERTRAPS_PARAM,
                         new String[]{".*/cgi-bin/.*\n.*/ignore/.*"});
        parameterMap.put(Constants.COMMENTS_PARAM, new String[]{""});
        parameterMap.put(Constants.ALIAS_PARAM, new String[]{""});
        parameterMap.put(Constants.EDITION_PARAM, new String[]{1l + ""});
        request.setParameterMap(parameterMap);
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new TestPageContext(request);
        DomainDefinition.processRequest(pageContext, I18N);
        //
        // and check that values have been updated
        //
        List<String> traps = ddao.read("netarkivet.dk").getCrawlerTraps();
        assertTrue("Did not find " + ".*/cgi-bin/.*",
                   traps.contains(".*/cgi-bin/.*"));
        assertTrue("Did not find " + ".*/ignore/.*",
                   traps.contains(".*/ignore/.*"));
    }

    /**
     * Test for impl. Traffic Reduction Task 4.1.2 - Add alias definition input to
     * interfaces.
     */
    public void testSetAlias() {
        DomainDAO ddao = DomainDAO.getInstance();
        String testAlias = "kb.dk";
        if (Domain.isValidDomainName(testAlias) && !ddao.exists(testAlias)) {
            Domain dd = Domain.getDefaultDomain(testAlias);
            ddao.create(dd);
        }
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        parameterMap.put(Constants.UPDATE_PARAM, new String[]{"1"});
        parameterMap.put(Constants.DOMAIN_PARAM, new String[]{"netarkivet.dk"});
        parameterMap.put(Constants.ALIAS_PARAM, new String[]{"kb.dk"});
        parameterMap.put(Constants.DEFAULT_PARAM, new String[]{"conf2"});
        parameterMap.put(Constants.EDITION_PARAM, new String[]{1l + ""});
        TestServletRequest request = new TestServletRequest();
        parameterMap.put(Constants.CRAWLERTRAPS_PARAM, new String[]{""});
        parameterMap.put(Constants.COMMENTS_PARAM, new String[]{""});
        request.setParameterMap(parameterMap);
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new TestPageContext(request);
        DomainDefinition.processRequest(pageContext, I18N);
        assertEquals("Netarkivet should be an alias of kb.dk now", "kb.dk",
                     ddao.read("netarkivet.dk").getAliasInfo().getAliasOf());
        parameterMap.put(Constants.ALIAS_PARAM, new String[]{
                ""}); // This means that the domain is no longer an alias.
        parameterMap.put(Constants.UPDATE_PARAM, new String[]{"1"});
        parameterMap.put(Constants.DOMAIN_PARAM, new String[]{"netarkivet.dk"});
        
        parameterMap.put(Constants.DEFAULT_PARAM, new String[]{"conf2"});
        parameterMap.put(Constants.EDITION_PARAM, new String[]{2l + ""});
        parameterMap.put(Constants.CRAWLERTRAPS_PARAM, new String[]{""});
        parameterMap.put(Constants.COMMENTS_PARAM, new String[]{""});
        request = new TestServletRequest();
        request.setParameterMap(parameterMap);
        DomainDefinition.processRequest(pageContext, I18N);
        assertEquals("Netarkivet should no longer be an alias", null,
                     ddao.read("netarkivet.dk").getAliasInfo());
    }

    /**
     * Test the makeDomainLink() method.
     */
    public void testMakeDomainLink() {
        String domainName = "foo.dk";
        assertEquals("Should have full HTML link",
                     "<a href=\"/HarvestDefinition/Definitions-edit-domain.jsp?name="
                + domainName + "\">" + domainName + "</a>",
                     DomainDefinition.makeDomainLink(domainName));

        try {
            DomainDefinition.makeDomainLink(null);
            fail("Should die on null domain Name");
        } catch (ArgumentNotValid e) {
            // expected
        }
    }

}
