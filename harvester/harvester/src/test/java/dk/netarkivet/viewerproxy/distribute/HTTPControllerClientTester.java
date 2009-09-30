/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
package dk.netarkivet.viewerproxy.distribute;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.StringAsserts;

/**
 * Unit-tests of the class HTTPControllerClient. 
 * Uses two dummy classes: An anonymous JspWriter,
 * and a MockHttpServletResponse.
 */
public class HTTPControllerClientTester extends TestCase {
    public HTTPControllerClientTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testRedirectForSimpleCommand()
            throws IOException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        JspWriter writer = new JspWriter(0, false) {
            public void newLine() throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void print(boolean b) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void print(char c) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void print(int i) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void print(long l) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void print(float v) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void print(double v) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void print(char[] chars) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void print(String string) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void print(Object object) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void println() throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void println(boolean b) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void println(char c) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void println(int i) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void println(long l) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void println(float v) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void println(double v) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void println(char[] chars) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void println(String string) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void println(Object object) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void clear() throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void clearBuffer() throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void write(char cbuf[], int off, int len) throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void flush() throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public void close() throws IOException {
                //To change body of implemented methods use File | Settings | File Templates.
            }

            public int getRemaining() {
                return 0;  //To change body of implemented methods use File | Settings | File Templates.
            }
        };
        MockHttpServletResponse response = new MockHttpServletResponse();
        HTTPControllerClient client = new HTTPControllerClient(
                response, writer, null);
        Method redirectForSimpleCommand = ReflectUtils.getPrivateMethod
                (HTTPControllerClient.class, "redirectForSimpleCommand",
                        String.class, Boolean.TYPE);
        String command1 = "fooBar";
        redirectForSimpleCommand.invoke(client, command1, false);
        assertNotNull("Test that redirect was sent", response.redirectedTo);
        StringAsserts.assertStringContains("Must have command in response",
                command1, response.redirectedTo);
        try {
            redirectForSimpleCommand.invoke(client, command1, true);
            fail("Should test that returnUrl is non-null");
        } catch (InvocationTargetException e) {
            assertTrue("Should have ArgumentNotValid cause in exception",
                    e.getCause() instanceof ArgumentNotValid);
        }

        String returnUrl = "anUrl";
        client = new HTTPControllerClient(response, writer, returnUrl);
        String command2 = "barfu";
        redirectForSimpleCommand.invoke(client, command2, true);
        assertNotNull("Test that redirect was sent", response.redirectedTo);
        StringAsserts.assertStringContains("Must have command in response",
                command2, response.redirectedTo);
        StringAsserts.assertStringContains("Must have returnUrl in response",
                returnUrl, response.redirectedTo);
        redirectForSimpleCommand.invoke(client, command1, false);
        StringAsserts.assertStringContains("Must have command in response",
                command1, response.redirectedTo);
        StringAsserts.assertStringNotContains("Must not have returnUrl in response",
                returnUrl, response.redirectedTo);
    }

    private static class MockHttpServletResponse implements HttpServletResponse {
        public String redirectedTo;

        public void addCookie(Cookie cookie) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean containsHeader(String string) {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String encodeURL(String string) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String encodeRedirectURL(String string) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String encodeUrl(String string) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String encodeRedirectUrl(String string) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void sendError(int i, String string) throws IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void sendError(int i) throws IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void sendRedirect(String string) throws IOException {
            redirectedTo = string;
        }

        public void setDateHeader(String string, long l) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void addDateHeader(String string, long l) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setHeader(String string, String string1) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void addHeader(String string, String string1) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setIntHeader(String string, int i) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void addIntHeader(String string, int i) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setStatus(int i) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setStatus(int i, String string) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getCharacterEncoding() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getContentType() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public ServletOutputStream getOutputStream() throws IOException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public PrintWriter getWriter() throws IOException {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setCharacterEncoding(String string) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setContentLength(int i) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setContentType(String string) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setBufferSize(int i) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public int getBufferSize() {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void flushBuffer() throws IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void resetBuffer() {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public boolean isCommitted() {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void reset() {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setLocale(Locale locale) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public Locale getLocale() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }
}
