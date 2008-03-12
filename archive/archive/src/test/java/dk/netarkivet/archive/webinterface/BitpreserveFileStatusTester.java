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
package dk.netarkivet.archive.webinterface;

/**
 * lc forgot to comment this!
 */

import javax.el.ELContext;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.el.ExpressionEvaluator;
import javax.servlet.jsp.el.VariableResolver;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import com.mockobjects.servlet.MockHttpServletRequest;

import dk.netarkivet.archive.arcrepository.bitpreservation.Constants;
import dk.netarkivet.archive.arcrepository.bitpreservation.FileBasedActiveBitPreservation;
import dk.netarkivet.archive.arcrepository.bitpreservation.FilePreservationState;
import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.distribute.arcrepository.Location;
import dk.netarkivet.harvester.webinterface.TestInfo;
import dk.netarkivet.harvester.webinterface.WebinterfaceTestCase;
import dk.netarkivet.testutils.CollectionAsserts;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestUtils;

public class BitpreserveFileStatusTester extends WebinterfaceTestCase {
    private static final String GET_INFO_METHOD = "getFilePreservationStatus";
    private static final String ADD_METHOD = "reestablishMissingFile";

    public BitpreserveFileStatusTester(String s) {
        super(s);
    }


    public void setUp() throws Exception {
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        super.setUp();
    }

    public void tearDown() throws Exception {
        Settings.reload();
        super.tearDown();
    }

    public void testProcessMissingRequest() throws Exception {
        if (!TestUtils.runningAs("SVC")) {
            return;
        }
        Settings.set(Settings.DIR_ARCREPOSITORY_BITPRESERVATION,
                TestInfo.WORKING_DIR.getAbsolutePath());
        MockFileBasedActiveBitPreservation mockabp = new MockFileBasedActiveBitPreservation();
        MockHttpServletRequest request = new MockHttpServletRequest();
        String ba1 = "SB";
        String ba2 = "KB";
        String filename1 = "foo";
        String filename2 = "bar";
        Locale defaultLocale = new Locale("da");
        // First test a working set of params
        Map<String, String[]> args = new HashMap<String, String[]>();
        args.put(dk.netarkivet.archive.webinterface.Constants.ADD_COMMAND,
                new String[] {
                    ba1 + Constants.STRING_FILENAME_SEPARATOR + filename1
                });
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.ADD_COMMAND,
                new String[] {
                    ba1 + Constants.STRING_FILENAME_SEPARATOR + filename1
                });
        args.put(dk.netarkivet.archive.webinterface.Constants.GET_INFO_COMMAND,
                new String[] { filename1 });
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.GET_INFO_COMMAND,
                new String[] { filename1 });
        args.put(dk.netarkivet.archive.webinterface.Constants.BITARCHIVE_NAME_PARAM,
                 new String[]{Location.get(ba1).getName()});
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.BITARCHIVE_NAME_PARAM,
                 new String[]{Location.get(ba1).getName()});
        request.setupGetParameterMap(args);
        request.setupGetParameterNames(new Vector(args.keySet()).elements());
        Map<String, FilePreservationState> status =
                BitpreserveFileState.processMissingRequest(getDummyPageContext(
                        defaultLocale, request),
                        new StringBuilder());
        assertEquals("Should have one call to reestablish",
                1, mockabp.getCallCount(ADD_METHOD));
        assertEquals("Should have one call to getFilePreservationStatus",
                1, mockabp.getCallCount(GET_INFO_METHOD));
        assertEquals("Should have one info element (with mock results)",
                null, status.get(filename1));
        // Check that we can call without any params
        mockabp.calls.clear();
        request = new MockHttpServletRequest();
        args.clear();
        args.put(dk.netarkivet.archive.webinterface.Constants.BITARCHIVE_NAME_PARAM,
                 new String[]{Location.get(ba1).getName()});
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.BITARCHIVE_NAME_PARAM,
                 new String[]{Location.get(ba1).getName()});
        request.setupGetParameterMap(args);
        status = BitpreserveFileState.processMissingRequest(
                getDummyPageContext(defaultLocale, request), new StringBuilder()
        );
        assertEquals("Should have no call to restablish",
                0, mockabp.getCallCount(ADD_METHOD));
        assertEquals("Should have no call to getFilePreservationStatus",
                0, mockabp.getCallCount(GET_INFO_METHOD));
        assertEquals("Should have no status",
                0, status.size());

        // Check that we can handle more than one call to each and that the
        // args are correct.
        mockabp.calls.clear();
        request = new MockHttpServletRequest();
        args.clear();
        args.put(dk.netarkivet.archive.webinterface.Constants.BITARCHIVE_NAME_PARAM,
                 new String[]{Location.get(ba2).getName()});
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.BITARCHIVE_NAME_PARAM,
                 new String[]{Location.get(ba2).getName()});
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.ADD_COMMAND,
                new String[] {
                    ba2 + Constants.STRING_FILENAME_SEPARATOR + filename1,
                    ba2 + Constants.STRING_FILENAME_SEPARATOR + filename1
                });
        args.put(dk.netarkivet.archive.webinterface.Constants.ADD_COMMAND,
                new String[] {
                    ba2 + Constants.STRING_FILENAME_SEPARATOR + filename1,
                    ba2 + Constants.STRING_FILENAME_SEPARATOR + filename1
                });
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.GET_INFO_COMMAND,
                new String[] { filename1, filename2, filename1 });
        args.put(dk.netarkivet.archive.webinterface.Constants.GET_INFO_COMMAND,
                new String[] { filename1, filename2, filename1 });
        request.setupGetParameterMap(args);
        status = BitpreserveFileState.processMissingRequest(getDummyPageContext(
                defaultLocale, request),
                new StringBuilder()
        );
        assertEquals("Should have two calls to restablish",
                2, mockabp.getCallCount(ADD_METHOD));
        assertEquals("Should have three calls to getFilePreservationStatus",
                3, mockabp.getCallCount(GET_INFO_METHOD));
        assertEquals("Should have two info elements",
                2, status.size());
        assertEquals("Should have info for filename1",
                null, status.get(filename1));
        assertEquals("Should have info for filename2",
                null, status.get(filename2));
        CollectionAsserts.assertIteratorEquals("Should have the args given add",
                Arrays.asList(new String[] { filename1 + "," + ba2,
                                             filename1 + "," + ba2}).iterator(),
                mockabp.calls.get(ADD_METHOD).iterator());
        CollectionAsserts.assertIteratorEquals("Should have the args given info",
                Arrays.asList(new String[] {
                    filename1, filename2, filename1 }).iterator(),
                mockabp.calls.get(GET_INFO_METHOD).iterator());
    }

    private PageContext getDummyPageContext(final Locale l,
                                            final ServletRequest request) {
        return new PageContext() {
            public void initialize(Servlet servlet, ServletRequest servletRequest,
                                   ServletResponse servletResponse,
                                   String string,
                                   boolean b, int i, boolean b1)
                    throws IOException,
                    IllegalStateException, IllegalArgumentException {
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
                        return l;
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
                return null;  //To change body of implemented methods use File | Settings | File Templates.
            }

            public void forward(String string)
                    throws ServletException, IOException {
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
                return null;  //To change body of implemented methods use File | Settings | File Templates.
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
        };
    }



    /** A placeholder for ActiveBitPreservation that's easy to ask questions
     * of.
     */
    class MockFileBasedActiveBitPreservation extends
                                             FileBasedActiveBitPreservation {
        public Map<String, List<String>> calls
                = new HashMap<String, List<String>>();

        public MockFileBasedActiveBitPreservation() throws NoSuchFieldException,
                IllegalAccessException {
            Field f = ReflectUtils.getPrivateField(
                    FileBasedActiveBitPreservation.class,
                    "instance");
            f.set(null, this);
        }
        public int getCallCount(String methodname) {
            if (calls.containsKey(methodname)) {
                return calls.get(methodname).size();
            } else {
                return 0;
            }
        }
        public void addCall(Map<String, List<String>> map,
                            String key, String args) {
            List<String> oldValue = map.get(key);
            if (oldValue == null) {
                oldValue = new ArrayList<String>();
            }
            oldValue.add(args);
            map.put(key, oldValue);
        }

        public void uploadMissingFiles(Location location, String... filename) {
            addCall(calls, ADD_METHOD,
                    filename[0] + "," + location.getName());
        }

        public FilePreservationState
        getFilePreservationState(String filename) {
            addCall(calls, GET_INFO_METHOD, filename);
            return null;
        }

        public void cleanup() {
            JMSConnectionTestMQ.useJMSConnectionTestMQ();
            super.cleanup();
        }
    }
}