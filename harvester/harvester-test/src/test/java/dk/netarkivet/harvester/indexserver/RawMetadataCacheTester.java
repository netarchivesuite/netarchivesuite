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
package dk.netarkivet.harvester.indexserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.arcrepository.TestArcRepositoryClient;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.archive.ArchiveBatchJob;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFile;
import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.preconfigured.MockupJMS;

/**
 * Unittests for the class RawMetadataCache.
 */
public class RawMetadataCacheTester extends CacheTestCase {
    MockupJMS mjms = new MockupJMS();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        mjms.setUp();
    }

    @After
    public void tearDown() throws Exception {
        mjms.tearDown();
        new TestArcRepositoryClient(TestInfo.WORKING_DIR).close();
        super.tearDown();
    }

    @Test
    public void testGetCacheFileName() throws Exception {
        RawMetadataCache cache = new RawMetadataCache("test1", null, null);
        assertEquals("Should get filename for cache files based on prefix" + " and id", "test1-42-cache", cache
                .getCacheFile(42L).getName());
        assertEquals("Should get dirname for cache files based on prefix", "test1", cache.getCacheFile(42L)
                .getParentFile().getName());

        // check that the matchers of the batchjob have the correct settings.
        Field job = ReflectUtils.getPrivateField(RawMetadataCache.class, "job");
        ArchiveBatchJob a = (ArchiveBatchJob) job.get(cache);
        assertTrue("The batchjob should tell which arguments they have.",
                a.toString().contains(" with arguments: URLMatcher = .*, mimeMatcher = .*"));
    }

    @Test
    public void testGetCacheDir() throws Exception {
        RawMetadataCache cache = new RawMetadataCache("test2", null, null);
        assertEquals("Should get dirname for cache files based on prefix", "test2", cache.getCacheDir().getName());
        assertEquals("Should get dirname for cache files in cache dir", "cache", cache.getCacheDir().getAbsoluteFile()
                .getParentFile().getName());
    }

    /**
     * Check that in migrated metadata files, RawMetadataCache updates deduplicate records to reflect migrated values
     * @throws Exception
     */
    @Test
    public void testCacheMigratedMetadata() throws Exception {
        TestArcRepositoryClient tarc = new TestArcRepositoryClient(new File(TestInfo.WORKING_DIR, "arcfiles"));
        Field arcrepfield = ReflectUtils.getPrivateField(RawMetadataCache.class, "arcrep");
        // Try one with just URL pattern.
        RawMetadataCache rmc = new RawMetadataCache("test30", Pattern.compile(MetadataFile.CRAWL_LOG_PATTERN), null);
        arcrepfield.set(rmc, tarc);
        Long id1 = rmc.cache(30L);
        assertEquals("Should have exactly the one id asked for", (Long) 30L, id1);
        File cacheFile1 = rmc.getCacheFile(id1);
        assertNotNull("Should have the file asked for", cacheFile1.exists());
        String newFileName = "2-1-20161205100306320-00000-4320~kb-test-har-004.kb.dk~8173.arc.gz";
        for(String line: org.apache.commons.io.FileUtils.readLines(cacheFile1) ){
              if (line.contains("http://www.kaarefc.dk/avatar.png")) {
                  String newDuplicate = "duplicate:\"" + newFileName + ",4434,20161205100310384\"";
                  assertTrue(line + " should contain new duplicate record " + newDuplicate ,line.contains(newDuplicate));
              }
            if (line.contains("http://jigsaw.w3.org/css-validator/images/vcss-blue.gif")) {
                String newDuplicate = "duplicate:\"" + newFileName + ",142487,20161205100318947\"";
                assertTrue(line + " should contain new duplicate record " + newDuplicate ,line.contains(newDuplicate));
            }
        }
    }

    @Test
    public void testCacheData() throws Exception {
        TestArcRepositoryClient tarc = new TestArcRepositoryClient(new File(TestInfo.WORKING_DIR, "arcfiles"));
        Field arcrepfield = ReflectUtils.getPrivateField(RawMetadataCache.class, "arcrep");
        // Try one with just URL pattern.
        RawMetadataCache rmc = new RawMetadataCache("test3", Pattern.compile(".*index/cdx.*"), null);
        arcrepfield.set(rmc, tarc);
        // TODO find out why it stops here!
        Long id1 = rmc.cache(4L);
        // TODO find out why it stops here!
        assertEquals("Should have exactly the one id asked for", (Long) 4L, id1);
        File cacheFile1 = rmc.getCacheFile(id1);
        assertNotNull("Should have the file asked for", cacheFile1.exists());
        FileAsserts.assertFileContains("Should have two entries in the result",
                "file 4 block 1\nmetadata file 4 block 2", cacheFile1);

        // Try one with just mime pattern
        rmc = new RawMetadataCache("test4", null, Pattern.compile("application/.-cdx"));
        arcrepfield.set(rmc, tarc);
        id1 = rmc.cache(4L);
        assertEquals("Should have exactly the one id asked for", (Long) 4L, id1);
        cacheFile1 = rmc.getCacheFile(id1);
        assertEquals("File should have correct name", "test4-4-cache", cacheFile1.getName());
        FileAsserts.assertFileContains("Should have two entries in the result",
                "file 4 block 1\nmetadata file 4 block 3", cacheFile1);

        // Try one with both patterns
        rmc = new RawMetadataCache("test5", Pattern.compile(".*/cdx\\?.*"), Pattern.compile(".*/x-cdx"));
        arcrepfield.set(rmc, tarc);
        id1 = rmc.cache(4L);
        assertEquals("Should have exactly the one id asked for", (Long) 4L, id1);
        cacheFile1 = rmc.getCacheFile(id1);
        assertTrue("Should have the file asked for", cacheFile1.exists());
        FileAsserts.assertFileContains("Should have one entry in the result", "file 4 block 1", cacheFile1);

        // Test missing job
        id1 = rmc.cache(5L);
        assertNull("Should get null for non-existing job", id1);

        // Test empty output
        rmc = new RawMetadataCache("test6", Pattern.compile(".*/cdx/.*"), Pattern.compile(".*/x-cdx"));
        arcrepfield.set(rmc, tarc);
        id1 = rmc.cache(4L);
        assertEquals("Should have exactly the one id asked for", (Long) 4L, id1);
        cacheFile1 = rmc.getCacheFile(id1);
        assertTrue("Should have the file asked for", cacheFile1.exists());
        FileAsserts.assertFileMatches("Should have nothing in the file", "\\A\\z", cacheFile1);

        // Test cached-ness
        rmc = new RawMetadataCache("test7", Pattern.compile(".*index/cdx.*"), null);
        arcrepfield.set(rmc, tarc);
        id1 = rmc.cache(4L);
        assertEquals("Should have exactly the one id asked for", (Long) 4L, id1);
        assertNotNull("Should have exactly the one id asked for", cacheFile1);
        cacheFile1 = rmc.getCacheFile(id1);
        FileAsserts.assertFileContains("Should have two entries in the result",
                "file 4 block 1\nmetadata file 4 block 2", cacheFile1);

        // Empty the cached copy to check that the cache is not overwritten
        cacheFile1.delete();
        cacheFile1.createNewFile();
        int prevNumCalls = tarc.batchCounter;
        id1 = rmc.cache(4L);
        assertEquals("Should have exactly the one id asked for", (Long) 4L, id1);
        cacheFile1 = rmc.getCacheFile(id1);
        assertEquals("Should not have caused an extra get call", prevNumCalls, tarc.batchCounter);
        assertNotNull("Should have the file asked for", cacheFile1.exists());
        FileAsserts.assertFileMatches("Should have nothing in the file", "\\A\\z", cacheFile1);
        // Remove the cached file to check that the cache is rewritten as needed
        cacheFile1.delete();
        rmc.cache(4L);
        assertEquals("Should have exactly the one id asked for", (Long) 4L, id1);
        cacheFile1 = rmc.getCacheFile(id1);
        FileAsserts.assertFileContains("Should have two entries in the result",
                "file 4 block 1\nmetadata file 4 block 2", cacheFile1);
        assertEquals("Should have seen one more call", prevNumCalls + 1, tarc.batchCounter);

        // Check abysmal failures
        tarc.tmpDir = new File("/dev");
        rmc = new RawMetadataCache("test8", Pattern.compile(".*/cdx\\?.*"), Pattern.compile(".*/x-cdx"));
        arcrepfield.set(rmc, tarc);
        rmc.cache(4L);
    }
}
