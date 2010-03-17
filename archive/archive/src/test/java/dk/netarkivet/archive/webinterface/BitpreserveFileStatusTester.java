/*$Id$
* $Revision$
* $Date$
* $Author$
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
package dk.netarkivet.archive.webinterface;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.servlet.jsp.JspWriter;

import junit.framework.TestCase;

import com.mockobjects.servlet.MockHttpServletRequest;
import com.mockobjects.servlet.MockJspWriter;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.arcrepository.bitpreservation.Constants;
import dk.netarkivet.archive.arcrepository.bitpreservation.FileBasedActiveBitPreservation;
import dk.netarkivet.archive.arcrepository.bitpreservation.PreservationState;
import dk.netarkivet.archive.arcrepositoryadmin.AdminData;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.webinterface.WebinterfaceTestCase;
import dk.netarkivet.testutils.CollectionAsserts;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/** Unittest for the class dk.netarkivet.archive.webinterface.BitpreserveFileState. */
public class BitpreserveFileStatusTester extends TestCase {
    private static final String GET_INFO_METHOD = "getFilePreservationStatus";
    private static final String ADD_METHOD = "reestablishMissingFile";
    private static final String ADD_COMMAND
            = dk.netarkivet.archive.webinterface.Constants.ADD_COMMAND;
    private static final String GET_INFO_COMMAND
            = dk.netarkivet.archive.webinterface.Constants.GET_INFO_COMMAND;
    private static final String BITARCHIVE_NAME_PARAM
            = dk.netarkivet.archive.webinterface.Constants.BITARCHIVE_NAME_PARAM;
    
    private static final String FIND_MISSING_FILES = "find-missing-files";
    private static final String FIND_CHECKSUM = "checksum";
    private static final String NUM_MISSING_FILES = "getNumberOfMissingFiles";
    private static final String NUM_FILES = "getNumberOfFiles";
    private static final String DATE_MISSING_FILES = "getDateForMissingFiles";
    private static final String NUM_CHANGED_FILES = "getNumberOfChangedFiles";
    private static final String DATE_CHANGED_FILES = "getDateForChangedFiles";

    ReloadSettings rs = new ReloadSettings();
    MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);

    public void setUp() throws Exception {
        rs.setUp();
        mtf.setUp();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        
        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN, 
                TestInfo.WORKING_DIR.getAbsolutePath());


        if (!Replica.isKnownReplicaId("TWO") || !Replica.isKnownReplicaId(
                "ONE")) {
            List<String> knownIds = new ArrayList<String>();

            fail("These tests assume, that ONE and TWO are known replica ids. Only known replicas are: "
                 + StringUtils.conjoin(", ",
                                       knownIds.toArray(
                                               Replica.getKnownIds())));
        }
        super.setUp();
    }

    public void tearDown() throws Exception {
        AdminData.getUpdateableInstance().close();
        mtf.tearDown();
        rs.tearDown();
        super.tearDown();
    }

    public void testProcessMissingRequest() throws Exception {

        Settings.set(ArchiveSettings.DIR_ARCREPOSITORY_BITPRESERVATION,
                     TestInfo.WORKING_DIR.getAbsolutePath());
        Settings.set(ArchiveSettings.DIRS_ARCREPOSITORY_ADMIN,
                     TestInfo.WORKING_DIR.getAbsolutePath());

        // Ensure that a admin data exists before we start.
        AdminData.getUpdateableInstance();

        MockFileBasedActiveBitPreservation mockabp
                = new MockFileBasedActiveBitPreservation();
        MockHttpServletRequest request = new MockHttpServletRequest();
        String replicaID1 = "ONE";
        String replicaID2 = "TWO";
        String filename1 = "foo";
        String filename2 = "bar";
        Locale defaultLocale = new Locale("da");

        // First test a working set of params
        Map<String, String[]> args = new HashMap<String, String[]>();
        args.put(ADD_COMMAND,
                 new String[]{
                         Replica.getReplicaFromId(replicaID1).getName()
                         + Constants.STRING_FILENAME_SEPARATOR + filename1
                 });
        request.setupAddParameter(ADD_COMMAND,
                                  new String[]{
                                          Replica.getReplicaFromId(
                                                  replicaID1).getName()
                                          + Constants.STRING_FILENAME_SEPARATOR
                                          + filename1
                                  });
        args.put(GET_INFO_COMMAND, new String[]{filename1});
        request.setupAddParameter(GET_INFO_COMMAND,
                                  new String[]{filename1});
        args.put(BITARCHIVE_NAME_PARAM,
                 new String[]{Replica.getReplicaFromId(replicaID1).getName()});
        request.setupAddParameter(BITARCHIVE_NAME_PARAM,
                                  new String[]{Replica.getReplicaFromId(
                                          replicaID1).getName()});
        request.setupGetParameterMap(args);
        request.setupGetParameterNames(
                new Vector<String>(args.keySet()).elements());
        Map<String, PreservationState> status =
                BitpreserveFileState.processMissingRequest(
                        WebinterfaceTestCase.getDummyPageContext(
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
        args.put(BITARCHIVE_NAME_PARAM,
                 new String[]{Replica.getReplicaFromId(replicaID1).getName()});
        request.setupAddParameter(BITARCHIVE_NAME_PARAM,
                                  new String[]{Replica.getReplicaFromId(
                                          replicaID1).getName()});
        request.setupGetParameterMap(args);
        status = BitpreserveFileState.processMissingRequest(
                WebinterfaceTestCase.getDummyPageContext(defaultLocale, request), new StringBuilder()
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
        args.put(BITARCHIVE_NAME_PARAM,
                 new String[]{Replica.getReplicaFromId(replicaID2).getName()});
        request.setupAddParameter(BITARCHIVE_NAME_PARAM,
                                  new String[]{Replica.getReplicaFromId(
                                          replicaID2).getName()});
        request.setupAddParameter(ADD_COMMAND,
                                  new String[]{
                                          Replica.getReplicaFromId(
                                                  replicaID2).getName()
                                          + Constants.STRING_FILENAME_SEPARATOR
                                          + filename1,
                                          Replica.getReplicaFromId(
                                                  replicaID2).getName()
                                          + Constants.STRING_FILENAME_SEPARATOR
                                          + filename1
                                  });
        args.put(ADD_COMMAND,
                 new String[]{
                         Replica.getReplicaFromId(replicaID2).getName()
                         + Constants.STRING_FILENAME_SEPARATOR + filename1,
                         Replica.getReplicaFromId(replicaID2).getName()
                         + Constants.STRING_FILENAME_SEPARATOR + filename1
                 });
        request.setupAddParameter(GET_INFO_COMMAND,
                                  new String[]{filename1, filename2,
                                               filename1});
        args.put(GET_INFO_COMMAND,
                 new String[]{filename1, filename2, filename1});
        request.setupGetParameterMap(args);
        status = BitpreserveFileState.processMissingRequest(
                WebinterfaceTestCase.getDummyPageContext(defaultLocale, request),
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


//        Iterator<String> it = mockabp.calls.get(ADD_METHOD).iterator();
//        while (it.hasNext()) {
//            System.out.println(it.next());
//        }

        CollectionAsserts.assertIteratorEquals("Should have the args given add",
                                               Arrays.asList(new String[]{
                                                       filename1 + ","
                                                       + replicaID2,
                                                       filename1 + ","
                                                       + replicaID2}).iterator(),
                                               mockabp.calls.get(
                                                       ADD_METHOD).iterator());

        CollectionAsserts.assertIteratorEquals(
                "Should have the args given info",
                Arrays.asList(new String[]{
                        filename1, filename2, filename1}).iterator(),
                mockabp.calls.get(GET_INFO_METHOD).iterator());
    }
    
    public void testUpdateRequest() throws NoSuchFieldException, IllegalAccessException {
        MockFileBasedActiveBitPreservation mockabp
                = new MockFileBasedActiveBitPreservation();
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        Locale l = new Locale("da");
        mockabp.calls.clear();
        
        // Setup to neither run checksum nor find-missing-files.
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.BITARCHIVE_NAME_PARAM,
                new String[]{Replica.getReplicaFromId("ONE").getName()});
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.FIND_MISSING_FILES_PARAM,
                (String[]) null);
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.CHECKSUM_PARAM,
                (String[]) null);
        
        BitpreserveFileState.processUpdateRequest(WebinterfaceTestCase.getDummyPageContext(l, request));
        
        assertFalse("No calls to Find Missing Files expected", 
                mockabp.calls.containsKey(FIND_MISSING_FILES));
        assertFalse("No calls to Find Checksum expected", 
                mockabp.calls.containsKey(FIND_CHECKSUM));
        mockabp.calls.clear();
        
        // Setup to run find-missing-files
        request = new MockHttpServletRequest();
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.BITARCHIVE_NAME_PARAM,
                new String[]{Replica.getReplicaFromId("ONE").getName()});
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.FIND_MISSING_FILES_PARAM,
                new String[]{"1"});
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.CHECKSUM_PARAM,
                (String[]) null);
        
        BitpreserveFileState.processUpdateRequest(WebinterfaceTestCase.getDummyPageContext(l, request));
        
        assertTrue("One calls to Find Missing Files expected", 
                mockabp.calls.containsKey(FIND_MISSING_FILES));
        assertFalse("No calls to Find Checksum expected", 
                mockabp.calls.containsKey(FIND_CHECKSUM));
        mockabp.calls.clear();

        // Setup to run find-checksum
        request = new MockHttpServletRequest();
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.BITARCHIVE_NAME_PARAM,
                new String[]{Replica.getReplicaFromId("ONE").getName()});
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.FIND_MISSING_FILES_PARAM,
                (String[]) null);
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.CHECKSUM_PARAM,
                new String[]{"1"});
        
        BitpreserveFileState.processUpdateRequest(WebinterfaceTestCase.getDummyPageContext(l, request));
        
        assertFalse("No calls to Find Missing Files expected", 
                mockabp.calls.containsKey(FIND_MISSING_FILES));
        assertTrue("One calls to Find Checksum expected", 
                mockabp.calls.containsKey(FIND_CHECKSUM));
        mockabp.calls.clear();

        // Setup to run find-missing-files and find-checksum
        request = new MockHttpServletRequest();
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.BITARCHIVE_NAME_PARAM,
                new String[]{Replica.getReplicaFromId("ONE").getName()});
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.FIND_MISSING_FILES_PARAM,
                new String[]{"1"});
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.CHECKSUM_PARAM,
                new String[]{"1"});
        
        BitpreserveFileState.processUpdateRequest(
                WebinterfaceTestCase.getDummyPageContext(l, request));
        
        assertTrue("One calls to Find Missing Files expected", 
                mockabp.calls.containsKey(FIND_MISSING_FILES));
        assertTrue("One calls to Find Checksum expected", 
                mockabp.calls.containsKey(FIND_CHECKSUM));
        mockabp.calls.clear();
    }

    /*
    public void testProcessChecksumRequest() throws NoSuchFieldException, IllegalAccessException {
        MockFileBasedActiveBitPreservation mockabp
                = new MockFileBasedActiveBitPreservation();
        MockHttpServletRequest request = new MockHttpServletRequest();
        
        Locale l = new Locale("da");
        mockabp.calls.clear();

        // Setup to neither run checksum nor find-missing-files.
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.BITARCHIVE_NAME_PARAM,
                new String[]{Replica.getReplicaFromId("ONE").getName()});
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.FILENAME_PARAM,
                (String[]) null);
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.FIX_ADMIN_CHECKSUM_PARAM,
                (String[]) null);
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.CREDENTIALS_PARAM,
                (String[]) null);
        request.setupAddParameter(dk.netarkivet.archive.webinterface.Constants.CHECKSUM_PARAM,
                (String[]) null);

        StringBuilder res = new StringBuilder();
        PreservationState ps = BitpreserveFileState.processChecksumRequest(res, getDummyPageContext(l, request));
        assertNull("Only null arguments, should give a null result.", ps);
        
    }
    */
    
    public void testMakeCheckbox() throws NoSuchFieldException, IllegalAccessException {
        String res = BitpreserveFileState.makeCheckbox("TEST-COMMAND", "TEST-ARG1", "TEST-ARG2");

        assertTrue("Should contain the TEST-COMMAND", res.contains("name=\"TEST-COMMAND\""));
        assertTrue("Should contain TEST-ARG1 and TEST-ARG2 seperated by " 
                + dk.netarkivet.archive.webinterface.Constants.STRING_FILENAME_SEPARATOR
                + " but the result was " + res, 
                res.contains("value=\"" + "TEST-ARG1" 
                        + dk.netarkivet.archive.webinterface.Constants.STRING_FILENAME_SEPARATOR 
                        + "TEST-ARG2" + "\""));
    }
    
    public void testPrintMissingFileStateForReplica() throws IOException, NoSuchFieldException, IllegalAccessException {
        MockFileBasedActiveBitPreservation fbabp = 
            new MockFileBasedActiveBitPreservation();
        JspWriter fw = new MockJspWriter();
        BitpreserveFileState.printMissingFileStateForReplica(fw, 
                Replica.getReplicaFromId("ONE"), new Locale("da"));
        BitpreserveFileState.printMissingFileStateForReplica(fw, 
                Replica.getReplicaFromId("TWO"), new Locale("da"));
        
        assertTrue("Number of missing files should be called by a bitarchive replica", 
                fbabp.calls.get(NUM_MISSING_FILES).contains(Replica.getReplicaFromId("ONE").getType().name()));
        assertFalse("Number of missing files should not be called by the Checksum replica.",
                fbabp.calls.get(NUM_MISSING_FILES).contains(Replica.getReplicaFromId("THREE").getType().name()));
        assertEquals("Number of missing files should be put into the array 4 times", 
                4, fbabp.calls.get(NUM_MISSING_FILES).size());
        
        assertTrue("Number of files should be called by replica ONE", 
                fbabp.calls.get(NUM_FILES).contains("ONE"));
        assertFalse("Number of file should not have been called by replica THREE", 
                fbabp.calls.get(NUM_FILES).contains("THREE"));
        
        assertTrue("Date for missing files should be called by replica TWO", 
                fbabp.calls.get(DATE_MISSING_FILES).contains("TWO"));
        assertFalse("Date for missing files should not have been called by replica THREE", 
                fbabp.calls.get(DATE_MISSING_FILES).contains("THREE"));
        
        fbabp.calls.clear();
    }
    
    public void testPrintChecksumErrorStateForReplica() throws IOException, NoSuchFieldException, IllegalAccessException {
        MockFileBasedActiveBitPreservation fbabp =
            new MockFileBasedActiveBitPreservation();
        JspWriter jw = new MockJspWriter();
        BitpreserveFileState.printChecksumErrorStateForReplica(jw, 
                Replica.getReplicaFromId("ONE"), new Locale("da"));
        
        assertTrue("Number of changed files should be called by a bitarchive replica", 
                fbabp.calls.get(NUM_CHANGED_FILES).contains("ONE"));
        assertFalse("Number of changed files should not be called by the Checksum replica.",
                fbabp.calls.get(NUM_CHANGED_FILES).contains(Replica.getReplicaFromId("THREE").getType().name()));
        
        assertTrue("Date for changed files should be called by replica ONE", 
                fbabp.calls.get(DATE_CHANGED_FILES).contains("ONE"));
        assertFalse("Date for changed files should not have been called by replica THREE", 
                fbabp.calls.get(DATE_CHANGED_FILES).contains("THREE"));

        
        fbabp.calls.clear();
    }

    /** A placeholder for ActiveBitPreservation that's easy to ask questions of. */
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

        public void uploadMissingFiles(Replica replica, String... filename) {
            addCall(calls, ADD_METHOD,
                    filename[0] + "," + replica.getId());
        }

        public PreservationState
        getPreservationState(String filename) {
            addCall(calls, GET_INFO_METHOD, filename);
            return null;
        }

        public Map<String, PreservationState> getPreservationStateMap(
                String... filenames) {
            Map<String, PreservationState> result =
                    new HashMap<String, PreservationState>();
            for (String filename : filenames) {
                addCall(calls, GET_INFO_METHOD, filename);
                result.put(filename, null);
            }
            return result;

        }

        public void findMissingFiles(Replica replica) {
            addCall(calls, FIND_MISSING_FILES, replica.getId());
        }
        
        public void findChangedFiles(Replica replica) {
            addCall(calls, FIND_CHECKSUM, replica.getId());
        }
        
        public long getNumberOfMissingFiles(Replica replica) {
            addCall(calls, NUM_MISSING_FILES, replica.getType().name());
            
            return 1L;
        }
        
        public long getNumberOfFiles(Replica replica) {
            addCall(calls, NUM_FILES, replica.getId());
            
            return 1L;
        }
        
        public Date getDateForMissingFiles(Replica replica) {
            addCall(calls, DATE_MISSING_FILES, replica.getId());
            
            return new Date(0L);
        }
        
        public long getNumberOfChangedFiles(Replica replica) {
            addCall(calls, NUM_CHANGED_FILES, replica.getId());
            
            return 1L;
        }
        
        public Date getDateForChangedFiles(Replica replica) {
            addCall(calls, DATE_CHANGED_FILES, replica.getId());
            
            return new Date(1L);
        }


        public void cleanup() {
            JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
            super.cleanup();
        }
    }
}