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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringTree;
import dk.netarkivet.testutils.StringAsserts;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/** Unit tests for the HTMLUtils utility class. */
@SuppressWarnings({"rawtypes", "unused"})
public class HTMLUtilsTester {
    ReloadSettings rs = new ReloadSettings();

    @Before
    public void setUp() {
        rs.setUp();
        SiteSection.cleanup();
    }

    @After
    public void tearDown() {
        rs.tearDown();
    }

    /**
     * Test expected behaviour: Escape double quotes, newlines and other special characters.
     *
     * @throws Exception
     */
    @Test
    public void testEscapeJavascriptQuotes() throws Exception {
        assertEquals("Null should be empty string", "", HTMLUtils.escapeJavascriptQuotes(null));
        assertEquals("Quotes should be escaped", "\\\"he\\'\\\"x\\\"\\\"st\\\"\\\"",
                HTMLUtils.escapeJavascriptQuotes("\"he'\"x\"\"st\"\""));
        assertEquals("Special characters should be escaped", "\\b\\f\\n\\r\\t\\v\\\\",
                HTMLUtils.escapeJavascriptQuotes("\b\f\n\r\t\u000B\\"));
        assertEquals("Other control characters should be escaped",
                "\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007"
                        + "\\u000E\\u000F\\u0010\\u0011\\u0012\\u0013\\u0014\\u0015"
                        + "\\u0016\\u0017\\u0018\\u0019\\u001A\\u001B\\u001C\\u001D" + "\\u001E\\u001F",
                HTMLUtils.escapeJavascriptQuotes("\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007"
                        + "\u000E\u000F\u0010\u0011\u0012\u0013\u0014" + "\u0015\u0016\u0017\u0018\u0019\u001A\u001B"
                        + "\u001C\u001D\u001E\u001F"));
    }

    /** Test URL encoding. */
    @Test
    public void testEncode() throws Exception {
        assertEquals("Should encode space as +", "a+b", HTMLUtils.encode("a b"));
        assertEquals("Should encode å in UTF-8", "%C3%A5", HTMLUtils.encode("å"));
    }

    /** Test URL decoding. */
    @Test
    public void testDecode() throws Exception {
        assertEquals("Should decode + as space", "a b", HTMLUtils.decode("a+b"));
        assertEquals("Should decode å in UTF-8", "å", HTMLUtils.decode("%C3%A5"));
        assertEquals("Should be reverse of eachother", "æblegrød med :// i og ()!!\"#¤%",
                HTMLUtils.decode(HTMLUtils.encode("æblegrød med :// i og ()!!\"#¤%")));
    }

    /** Test header. */
    @Test
    public void testGenerateHeader() throws Exception {
        JspWriterMockup out = new JspWriterMockup();

        ServletRequest confRequest = makeHttpServletRequest("HarvestDefinition/Definitions-selective-harvests.jsp");

        PageContext pageContext = new WebinterfaceTestCase.TestPageContext(confRequest, out, new Locale("da"));
        HTMLUtils.generateHeader("TestTitle", pageContext);
        String result = out.sw.toString();
        StringAsserts.assertStringContains("Should contain title", "TestTitle", result);
        // Test navigation tree
        for (SiteSection ss : SiteSection.getSections()) {
            JspWriterMockup jwm = new JspWriterMockup();
            StringBuilder sb = new StringBuilder();
            ss.generateNavigationTree(sb, (HttpServletRequest)confRequest, "http://foo.bar", null, new Locale("da"));
            jwm.print(sb.toString());
            String tree = jwm.sw.toString();
            StringAsserts.assertStringContains("Should contain site section navigation tree for this sitesection",
                    tree, result);
        }

        // Test locale
        StringTree<String> webinterfaceSettings = Settings.getTree(CommonSettings.WEBINTERFACE_SETTINGS);

        for (StringTree<String> language : webinterfaceSettings.getSubTrees(CommonSettings.WEBINTERFACE_LANGUAGE)) {
            String locale = language.getValue(CommonSettings.WEBINTERFACE_LANGUAGE_LOCALE);
            String name = language.getValue(CommonSettings.WEBINTERFACE_LANGUAGE_NAME);
            StringAsserts.assertStringContains("Should contain link to locale", "locale=" + locale, result);
            StringAsserts.assertStringContains("Should contain name of locale",
                    "name=" + HTMLUtils.encodeAndEscapeHTML(name), result);
        }

        out = new JspWriterMockup();
        confRequest = makeHttpServletRequest("http://foo.bar/History/Harveststatus-jobdetails.jsp");

        pageContext = new WebinterfaceTestCase.TestPageContext(confRequest, out, new Locale("en"));
        HTMLUtils.generateHeader(pageContext);
        result = out.sw.toString();
        StringAsserts.assertStringContains("Should contain English title", "Details for Job", result);
        out = new JspWriterMockup();
        pageContext = new WebinterfaceTestCase.TestPageContext(confRequest, out, new Locale("da"));
        HTMLUtils.generateHeader(pageContext);
        result = out.sw.toString();
        StringAsserts.assertStringContains("Should contain Danish title", "Jobdetaljer", result);
    }

    /** Test footer. */
    @Test
    public void testGenerateFooter() throws Exception {
        JspWriterMockup out = new JspWriterMockup();
        ServletRequest confRequest = makeHttpServletRequest("/HarvestDefinition/Definitions-selective-harvests.jsp");

        PageContext pageContext = new WebinterfaceTestCase.TestPageContext(confRequest, out, new Locale("da"));
        HTMLUtils.generateHeader("TestTitle", pageContext);
        HTMLUtils.generateFooter(out);
        String result = out.sw.toString();
        StringAsserts.assertStringContains("The version string must be present", Constants.getVersionString(true), result);
        StringAsserts.assertStringContains("The environment name must be there", "DEV", result);
    }

    /** Test makeTableElement. */
    @Test
    public void testMakeTableElement() {
        assertEquals("Should escape values", "<td>&lt;</td>", HTMLUtils.makeTableElement("<"));
    }

    /** Test null is hyphenated. */
    @Test
    public void testNullToHyphen() {
        assertEquals("Should give hyphen", "-", HTMLUtils.nullToHyphen(null));
        assertEquals("Should give text", "text", HTMLUtils.nullToHyphen("text"));
        assertEquals("Should give nothing", "", HTMLUtils.nullToHyphen(""));
    }

    /** Test HTML escaping. */
    @Test
    public void testEscapeHtmlValues() {
        assertEquals("Should return empty string on null", "", HTMLUtils.escapeHtmlValues(null));
        assertEquals("Should escape values", "&lt;&gt;'&quot;&amp;amp;", HTMLUtils.escapeHtmlValues("<>'\"&amp;"));
    }

    @Test
    public void testGetRowClass() throws Exception {
        assertEquals("Should return white row", "row0", HTMLUtils.getRowClass(0));
        assertEquals("Should return white row", "row0", HTMLUtils.getRowClass(1));
        assertEquals("Should return white row", "row0", HTMLUtils.getRowClass(2));
        assertEquals("Should return grey row", "row1", HTMLUtils.getRowClass(3));
        assertEquals("Should return grey row", "row1", HTMLUtils.getRowClass(4));
        assertEquals("Should return grey row", "row1", HTMLUtils.getRowClass(5));
        assertEquals("Should return white row", "row0", HTMLUtils.getRowClass(6));
    }

    @Test
    public void testParseOptionalLong() {
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        parameterMap.put("aLong", new String[] {"10"});
        WebinterfaceTestCase.TestServletRequest request = new WebinterfaceTestCase.TestServletRequest();
        request.setParameterMap(parameterMap);
        // I18n I18N = new I18n(dk.netarkivet.common.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new WebinterfaceTestCase.TestPageContext(request);

        assertEquals("Should be able to parse simple long", Long.valueOf(10L),
                HTMLUtils.parseOptionalLong(pageContext, "aLong", -1L));

        parameterMap.put("aLong", new String[] {" -11  "});

        assertEquals("Should be able to parse spaced negative long", Long.valueOf(-11L),
                HTMLUtils.parseOptionalLong(pageContext, "aLong", -1L));

        assertEquals("Should get default if not set", Long.valueOf(-1L),
                HTMLUtils.parseOptionalLong(pageContext, "anotherLong", -1L));
        parameterMap.put("aLong", new String[] {Long.toString(((long) Integer.MAX_VALUE) * 5)});
        assertEquals("Should be able to parse large long", new Long(((long) Integer.MAX_VALUE) * 5),
                HTMLUtils.parseOptionalLong(pageContext, "aLong", -1L));

        parameterMap.put("aLong", new String[] {""});
        assertEquals("Should get default from empty param", Long.valueOf(-2L),
                HTMLUtils.parseOptionalLong(pageContext, "aLong", -2L));

        parameterMap.put("aLong", new String[] {"   "});
        assertEquals("Should get default from space-only param", Long.valueOf(-2L),
                HTMLUtils.parseOptionalLong(pageContext, "aLong", -2L));

        parameterMap.put("aLong", new String[] {"   "});
        assertEquals("Should get null default from space-only param", null,
                HTMLUtils.parseOptionalLong(pageContext, "aLong", null));

        try {
            parameterMap.put("noLong", new String[] {"not a long"});
            HTMLUtils.parseOptionalLong(pageContext, "noLong", -1L);
            fail("Should have died on bad format");
        } catch (ForwardedToErrorPage e) {
            // expected
        }

        try {
            parameterMap.put("noLong", new String[] {" 2.5"});
            Long noLong = HTMLUtils.parseOptionalLong(pageContext, "noLong", -1L);
            /*
             * fail("Should have died on float format, but was interpreted as the long value : " + noLong);
             */
        } catch (ForwardedToErrorPage e) {
            fail("parse method is documented not to use all of a given string as 2.5");
        }

    }

    @Test
    public void testParseOptionalDate() {
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        parameterMap.put("aDate", new String[] {"10/8 2007 6:17"});
        WebinterfaceTestCase.TestServletRequest request = new WebinterfaceTestCase.TestServletRequest();
        request.setParameterMap(parameterMap);
        // I18n I18N = new I18n(
        // dk.netarkivet.common.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new WebinterfaceTestCase.TestPageContext(request);

        GregorianCalendar calendar = new GregorianCalendar(2007, Calendar.AUGUST, 10, 6, 17, 00);
        assertEquals("Should be able to parse simple date", calendar.getTime(),
                HTMLUtils.parseOptionalDate(pageContext, "aDate", "dd/M yyyy HH:mm", null));

        assertEquals("Should get default if not set", calendar.getTime(),
                HTMLUtils.parseOptionalDate(pageContext, "anotherDate", "dd/M yyyy HH:mm", calendar.getTime()));

        parameterMap.put("aDate", new String[] {""});
        assertEquals("Should get default from empty param", calendar.getTime(),
                HTMLUtils.parseOptionalDate(pageContext, "aDate", "dd/M yyyy HH:mm", calendar.getTime()));

        parameterMap.put("aDate", new String[] {"   "});
        assertEquals("Should get default from space-only param", calendar.getTime(),
                HTMLUtils.parseOptionalDate(pageContext, "aDate", "dd/M yyyy HH:mm", calendar.getTime()));

        parameterMap.put("aDate", new String[] {"   "});
        assertEquals("Should get null default from space-only param", null,
                HTMLUtils.parseOptionalDate(pageContext, "aDate", "dd/M yyyy HH:mm", null));

        try {
            parameterMap.put("noDate", new String[] {"not a date"});
            HTMLUtils.parseOptionalDate(pageContext, "noDate", "dd/M yyyy HH:mm", null);
            fail("Should have died on bad format");
        } catch (ForwardedToErrorPage e) {
            // expected
        }
    }

    /**
     * Make a HttpServletRequest with the given requestUrl.
     *
     * @param requestUrl
     * @return
     */
    private HttpServletRequest makeHttpServletRequest(final String requestUrl) {
        HttpServletRequest requestStub = mock(HttpServletRequest.class);
        when(requestStub.getRequestURL()).thenReturn(new StringBuffer(requestUrl));
        return requestStub;
    }
}
