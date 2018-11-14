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

import java.io.File;

import org.junit.Ignore;
import org.junit.Test;

import ch.qos.logback.classic.Level;
import dk.netarkivet.testutils.LogbackRecorder;

/**
 * Unit tests for the abstract class FileBasedCache.
 */
public class FileBasedCacheTester {

    @Test
    @Ignore("Log file does not contain expected string")
    public void testGetIndex() throws Exception {
        LogbackRecorder lr = LogbackRecorder.startRecorder();
        FileBasedCache<String> cache = new FileBasedCache<String>("Test") {
            /**
             * Get the file that caches content for the given ID.
             *
             * @param id Some sort of id that uniquely identifies the item within the cache.
             * @return A file (possibly non-existing or empty) that can cache the data for the id.
             */
            public File getCacheFile(String id) {
                return TestInfo.ARC_FILE_1;
            }

            /**
             * Fill in actual data in the file in the cache. This is the workhorse method that is allowed to modify the
             * cache. When this method is called, the cache can assume that getCacheFile(id) does not exist.
             *
             * @param id Some identifier for the item to be cached.
             * @return An id of content actually available. In most cases, this will be the same as id, but for complex
             * I it could be a subset (or null if the type argument I is a simple type). If the return value is not the
             * same as id, the file will not contain cached data, and may not even exist.
             */
            protected String cacheData(String id) {
                return null;
            }

            String nextId = "";

            /**
             * Fill in actual data in the file in the cache. This is the workhorse method that is allowed to modify the
             * cache. When this method is called, the cache can assume that getCacheFile(id) does not exist.
             *
             * @param id Some identifier for the item to be cached.
             * @return An id of content actually available. In most cases, this will be the same as id, but for complex
             * I it could be a subset (or null if the type argument I is a simple type). If the return value is not the
             * same as id, the file will not contain cached data, and may not even exist.
             */
            public String cache(String id) {
                if (nextId.length() < 4) {
                    nextId += "X";
                }
                return nextId;
            }
        };
        cache.getIndex("A");
        lr.assertLogNotContainsLevel("Should not give warning on cache retry", Level.WARN);
        lr.stopRecorder();
    }

}
