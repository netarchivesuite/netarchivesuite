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
package dk.netarkivet.archive.indexserver.distribute;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.indexserver.RequestType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.preconfigured.MockupIndexServer;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.MoveTestFiles;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

public class IndexRequestClientTester extends TestCase {
    private static final Set<Long> JOB_SET = new HashSet<Long>(Arrays.asList(new Long[]{2L,3L,5L,7L,11L}));

    private UseTestRemoteFile ulrf = new UseTestRemoteFile();
    private PreventSystemExit pse = new PreventSystemExit();
    private PreserveStdStreams pss = new PreserveStdStreams();
    private MoveTestFiles mtf = new MoveTestFiles(TestInfo.ORIGINALS_DIR,
            TestInfo.WORKING_DIR);
    private MockupJMS mjms = new MockupJMS();
    private MockupIndexServer mis;
    public IndexRequestClientTester() {
    }
    public void setUp(){
        Settings.set(Settings.CACHE_DIR,
                new File(TestInfo.WORKING_DIR, "cache").getAbsolutePath());
        Settings.set(Settings.DIR_COMMONTEMPDIR,
                new File(TestInfo.WORKING_DIR, "commontempdir").getAbsolutePath());

        ulrf.setUp();
        mjms.setUp();
        mtf.setUp();
        pss.setUp();
        pse.setUp();
        mis = new MockupIndexServer(mtf.working(TestInfo.DUMMY_CACHEDIR));
        mis.setUp();
    }
    public void tearDown() throws NoSuchFieldException, IllegalAccessException {
        mis.tearDown();
        pse.tearDown();
        pss.tearDown();
        mtf.tearDown();
        mjms.tearDown();
        ulrf.tearDown();
        Field f = ReflectUtils.getPrivateField(IndexRequestClient.class, "clients");
        f.set(null, new EnumMap<RequestType, IndexRequestClient>(RequestType.class));
        f = ReflectUtils.getPrivateField(IndexRequestClient.class, "synchronizer");
        f.set(null, null);
        Settings.reload();
    }
    /**
     * Verify that factory method
     *  - only throws exceptions if given a null value
     *  - returns non-null values.
     */
    public void testGetInstance() {
        for (RequestType t : RequestType.values()) {
            assertNotNull(
                "Request client for " + t + " indexes should be non-null",
                IndexRequestClient.getInstance(t));
        }
        try {
            IndexRequestClient.getInstance(null);
            fail("Factory method should not accept null input");
        } catch (ArgumentNotValid e) {
            //Expected
        }
    }

    /**
     * Verify that the getJobIndex method
     *  - accepts all non-null lists (including an empty one)
     *  - throws exceptions on null
     *  - submits an IndexRequestMessage representing the input
     *  - returns the response on the message to the caller
     *  - throws an exception if response is not OK.
     */
    public void testGetJobIndexFullNonemptySet() throws IOException {
        testNormalDirResponse(IndexRequestClient.getInstance(RequestType.FULL_CRAWL_LOG),
                RequestType.FULL_CRAWL_LOG,JOB_SET);
    }

    public void testGetJobIndexFullEmptySet() throws IOException {
        testNormalDirResponse(IndexRequestClient.getInstance(RequestType.FULL_CRAWL_LOG),
                RequestType.FULL_CRAWL_LOG, Collections.<Long>emptySet());
    }

    public void testGetJobIndexFullFailures() {
        assertFailsOnNull(IndexRequestClient.getInstance(RequestType.FULL_CRAWL_LOG));
        testFailedResponse(IndexRequestClient.getInstance(RequestType.FULL_CRAWL_LOG));
    }

    /**
     * Verify that the getJobIndex method
     *  - accepts all non-null lists (including an empty one)
     *  - throws exceptions on null
     *  - submits an IndexRequestMessage representing the input
     *  - returns the response on the message to the caller
     *  - throws an exception if response is not OK.
     */
    public void testGetJobIndexDedupNonemptySet() throws IOException {
        testNormalDirResponse(IndexRequestClient.getInstance(RequestType.DEDUP_CRAWL_LOG),
                RequestType.DEDUP_CRAWL_LOG,JOB_SET);
    }

    public void testGetJobIndexDedupEmptySet() throws IOException {
        testNormalDirResponse(IndexRequestClient.getInstance(RequestType.DEDUP_CRAWL_LOG),
                RequestType.DEDUP_CRAWL_LOG, Collections.<Long>emptySet());
    }

    public void testGetJobIndexDedupFailures() {
        assertFailsOnNull(IndexRequestClient.getInstance(RequestType.DEDUP_CRAWL_LOG));
        testFailedResponse(IndexRequestClient.getInstance(RequestType.DEDUP_CRAWL_LOG));
    }

    /**
     * Verify that the getJobIndex method
     *  - accepts all non-null lists (including an empty one)
     *  - throws exceptions on null
     *  - submits an IndexRequestMessage representing the input
     *  - returns the response on the message to the caller
     *  - throws an exception if response is not OK.
     */
    public void testGetJobIndexCdxNonemptySet() throws IOException {
        testNormalFileResponse(IndexRequestClient.getInstance(RequestType.CDX),
                RequestType.CDX,JOB_SET);
    }

    public void testGetJobIndexCdxEmptySet() throws IOException {
        testNormalFileResponse(IndexRequestClient.getInstance(RequestType.CDX),
                RequestType.CDX, Collections.<Long>emptySet());
    }

    public void testGetJobIndexCdxFailures() {
        assertFailsOnNull(IndexRequestClient.getInstance(RequestType.CDX));
        testFailedResponse(IndexRequestClient.getInstance(RequestType.CDX));
    }

    private void clearCache(String cacheName) {
        File[] files =
                new File(Settings.get(Settings.CACHE_DIR), cacheName).listFiles();
        if (files != null) {
            for(File f : files) {
                FileUtils.removeRecursively(f);
            }
        }
    }

    private void assertFailsOnNull(IndexRequestClient client) {
        try {
            client.cacheData(null);
            fail("A null list of job ids should not be accepted");
        } catch (ArgumentNotValid e) {
            //Expected
        }
    }
    private void testNormalFileResponse(IndexRequestClient client, RequestType t,
                                    Set<Long> jobSet)
            throws IOException {
        mis.tearDown();

        mis = new MockupIndexServer(TestInfo.DUMMY_CACHEFILE);
        mis.setUp();
        mis.resetMsgList();
        mis.setResponseSuccessfull(true);
        File result = client.getIndex(jobSet).getIndexFile();
        List<IndexRequestMessage> sent = mis.getMsgList();
        assertEquals("Should send exactly one request to the index server",
                1, sent.size());
        IndexRequestMessage msg = sent.get(0);
        assertTrue("Should send a message that is ok",
                msg.isOk());
        assertEquals("Should not change list of jobs",
                jobSet, msg.getRequestedJobs());
        assertEquals("Should send request for the given type of index",
                     t,msg.getRequestType());
        assertEquals("Should return index file unchanged",
                     FileUtils.readFile(mtf.working(TestInfo.DUMMY_INDEX_FILE)),
                     FileUtils.readFile(result));
    }

    private void testNormalDirResponse(IndexRequestClient client, RequestType t,
            Set<Long> jobSet)
            throws IOException {
        mis.tearDown();

        mis = new MockupIndexServer(TestInfo.DUMMY_CACHEDIR);
        mis.setUp();
        mis.resetMsgList();
        mis.setResponseSuccessfull(true);
        //mis.setMultiFile(true);
        Set<Long> jobSet2 = new HashSet<Long>();
        jobSet2.addAll(jobSet);
        jobSet2.add(6L);
        List<IndexRequestMessage> sent = mis.getMsgList();
        File result = client.getIndex(jobSet2).getIndexFile();
        assertEquals("Should send exactly one request to the index server",
                1, sent.size());
        IndexRequestMessage msg = sent.get(0);
        assertTrue("Should not send a message that is not ok", msg.isOk());
        assertEquals("Should not change list of jobs",
                jobSet2, msg.getRequestedJobs());
        assertEquals("Should send request for the given type of index",
                t,msg.getRequestType());
        assertTrue("Result should be directory", result.isDirectory());
        assertEquals("Should return index file unchanged",
                FileUtils.readFile(mtf.working(TestInfo.DUMMY_INDEX_FILE)),
                FileUtils.readFile(new File(result,
                        TestInfo.DUMMY_INDEX_FILE.getName())));

    }
    private void testFailedResponse(IndexRequestClient client) {
        mis.tearDown();

        mis = new MockupIndexServer(TestInfo.DUMMY_CACHEFILE);
        mis.setUp();
        mis.resetMsgList();
        mis.setResponseSuccessfull(false);
        try {
            clearCache(client.getCacheDir().getName());
            client.getIndex(JOB_SET);
            fail("Error in response should cause an exception");
        } catch (IllegalState e) {
            //Expected
        }
    }
}
