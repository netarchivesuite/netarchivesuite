/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import javax.el.ELContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/**
 * A TestCase subclass specifically tailored to test webinterface classes,
 * primarily the classes in dk.netarkivet.harvester.webinterface:
 * HarvestStatusTester, EventHarvestTester, DomainDefinitionTester,
 * ScheduleDefinitionTester, SnapshotHarvestDefinitionTester but also
 * dk.netarkivet.archive.webinterface.BitpreserveFileStatusTester
 */
public class WebinterfaceTestCase extends TestCase {
    ReloadSettings rs = new ReloadSettings();

    public WebinterfaceTestCase(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        rs.setUp();
        TestFileUtils.copyDirectoryNonCVS(TestInfo.ORIGINALS_DIR,
                TestInfo.WORKING_DIR);
   }

    public void tearDown() throws Exception {
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        rs.tearDown();
        super.tearDown();
    }


    /**
     * A dummy class implementing only the methods for getting parameters. A
     * single setter method is provided to set the parameter map.
     */
    public static class TestServletRequest implements ServletRequest {
        Map<String, Object> attributes = new HashMap<String, Object>();
        Map<String, String[]> parameterMap = new HashMap<String, String[]>();

        public void setParameterMap(Map<String, String[]> parameterMap) {
            this.parameterMap = parameterMap;
        }

        public Object getAttribute(String string) {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public Enumeration getAttributeNames() {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public String getCharacterEncoding() {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void setCharacterEncoding(String string)
                throws UnsupportedEncodingException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public int getContentLength() {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public String getContentType() {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public ServletInputStream getInputStream() throws IOException {
            //TODO: implement method
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
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public String[] getParameterValues(String string) {
            return parameterMap.get(string);
        }

        public Map getParameterMap() {
            return parameterMap;
        }

        public String getProtocol() {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public String getScheme() {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public String getServerName() {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public int getServerPort() {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public BufferedReader getReader() throws IOException {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public String getRemoteAddr() {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public String getRemoteHost() {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public void setAttribute(String string, Object object) {
            attributes.put(string, object);
        }

        public void removeAttribute(String string) {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public Locale getLocale() {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public Enumeration getLocales() {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public boolean isSecure() {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public RequestDispatcher getRequestDispatcher(String string) {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public String getRealPath(String string) {
            //TODO: implement method
            throw new NotImplementedException("Not implemented");
        }

        public int getRemotePort() {
            // TODO Auto-generated method stub
            return 0;
        }

        public String getLocalName() {
            // TODO Auto-generated method stub
            return null;
        }

        public String getLocalAddr() {
            // TODO Auto-generated method stub
            return null;
        }

        public int getLocalPort() {
            // TODO Auto-generated method stub
            return 0;
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

        public TestPageContext(ServletRequest request, JspWriter out,
                               Locale locale) {
            this.request = request;
            this.out = out;
            this.locale = locale;
        }

        public void initialize(Servlet servlet, ServletRequest servletRequest,
                               ServletResponse servletResponse, String string,
                               boolean b, int i, boolean b1) throws IOException,
                                                                    IllegalStateException,
                                                                    IllegalArgumentException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void release() {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public HttpSession getSession() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Object getPage() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public ServletRequest getRequest() {
            return request;
        }

        public ServletResponse getResponse() {
            return new ServletResponse() {

                public String getCharacterEncoding() {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public String getContentType() {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public ServletOutputStream getOutputStream()
                        throws IOException {
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
                    return locale;
                }
            };
        }

        public Exception getException() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public ServletConfig getServletConfig() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public ServletContext getServletContext() {
            return new ServletContext() {
                public String getContextPath() {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public ServletContext getContext(String string) {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public int getMajorVersion() {
                    return 0;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public int getMinorVersion() {
                    return 0;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public String getMimeType(String string) {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public Set getResourcePaths(String string) {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public URL getResource(String string)
                        throws MalformedURLException {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public InputStream getResourceAsStream(String string) {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public RequestDispatcher getRequestDispatcher(String string) {
                    return new RequestDispatcher() {

                        public void forward(ServletRequest servletRequest,
                                            ServletResponse servletResponse)
                                throws ServletException, IOException {
                        }

                        public void include(ServletRequest servletRequest,
                                            ServletResponse servletResponse)
                                throws ServletException, IOException {
                            //To change body of implemented methods use File | Settings | File Templates.
                        }
                    };
                }

                public RequestDispatcher getNamedDispatcher(String string) {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public Servlet getServlet(String string)
                        throws ServletException {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public Enumeration getServlets() {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public Enumeration getServletNames() {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public void log(String string) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }

                public void log(Exception exception, String string) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }

                public void log(String string, Throwable throwable) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }

                public String getRealPath(String string) {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public String getServerInfo() {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public String getInitParameter(String string) {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public Enumeration getInitParameterNames() {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public Object getAttribute(String string) {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public Enumeration getAttributeNames() {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                public void setAttribute(String string, Object object) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }

                public void removeAttribute(String string) {
                    //To change body of implemented methods use File | Settings | File Templates.
                }

                public String getServletContextName() {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }
            };
        }

        public void forward(String string) throws ServletException,
                                                  IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void include(String string)
                throws ServletException, IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void include(String string, boolean b)
                throws ServletException, IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void handlePageException(Exception exception)
                throws ServletException, IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void handlePageException(Throwable throwable)
                throws ServletException, IOException {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setAttribute(String string, Object object) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setAttribute(String string, Object object, int i) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public Object getAttribute(String string) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Object getAttribute(String string, int i) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Object findAttribute(String string) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public void removeAttribute(String string) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void removeAttribute(String string, int i) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public int getAttributesScope(String string) {
            return 0;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public Enumeration<String> getAttributeNamesInScope(int i) {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public JspWriter getOut() {
            return out;
        }

        public ExpressionEvaluator getExpressionEvaluator() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public VariableResolver getVariableResolver() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public ELContext getELContext() {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    public static PageContext getDummyPageContext(final Locale l,
                                           final ServletRequest request) {
        return new PageContext() {
            public void initialize(Servlet servlet,
                                   ServletRequest servletRequest,
                                   ServletResponse servletResponse,
                                   String string,
                                   boolean b, int i, boolean b1)
                    throws IOException,
                           IllegalStateException, IllegalArgumentException {
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

                    public ServletOutputStream getOutputStream()
                            throws IOException {
                        return null;
                    }

                    public PrintWriter getWriter() throws IOException {
                        return null;
                    }

                    public void setCharacterEncoding(String string) {
                    }

                    public void setContentLength(int i) {
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

            public void forward(String string)
                    throws ServletException, IOException {
            }

            public void include(String string)
                    throws ServletException, IOException {
            }

            public void include(String string, boolean b)
                    throws ServletException, IOException {
            }

            public void handlePageException(Exception exception)
                    throws ServletException, IOException {
            }

            public void handlePageException(Throwable throwable)
                    throws ServletException, IOException {
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

            public ELContext getELContext() {
                return null;
            }
        };
    }
}
