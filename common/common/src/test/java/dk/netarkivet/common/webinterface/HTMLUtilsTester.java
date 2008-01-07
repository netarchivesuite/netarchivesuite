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
package dk.netarkivet.common.webinterface;
/**
 * kfc forgot to comment this!
 */

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import junit.framework.TestCase;

import dk.netarkivet.common.Constants;
import dk.netarkivet.common.Settings;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.harvester.webinterface.JspTestCase;
import dk.netarkivet.harvester.webinterface.WebinterfaceTestCase;
import dk.netarkivet.testutils.StringAsserts;


public class HTMLUtilsTester extends TestCase {
    public HTMLUtilsTester(String s) {
        super(s);
    }

    public void setUp() {
        Settings.reload();
        SiteSection.cleanup();
    }

    public void tearDown() {
    }

    /**
     * Test expected behaviour: Escape double quotes, newlines and other special
     * characters
     * @throws Exception
     */
    public void testEscapeJavascriptQuotes() throws Exception {
        assertEquals("Null should be empty string",
                     "", HTMLUtils.escapeJavascriptQuotes(null));
        assertEquals("Quotes should be escaped",
                     "\\\"he\\'\\\"x\\\"\\\"st\\\"\\\"", HTMLUtils.escapeJavascriptQuotes("\"he'\"x\"\"st\"\""));
        assertEquals("Special characters should be escaped",
                     "\\b\\f\\n\\r\\t\\v\\\\", HTMLUtils.escapeJavascriptQuotes("\b\f\n\r\t\u000B\\"));
        assertEquals("Other control characters should be escaped",
                     "\\u0000\\u0001\\u0002\\u0003\\u0004\\u0005\\u0006\\u0007\\u000E\\u000F\\u0010\\u0011\\u0012\\u0013\\u0014\\u0015\\u0016\\u0017\\u0018\\u0019\\u001A\\u001B\\u001C\\u001D\\u001E\\u001F",
                     HTMLUtils.escapeJavascriptQuotes("\u0000\u0001\u0002\u0003\u0004\u0005\u0006\u0007\u000E\u000F\u0010\u0011\u0012\u0013\u0014\u0015\u0016\u0017\u0018\u0019\u001A\u001B\u001C\u001D\u001E\u001F"));
    }

    /** Test URL encoding. */
    public void testEncode() throws Exception {
        assertEquals("Should encode space as +", "a+b", HTMLUtils.encode("a b"));
        assertEquals("Should encode å in UTF-8", "%C3%A5", HTMLUtils.encode("å"));
    }

    /** Test URL decoding. */
    public void testDecode() throws Exception {
        assertEquals("Should decode + as space", "a b", HTMLUtils.decode("a+b"));
        assertEquals("Should decode å in UTF-8", "å", HTMLUtils.decode("%C3%A5"));
        assertEquals("Should be reverse of eachother",
                     "æblegrød med :// i og ()!!\"#¤%",
                     HTMLUtils.decode(HTMLUtils.encode("æblegrød med :// i og ()!!\"#¤%")));
    }

    /** Test header */
    public void testGenerateHeader() throws Exception {
        JspWriterMockup out = new JspWriterMockup();

        ServletRequest confRequest = new HttpServletRequest() {

            public String getAuthType() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Cookie[] getCookies() {
                return new Cookie[0];  //To change body of implemented methods use File | Settings | File Templates.
            }

            public long getDateHeader(String string) {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getHeader(String string) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Enumeration getHeaders(String string) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Enumeration getHeaderNames() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public int getIntHeader(String string) {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getMethod() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getPathInfo() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getPathTranslated() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getContextPath() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getQueryString() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getRemoteUser() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean isUserInRole(String string) {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Principal getUserPrincipal() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getRequestedSessionId() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getRequestURI() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public StringBuffer getRequestURL() {
                return new StringBuffer("/HarvestDefinition/Definitions-selective-harvests.jsp");
            }

            public String getServletPath() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public HttpSession getSession(boolean b) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public HttpSession getSession() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean isRequestedSessionIdValid() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean isRequestedSessionIdFromCookie() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean isRequestedSessionIdFromURL() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean isRequestedSessionIdFromUrl() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Object getAttribute(String string) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Enumeration getAttributeNames() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getCharacterEncoding() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public void setCharacterEncoding(String string)
                    throws UnsupportedEncodingException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public int getContentLength() {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getContentType() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public ServletInputStream getInputStream() throws IOException {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getParameter(String string) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Enumeration getParameterNames() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String[] getParameterValues(String string) {
                return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Map getParameterMap() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getProtocol() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getScheme() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getServerName() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public int getServerPort() {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public BufferedReader getReader() throws IOException {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getRemoteAddr() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getRemoteHost() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public void setAttribute(String string, Object object) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void removeAttribute(String string) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public Locale getLocale() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Enumeration getLocales() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean isSecure() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public RequestDispatcher getRequestDispatcher(String string) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getRealPath(String string) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public int getRemotePort() {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getLocalName() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getLocalAddr() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public int getLocalPort() {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }
        };
        PageContext pageContext
                = new WebinterfaceTestCase.TestPageContext(confRequest, out,
                new Locale("da"));
        HTMLUtils.generateHeader("TestTitle", pageContext);
        String result = out.sw.toString();
        StringAsserts.assertStringContains("Should contain title",
                                           "TestTitle",
                                           result);
        // Test navigation tree
        for (SiteSection ss : SiteSection.getSections()) {
            JspWriterMockup jwm = new JspWriterMockup();
            ss.generateNavigationTree(jwm, "http://foo.bar", new Locale("da"));
            String tree = jwm.sw.toString();
            StringAsserts.assertStringContains("Should contain site section navigation tree for this sitesection",
                                               tree,
                                               result);
        }

        //Test locale
        int i = 0;
        for (String locale : Settings.getAll(Settings.LANGUAGE_LOCALE)) {
            StringAsserts.assertStringContains("Should contain link to locale",
                                               "locale=" + locale,
                                               result);
            StringAsserts.assertStringContains("Should contain name of locale",
                                               "name=" + Settings.getAll(Settings.LANGUAGE_NAME)[i++],
                                               result);
        }

        out = new JspWriterMockup();
        confRequest = new HttpServletRequest() {

            public String getAuthType() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Cookie[] getCookies() {
                return new Cookie[0];  //To change body of implemented methods use File | Settings | File Templates.
            }

            public long getDateHeader(String string) {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getHeader(String string) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Enumeration getHeaders(String string) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Enumeration getHeaderNames() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public int getIntHeader(String string) {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getMethod() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getPathInfo() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getPathTranslated() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getContextPath() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getQueryString() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getRemoteUser() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean isUserInRole(String string) {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Principal getUserPrincipal() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getRequestedSessionId() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getRequestURI() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public StringBuffer getRequestURL() {
                return new StringBuffer("http://foo.bar/History/Harveststatus-jobdetails.jsp");
            }

            public String getServletPath() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public HttpSession getSession(boolean b) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public HttpSession getSession() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean isRequestedSessionIdValid() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean isRequestedSessionIdFromCookie() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean isRequestedSessionIdFromURL() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean isRequestedSessionIdFromUrl() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Object getAttribute(String string) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Enumeration getAttributeNames() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getCharacterEncoding() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public void setCharacterEncoding(String string)
                    throws UnsupportedEncodingException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public int getContentLength() {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getContentType() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public ServletInputStream getInputStream() throws IOException {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getParameter(String string) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Enumeration getParameterNames() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String[] getParameterValues(String string) {
                return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Map getParameterMap() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getProtocol() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getScheme() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getServerName() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public int getServerPort() {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public BufferedReader getReader() throws IOException {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getRemoteAddr() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getRemoteHost() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public void setAttribute(String string, Object object) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void removeAttribute(String string) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public Locale getLocale() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Enumeration getLocales() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean isSecure() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public RequestDispatcher getRequestDispatcher(String string) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getRealPath(String string) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public int getRemotePort() {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getLocalName() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getLocalAddr() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public int getLocalPort() {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }
        };
        pageContext = new WebinterfaceTestCase.TestPageContext(confRequest, out,
                new Locale("en"));
        HTMLUtils.generateHeader(pageContext);
        result = out.sw.toString();
        StringAsserts.assertStringContains("Should contain English title",
                     "Details for Job",
                     result);
        out = new JspWriterMockup();
        pageContext
                = new WebinterfaceTestCase.TestPageContext(confRequest, out, new Locale("da"));
        HTMLUtils.generateHeader(pageContext);
        result = out.sw.toString();
        StringAsserts.assertStringContains("Should contain Danish title",
                                           "Jobdetaljer",
                                           result);
    }

    /** Test footer */
    public void testGenerateFooter() throws Exception {
        JspWriterMockup out = new JspWriterMockup();
        ServletRequest confRequest = new HttpServletRequest() {

            public String getAuthType() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Cookie[] getCookies() {
                return new Cookie[0];  //To change body of implemented methods use File | Settings | File Templates.
            }

            public long getDateHeader(String string) {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getHeader(String string) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Enumeration getHeaders(String string) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Enumeration getHeaderNames() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public int getIntHeader(String string) {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getMethod() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getPathInfo() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getPathTranslated() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getContextPath() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getQueryString() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getRemoteUser() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean isUserInRole(String string) {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Principal getUserPrincipal() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getRequestedSessionId() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getRequestURI() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public StringBuffer getRequestURL() {
                return new StringBuffer("/HarvestDefinition/Definitions-selective-harvests.jsp");
            }

            public String getServletPath() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public HttpSession getSession(boolean b) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public HttpSession getSession() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean isRequestedSessionIdValid() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean isRequestedSessionIdFromCookie() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean isRequestedSessionIdFromURL() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean isRequestedSessionIdFromUrl() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Object getAttribute(String string) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Enumeration getAttributeNames() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getCharacterEncoding() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public void setCharacterEncoding(String string)
                    throws UnsupportedEncodingException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public int getContentLength() {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getContentType() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public ServletInputStream getInputStream() throws IOException {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getParameter(String string) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Enumeration getParameterNames() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String[] getParameterValues(String string) {
                return new String[0];  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Map getParameterMap() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getProtocol() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getScheme() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getServerName() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public int getServerPort() {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public BufferedReader getReader() throws IOException {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getRemoteAddr() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getRemoteHost() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public void setAttribute(String string, Object object) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void removeAttribute(String string) {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public Locale getLocale() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public Enumeration getLocales() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public boolean isSecure() {
                return false;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public RequestDispatcher getRequestDispatcher(String string) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getRealPath(String string) {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public int getRemotePort() {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getLocalName() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public String getLocalAddr() {
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public int getLocalPort() {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }
        };
        PageContext pageContext
                = new WebinterfaceTestCase.TestPageContext(confRequest, out, new Locale("da"));
        HTMLUtils.generateHeader("TestTitle", pageContext);
        HTMLUtils.generateFooter(out);
        String result = out.sw.toString();
        JspTestCase.assertValidXHTML(result);
        StringAsserts.assertStringContains("The version string must be present",
                                           Constants.getVersionString(),
                                           result);
        StringAsserts.assertStringContains("The environment name must be there",
                                           "DEV",
                                           result);
    }

    /** Test makeTableElement. */
    public void testMakeTableElement() {
        assertEquals("Should escape values", "<td>&lt;</td>",
                     HTMLUtils.makeTableElement("<"));
    }

    /** Test null is hyphenated. */
    public void testNullToHyphen() {
        assertEquals("Should give hyphen", "-", HTMLUtils.nullToHyphen(null));
        assertEquals("Should give text", "text", HTMLUtils.nullToHyphen("text"));
        assertEquals("Should give nothing", "", HTMLUtils.nullToHyphen(""));
    }

    /** Test HTML escaping. */
    public void testEscapeHtmlValues() {
        assertEquals("Should return empty string on null", "",
                     HTMLUtils.escapeHtmlValues(null));
        assertEquals("Should escape values",
                     "&lt;&gt;'&quot;&amp;amp;",
                     HTMLUtils.escapeHtmlValues("<>'\"&amp;"));
    }

    public void testGetRowClass() throws Exception {
        assertEquals("Should return white row",
                     "row0", HTMLUtils.getRowClass(0));
        assertEquals("Should return white row",
                     "row0", HTMLUtils.getRowClass(1));
        assertEquals("Should return white row",
                     "row0", HTMLUtils.getRowClass(2));
        assertEquals("Should return grey row",
                     "row1", HTMLUtils.getRowClass(3));
        assertEquals("Should return grey row",
                     "row1", HTMLUtils.getRowClass(4));
        assertEquals("Should return grey row",
                     "row1", HTMLUtils.getRowClass(5));
        assertEquals("Should return white row",
                     "row0", HTMLUtils.getRowClass(6));
    }


    public void testParseOptionalLong() {
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        parameterMap.put("aLong", new String[]{ "10" });
        WebinterfaceTestCase.TestServletRequest request
                = new WebinterfaceTestCase.TestServletRequest();
        request.setParameterMap(parameterMap);
        I18n I18N = new I18n(dk.netarkivet.common.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new WebinterfaceTestCase.TestPageContext(request);

        assertEquals("Should be able to parse simple long",
                new Long(10L), HTMLUtils.parseOptionalLong(pageContext, "aLong", -1L));

        parameterMap.put("aLong", new String[]{ " -11  " });

        assertEquals("Should be able to parse spaced negative long",
                new Long(-11L), HTMLUtils.parseOptionalLong(pageContext, "aLong", -1L));

        assertEquals("Should get default if not set",
                new Long(-1L),  HTMLUtils.parseOptionalLong(pageContext,
                "anotherLong", -1L));

        parameterMap.put("aLong", new String[]{ Long.toString(((long) Integer.MAX_VALUE) * 5) });
        assertEquals("Should be able to parse large long",
                new Long(((long) Integer.MAX_VALUE) * 5),
                HTMLUtils.parseOptionalLong(pageContext, "aLong", -1L));

        parameterMap.put("aLong", new String[]{ ""} );
        assertEquals("Should get default from empty param",
                new Long(-2),
                HTMLUtils.parseOptionalLong(pageContext, "aLong", -2L));

        parameterMap.put("aLong", new String[]{ "   "} );
        assertEquals("Should get default from space-only param",
                new Long(-2),
                HTMLUtils.parseOptionalLong(pageContext, "aLong", -2L));

        parameterMap.put("aLong", new String[]{ "   "} );
        assertEquals("Should get null default from space-only param",
                null,
                HTMLUtils.parseOptionalLong(pageContext, "aLong", null));

        try {
            parameterMap.put("noLong", new String[] { "not a long" });
            HTMLUtils.parseOptionalLong(pageContext, "noLong", -1L);
            fail("Should have died on bad format");
        } catch (ForwardedToErrorPage e) {
            // expected
        }

        try {
            parameterMap.put("noLong", new String[] { " 2.5" });
            HTMLUtils.parseOptionalLong(pageContext, "noLong", -1L);
            fail("Should have died on float format");
        } catch (ForwardedToErrorPage e) {
            // expected
        }

    }

       public void testParseOptionalDate() {
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();
        parameterMap.put("aDate", new String[]{ "10/8 2007 6:17" });
        WebinterfaceTestCase.TestServletRequest request
                = new WebinterfaceTestCase.TestServletRequest();
        request.setParameterMap(parameterMap);
        I18n I18N = new I18n(dk.netarkivet.common.Constants.TRANSLATIONS_BUNDLE);
        PageContext pageContext = new WebinterfaceTestCase.TestPageContext(request);

           GregorianCalendar calendar = new GregorianCalendar(2007,
                                                              Calendar.AUGUST,
                                                              10, 6, 17, 00);
           assertEquals("Should be able to parse simple date",
                        calendar.getTime(),
                HTMLUtils.parseOptionalDate(pageContext, "aDate", "dd/M yyyy HH:mm", null));

        assertEquals("Should get default if not set",
                calendar.getTime(),  HTMLUtils.parseOptionalDate(pageContext,
                "anotherDate", "dd/M yyyy HH:mm", calendar.getTime()));

        parameterMap.put("aDate", new String[]{ ""} );
        assertEquals("Should get default from empty param",
                     calendar.getTime(),  HTMLUtils.parseOptionalDate(pageContext,
                     "aDate", "dd/M yyyy HH:mm", calendar.getTime()));

        parameterMap.put("aDate", new String[]{ "   "} );
        assertEquals("Should get default from space-only param",
                     calendar.getTime(),  HTMLUtils.parseOptionalDate(pageContext,
                     "aDate", "dd/M yyyy HH:mm", calendar.getTime()));

        parameterMap.put("aDate", new String[]{ "   "} );
        assertEquals("Should get null default from space-only param",
                     null,  HTMLUtils.parseOptionalDate(pageContext,
                     "aDate", "dd/M yyyy HH:mm", null));

        try {
            parameterMap.put("noDate", new String[] { "not a date" });
            HTMLUtils.parseOptionalDate(pageContext, "noDate", "dd/M yyyy HH:mm", null);
            fail("Should have died on bad format");
        } catch (ForwardedToErrorPage e) {
            // expected
        }
    }
}