/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.util.Date;
import java.util.Iterator;


/**
 * Tests for class dk.netarkivet.datamodel.FullHarvest
 */


public class FullHarvestTester extends DataModelTestCase {
    public FullHarvestTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    /** Test that the maxbytes field is correctly stored and reloaded.
     * This field is added to the DB after the DB went into production, so
     * unit tests are needed.
     * @throws Exception
     */
    public void testMaxBytes() throws Exception {
        final HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        FullHarvest fh = HarvestDefinition.createFullHarvest("testfullharvest",
                                                             "comment", null, 200L, Constants.DEFAULT_MAX_BYTES);
        assertEquals("Should have default number of max bytes from start",
                     Constants.DEFAULT_MAX_BYTES, fh.getMaxBytes());
        final int maxBytes = 201 * 1024 * 1024;
        fh.setMaxBytes(maxBytes);
        assertEquals("Should have set number of max bytes after update",
                     maxBytes, fh.getMaxBytes());
        hddao.create(fh);
        FullHarvest fh2 = (FullHarvest)hddao.read(fh.getOid());
        assertEquals("Should have set number of max bytes after read",
                     maxBytes, fh2.getMaxBytes());

        final int maxBytes2 = 202 * 1024 * 1024;
        fh2.setMaxBytes(maxBytes2);
        hddao.update(fh2);
        FullHarvest fh3 = (FullHarvest)hddao.read(fh.getOid());
        assertEquals("Should have set number of max bytes after read",
                     maxBytes2, fh3.getMaxBytes());
    }

    /**
     * Test that in getDomainConfigurations() only DomainConfigurations are returned for Domains
     * which are not aliases, or where the alias information is expired.
     *
     */
    public void testGetDomainsConfigurations() {
        DomainDAO ddao = DomainDAO.getInstance();
        FullHarvest fh = HarvestDefinition.createFullHarvest("testfullharvest",
                                                             "comment", null, 200L, Constants.DEFAULT_MAX_BYTES);
        // Test, at der findes DomainConfiguration objekter for de domæner, der ikke skal springes over.
        // Der findes ikke nogen aliasdomæner i test-databasen, så dette trin skulle gå godt.
        Iterator<Domain> domainIterator = ddao.getAllDomains();

        while(domainIterator.hasNext()) {
            assertDomainConfigurationsForDomain(fh.getDomainConfigurations(), domainIterator.next().getName());
        }

        // Add two new domains which both are aliases
        dk.netarkivet.harvester.webinterface.DomainDefinition.createDomains("alias1.dk",
                "alias2.dk");
        Domain aliasDomain = ddao.read("alias1.dk");
        //Very old alias indeed
        aliasDomain.setAliasInfo(new AliasInfo("alias1.dk", "kb.dk", new Date(0L)));
        ddao.update(aliasDomain);
        aliasDomain = ddao.read("alias2.dk");
        aliasDomain.updateAlias("netarkivet.dk");
        ddao.update(aliasDomain);
        fh = HarvestDefinition.createFullHarvest("testfullharvest-1",
                                                 "comment", null, 200L, Constants.DEFAULT_MAX_BYTES);
        assertNoAliasDomainConfigurations(fh.getDomainConfigurations());
    }

    /**
     * This tests the fix to the bug known as FR1773. The requirement is that a
     * domain for which the status was "Harvesting aborted" on the previous harvest
     * should not be included in this harvest
     */
    public void testGetDomainsPreviousHarvestAborted() {
          DomainDAO ddao = DomainDAO.getInstance();
        FullHarvest previousHarvest = HarvestDefinition.
                createFullHarvest("previous", "comment", null, 200L, 10000L);
        HarvestDefinitionDAO hddao = HarvestDefinitionDAO.getInstance();
        hddao.create(previousHarvest);
        previousHarvest = (FullHarvest) hddao.getHarvestDefinition("previous");
        Domain d = ddao.read("netarkivet.dk");
          HarvestInfo hi = new HarvestInfo(previousHarvest.getOid(),
                                           "netarkivet.dk",
                                           d.getDefaultConfiguration().getName(),
                                           new Date(),
                                           200L,
                                           50L,
                                           StopReason.DOWNLOAD_UNFINISHED);
        d.getHistory().addHarvestInfo(hi);
        ddao.update(d);
        FullHarvest newHarvest = HarvestDefinition.createFullHarvest("new", "comment", previousHarvest.getOid(), 500L, 100000L);
        hddao.create(newHarvest);
        newHarvest = (FullHarvest) hddao.getHarvestDefinition("new");
        Iterator<DomainConfiguration> configs = newHarvest.getDomainConfigurations();
        while (configs.hasNext()) {
             if (configs.next().getDomain().getName().equals("netarkivet.dk")) {
                 fail("DomainConfiguration for netarkivet.dk found but should "
                      + "be absent because it has status DOWNLOAD_UNFINISHED in"
                      + " the previous harvest");
             }
        }
    }

    /**
     * Test that the FullHarvester.getDomainConfigurations() part of bug 716 is fixed.
     *
     */
     public void testGetDomainsConfigurationsBug716() {

         DomainDAO ddao = DomainDAO.getInstance();
         // Create a previous FullHarvest for this test previousFullHarvest
         FullHarvest previousFullHarvest = HarvestDefinition.createFullHarvest("previousfullharvest",
                 "comment", null, 200L, 10000L);
         HarvestDefinitionDAO hdao = HarvestDefinitionDAO.getInstance();
         hdao.create(previousFullHarvest);
         previousFullHarvest = (FullHarvest) hdao.getHarvestDefinition("previousfullharvest");

         // Create a FullHarvest, that has previousFullHarvest as previous FullHarvest
         FullHarvest fh = HarvestDefinition.createFullHarvest("previousfullharvest",
                 "comment", previousFullHarvest.getOid(), 200L, Constants.DEFAULT_MAX_BYTES);

         // Create one HarvestInfo objects for netarkivet.dk for this FullHarvest.
         Domain d = ddao.read("netarkivet.dk");
         HarvestInfo hi = new HarvestInfo(
                 previousFullHarvest.getOid(),
                 "netarkivet.dk",
                 d.getDefaultConfiguration().getName(), new Date(),
                 200L, 50L, StopReason.CONFIG_SIZE_LIMIT);

         d.getHistory().addHarvestInfo(hi);
         ddao.update(d);

         /** Denne kode kaldes i HarvestSchedulerMonitorServer.processCrawlData()
          *  for at bestemme om dens StopReason er StopReason.CONFIG_SIZE_LIMIT;
          *
          *  long configMaxBytes = domain.getConfiguration(
          *        configurationMap.get(domainName)).getMaxBytes();
          *        if (configMaxBytes  != Constants.HERITRIX_MAXBYTES_INFINITY
          *                   && bytesReceived >= configMaxBytes) {
          *            stopReason = StopReason.CONFIG_SIZE_LIMIT;
          */

         // Check, that we only have one harvestInfo object defined from previous harvest
         // and this is related to domain netarkivet.dk
         Iterator<HarvestInfo> it =
             ddao.getHarvestInfoBasedOnPreviousHarvestDefinition(
                 fh.getPreviousHarvestDefinition());
         boolean found = false;
         while (it.hasNext()) {
             HarvestInfo hi1 = it.next();
             if (hi1.getDomainName().equalsIgnoreCase("netarkivet.dk")){
                 found = true;
             }
         }
         if (!found) {
             fail ("netarkivet.dk domain not found among HarvestInfo objects from previous harvest");
         }
         // Check, that iterator contains DomainConfiguration for netarkivet.dk
         assertDomainConfigurationsForDomain(fh.getDomainConfigurations(), "netarkivet.dk");

         // Change MaxBytes for d.getDefaultConfiguration() to 200L
         // Then iterator should no longer contain DomainConfiguration for netarkivet.dk

         d.getDefaultConfiguration().setMaxBytes(200L);
         ddao.update(d);
         assertNoDomainConfigurationsForDomain(fh.getDomainConfigurations(), "netarkivet.dk");
     }



    private void assertDomainConfigurationsForDomain(Iterator<DomainConfiguration> domainConfigurations, String anotherDomainName) {
        while (domainConfigurations.hasNext()) {
            String domainName = domainConfigurations.next().getDomain().getName();
            if (domainName.equals(anotherDomainName)){
                return;
            }
        }
        fail ("DomainConfiguration for Domain '" + anotherDomainName + "' not found");
    }

    private void assertNoDomainConfigurationsForDomain(Iterator<DomainConfiguration> domainConfigurations, String anotherDomainName) {
        while (domainConfigurations.hasNext()) {
            String domainName = domainConfigurations.next().getDomain().getName();
            if (domainName.equals(anotherDomainName)){
                fail ("DomainConfiguration for Domain '" + anotherDomainName + "' found");
            }
        }
    }

    private void assertNoAliasDomainConfigurations(Iterator<DomainConfiguration> iterator) {
        while (iterator.hasNext()) {
            Domain d = iterator.next().getDomain();
            if (d.getAliasInfo() != null) {
                AliasInfo ai = new AliasInfo(d.getName(), d.getAliasInfo().getAliasOf(), d.getAliasInfo().getLastChange());
                if (!ai.isExpired()) {
                    fail("There should not have be DomainConfigurations for alias domain: "
                         + d.getName());
                }
            }
         }
    }

}