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
package dk.netarkivet.common.webinterface;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;

/**
 * Tests of the SiteSection class.
 */
public class SiteSectionTester {

    @Test
    public void testGetTitle() throws Exception {
        SiteSection site = new SiteSection("testSite", "pref", 2, new String[][] { {"page1", "title1"},
                {"page2", "pagetitle;details.for.job"}, {"page3", "title3"}}, "HarvestDefinition",
                "dk.netarkivet.harvester.Translations") {
            public void initialize() {
            }

            public void close() {
            }
        };
        assertEquals("Should generate correct title for page1", "title1",
                site.getTitle(null, "http://foo.dk/HarvestDefinition/pref-page1.jsp", Locale.getDefault()));
        assertEquals("Should generate correct title for page2 in Danish", "Jobdetaljer",
                site.getTitle(null, "http://foo.dk/page1/HarvestDefinition/pref-page2.jsp", new Locale("da")));
        assertEquals("Should generate correct title for page2 in English", "Details for Job",
                site.getTitle(null, "http://foo.dk/page1/HarvestDefinition/pref-page2.jsp", new Locale("en")));
        assertNull("Should generate no title for invalid url",
                site.getTitle(null, "http://foo.d:ge2.jsp", Locale.getDefault()));
        assertNull("Should generate no title for wrong url",
                site.getTitle(null, "http://foo.dk/page1/HarvestDefinition/perf-page2.jsp", Locale.getDefault()));
        assertNull("Should generate no title for wrong url",
                site.getTitle(null, "http://foo.dk/page1/HarvestDefinition/pref-page2.jp", Locale.getDefault()));
        assertNull("Should generate no title for wrong url",
                site.getTitle(null, "http://foo.dk/HarvestDefinition/pref-page2.jsp/page1", Locale.getDefault()));
        assertNull("Should generate no title for wrong url",
                site.getTitle(null, "http://foo.dk/NotHD/pref-page2.jsp", Locale.getDefault()));
        assertNull("Should generate no title for null url", site.getTitle(null, null, Locale.getDefault()));
    }

    @Test
    public void testSiteSection() throws Exception {
        try {
            new SiteSection(null, "d", 0, new String[][] {}, "HarvestDefinition",
                    "dk.netarkivet.harvester.Translations") {
                public void initialize() {
                }

                public void close() {
                }
            };
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Must mention parameter", "mainname", e.getMessage());
        }

        try {
            new SiteSection("", "c", 0, new String[][] {}, "HarvestDefinition", "dk.netarkivet.harvester.Translations") {
                public void initialize() {
                }

                public void close() {
                }
            };
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Must mention parameter", "mainname", e.getMessage());
        }

        try {
            new SiteSection("a", null, 0, new String[][] {}, "HarvestDefinition",
                    "dk.netarkivet.harvester.Translations") {
                public void initialize() {
                }

                public void close() {
                }
            };
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Must mention parameter", "prefix", e.getMessage());
        }

        try {
            new SiteSection("b", "", 0, new String[][] {}, "HarvestDefinition", "dk.netarkivet.harvester.Translations") {
                public void initialize() {
                }

                public void close() {
                }
            };
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Must mention parameter", "prefix", e.getMessage());
        }

        try {
            new SiteSection("b", "e", -1, new String[][] {}, "HarvestDefinition",
                    "dk.netarkivet.harvester.Translations") {
                public void initialize() {
                }

                public void close() {
                }
            };
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Must mention parameter", "visiblePages", e.getMessage());
        }

        try {
            new SiteSection("b", "e", 1, null, "HarvestDefinition", "dk.netarkivet.harvester.Translations") {
                public void initialize() {
                }

                public void close() {
                }
            };
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Must mention parameter", "pagesAndTitles", e.getMessage());
        }

        try {
            new SiteSection("b", "e", 1, new String[][] {}, "HarvestDefinition", "dk.netarkivet.harvester.Translations") {
                public void initialize() {
                }

                public void close() {
                }
            };
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Must mention parameter", "pagesAndTitles", e.getMessage());
        }

        try {
            new SiteSection("b", "e", 1, new String[][] {{"foo"}}, "HarvestDefinition",
                    "dk.netarkivet.harvester.Translations") {
                public void initialize() {
                }

                public void close() {
                }
            };
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Must mention error", "page and title", e.getMessage());
        }

        try {
            new SiteSection("b", "e", 1, new String[][] { {"foo", "bar"}, {"and", "some", "more"}},
                    "HarvestDefinition", "dk.netarkivet.harvester.Translations") {
                public void initialize() {
                }

                public void close() {
                }
            };
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Must mention parameter", "page and title", e.getMessage());
        }

        try {
            new SiteSection("b", "e", 1, new String[][] { {"foo", "bar"}, {"and", "some", "more"}},
                    "HarvestDefinition", null) {
                public void initialize() {
                }

                public void close() {
                }
            };
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Must mention parameter", "bundle", e.getMessage());
        }

        // Must also be able to create a working object.
        new SiteSection("b", "e", 1, new String[][] { {"foo", "bar"}, {"and", "more"}}, "HarvestDefinition",
                "dk.netarkivet.harvester.Translations") {
            public void initialize() {
            }

            public void close() {
            }
        };
    }

    /**
     * Verify functionality of static method SiteSection.getSections(). TLR have experienced strange effect, where left
     * menu consisted of the entries duplicated as follows: Definitioner Definitioner HøstningsStatus HøstningsStatus
     * Bitbevaring Bitbevaring Kvalitetssikring KvalitetsSikring Systemstatus Systemstatus
     * <p>
     * This is rapported as bug 879.
     */
    @Test
    public void testGetSections() throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        for (int j = 0; j < 10; j++) {
            ReflectUtils.getPrivateField(SiteSection.class, "sections").set(null, null);
            List<Thread> threads = new ArrayList<Thread>();
            for (int i = 0; i < 1000; i++) {
                threads.add(new Thread() {
                    public void run() {
                        SiteSection.getSections();
                    }
                });
            }
            for (Thread t : threads) {
                t.start();
            }
            for (Thread t : threads) {
                t.join();
            }
            List<SiteSection> sections = SiteSection.getSections();
            assertEquals("#sections must be identical length of list returned from " + "Settings",
                    Settings.getAll(CommonSettings.SITESECTION_CLASS).length, sections.size());
        }
    }

    /**
     * Check dirname.
     */
    @Test
    public void testGetDirname() throws Exception {
        SiteSection site = new SiteSection("b", "e", 1, new String[][] { {"foo", "bar"}, {"and", "more"}},
                "HarvestDefinition", "dk.netarkivet.harvester.Translations") {
            public void initialize() {
            }

            public void close() {
            }
        };
        assertEquals("Should have right dirname", "HarvestDefinition", site.getDirname());
    }

    /**
     * Test isDeployed
     *
     * @throws Exception
     */
    @Test
    public void testIsDeployed() throws Exception {
        try {
            SiteSection.isDeployed(null);
            fail("Should throw ArgumentNotValid on null param");
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Should throws expected exception", "String dirName", e.getMessage());
        }
        try {
            SiteSection.isDeployed("");
            fail("Should throw ArgumentNotValid on empty param");
        } catch (ArgumentNotValid e) {
            StringAsserts.assertStringContains("Should throws expected exception", "String dirName", e.getMessage());
        }
        assertTrue("Should find deployed sitesection", SiteSection.isDeployed("QA"));
        assertFalse("Should not find undeployed sitesection", SiteSection.isDeployed("Fnord"));
    }

    @Test
    public void testGenerateNavigationTree() throws Exception {
        SiteSection site = new SiteSection("table.job.harvestname", "pref", 2, new String[][] { {"page1", "title1"},
                {"page2", "pagetitle;details.for.job"}, {"page3", "title3"}}, "HarvestDefinition",
                "dk.netarkivet.harvester.Translations") {
            public void initialize() {
            }

            public void close() {
            }
        };
        // English, URL not in site section
        JspWriterMockup jwm = new JspWriterMockup();
        StringBuilder sb = new StringBuilder();
        site.generateNavigationTree(sb, null, "http://foo.bar", null, new Locale("en"));
        jwm.print(sb.toString());
        String result = jwm.sw.toString();
        StringAsserts.assertStringContains("Should contain main title in English", "Harvest name", result);
        StringAsserts.assertStringNotContains("Should not contain subpage 1 title", "title1", result);
        StringAsserts.assertStringNotContains("Should not contain subpage 2", "page2", result);

        // English, URL in site section
        jwm = new JspWriterMockup();
        sb.setLength(0);
        site.generateNavigationTree(sb, null, "http://foo.bar/HarvestDefinition/pref-page3.jsp", null, new Locale("en"));
        jwm.print(sb.toString());
        result = jwm.sw.toString();
        StringAsserts.assertStringContains("Should contain main title in English", "Harvest name", result);
        StringAsserts.assertStringContains("Should contain subpage", "page1", result);
        StringAsserts.assertStringContains("Should contain subpage", "title1", result);
        StringAsserts.assertStringContains("Should contain subpage", "page2", result);
        StringAsserts.assertStringContains("Should contain subpage, English title", "Details for Job", result);
        StringAsserts.assertStringNotContains("Should not contain subpage 3", "page3", result);
        StringAsserts.assertStringNotContains("Should contain subpage 3", "title3", result);

        // Danish, URL in site section
        jwm = new JspWriterMockup();
        sb.setLength(0);
        site.generateNavigationTree(sb, null, "http://foo.bar/HarvestDefinition/pref-page3.jsp", null, new Locale("da"));
        jwm.print(sb.toString());
        result = jwm.sw.toString();
        StringAsserts.assertStringContains("Should contain main title in Danish", "Høstning", result);
        StringAsserts.assertStringContains("Should contain subpage", "page1", result);
        StringAsserts.assertStringContains("Should contain subpage", "title1", result);
        StringAsserts.assertStringContains("Should contain subpage", "page2", result);
        StringAsserts.assertStringContains("Should contain subpage, Danish title", "Jobdetaljer", result);
        StringAsserts.assertStringNotContains("Should not contain subpage 3", "page3", result);
        StringAsserts.assertStringNotContains("Should contain subpage 3", "title3", result);
    }
}
