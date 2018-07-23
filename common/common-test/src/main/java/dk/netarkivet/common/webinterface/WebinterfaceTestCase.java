/*
 * #%L
 * Netarchivesuite - common - test
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.el.ELContext;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;

import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;

import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * A TestCase subclass specifically tailored to test webinterface classes, primarily the classes in
 * dk.netarkivet.harvester.webinterface: HarvestStatusTester, EventHarvestTester, DomainDefinitionTester,
 * ScheduleDefinitionTester, SnapshotHarvestDefinitionTester but also
 * dk.netarkivet.archive.webinterface.BitpreserveFileStatusTester
 */
@SuppressWarnings({"rawtypes", "deprecation"})
public class WebinterfaceTestCase {
    ReloadSettings rs = new ReloadSettings();

    public void setUp() throws Exception {
        rs.setUp();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
    }

    public void tearDown() throws Exception {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        rs.tearDown();
    }

    /**
     * A dummy class implementing only the methods for getting parameters. A single setter method is provided to set the
     * parameter map.
     */
    public static class TestServletRequest implements ServletRequest {
        Map<String, Object> attributes = new HashMap<String, Object>();
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();

        public void setParameterMap(Map<String, String[]> parameterMap) {
            this.parameterMap = parameterMap;
        }

        public Object getAttribute(String string) {
            throw new NotImplementedException("Not implemented");
        }

        public Enumeration getAttributeNames() {
            throw new NotImplementedException("Not implemented");
        }

        public String getCharacterEncoding() {
            throw new NotImplementedException("Not implemented");
        }

        public void setCharacterEncoding(String string) throws UnsupportedEncodingException {
            throw new NotImplementedException("Not implemented");
        }

        public int getContentLength() {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public long getContentLengthLong() {
            return 0;
        }

        public String getContentType() {
            throw new NotImplementedException("Not implemented");
        }

        public ServletInputStream getInputStream() throws IOException {
            throw new NotImplementedException("Not implemented");
        }

        public String getParameter(String string) {
            String[] val = parameterMap.get(string);
            if (val == null) {
                return null;
            }
            return val[0];
        }

        public Enumeration getParameterNames() {
            throw new NotImplementedException("Not implemented");
        }

        public String[] getParameterValues(String string) {
            return parameterMap.get(string);
        }

        public Map getParameterMap() {
            return parameterMap;
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
            return 0;
        }

        public String getLocalName() {
            return null;
        }

        public String getLocalAddr() {
            return null;
        }

        public int getLocalPort() {
            return 0;
        }

        @Override
        public ServletContext getServletContext() {
            return null;
        }

        @Override
        public AsyncContext startAsync() throws IllegalStateException {
            return null;
        }

        @Override
        public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
                throws IllegalStateException {
            return null;
        }

        @Override
        public boolean isAsyncStarted() {
            return false;
        }

        @Override
        public boolean isAsyncSupported() {
            return false;
        }

        @Override
        public AsyncContext getAsyncContext() {
            return null;
        }

        @Override
        public DispatcherType getDispatcherType() {
            return null;
        }

    }

    public static class TestPageContext extends PageContext {
        private final ServletRequest request;
        private JspWriter out;
        private final Locale locale;

        public TestPageContext(ServletRequest request) {
            this.request = request;
            this.locale = new Locale("en");
        }

        public TestPageContext(ServletRequest request, JspWriter out, Locale locale) {
            this.request = request;
            this.out = out;
            this.locale = locale;
        }

        public void initialize(Servlet servlet, ServletRequest servletRequest, ServletResponse servletResponse,
                String string, boolean b, int i, boolean b1) throws IOException, IllegalStateException,
                IllegalArgumentException {
        }

        public void release() {
        }

        public HttpSession getSession() {
            return null;
        }

        public Object getPage() {
            return null;
        }

        public ServletRequest getRequest() {
            return request;
        }

        public ServletResponse getResponse() {
            return new ServletResponse() {

                public String getCharacterEncoding() {
                    return null; 
                }

                public String getContentType() {
                    return null; 
                }

                public ServletOutputStream getOutputStream() throws IOException {
                    return null; 
                }

                public PrintWriter getWriter() throws IOException {
                    return null; 
                }

                public void setCharacterEncoding(String string) {
                }

                public void setContentLength(int i) {
                }

                @Override
                public void setContentLengthLong(long l) {

                }

                public void setContentType(String string) {
                }

                public void setBufferSize(int i) {
                }

                public int getBufferSize() {
                    return 0;
                }

                public void flushBuffer() throws IOException {
                }

                public void resetBuffer() {
                }

                public boolean isCommitted() {
                    return false;
                }

                public void reset() {
                }

                public void setLocale(Locale locale) {
                    // To change body of implemented methods use File | Settings
                    // | File Templates.
                }

                public Locale getLocale() {
                    return locale;
                }
            };
        }

        public Exception getException() {
            return null;
        }

        public ServletConfig getServletConfig() {
            return null;
        }

        public ServletContext getServletContext() {
            return new ServletContext() {
                public String getContextPath() {
                    return null;
                }

                public ServletContext getContext(String string) {
                    return null; 
                }

                public int getMajorVersion() {
                    return 0; 
                }

                public int getMinorVersion() {
                    return 0; 
                }

                @Override
                public int getEffectiveMajorVersion() {
                    return 0;
                }

                @Override
                public int getEffectiveMinorVersion() {
                    return 0;
                }

                public String getMimeType(String string) {
                    return null;
                }

                public Set getResourcePaths(String string) {
                    return null; 
                }

                public URL getResource(String string) throws MalformedURLException {
                    return null; 
                }

                public InputStream getResourceAsStream(String string) {
                    return null; 
                }

                public RequestDispatcher getRequestDispatcher(String string) {
                    return new RequestDispatcher() {

                        public void forward(ServletRequest servletRequest, ServletResponse servletResponse)
                                throws ServletException, IOException {
                        }

                        public void include(ServletRequest servletRequest, ServletResponse servletResponse)
                                throws ServletException, IOException {
                     
                        }
                    };
                }

                public RequestDispatcher getNamedDispatcher(String string) {
                    return null; 
                }

                public Servlet getServlet(String string) throws ServletException {
                    return null; 
                }

                public Enumeration getServlets() {
                    return null; 
                }

                public Enumeration getServletNames() {
                    return null; 
                }

                public void log(String string) {
                   
                }

                public void log(Exception exception, String string) {
                   
                }

                public void log(String string, Throwable throwable) {
                   
                }

                public String getRealPath(String string) {
                    return null; 
                }

                public String getServerInfo() {
                    return null; 
                }

                public String getInitParameter(String string) {
                    return null; 
                }

                public Enumeration getInitParameterNames() {
                    return null; 
                }

                @Override
                public boolean setInitParameter(String s, String s2) {
                    return false;
                }

                public Object getAttribute(String string) {
                    return null; 
                }

                public Enumeration getAttributeNames() {
                    return null; 
                }

                public void setAttribute(String string, Object object) {
                }

                public void removeAttribute(String string) {
                }

                public String getServletContextName() {
                    return null;
                }

                @Override
                public ServletRegistration.Dynamic addServlet(String s, String s2) {
                    return null;
                }

                @Override
                public ServletRegistration.Dynamic addServlet(String s, Servlet servlet) {
                    return null;
                }

                @Override
                public ServletRegistration.Dynamic addServlet(String s, Class<? extends Servlet> aClass) {
                    return null;
                }

                @Override
                public <T extends Servlet> T createServlet(Class<T> tClass) throws ServletException {
                    return null;
                }

                @Override
                public ServletRegistration getServletRegistration(String s) {
                    return null;
                }

                @Override
                public Map<String, ? extends ServletRegistration> getServletRegistrations() {
                    return null;
                }

                @Override
                public FilterRegistration.Dynamic addFilter(String s, String s2) {
                    return null;
                }

                @Override
                public FilterRegistration.Dynamic addFilter(String s, Filter filter) {
                    return null;
                }

                @Override
                public FilterRegistration.Dynamic addFilter(String s, Class<? extends Filter> aClass) {
                    return null;
                }

                @Override
                public <T extends Filter> T createFilter(Class<T> tClass) throws ServletException {
                    return null;
                }

                @Override
                public FilterRegistration getFilterRegistration(String s) {
                    return null;
                }

                @Override
                public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
                    return null;
                }

                @Override
                public SessionCookieConfig getSessionCookieConfig() {
                    return null;
                }

                @Override
                public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {

                }

                @Override
                public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
                    return null;
                }

                @Override
                public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
                    return null;
                }

                @Override
                public void addListener(String s) {

                }

                @Override
                public <T extends EventListener> void addListener(T t) {

                }

                @Override
                public void addListener(Class<? extends EventListener> aClass) {

                }

                @Override
                public <T extends EventListener> T createListener(Class<T> tClass) throws ServletException {
                    return null;
                }

                @Override
                public JspConfigDescriptor getJspConfigDescriptor() {
                    return null;
                }

                @Override
                public ClassLoader getClassLoader() {
                    return null;
                }

                @Override
                public void declareRoles(String... strings) {

                }

                @Override
                public String getVirtualServerName() {
                    return null;
                }

            };
        }

        public void forward(String string) throws ServletException, IOException {
        }

        public void include(String string) throws ServletException, IOException {
        }

        public void include(String string, boolean b) throws ServletException, IOException {
        }

        public void handlePageException(Exception exception) throws ServletException, IOException {
        }

        public void handlePageException(Throwable throwable) throws ServletException, IOException {
        }

        public void setAttribute(String string, Object object) {
        }

        public void setAttribute(String string, Object object, int i) {
        }

        public Object getAttribute(String string) {
            return null; 
        }

        public Object getAttribute(String string, int i) {
            return null; 
        }

        public Object findAttribute(String string) {
            return null; 
        }

        public void removeAttribute(String string) {
        }

        public void removeAttribute(String string, int i) {
        }

        public int getAttributesScope(String string) {
            return 0; 
        }

        public Enumeration<String> getAttributeNamesInScope(int i) {
            return null; 
        }

        public JspWriter getOut() {
            return out;
        }

        public ExpressionEvaluator getExpressionEvaluator() {
            return null; 
        }

        public VariableResolver getVariableResolver() {
            return null; 
        }

		@Override
		public ELContext getELContext() {
			return null;
		}
    }

    public static PageContext getDummyPageContext(final Locale l, final ServletRequest request) {
        return new PageContext() {
            public void initialize(Servlet servlet, ServletRequest servletRequest, ServletResponse servletResponse,
                    String string, boolean b, int i, boolean b1) throws IOException, IllegalStateException,
                    IllegalArgumentException {
            }

            public void release() {
            }

            public HttpSession getSession() {
                return null;
            }

            public Object getPage() {
                return null;
            }

            public ServletRequest getRequest() {
                return request;
            }

            public ServletResponse getResponse() {
                return new ServletResponse() {
                    public String getCharacterEncoding() {
                        return null;
                    }

                    public String getContentType() {
                        return null;
                    }

                    public ServletOutputStream getOutputStream() throws IOException {
                        return null;
                    }

                    public PrintWriter getWriter() throws IOException {
                        return null;
                    }

                    public void setCharacterEncoding(String string) {
                    }

                    public void setContentLength(int i) {
                    }

                    @Override
                    public void setContentLengthLong(long l) {

                    }

                    public void setContentType(String string) {
                    }

                    public void setBufferSize(int i) {
                    }

                    public int getBufferSize() {
                        return 0;
                    }

                    public void flushBuffer() throws IOException {
                    }

                    public void resetBuffer() {
                    }

                    public boolean isCommitted() {
                        return false;
                    }

                    public void reset() {
                    }

                    public void setLocale(Locale locale) {
                    }

                    public Locale getLocale() {
                        return l;
                    }
                };
            }

            public Exception getException() {
                return null;
            }

            public ServletConfig getServletConfig() {
                return null;
            }

            public ServletContext getServletContext() {
                return null;
            }

            public void forward(String string) throws ServletException, IOException {
            }

            public void include(String string) throws ServletException, IOException {
            }

            public void include(String string, boolean b) throws ServletException, IOException {
            }

            public void handlePageException(Exception exception) throws ServletException, IOException {
            }

            public void handlePageException(Throwable throwable) throws ServletException, IOException {
            }

            public void setAttribute(String string, Object object) {
            }

            public void setAttribute(String string, Object object, int i) {
            }

            public Object getAttribute(String string) {
                return null;
            }

            public Object getAttribute(String string, int i) {
                return null;
            }

            public Object findAttribute(String string) {
                return null;
            }

            public void removeAttribute(String string) {
            }

            public void removeAttribute(String string, int i) {
            }

            public int getAttributesScope(String string) {
                return 0;
            }

            public Enumeration<String> getAttributeNamesInScope(int i) {
                return null;
            }

            public JspWriter getOut() {
                return null;
            }

            public ExpressionEvaluator getExpressionEvaluator() {
                return null;
            }

            public VariableResolver getVariableResolver() {
                return null;
            }

			@Override
			public ELContext getELContext() {
				return null;
			}
        };
    }
}
