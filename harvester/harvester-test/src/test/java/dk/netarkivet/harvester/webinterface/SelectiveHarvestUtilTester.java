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
package dk.netarkivet.harvester.webinterface;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;

import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.IteratorUtils;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.PartialHarvest;
import dk.netarkivet.testutils.CollectionAsserts;
import dk.netarkivet.testutils.ReflectUtils;

@SuppressWarnings({"unchecked", "rawtypes"})
public class SelectiveHarvestUtilTester extends DataModelTestCase {

    /**
     * Verify that the correct domain configurations are created. This test handles the normal usage.
     * <p>
     * DISABLED 20140528 as it fails intermittently /tra FIXME: https://sbforge.org/jira/browse/NAS-2320
     */
    public void DISABLED_20140528_testgetDomainConfigurations() throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        // Construct map corresponding to
        // netarkivet.dk -> Engelsk_netarkiv_et_niveau
        // statsbiblioteket.dk -> fuld_dybde
        Map<String, String[]> confs = new HashMap<String, String[]>();
        String[] val = new String[1];
        val[0] = "Engelsk_netarkiv_et_niveau";
        confs.put(Constants.DOMAIN_IDENTIFIER + "netarkivet.dk", val);

        val = new String[1];
        val[0] = "fuld_dybde";
        confs.put(Constants.DOMAIN_IDENTIFIER + "statsbiblioteket.dk", val);

        // verify correct configurations are created
        Method getDomainConfigurations = ReflectUtils.getPrivateMethod(SelectiveHarvestUtil.class,
                "getDomainConfigurations", Map.class);
        List<DomainConfiguration> dc = (List<DomainConfiguration>) getDomainConfigurations.invoke(null, confs);
        assertEquals("2 configurations expected", 2, dc.size());

        DomainConfiguration d1 = dc.get(0);
        DomainConfiguration d2 = dc.get(1);

        assertEquals("Netarkiv - engelsk forventet", "Engelsk_netarkiv_et_niveau", d1.getName());
        assertEquals("statsbiblioteket - fuld_dybde forventet", "fuld_dybde", d2.getName());
    }

    /**
     * Tests the private static method SelectiveHarvest.addDomainsToConfigurations. This verifies that the correct
     * domain configurations are added This test handles the normal usage
     */
    public void testaddDomainConfigurations() throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {

        // Create empty list and add one configuration
        List<DomainConfiguration> dc = new ArrayList<DomainConfiguration>();
        List<String> unknowns = new ArrayList<String>();
        List<String> illegals = new ArrayList<String>();
        Method addDomainsToConfigurations = ReflectUtils.getPrivateMethod(SelectiveHarvestUtil.class,
                "addDomainsToConfigurations", List.class, String.class, List.class, List.class);
        addDomainsToConfigurations.invoke(null, dc, "netarkivet.dk\nstatsbiblioteket.dk", unknowns, illegals);

        assertEquals("2 configurations expected", 2, dc.size());
        assertEquals("No unknown domains expected", 0, unknowns.size());
        assertEquals("No illegal domains expected", 0, illegals.size());

        DomainConfiguration d1 = dc.get(0);
        DomainConfiguration d2 = dc.get(1);

        assertEquals("Netarkiv - engelsk forventet", "Dansk_netarkiv_fuld_dybde", d1.getName());
        assertEquals("Statsbiblioteket - fuld_dybde forventet", "fuld_dybde", d2.getName());
    }

    /**
     * Verify that Unknown domain names are reported
     */
    public void testaddInvalidDomainConfigurations() throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {

        // Create empty list and add two valid and two invalid domains
        List<DomainConfiguration> dc = new ArrayList<DomainConfiguration>();
        List<String> unknowns = new ArrayList<String>();
        List<String> illegals = new ArrayList<String>();
        Method addDomainsToConfigurations = ReflectUtils.getPrivateMethod(SelectiveHarvestUtil.class,
                "addDomainsToConfigurations", List.class, String.class, List.class, List.class);
        addDomainsToConfigurations.invoke(null, dc,
                "netarkivet.dk\nunknowun1.dk\nstatsbiblioteket.dk\nunknown2.dj\nklaf-foo", unknowns, illegals);
        assertEquals("2 configurations expected", 2, dc.size());
        CollectionAsserts.assertListEquals("Expected two unknown domains", unknowns, "unknowun1.dk", "unknown2.dj");
        CollectionAsserts.assertListEquals("Expected one illegal domain", illegals, "klaf-foo");
    }

    /**
     * Test the normal update scenario where the definition does not already exist.
     */
    public void testUpdateNew() {
        final Map<String, String[]> confs = new HashMap<String, String[]>();
        confs.put(Constants.UPDATE_PARAM, new String[] {"1"});
        confs.put(Constants.CREATENEW_PARAM, new String[] {"1"});
        confs.put(Constants.HARVEST_PARAM, new String[] {"web-test1"});
        confs.put(Constants.COMMENTS_PARAM, new String[] {"kommentar"});
        confs.put(Constants.SCHEDULE_PARAM, new String[] {"Hver hele time"});
        confs.put(Constants.DOMAINLIST_PARAM, new String[] {"netarkivet.dk"});
        confs.put(Constants.AUDIENCE_PARAM, new String[] {"unittesters"});

        List<String> unknownDomains = new ArrayList<String>();
        List<String> illegalDomains = new ArrayList<String>();
        ServletRequest confRequest = dummyRequest(confs);
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new HarvesterWebinterfaceTestCase.TestPageContext(confRequest);
        SelectiveHarvestUtil.processRequest(pageContext, I18N, unknownDomains, illegalDomains);
        PartialHarvest hdd = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition("web-test1");
        assertNotNull("New HD should exist", hdd);
        assertEquals("Check af felter", "kommentar", hdd.getComments());
        assertEquals("Check af felter", "web-test1", hdd.getName());
        assertEquals("Check af felter", "Hver hele time", hdd.getSchedule().getName());
        assertEquals("Alle domainer er kendte", 0, unknownDomains.size());
        assertEquals("Alle domainer er gyldige", 0, illegalDomains.size());
        Iterator<DomainConfiguration> itt = hdd.getDomainConfigurations();
        assertEquals("Forventet default configuration", "Dansk_netarkiv_fuld_dybde", itt.next().getName());
        assertFalse("Forventet eksakt en konfiguration", itt.hasNext());

    }

    /**
     * Unit-test for Test the update scenario where we do not add any new domains. not already exist.
     */
    public void testUpdateNoadd() {
        final Map<String, String[]> confs = new HashMap<String, String[]>();
        confs.put(Constants.UPDATE_PARAM, new String[] {"1"});
        confs.put(Constants.HARVEST_PARAM, new String[] {"Testhøstning"});
        confs.put(Constants.COMMENTS_PARAM, new String[] {"kommentar"});
        confs.put(Constants.SCHEDULE_PARAM, new String[] {"Hver hele time"});
        confs.put(Constants.DOMAINLIST_PARAM, new String[] {""});
        confs.put(Constants.EDITION_PARAM, new String[] {"1"});
        confs.put(Constants.DOMAIN_IDENTIFIER + "netarkivet.dk", new String[] {"Dansk_netarkiv_fuld_dybde"});
        confs.put(Constants.AUDIENCE_PARAM, new String[] {"unittesters"});

        List<String> unknownDomains = new ArrayList<String>();
        List<String> illegalDomains = new ArrayList<String>();
        ServletRequest confRequest = dummyRequest(confs);
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new HarvesterWebinterfaceTestCase.TestPageContext(confRequest);
        SelectiveHarvestUtil.processRequest(pageContext, I18N, unknownDomains, illegalDomains);
        PartialHarvest hdd = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition("Testhøstning");
        assertEquals("Check af felter", "kommentar", hdd.getComments());
        assertEquals("Check af felter", "Testhøstning", hdd.getName());
        assertEquals("Check af felter", "Hver hele time", hdd.getSchedule().getName());
        assertEquals("Alle domainer er kendte", 0, unknownDomains.size());
        assertEquals("Alle domainer er gyldige", 0, illegalDomains.size());
        Iterator<DomainConfiguration> itt = hdd.getDomainConfigurations();
        assertEquals("Forventet default configuration", "Dansk_netarkiv_fuld_dybde", itt.next().getName());
        assertFalse("Forventet eksakt en konfiguration", itt.hasNext());

    }

    /**
     * Test the normal update scenario where the definition does already exist.
     */
    public void testUpdateExists() {

        // To an existing partialharvestden named "Testhøstning"
        // a new konfiguration is added, and the configurationen
        // for the existing domain (netarkivet.dk) is modified,
        // and the schedule changed

        Map<String, String[]> confs = new HashMap<String, String[]>();
        confs.put(Constants.UPDATE_PARAM, new String[] {"1"});
        confs.put(Constants.HARVEST_PARAM, new String[] {"Testhøstning"});
        confs.put(Constants.COMMENTS_PARAM, new String[] {"kommentar"});
        confs.put(Constants.SCHEDULE_PARAM, new String[] {"Hver hele time"});
        confs.put(Constants.DOMAINLIST_PARAM, new String[] {"statsbiblioteket.dk"});
        confs.put(Constants.EDITION_PARAM, new String[] {"1"});
        confs.put(Constants.DOMAIN_IDENTIFIER + "netarkivet.dk", new String[] {"Engelsk_netarkiv_et_niveau"});
        confs.put(Constants.AUDIENCE_PARAM, new String[] {"unittesters"});

        List<String> unknownDomains = new ArrayList<String>();
        List<String> illegalDomains = new ArrayList<String>();

        ServletRequest confRequest = dummyRequest(confs);
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new HarvesterWebinterfaceTestCase.TestPageContext(confRequest);
        SelectiveHarvestUtil.processRequest(pageContext, I18N, unknownDomains, illegalDomains);
        PartialHarvest hdd = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition("Testhøstning");
        assertEquals("Check fields", "kommentar", hdd.getComments());
        assertEquals("Check fields", "Testhøstning", hdd.getName());
        assertEquals("Check fields", "Hver hele time", hdd.getSchedule().getName());
        assertEquals("There should be no unknown domains", 0, unknownDomains.size());
        assertEquals("There should be no illegal domains", 0, illegalDomains.size());

        Map<String, DomainConfiguration> dcMap = new HashMap<String, DomainConfiguration>();
        List<DomainConfiguration> dcs = IteratorUtils.toList(hdd.getDomainConfigurations());
        List<String> nameList = new ArrayList<String>();

        for (DomainConfiguration dc : dcs) {
            nameList.add(dc.getName());
            dcMap.put(dc.getName(), dc);
        }
        Collections.sort(nameList);
        assertTrue("Should be precisely 2 configurations, but there are " + nameList.size(), nameList.size() == 2);
        assertEquals("New configuration expected", "Engelsk_netarkiv_et_niveau", nameList.get(0));
        assertEquals("New default configuration expected", "fuld_dybde", nameList.get(1));
    }

    public void testSetNewDate() {

        // Make parameters for an update to existing templates.
        // Be sure to include next date field.
        Map<String, String[]> confs = new HashMap<String, String[]>();
        confs.put(Constants.UPDATE_PARAM, new String[] {"1"});
        confs.put(Constants.HARVEST_PARAM, new String[] {"Testhøstning"});
        confs.put(Constants.COMMENTS_PARAM, new String[] {"kommentar"});
        confs.put(Constants.SCHEDULE_PARAM, new String[] {"Hver hele time"});
        confs.put(Constants.EDITION_PARAM, new String[] {"1"});
        confs.put(Constants.DOMAINLIST_PARAM, new String[] {""});
        confs.put(Constants.DOMAIN_IDENTIFIER + "netarkivet.dk", new String[] {"Engelsk_netarkiv_et_niveau"});
        confs.put(Constants.AUDIENCE_PARAM, new String[] {"unittesters"});

        confs.put(Constants.NEXTDATE_PARAM, new String[] {"7/10 2007 12:00"});
        ServletRequest confRequest = dummyRequest(confs);

        // Do the call
        List<String> unknownDomains = new ArrayList<String>();
        List<String> illegalDomains = new ArrayList<String>();
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new HarvesterWebinterfaceTestCase.TestPageContext(confRequest);
        SelectiveHarvestUtil.processRequest(pageContext, I18N, unknownDomains, illegalDomains);

        // Check result
        PartialHarvest hdd = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition("Testhøstning");
        assertEquals("Check fields", "kommentar", hdd.getComments());
        assertEquals("Check fields", "Testhøstning", hdd.getName());
        assertEquals("Check fields", "Hver hele time", hdd.getSchedule().getName());
        assertEquals("All domains should be known", 0, unknownDomains.size());
        assertEquals("All domains should be legal", 0, illegalDomains.size());

        Map<String, DomainConfiguration> dcMap = new HashMap<String, DomainConfiguration>();
        List<String> nameList = new ArrayList<String>();
        Iterator<DomainConfiguration> itt = hdd.getDomainConfigurations();
        while (itt.hasNext()) {
            DomainConfiguration dc = itt.next();
            nameList.add(dc.getName());
            dcMap.put(dc.getName(), dc);
        }

        Collections.sort(nameList);
        assertTrue("Expected exactly one configuration, but there was " + nameList.size(), nameList.size() == 1);
        assertEquals("Expected original configuration", "Engelsk_netarkiv_et_niveau", nameList.get(0));

        // This checks next date to be correct
        assertEquals("Should have the new next-date",
                new GregorianCalendar(2007, Calendar.OCTOBER, 7, 12, 00, 00).getTime(), hdd.getNextDate());
    }

    private ServletRequest dummyRequest(final Map<String, String[]> confs) {
        return mock(ServletRequest.class);
    }
}
