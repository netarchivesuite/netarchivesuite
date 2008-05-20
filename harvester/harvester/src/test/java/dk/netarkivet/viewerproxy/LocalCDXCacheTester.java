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
package dk.netarkivet.viewerproxy;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.DigestOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;

import dk.netarkivet.archive.arcrepository.distribute.JMSArcRepositoryClient;
import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.distribute.TestRemoteFile;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClient;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.MD5;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.common.utils.arc.FileBatchJob;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.LogUtils;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestFileUtils;
import dk.netarkivet.testutils.preconfigured.UseTestRemoteFile;

/**
 * Unit tests for the LocalCDXCache class.
 */
public class LocalCDXCacheTester extends TestCase {
    // Whether the batch call must throw exception if called
    private boolean batchMustDie;
    // How many milliseconds the batch process should pause (for testing
    // concurrency.
    private int batchPauseMilliseconds;
    // How many calls have been made to batch
    private int batchCounter;
    UseTestRemoteFile utrf = new UseTestRemoteFile();

    public LocalCDXCacheTester(String s) {
        super(s);
    }

    public void setUp() {
        JMSConnectionTestMQ.useJMSConnectionTestMQ();
        TestFileUtils.copyDirectoryNonCVS(
                TestInfo.ORIGINALS_DIR, TestInfo.WORKING_DIR);
        Settings.set(Settings.VIEWERPROXY_DIR, 
                TestInfo.WORKING_DIR.getAbsolutePath());
        utrf.setUp();
        batchMustDie = false;
        batchPauseMilliseconds = 0;
        batchCounter = 0;
    }

    public void tearDown() throws NoSuchFieldException, IllegalAccessException {
        ArcRepositoryClientFactory.getViewerInstance().close();
        utrf.tearDown();
        FileUtils.removeRecursively(TestInfo.WORKING_DIR);
        Field dirField = ReflectUtils.getPrivateField(
                LocalCDXCache.class, "CACHE_DIR");
        FileUtils.removeRecursively((File) dirField.get(null));
        JMSConnectionTestMQ.clearTestQueues();
        Settings.reload();
    }

    public void testGetIndexFile() throws Exception {
        LocalCDXCache cache = new LocalCDXCache(
                ArcRepositoryClientFactory.getViewerInstance());
        Method getIndexFile = ReflectUtils.getPrivateMethod(
                LocalCDXCache.class, "getIndexFile", Set.class);
        // Test that we get a simple filename for a simple ID.
        File f = (File) getIndexFile.invoke(cache, set(1L));
        assertNotNull("Should get a file object back", f);
        assertEquals("Should have the expected filename",
                     "job-1-index.cdx", f.getName());

        f = (File) getIndexFile.invoke(cache, set(3L, 2L));
        assertEquals("Should have the expected filename for two numbers",
                     "job-2-3-index.cdx", f.getName());

        f = (File) getIndexFile.invoke(cache, set(2L, 3L));
        assertEquals("Should have the expected filename for two numbers,"
                     + " regardless of order",
                     "job-2-3-index.cdx", f.getName());

        f = (File) getIndexFile.invoke(cache, set(5L, 6L, 7L, 8L));
        assertEquals("Should not start using checksum at 4 jobs",
                     "job-5-6-7-8-index.cdx", f.getName());

        f = (File) getIndexFile.invoke(cache, set(5L, 6L, 7L, 8L, 9L));
        assertEquals("Should start using checksum when ID list is too long",
                     "job-5-6-7-8-d8faf89ca4ab4c58e81d21092e9447d9-index.cdx", f.getName());

        f = (File) getIndexFile.invoke(cache, set(9L, 7L, 6L, 8L, 5L));
        assertEquals("Checksum should be independent of order",
                     "job-5-6-7-8-d8faf89ca4ab4c58e81d21092e9447d9-index.cdx", f.getName());

        f = (File) getIndexFile.invoke(cache, set(5L, 6L, 7L, 8L, 9L, 10L));
        assertEquals("Checksum should change with extra jobids",
                     "job-5-6-7-8-fb502280d5969d4d22788d3fe9da6d21-index.cdx", f.getName());

        f = (File) getIndexFile.invoke(cache, set(5L, 6L, 7L, 8L, 10L, 9L));
        assertEquals("Checksum should be independent of order of last ids",
                     "job-5-6-7-8-fb502280d5969d4d22788d3fe9da6d21-index.cdx", f.getName());
    }

    public void testGetJobIndex() throws Exception {
        final LocalCDXCache cache = setupCache();

        // Check that an index file with correct content is created
        List<String> expectedData = FileUtils.readListFromFile(
                new File(TestInfo.WORKING_DIR, "2-metadata-1.arc-contents"));
        Collections.sort(expectedData);
        File realData = cache.getIndex(set(2L)).getIndex();
        assertEquals("Returned data should be the same as expected (sorted) data",
                     listToString(expectedData),
                     FileUtils.readFile(realData));

        expectedData.addAll(FileUtils.readListFromFile(
                new File(TestInfo.WORKING_DIR, "4-metadata-1.arc-contents")));
        Collections.sort(expectedData);
        realData = cache.getIndex(set(2L, 4L)).getIndex();
        assertEquals("Returned data should be the same as expected (sorted) data"
                     + " with multiple files",
                     listToString(expectedData), FileUtils.readFile(realData));

        expectedData.addAll(FileUtils.readListFromFile(
                new File(TestInfo.WORKING_DIR, "70-metadata-1.arc-contents")));
        Collections.sort(expectedData);
        realData = cache.getIndex(set(2L, 1L, 4L, 70L, 3L)).getIndex();
        assertEquals("Returned data should be the same as expected (sorted)"
                     + " data, even when some jobs are missing",
                     listToString(expectedData), FileUtils.readFile(realData));


        // Check that error scenarios are handled correctly
        try {
            cache.getIndex(null);
            fail("Should die on null argument");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            Set<Long> jobIDs = Collections.emptySet();
            cache.getIndex(jobIDs);
            fail("Should die on null argument");
        } catch (ArgumentNotValid e) {
            // expected
        }

        File f = cache.getIndex(set(-1L)).getIndex();
        assertTrue("A file should result from indexing just a non-existing job",
                   f.exists());
        assertEquals("Empty file should result from indexing just a non-existing job",
                     0, f.length());

        realData = cache.getIndex(set(-1L, 2L)).getIndex();
        expectedData = FileUtils.readListFromFile(
                new File(TestInfo.WORKING_DIR, "2-metadata-1.arc-contents"));
        Collections.sort(expectedData);
        assertEquals("Errors should not prevent working data from being returned",
                     listToString(expectedData),
                     FileUtils.readFile(realData));

        // Check that we cache the files
        realData = cache.getIndex(set(2L, 4L)).getIndex();
        // Kill a cache file and see that we get that content
        OutputStream out = new FileOutputStream(realData);
        out.write("foo".getBytes());
        out.close();
        realData = cache.getIndex(set(2L, 4L)).getIndex();
        assertEquals("Should get smashed cache file",
                     "foo", FileUtils.readFile(realData));

        // Check that batch isn't called unnecessarily
        realData = cache.getIndex(set(2L, 70L)).getIndex();
        batchMustDie = true;
        realData = cache.getIndex(set(2L, 70L)).getIndex();
        batchMustDie = false;

        // Check that async calling works
        // First make sure to destroy the file.
        realData = cache.getIndex(set(4L, 70L)).getIndex();
        FileUtils.remove(realData);
        batchPauseMilliseconds = 10;
        batchCounter = 0;
        final int NUM_THREADS = 100;
        List<Thread> threads = new ArrayList<Thread>(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            Thread thread = new Thread() {
                public void run() {
                    cache.getIndex(set(4L, 70L));
                }
            };
            thread.start();
            threads.add(thread);
        }
        boolean anyAlive;
        do {
            anyAlive = false;
            for (int i = 0; i < NUM_THREADS; i++) {
                if (threads.get(i).isAlive()) {
                    anyAlive = true;
                }
            }
            Thread.sleep(10);
        } while (anyAlive);

        assertEquals("Should have exactly one call to batch()",
                     1, batchCounter);
        expectedData = FileUtils.readListFromFile(
                new File(TestInfo.WORKING_DIR, "4-metadata-1.arc-contents"));
        expectedData.addAll(FileUtils.readListFromFile(
                new File(TestInfo.WORKING_DIR, "70-metadata-1.arc-contents")));
        Collections.sort(expectedData);
        assertEquals("Should have the correct output",
                     listToString(expectedData), FileUtils.readFile(realData));
        // Can we do better than just making a bunch of threads getting the
        // same file?
    }


    public void testRetrieveIndex() throws Exception {
        LocalCDXCache cache = setupCache();
        Method retrieveIndex = ReflectUtils.getPrivateMethod(
                LocalCDXCache.class,
                "retrieveIndex", Set.class, OutputStream.class);

        String cdxData = FileUtils.readFile(
                new File(TestInfo.WORKING_DIR, "2-metadata-1.arc-contents"));

        // Check normal operation
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        DigestOutputStream md5 = new DigestOutputStream(
                sink, MD5.getMessageDigestInstance());
        md5.getMessageDigest().reset();
        retrieveIndex.invoke(cache, set(2L), md5);
        assertEquals("Normal file should show right contents",
                     cdxData, sink.toString());

        List<String> cdxData2 = FileUtils.readListFromFile(
                new File(TestInfo.WORKING_DIR, "4-metadata-1.arc-contents"));
        cdxData2.addAll(FileUtils.readListFromFile(
                new File(TestInfo.WORKING_DIR, "70-metadata-1.arc-contents")));
        Collections.sort(cdxData2);

        sink.reset();

        retrieveIndex.invoke(cache, set(4L, 70L), sink);
        List<String> cdxRead = Arrays.asList(sink.toString().split("\n"));
        Collections.sort(cdxRead);
        assertEquals("Contents should be the same (but in no specific order)",
                     listToString(cdxData2),
                     listToString(cdxRead));

        // Check cleanup
        assertFalse("Temporary file should be removed",
                    new File(new File(TestInfo.WORKING_DIR, "tmp"),
                             "2-metadata-1.arc").exists());

        // Check error scenarios
        sink.reset();
        retrieveIndex.invoke(cache, set(1L, 70L, 4L), sink);
        cdxRead = Arrays.asList(sink.toString().split("\n"));
        Collections.sort(cdxRead);
        assertEquals("Missing files should be not stop the output",
                     listToString(cdxData2),
                     listToString(cdxRead));

        LogUtils.flushLogs(LocalCDXCache.class.getName());
        FileAsserts.assertFileContains(
                "Should have a message about failed files in the log",
                "INFO: Only found 2 files when asking for jobs [4, 70, 1]",
                TestInfo.LOG_FILE);

        sink.reset();
        // Force a null RemoteFile by blocking tmpfile
        File file =
                new File(new File(TestInfo.WORKING_DIR, "tmp"), "batchOutput");
        file.delete();
        file.mkdir();
        retrieveIndex.invoke(cache, set(1L, 70L, 4L), sink);
    }

    /** Set up a CDXCache with a fake arcrepositoryclient.
     *
     * @return Cache object that reads files from WORKING_DIR, placing working
     * copies in the tmp dir so they can be removed without disturbing the next
     * call.
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private LocalCDXCache setupCache() throws NoSuchFieldException,
                                              IllegalAccessException {
        LocalCDXCache cache = new LocalCDXCache(
                ArcRepositoryClientFactory.getViewerInstance());
        final File tmpDir = new File(TestInfo.WORKING_DIR, "tmp");
        FileUtils.createDir(tmpDir);

        // Set up a dummy client that serves files out of the workingdir.
        ArcRepositoryClient dummyARC = new JMSArcRepositoryClient() {
            public BatchStatus batch(FileBatchJob job, String locationName) {
                batchCounter++;
                if (batchMustDie) {
                    throw new IOFailure("Committing suicide as ordered, SIR!");
                }
                if (batchPauseMilliseconds > 0) {
                    try {
                        Thread.sleep(batchPauseMilliseconds);
                    } catch (InterruptedException e) {
                        // Don't care.
                    }
                }

                File f = new File(tmpDir, "batchOutput");
                OutputStream os = null;
                try {
                    os = new FileOutputStream(f);
                } catch (IOException e) {
                    return new BatchStatus(locationName,
                                           new ArrayList<File>(), 0, null);
                }
                File[] files = TestInfo.WORKING_DIR.listFiles(
                        new FilenameFilter() {
                            public boolean accept(File dir, String name) {
                                return name.endsWith(".arc");
                            }
                        }
                );
                job.initialize(os);
                int processed = 0;
                List<File> failures = new ArrayList<File>();
                for (File f1 : files) {
                    if (job.getFilenamePattern().matcher(f1.getName()).matches()) {
                        processed++;
                        if (!job.processFile(f1, os)) {
                            failures.add(f1);
                        }
                    }
                }
                job.finish(os);
                try {
                    os.close();
                } catch (IOException e) {
                    fail("Error in close: " + e);
                }
                return new BatchStatus(locationName,
                                       failures, processed, 
                                       new TestRemoteFile(f,
                                               batchMustDie,
                                               batchMustDie,
                                               batchMustDie));
            }
        };
        Field arcField = ReflectUtils.getPrivateField(LocalCDXCache.class,
                "arcRepos");
        arcField.set(cache, dummyARC);
        return cache;
    }

    public static <T> Set<T> set(T... objects) {
        return new HashSet<T>(Arrays.asList(objects));
    }

    /** Turn a list of strings into a single string, newline-terminating
     * each string.
     *
     * @param objects a list of strings
     * @return Newline separated list
     */
    public static String listToString(List<String> objects) {
        return StringUtils.conjoin("\n", objects) + "\n";
    }
}
