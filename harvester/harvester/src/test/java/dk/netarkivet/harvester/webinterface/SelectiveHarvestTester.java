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
package dk.netarkivet.harvester.webinterface;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.PageContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.GregorianCalendar;
import java.util.Calendar;

import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.harvester.datamodel.DataModelTestCase;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionDAO;
import dk.netarkivet.harvester.datamodel.PartialHarvest;
import dk.netarkivet.testutils.CollectionAsserts;
import dk.netarkivet.testutils.ReflectUtils;

public class SelectiveHarvestTester extends DataModelTestCase {
    public SelectiveHarvestTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    /**
     * Verify that the correct domain configurations are created.
     * This test handles the normal usage.
     */
    public void testgetDomainConfigurations() throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {
        // Construct map corresponding to
        // dr.dk -> Engelsk_netarkiv_et_niveau
        // statsbiblioteket -> fuld_dybde
        Map<String , String[]> confs= new HashMap<String , String[]>();
        String[] val = new String[1];
        val[0]="Engelsk_netarkiv_et_niveau";
        confs.put(Constants.DOMAIN_IDENTIFIER + "netarkivet.dk", val);

        val = new String[1];
        val[0]="fuld_dybde";
        confs.put(Constants.DOMAIN_IDENTIFIER + "statsbiblioteket.dk", val);

        // verify correct configurations are created
        Method getDomainConfigurations = ReflectUtils.getPrivateMethod(
                SelectiveHarvest.class, "getDomainConfigurations", Map.class);
        List<DomainConfiguration> dc = (List<DomainConfiguration>)
                getDomainConfigurations.invoke(null, confs);
        assertEquals("2 configurations expected", 2, dc.size() );

        DomainConfiguration d1 = dc.get(0);
        DomainConfiguration d2 = dc.get(1);

        assertEquals("Netarkiv - engelsk forventet", "Engelsk_netarkiv_et_niveau", d1.getName());
        assertEquals("statsbiblioteket - fuld_dybde forventet", "fuld_dybde", d2.getName());
    }


    /**
     * Tests the private static method
     * SelectiveHarvest.addDomainsToConfigurations.
     * This verifies that the correct domain configurations are added
     * This test handles the normal usage
     */
    public void testaddDomainConfigurations() throws NoSuchMethodException,
            IllegalAccessException, InvocationTargetException {

        // Create empty list and add one configuration
        List<DomainConfiguration> dc = new ArrayList<DomainConfiguration>();
        List<String> unknowns = new ArrayList<String>();
        List<String> illegals = new ArrayList<String>();
        Method addDomainsToConfigurations = ReflectUtils.getPrivateMethod(
                SelectiveHarvest.class, "addDomainsToConfigurations",
                List.class, String.class, List.class, List.class);
        addDomainsToConfigurations.invoke(null, dc,
                "netarkivet.dk\nstatsbiblioteket.dk", unknowns, illegals);

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
    public void testaddInvalidDomainConfigurations()
            throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {

        // Create empty list and add two valid and two invalid domains
        List<DomainConfiguration> dc = new ArrayList<DomainConfiguration>();
        List<String> unknowns = new ArrayList<String>();
        List<String> illegals = new ArrayList<String>();
        Method addDomainsToConfigurations = ReflectUtils.getPrivateMethod(
                SelectiveHarvest.class, "addDomainsToConfigurations",
                List.class, String.class, List.class, List.class);
        addDomainsToConfigurations.invoke(null, dc,
                 "netarkivet.dk\nunknowun1.dk\nstatsbiblioteket.dk\nunknown2.dj\nklaf-foo",
                unknowns, illegals);
        assertEquals("2 configurations expected", 2, dc.size() );
        CollectionAsserts.assertListEquals("Expected two unknown domains",
                unknowns, "unknowun1.dk", "unknown2.dj");
        CollectionAsserts.assertListEquals("Expected one illegal domain",
                illegals, "klaf-foo");
    }

    /**
     * Test the normal update scenario where the definition does
     * not already exist.
     */
    public void testUpdateNew() {
        final Map<String , String[]> confs= new HashMap<String , String[]>();
        confs.put(Constants.UPDATE_PARAM, new String[]{"1"});
        confs.put(Constants.CREATENEW_PARAM, new String[]{"1"});
        confs.put(Constants.HARVEST_PARAM, new String[]{"web-test1"});
        confs.put(Constants.COMMENTS_PARAM, new String[]{"kommentar"});
        confs.put(Constants.SCHEDULE_PARAM, new String[]{"Hver hele time"});
        confs.put(Constants.DOMAINLIST_PARAM, new String[]{"netarkivet.dk"});

        List<String> unknownDomains = new ArrayList<String>();
        List<String> illegalDomains = new ArrayList<String>();
        ServletRequest confRequest = dummyRequest(confs);
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new WebinterfaceTestCase.TestPageContext(confRequest);
        SelectiveHarvest.processRequest(
                pageContext, I18N, unknownDomains, illegalDomains);
        PartialHarvest hdd = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition("web-test1");
        assertNotNull("New HD should exist", hdd);
        assertEquals("Check af felter","kommentar",hdd.getComments());
        assertEquals("Check af felter","web-test1",hdd.getName());
        assertEquals("Check af felter","Hver hele time",hdd.getSchedule().getName());
        assertEquals("Alle domainer er kendte",0, unknownDomains.size());
        assertEquals("Alle domainer er gyldige",0, illegalDomains.size());
        Iterator<DomainConfiguration> itt =  hdd.getDomainConfigurations();
        assertEquals("Forventet default configuration", "Dansk_netarkiv_fuld_dybde", itt.next().getName());
        assertFalse("Forventet eksakt en konfiguration",  itt.hasNext());

    }

    /**
     * Unit-test for 
     * Test the update scenario where we do not add any new domains.
     * not already exist.
     */
    public void testUpdateNoadd() {
        final Map<String , String[]> confs= new HashMap<String , String[]>();
        confs.put(Constants.UPDATE_PARAM, new String[]{"1"});
        confs.put(Constants.HARVEST_PARAM, new String[]{"Testhøstning"});
        confs.put(Constants.COMMENTS_PARAM, new String[]{"kommentar"});
        confs.put(Constants.SCHEDULE_PARAM, new String[]{"Hver hele time"});
        confs.put(Constants.DOMAINLIST_PARAM, new String[]{""});
        confs.put(Constants.EDITION_PARAM, new String[]{"1"});
        confs.put(Constants.DOMAIN_IDENTIFIER + "netarkivet.dk",
                new String[]{"Dansk_netarkiv_fuld_dybde"});

        List<String> unknownDomains = new ArrayList<String>();
        List<String> illegalDomains = new ArrayList<String>();
        ServletRequest confRequest = dummyRequest(confs);
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new WebinterfaceTestCase.TestPageContext(confRequest);
        SelectiveHarvest.processRequest(pageContext, I18N,
                unknownDomains, illegalDomains);
        PartialHarvest hdd = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition("Testhøstning");
        assertEquals("Check af felter","kommentar",hdd.getComments());
        assertEquals("Check af felter","Testhøstning",hdd.getName());
        assertEquals("Check af felter","Hver hele time",hdd.getSchedule().getName());
        assertEquals("Alle domainer er kendte",0, unknownDomains.size());
        assertEquals("Alle domainer er gyldige",0, illegalDomains.size());
        Iterator<DomainConfiguration> itt =  hdd.getDomainConfigurations();
        assertEquals("Forventet default configuration", "Dansk_netarkiv_fuld_dybde",
                itt.next().getName());
        assertFalse("Forventet eksakt en konfiguration",  itt.hasNext());

    }

    /**
     * Test the normal update scenario where the definition does
     * already exist.
     */
    public void testUpdateExists() {

        // Til den eksisterende høstning: Testhøstning
        // tilføjes et nyt domæne, konfigurationen for det eksisterende
        // domæne ændres og schedule ændres
        Map<String , String[]> confs= new HashMap<String , String[]>();
        confs.put(Constants.UPDATE_PARAM, new String[]{"1"});
        confs.put(Constants.HARVEST_PARAM, new String[]{"Testhøstning"});
        confs.put(Constants.COMMENTS_PARAM, new String[]{"kommentar"});
        confs.put(Constants.SCHEDULE_PARAM, new String[]{"Hver hele time"});
        confs.put(Constants.DOMAINLIST_PARAM, new String[]{"statsbiblioteket.dk"});
        confs.put(Constants.EDITION_PARAM, new String[]{"1"});
        confs.put(Constants.DOMAIN_IDENTIFIER + "netarkivet.dk",
                  new String[]{"Engelsk_netarkiv_et_niveau"});

        List<String> unknownDomains = new ArrayList<String>();
        List<String> illegalDomains = new ArrayList<String>();
        HarvestDefinitionDAO.getInstance().getHarvestDefinition("Testhøstning").getEdition();
        ServletRequest confRequest = dummyRequest(confs);
        I18n I18N = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new WebinterfaceTestCase.TestPageContext(confRequest);
        SelectiveHarvest.processRequest(pageContext, I18N, unknownDomains, illegalDomains);
        PartialHarvest hdd = (PartialHarvest) HarvestDefinitionDAO.getInstance().getHarvestDefinition("Testhøstning");
        assertEquals("Check af felter","kommentar",hdd.getComments());
        assertEquals("Check af felter","Testhøstning",hdd.getName());
        assertEquals("Check af felter","Hver hele time",hdd.getSchedule().getName());
        assertEquals("Alle domainer er kendte",0, unknownDomains.size());
        assertEquals("Alle domainer er lovlige",0, illegalDomains.size());
        Iterator<DomainConfiguration> itt =  hdd.getDomainConfigurations();
        List<String> nameList = new ArrayList<String>();
        nameList.add(itt.next().getName());
        nameList.add(itt.next().getName());
        Collections.sort(nameList);

        assertEquals("Forventet ny configuration", "Engelsk_netarkiv_et_niveau", nameList.get(0));
        assertEquals("Forventet ny default configuration", "fuld_dybde", nameList.get(1) );
        assertFalse("Forventet eksakt to konfiguration",  itt.hasNext());

    }

    public void testSetNewDate() {

        // Make parameters for an update to existing templates.
        // Be sure to include next date field.
        Map<String, String[]> confs = new HashMap<String, String[]>();
        confs.put(Constants.UPDATE_PARAM, new String[]{"1"});
        confs.put(Constants.HARVEST_PARAM, new String[]{"Testhøstning"});
        confs.put(Constants.COMMENTS_PARAM, new String[]{"kommentar"});
        confs.put(Constants.SCHEDULE_PARAM, new String[]{"Hver hele time"});
        confs.put(Constants.EDITION_PARAM, new String[]{"1"});
        confs.put(Constants.DOMAINLIST_PARAM, new String[]{""});
        confs.put(Constants.DOMAIN_IDENTIFIER + "netarkivet.dk",
                  new String[]{"Engelsk_netarkiv_et_niveau"});
        confs.put(Constants.NEXTDATE_PARAM, new String[]{"7/10 2007 12:00"});
        ServletRequest confRequest = dummyRequest(confs);

        //Do the call
        List<String> unknownDomains = new ArrayList<String>();
        List<String> illegalDomains = new ArrayList<String>();
        I18n I18N = new I18n(
                dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new WebinterfaceTestCase.TestPageContext(
                confRequest);
        SelectiveHarvest.processRequest(pageContext, I18N, unknownDomains,
                                        illegalDomains);

        //Check result
        PartialHarvest hdd = (PartialHarvest) HarvestDefinitionDAO.getInstance()
                .getHarvestDefinition("Testhøstning");
        assertEquals("Check fields", "kommentar", hdd.getComments());
        assertEquals("Check fields", "Testhøstning", hdd.getName());
        assertEquals("Check fields", "Hver hele time",
                     hdd.getSchedule().getName());
        assertEquals("Alle domains known", 0, unknownDomains.size());
        assertEquals("Alle domains legal", 0, illegalDomains.size());
        Iterator<DomainConfiguration> itt = hdd.getDomainConfigurations();
        List<String> nameList = new ArrayList<String>();
        nameList.add(itt.next().getName());
        Collections.sort(nameList);
        assertEquals("Expected original configuration",
                     "Engelsk_netarkiv_et_niveau", nameList.get(0));
        assertFalse("Expected exactly one configuration", itt.hasNext());

        //This checks next date to be correct
        assertEquals("Should have the new next-date", new GregorianCalendar(
                2007, Calendar.OCTOBER, 7, 12, 00, 00).getTime(),
                                                      hdd.getNextDate());
    }

    private ServletRequest dummyRequest(final Map<String, String[]> confs) {
        return new ServletRequest() {
            public Map<String, Object> attributes = new HashMap<String, Object>();


            public Object getAttribute(String string) {
                throw new NotImplementedException("Not implemented");
            }

            public Enumeration getAttributeNames() {
                throw new NotImplementedException("Not implemented");
            }

            public String getCharacterEncoding() {
                throw new NotImplementedException("Not implemented");
            }

            public void setCharacterEncoding(String string)
                    throws UnsupportedEncodingException {
                throw new NotImplementedException("Not implemented");
            }

            public int getContentLength() {
                throw new NotImplementedException("Not implemented");
            }

            public String getContentType() {
                throw new NotImplementedException("Not implemented");
            }

            public ServletInputStream getInputStream() throws IOException {
                throw new NotImplementedException("Not implemented");
            }

            public String getParameter(String string) {
                String[] strings = confs.get(string);
                return strings == null || strings.length == 0 ? null : strings[0];
            }

            public Enumeration getParameterNames() {
                throw new NotImplementedException("Not implemented");
            }

            public String[] getParameterValues(String string) {
                throw new NotImplementedException("Not implemented");
            }

            public Map getParameterMap() {
                return confs;
            }

            public String getProtocol() {
                throw new NotImplementedException("Not implemented");
            }

            public String getScheme() {
                throw new NotImplementedException("Not implemented");
            }

            public String getServerName() {
                throw new NotImplementedException("Not implemented");
            }

            public int getServerPort() {
                throw new NotImplementedException("Not implemented");
            }

            public BufferedReader getReader() throws IOException {
                throw new NotImplementedException("Not implemented");
            }

            public String getRemoteAddr() {
                throw new NotImplementedException("Not implemented");
            }

            public String getRemoteHost() {
                throw new NotImplementedException("Not implemented");
            }

            public void setAttribute(String string, Object object) {
                attributes.put(string, object);
            }

            public void removeAttribute(String string) {
                throw new NotImplementedException("Not implemented");
            }

            public Locale getLocale() {
                throw new NotImplementedException("Not implemented");
            }

            public Enumeration getLocales() {
                throw new NotImplementedException("Not implemented");
            }

            public boolean isSecure() {
                throw new NotImplementedException("Not implemented");
            }

            public RequestDispatcher getRequestDispatcher(String string) {
                throw new NotImplementedException("Not implemented");
            }

            public String getRealPath(String string) {
                throw new NotImplementedException("Not implemented");
            }

            public int getRemotePort() {
                throw new NotImplementedException("Not implemented");
            }

            public String getLocalName() {
                throw new NotImplementedException("Not implemented");
            }

            public String getLocalAddr() {
                throw new NotImplementedException("Not implemented");
            }

            public int getLocalPort() {
                throw new NotImplementedException("Not implemented");
            }
        };
    }

}
