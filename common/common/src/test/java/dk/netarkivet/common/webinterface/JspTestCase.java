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

import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.jsp.JspApplicationContext;
import javax.servlet.jsp.JspEngineInfo;
import javax.servlet.jsp.JspFactory;
import javax.servlet.jsp.PageContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import com.mockobjects.servlet.MockHttpServletRequest;
import com.mockobjects.servlet.MockHttpServletResponse;
import com.mockobjects.servlet.MockJspWriter;
import com.mockobjects.servlet.MockPageContext;
import junit.framework.TestCase;
import org.apache.jasper.JasperException;
import org.apache.jasper.JspC;
import org.apache.jasper.runtime.HttpJspBase;
import org.apache.tools.ant.filters.StringInputStream;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.NotImplementedException;
import dk.netarkivet.common.utils.ApplicationUtils;

/**
 * An attempt at making a subclass of TestCase suitable for testing JSP pages.
 * Only used by HarveststatusPerdomainTester.
 * Is currently not working!
 */
public class JspTestCase extends TestCase {
    protected static final File WEB_BASE_DIR = new File("webpages/HarvestDefinition");
    public static final File TOP_DATA_DIR =
            new File("tests/dk/netarkivet/harvester/datamodel/data/");
    protected static final File WORKING_DIR = new File(TOP_DATA_DIR, "working");
    protected static Map<String, Class<HttpJspBase>> compiledPages =
            new HashMap<String, Class<HttpJspBase>>();
    protected HttpJspBase instance;
    protected MockHttpServletRequest request;
    protected MockHttpServletResponse response;
    protected StringBuilder output;

    /** Setup for testing a given webpage.
     * Note that all parameters used by the JSP page must be defined using
     * request.setupAddParameter().  Calling it multiple times will add
     * parameter values rather than override them.
     *
     * @param jspPage The name of the page (under webpages/HarvestDefinition)
     */
    public void setUp(final String jspPage) throws JasperException,
            MalformedURLException, ClassNotFoundException,
            IllegalAccessException, InstantiationException{
        /*
        ApplicationUtils.dirMustExist(WORKING_DIR);
        if (!compiledPages.containsKey(jspPage)) {
            JspC jspc = new JspC();
            jspc.setArgs(new String[] {
                "-uriroot", WEB_BASE_DIR.getAbsolutePath(),
                "-compile",
                "-d", WORKING_DIR.getAbsolutePath(),
                jspPage } );
            jspc.execute();
            URLClassLoader loader = new URLClassLoader(
                    new URL[] { new URL("file://" + WORKING_DIR.getAbsolutePath()) } );
            Class<HttpJspBase> c = (Class<HttpJspBase>)
                    loader.loadClass("org.apache.jsp.Harveststatus_002dperdomain_jsp");
            compiledPages.put(jspPage, c);
        }

        instance = getCompiledPage(jspPage);
        request = new MockHttpServletRequest() {
            String encoding;

            public StringBuffer getRequestURL() {
                return new StringBuffer(jspPage);
            }

            public String getCharacterEncoding() {
                return encoding;
            }

            public void setCharacterEncoding(String s) {
                encoding = s;
            }
//
//            public int getRemotePort() {
//                //TODO: implement method
//                throw new NotImplementedException("Not implemented");
//            }
//
//            public String getLocalName() {
//                //TODO: implement methods
//                throw new NotImplementedException("Not implemented");
//            }
//
//            public String getLocalAddr() {
//                //TODO: implement method
//                throw new NotImplementedException("Not implemented");
//            }

//            public int getLocalPort() {
//                //TODO: implement method
//                throw new NotImplementedException("Not implemented");
//            }
        };
        response = new MockHttpServletResponse();

        // Things we want *all* pages to conform to
        response.setExpectedContentType("text/html;charset=UTF-8");
    */
    }

    protected void tearDown() {
        //FileUtils.removeRecursively(WORKING_DIR);
    }

    private HttpJspBase getCompiledPage(final String webpage)
            throws InstantiationException, IllegalAccessException {
        Class<HttpJspBase> c = compiledPages.get(webpage);
        HttpJspBase instance = c.newInstance();
        return instance;
    }

    public void runPage() throws IOException, ServletException {
        /*response.setExpectedErrorNothing();
        output = new StringBuilder();
        JspFactory factory = new JspFactory() {
            public PageContext getPageContext(Servlet servlet,
                                              ServletRequest servletRequest,
                                              ServletResponse servletResponse,
                                              String s, boolean b, int i, boolean b1) {
                final MockPageContext mockPageContext = new MockPageContext() {
//                    public void include(String s, boolean b) throws ServletException,
//                            IOException {
//                        //TODO: implement method
//                        throw new NotImplementedException("Not implemented");
//                    }

                    public void handlePageException(Throwable e) {
                        throw new IOFailure("Exception in page", e);
                    }

//                    public ExpressionEvaluator getExpressionEvaluator() {
//                        //TODO: implement method
//                        throw new NotImplementedException("Not implemented");
//                    }
//
//                    public VariableResolver getVariableResolver() {
//                        //TODO: implement method
//                        throw new NotImplementedException("Not implemented");
//                    }
//
//                    public ELContext getELContext() {
//                        //TODO: implement method
//                        throw new NotImplementedException("Not implemented");
//                    }
                };
                mockPageContext.setJspWriter(new MockJspWriter() {
                    public void print(String s) {
                        output.append(s);
                    }
                    public void write(String s) {
                        output.append(s);
                    }
                });
                return mockPageContext;
            }

            public void releasePageContext(PageContext pageContext) {
            }

            public JspEngineInfo getEngineInfo() {
                return null;
            }

            public JspApplicationContext getJspApplicationContext(
                    ServletContext servletContext) {
                //TODO: implement method
                throw new NotImplementedException("Not implemented");
            }

        };
        JspFactory.setDefaultFactory(factory);
        instance._jspService(request, response);
        response.verify();*/
    }

    /** Assert that this page has returned valid XHTML.  Unfortunately the
     * initial newlines caused by the import statements confuse the XHTML
     * parser, so no pages ever validate:(
     *
     * @throws SAXException
     * @throws IOException
     */
    public static void assertValidXHTML(
            String xhtml) throws SAXException, IOException,
            ParserConfigurationException {
        try {
            DocumentBuilder db;
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setValidating(true);
            db = dbf.newDocumentBuilder();
            db.setEntityResolver(new EntityResolver() {
                public InputSource resolveEntity(String publicId, String
                        systemId)
                        throws IOException {
                    if (systemId.equals(
                            "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd")) {
                        // return a special input source
                        return new InputSource(new FileReader(new File(
                                TOP_DATA_DIR + "/DTD/xhtml",
                                "xhtml1-strict.dtd")));
                    } else if (systemId.equals(
                                "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd")) {
                            // return a special input source
                            return new InputSource(new FileReader(new File(
                                    TOP_DATA_DIR + "/DTD/xhtml",
                                    "xhtml1-transitional.dtd")));
                    } else if (systemId.endsWith(
                                "xhtml-lat1-ent")) {
                            // return a special input source
                            return new InputSource(new FileReader(new File(
                                    TOP_DATA_DIR + "/DTD/xhtml",
                                    "xhtml-lat1-ent")));
                    } else {
                        // use the default behaviour
                        return null;
                    }
                }
            });
            db.setErrorHandler(new ErrorHandler() {
                public void warning(SAXParseException exception) {
                    return;
                }

                public void error(SAXParseException exception) {
                    throw new IOFailure("Not correct XHTML " + exception,
                            exception);
                }

                public void fatalError(SAXParseException exception) {
                    throw new IOFailure("Not correct XHTML " + exception,
                            exception);
                }
            });
            db.parse(new StringInputStream(xhtml));
        } catch (IOFailure e) {
            fail("Failed to validate as XHTML: " + e + "\n" + xhtml);
        }
    }

}
