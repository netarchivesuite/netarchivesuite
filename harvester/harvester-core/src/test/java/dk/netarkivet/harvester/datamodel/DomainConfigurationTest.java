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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.Test;

import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.datamodel.eav.ContentAttrType_Generic;
import dk.netarkivet.harvester.datamodel.eav.ContentAttribute_Generic;
import dk.netarkivet.harvester.datamodel.eav.EAV;
import dk.netarkivet.harvester.test.utils.OrderXmlBuilder;

/**
 * Tests for the DomainConfiguration class. Also widely tested from other places.
 */

public class DomainConfigurationTest {
    public static final String PASSWORD_NAME = "Secret";
    public static final String PASSWORD_COMMENT = "We don't know where this came from.";
    public static final String PASSWORD_PASSWORD_DOMAIN = "Area51";
    public static final String PASSWORD_REALM = "SecretLaBOratory";
    public static final String PASSWORD_USERNAME = "Mulder";
    public static final String PASSWORD_PASSWORD = "TrustNo1";
    public static final Password PASSWORD = new Password(PASSWORD_NAME, PASSWORD_COMMENT, PASSWORD_PASSWORD_DOMAIN,
            PASSWORD_REALM, PASSWORD_USERNAME, PASSWORD_PASSWORD);

    @Test
    public void testMaxBytes() {
        DomainConfiguration newcfg = createDefaultDomainConfiguration();
        assertEquals("maxBytes should start out at default value", Constants.DEFAULT_MAX_BYTES, newcfg.getMaxBytes());

        final int maxBytes = 201 * 1024 * 1024;
        newcfg.setMaxBytes(maxBytes);
        assertEquals("maxBytes should reflect newly set value", maxBytes, newcfg.getMaxBytes());
    }


    @Test
    public void testAddPassword() {
        DomainConfiguration conf = createDefaultDomainConfiguration();
        Domain domain = mock(Domain.class);
        when(domain.getPassword(PASSWORD.getName())).thenReturn(PASSWORD);
        conf.addPassword(domain, PASSWORD);
        assertTrue("Configuration uses password", conf.usesPassword(PASSWORD_NAME));
    }

    @Test
    public void testRemovePassword() {
        DomainConfiguration conf = createDefaultDomainConfiguration();
        Domain domain = mock(Domain.class);
        when(domain.getPassword(PASSWORD.getName())).thenReturn(PASSWORD);
        conf.addPassword(domain, PASSWORD);
        conf.removePassword(PASSWORD_NAME);
        assertFalse("Configuration should have password removed", conf.usesPassword(PASSWORD_NAME));
    }

    @Test(expected = UnknownID.class)
    public void testRemoveUnusedPassword() {
        DomainConfiguration conf = createDefaultDomainConfiguration();
        conf.removePassword(PASSWORD_NAME);
    }

    @Test (expected = PermissionDenied.class)
    public void testAddPasswordInconsistentName() {
        DomainConfiguration conf = createDefaultDomainConfiguration();
        Domain domain = mock(Domain.class);
        when(domain.getPassword(anyString())).thenReturn(PASSWORD);
        conf.addPassword(domain, new Password("Other name", PASSWORD_COMMENT,
                PASSWORD_PASSWORD_DOMAIN, PASSWORD_REALM, PASSWORD_USERNAME,
                PASSWORD_PASSWORD));
        assertTrue("Configuration uses password", conf.usesPassword(PASSWORD_NAME));
    }


    @Test
    public void testExpectedNumberOfObjectsDefaults() {
        DomainConfiguration dc = createDefaultDomainConfiguration();
        assertEquals("Unharvested config should return the default given number", 5000,
                dc.getExpectedNumberOfObjects(-1L, -1L));
    }

    @Test
    public void testExpectedNumberOfObjectsWithSetMaxObjects() {
        DomainConfiguration dc = createDefaultDomainConfiguration();
        assertEquals("Unharvested config should return the default given number", 5000,
                dc.getExpectedNumberOfObjects(-1L, -1L));
        dc.setMaxObjects(4000);
        assertEquals("Unharvested config should the set number of objects", 4000,
                dc.getExpectedNumberOfObjects(-1L, -1L));
        assertEquals("Unharvested config should expect the configured number (4000 objects)", 4000,
                dc.getExpectedNumberOfObjects(6000, -1L));
    }

    @Test
    public void testExpectedNumberOfObjectsDownloadComplete()  {
        DomainConfiguration dc = createDefaultDomainConfiguration();
        addHistoryObject(dc, 100L, 1L, StopReason.DOWNLOAD_COMPLETE);
        dc.setMaxObjects(1100);
        assertEquals("Completed harvest should add 10% of difference", 200L, dc.getExpectedNumberOfObjects(-1L, -1L));
        assertEquals("Completed harvest should add 10% of difference", 300L, dc.getExpectedNumberOfObjects(2100L, -1L));
    }

    @Test
    public void testExpectedNumberOfObjectsObjectLimit() {
        DomainConfiguration dc = createDefaultDomainConfiguration();
        addHistoryObject(dc, 100L, 1L, StopReason.OBJECT_LIMIT);
        dc.setMaxObjects(1100);
        assertEquals("Unfinished harvest should add 50% of difference", 600L, dc.getExpectedNumberOfObjects(-1L, -1L));
    }

    @Test
    public void testExpectedNumberOfObjectsOverrideflag() {
        DomainConfiguration dc = createDefaultDomainConfiguration();
        addHistoryObject(dc, 100L, 1L, StopReason.OBJECT_LIMIT);
        dc.setMaxObjects(1100);
        assertEquals("Unfinished harvest should add 50% of difference", 600L, dc.getExpectedNumberOfObjects(-1L, -1L));

        dc.setMaxObjects(5000);
        assertEquals("Override flag should not affect lower limits", 1150L, dc.getExpectedNumberOfObjects(2200L, -1L));
        System.out.println("-------");
        dc.setMaxObjects(200);
        assertEquals("Override flag should be a maximum of the end result", 200L,
                dc.getExpectedNumberOfObjects(2200L, -1L));

        dc.setMaxObjects(1000);
        assertEquals("Override flag should be a maximum of the end result", 1000L,
                dc.getExpectedNumberOfObjects(2200L, -1L));
    }

    @Test
    public void testExpectedNumberOfObjects() {
        DomainConfiguration dc = createDefaultDomainConfiguration();
        addHistoryObject(dc, 100L, 1L, StopReason.SIZE_LIMIT);
        dc.setMaxObjects(1100);
        assertEquals("Unfinished harvest should add 50% of difference", 1100L,
                dc.getExpectedNumberOfObjects(2100L, -1L));
    }

    @Test
    public void testExpectedNumberOfObjectsNewerSmallUnfinishedHarvest() {
        DomainConfiguration dc = createDefaultDomainConfiguration();
        addHistoryObject( dc, 200L, 1L, StopReason.DOWNLOAD_COMPLETE);
        addSecondHistoryObject(dc, 100L, 1L, StopReason.SIZE_LIMIT);
        assertEquals("Newer smaller unfinished harvest should not affect expectation", 400L,
                dc.getExpectedNumberOfObjects(2200L, -1L));
    }

    @Test
    public void testExpectedNumberOfObjectsNewerLargeUnfinishedHarvest() {
        DomainConfiguration dc = createDefaultDomainConfiguration();
        addHistoryObject(dc, 200L, 1L, StopReason.DOWNLOAD_COMPLETE);
        addSecondHistoryObject(dc, 300L, 1L, StopReason.SIZE_LIMIT);
        dc.setMaxObjects(2300);
        assertEquals("Newer larger unfinished harvest should define expectation", 1300L,
                dc.getExpectedNumberOfObjects(-1L, -1L));
    }

    @Test
    public void testExpectedNumberOfObjectsOlderLargeUnfinishedHarvest() {
        DomainConfiguration dc = createDefaultDomainConfiguration();
        addHistoryObject(dc, 300L, 1L, StopReason.SIZE_LIMIT);
        addSecondHistoryObject(dc, 200L, 1L, StopReason.DOWNLOAD_COMPLETE);
        assertEquals("Older larger unfinished harvest should not affect expectation", 400L,
                dc.getExpectedNumberOfObjects(2200L, -1L));
    }

    @Test
    public void testExpectedNumberOfObjects40ObjectsHarvested() {
        DomainConfiguration dc = createDefaultDomainConfiguration();
        addHistoryObject(dc, 40L, 400L, StopReason.SIZE_LIMIT);
        assertEquals("The expected object size should be 38000 because only 40 objects were harvested", 85L,
                dc.getExpectedNumberOfObjects(-1L, 5000000L));
    }

    @Test
    public void testExpectedNumberOfObjects100ObjectsHarvested() {
        DomainConfiguration dc = createDefaultDomainConfiguration();
        addHistoryObject(dc, 100L, 1000L, StopReason.SIZE_LIMIT);
        assertEquals("The expected object size should be 10 because 100 objects were harvested", 250050L,
                dc.getExpectedNumberOfObjects(-1L, 5000000L));
    }

    @Test
    public void testExpectedNumberOfObjectsHeritrix1On1() {
        DomainConfiguration dc = createDefaultDomainConfiguration();
        addHistoryObject(dc, 1L, 1L, StopReason.SIZE_LIMIT);
        assertEquals("When heritrix writes 1/1 we shouldn't expect too many objects next time", 66L,
                dc.getExpectedNumberOfObjects(-1L, 5000000L));
    }

    @Test
    public void testExpectedNumberOfObjectsLargeExpectation() {
        DomainConfiguration dc = createDefaultDomainConfiguration();
        addHistoryObject(dc, 10L, 10000000L, StopReason.SIZE_LIMIT);
        assertEquals("Even on small harvests we should trust large expectations", 5L,
                dc.getExpectedNumberOfObjects(-1L, 5000000L));
    }

    @Test
    public void testSetSeedlist() {
        DomainConfiguration dc = createDefaultDomainConfiguration();
        String seedlistName = "default-domain.org";
        SeedList domainSeedList = new SeedList(seedlistName, Arrays.asList(new String[] {"default-domain.org"}));
        Domain mockDomain = mock(Domain.class);
        when(mockDomain.getSeedList(seedlistName)).thenReturn(domainSeedList);

        dc.setSeedLists(mockDomain, Arrays.asList(new SeedList[] {domainSeedList}));
        SeedList confSeedList = dc.getSeedLists().next();
        assertEquals(domainSeedList, confSeedList);
    }

    @Test  (expected = PermissionDenied.class)
    public void testSetSeedlistUnknownSeedListName() {
        DomainConfiguration dc = createDefaultDomainConfiguration();
        String seedlistName = "default-domain.org";
        SeedList domainSeedList = new SeedList(seedlistName, Arrays.asList(new String[] {"default-domain.org"}));
        Domain mockDomain = mock(Domain.class);
        when(mockDomain.getSeedList(seedlistName)).thenReturn(null);

        dc.setSeedLists(mockDomain, Arrays.asList(new SeedList[] {domainSeedList}));
    }

    @Test  (expected = PermissionDenied.class)
    public void testSetSeedlistInconsistententSeedList() {
        DomainConfiguration dc = createDefaultDomainConfiguration();
        String seedlistName = "default-domain.org";
        SeedList domainSeedList = new SeedList(seedlistName, Arrays.asList(new String[] {"default-domain.org"}));
        SeedList newSeedList = new SeedList(seedlistName, Arrays.asList(new String[] {"other-domain.org"}));
        Domain mockDomain = mock(Domain.class);
        when(mockDomain.getSeedList(seedlistName)).thenReturn(domainSeedList);

        dc.setSeedLists(mockDomain, Arrays.asList (new SeedList[] {newSeedList}));
    }



    private void addHistoryObject(DomainConfiguration dc, final long countObjectRetrieved,
            long sizeDataRetrieved, final StopReason stopReason) {
        dc.getDomainhistory().addHarvestInfo(
                new HarvestInfo(Long.valueOf(1L), "default-domain.org", dc.getName(),
                        new GregorianCalendar(1970, 01, 01).getTime(), sizeDataRetrieved,
                        countObjectRetrieved, stopReason));
    }

    private void addSecondHistoryObject(DomainConfiguration dc, final long countObjectRetrieved,
            long sizeDataRetrieved, final StopReason stopReason) {
        dc.getDomainhistory().addHarvestInfo(
                new HarvestInfo(Long.valueOf(1L), "default-domain.org", dc.getName(),
                        new GregorianCalendar(1980, 01, 01).getTime(), sizeDataRetrieved,
                        countObjectRetrieved, stopReason));
    }

    public static DomainConfiguration createDefaultDomainConfiguration() {
        return createDefaultDomainConfiguration("defaultdomain.org");
    }

    public static DomainConfiguration createDefaultDomainConfiguration(String name) {
        SeedList seedList = new SeedList("SeedList1", "netarchivesuite.org");
        DomainConfiguration domainConfiguration = new DomainConfiguration(
                name, name, new DomainHistory(),
                new ArrayList<String>(), Arrays.asList(new SeedList[] {seedList}), new ArrayList<Password>());
        domainConfiguration.setOrderXmlName(OrderXmlBuilder.DEFAULT_ORDER_XML_NAME);
        return domainConfiguration;
    }



    public static List<EAV.AttributeAndType> getAttributes(int maxHops, boolean obeyRobots, boolean extractJS) {
        List<EAV.AttributeAndType> attributeAndTypes = new ArrayList<>();
        ContentAttrType_Generic atMaxHops = new ContentAttrType_Generic();
        atMaxHops.tree_id = 2;
        atMaxHops.id = 1;
        atMaxHops.datatype = 1;
        atMaxHops.viewtype = 1;
        atMaxHops.def_int = 20;
        ContentAttribute_Generic aatMaxHops = new ContentAttribute_Generic(atMaxHops);
        aatMaxHops.setInteger(maxHops);
        attributeAndTypes.add(new EAV.AttributeAndType(atMaxHops, aatMaxHops));

        ContentAttrType_Generic atHonorRobots = new ContentAttrType_Generic();
        atHonorRobots.tree_id = 2;
        atHonorRobots.id = 2;
        atHonorRobots.datatype = 1;
        atHonorRobots.viewtype = 6;
        atHonorRobots.def_int = 0;
        ContentAttribute_Generic aatHonorRobots = new ContentAttribute_Generic(atHonorRobots);
        if (obeyRobots) {
            aatHonorRobots.setInteger(1);
        } else {
            aatHonorRobots.setInteger(0);
        }
        attributeAndTypes.add(new EAV.AttributeAndType(atHonorRobots, aatHonorRobots));

        ContentAttrType_Generic atExtractJS = new ContentAttrType_Generic();
        atExtractJS.tree_id = 2;
        atExtractJS.id = 3;
        atExtractJS.datatype = 1;
        atExtractJS.viewtype = 5;
        atExtractJS.def_int = 1;
        ContentAttribute_Generic aatExtractJS = new ContentAttribute_Generic(atExtractJS);
        if (extractJS) {
            aatExtractJS.setInteger(1);
        } else {
            aatExtractJS.setInteger(0);
        }
        attributeAndTypes.add(new EAV.AttributeAndType(atExtractJS, aatExtractJS));

        return attributeAndTypes;
    }


}
