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
package dk.netarkivet.archive.indexserver;
/**
 * lc forgot to comment this!
 */

import java.io.File;
import java.lang.reflect.Field;
import java.util.regex.Pattern;

import dk.netarkivet.testutils.FileAsserts;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.TestArcRepositoryClient;
import dk.netarkivet.testutils.preconfigured.MockupJMS;


public class RawMetadataCacheTester extends CacheTestCase {
    MockupJMS mjms = new MockupJMS();

    public RawMetadataCacheTester(String s) {
        super(s);
    }

    public void setUp() throws Exception {
        super.setUp();
        mjms.setUp();
    }

    public void tearDown() throws Exception {
        mjms.tearDown();
        new TestArcRepositoryClient(TestInfo.WORKING_DIR).close();
        super.tearDown();
    }

    public void testGetCacheFileName() throws Exception {
        RawMetadataCache cache = new RawMetadataCache("test1", null, null);
        assertEquals("Should get filename for cache files based on prefix"
                + " and id",
                "test1-42-cache", cache.getCacheFile(42L).getName());
        assertEquals("Should get dirname for cache files based on prefix",
                "test1", cache.getCacheFile(42L).getParentFile().getName());
    }

    public void testGetCacheDir() throws Exception {
        RawMetadataCache cache = new RawMetadataCache("test2", null, null);
        assertEquals("Should get dirname for cache files based on prefix",
                "test2", cache.getCacheDir().getName());
        assertEquals("Should get dirname for cache files in cache dir",
                "cache",
                cache.getCacheDir().getAbsoluteFile().getParentFile().getName());
    }

    public void testCacheData() throws Exception {
        TestArcRepositoryClient tarc = new TestArcRepositoryClient(
                new File(TestInfo.WORKING_DIR, "arcfiles"));
        Field arcrepfield = ReflectUtils.getPrivateField(RawMetadataCache.class,
                "arcrep");
        // Try one with just URL pattern.
        RawMetadataCache rmc = new RawMetadataCache("test3",
                Pattern.compile(".*index/cdx.*"), null);
        arcrepfield.set(rmc, tarc);
        Long id1 = rmc.cache(4L);
        assertEquals("Should have exactly the one id asked for",
                (Long) 4L, id1);
        File cacheFile1 = rmc.getCacheFile(id1);
        assertNotNull("Should have the file asked for", cacheFile1.exists());
        FileAsserts.assertFileContains("Should have two entries in the result",
                "file 4 block 1\nmetadata file 4 block 2", cacheFile1);

        // Try one with just mime pattern
        rmc = new RawMetadataCache("test4",
                null, Pattern.compile("application/.-cdx"));
        arcrepfield.set(rmc, tarc);
        id1 = rmc.cache(4L);
        assertEquals("Should have exactly the one id asked for",
                (Long) 4L, id1);
        cacheFile1 = rmc.getCacheFile(id1);
        assertEquals("File should have correct name", "test4-4-cache",
                cacheFile1.getName());
        FileAsserts.assertFileContains("Should have two entries in the result",
                "file 4 block 1\nmetadata file 4 block 3", cacheFile1);

        // Try one with both patterns
        rmc = new RawMetadataCache("test5",
                Pattern.compile(".*/cdx\\?.*"),
                Pattern.compile(".*/x-cdx"));
        arcrepfield.set(rmc, tarc);
        id1 = rmc.cache(4L);
        assertEquals("Should have exactly the one id asked for",
                (Long) 4L, id1);
        cacheFile1 = rmc.getCacheFile(id1);
        assertTrue("Should have the file asked for", cacheFile1.exists());
        FileAsserts.assertFileContains("Should have one entry in the result",
                "file 4 block 1", cacheFile1);

        // Test missing job
        id1 = rmc.cache(5L);
        assertNull("Should get null for non-existing job", id1);

        // Test empty output
        rmc = new RawMetadataCache("test6",
                Pattern.compile(".*/cdx/.*"),
                Pattern.compile(".*/x-cdx"));
        arcrepfield.set(rmc, tarc);
        id1 = rmc.cache(4L);
        assertEquals("Should have exactly the one id asked for",
                (Long) 4L, id1);
        cacheFile1 = rmc.getCacheFile(id1);
        assertTrue("Should have the file asked for", cacheFile1.exists());
        FileAsserts.assertFileMatches("Should have nothing in the file",
                "\\A\\z", cacheFile1);

        // Test cached-ness
        rmc = new RawMetadataCache("test7",
                Pattern.compile(".*index/cdx.*"), null);
        arcrepfield.set(rmc, tarc);
        id1 = rmc.cache(4L);
        assertEquals("Should have exactly the one id asked for",
                (Long) 4L, id1);
        assertNotNull("Should have exactly the one id asked for", cacheFile1);
        cacheFile1 = rmc.getCacheFile(id1);
        FileAsserts.assertFileContains("Should have two entries in the result",
                "file 4 block 1\nmetadata file 4 block 2", cacheFile1);

        // Empty the cached copy to check that the cache is not overwritten
        cacheFile1.delete();
        cacheFile1.createNewFile();
        int prevNumCalls = tarc.batchCounter;
        id1 = rmc.cache(4L);
        assertEquals("Should have exactly the one id asked for",
                (Long) 4L, id1);
        cacheFile1 = rmc.getCacheFile(id1);
        assertEquals("Should not have caused an extra get call",
                prevNumCalls, tarc.batchCounter);
        assertNotNull("Should have the file asked for", cacheFile1.exists());
        FileAsserts.assertFileMatches("Should have nothing in the file",
                "\\A\\z", cacheFile1);
        // Remove the cached file to check that the cache is rewritten as needed
        cacheFile1.delete();
        rmc.cache(4L);
        assertEquals("Should have exactly the one id asked for",
                (Long) 4L, id1);
        cacheFile1 = rmc.getCacheFile(id1);
        FileAsserts.assertFileContains("Should have two entries in the result",
                "file 4 block 1\nmetadata file 4 block 2", cacheFile1);
        assertEquals("Should have seen one more call",
                prevNumCalls + 1, tarc.batchCounter);

        // Check abysmal failures
        tarc.tmpDir = new File("/dev");
        rmc = new RawMetadataCache("test8",
                Pattern.compile(".*/cdx\\?.*"),
                Pattern.compile(".*/x-cdx"));
        arcrepfield.set(rmc, tarc);
        rmc.cache(4L);
    }
}