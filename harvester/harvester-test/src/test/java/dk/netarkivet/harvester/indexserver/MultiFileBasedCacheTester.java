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
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Tests for multifilebasedcache, i.e. file name generation.
 */
public class MultiFileBasedCacheTester {

    private MultiFileBasedCache<Long> multiFileBasedCache = new MultiFileBasedCache<Long>("Test") {

        protected Set<Long> cacheData(Set<Long> id) {
            // Not used
            return null;
        }
    };

    @Test
    public void testGetCacheFile() throws Exception {
        try {
            multiFileBasedCache.getCacheFile(null);
            fail("Should throw exception");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        assertEquals("Expect value on 1 id", "1-cache",
                multiFileBasedCache.getCacheFile(new HashSet<Long>(Arrays.asList(new Long[] {1L}))).getName());
        assertEquals("Expect value on 2 ids", "1-2-cache",
                multiFileBasedCache.getCacheFile(new HashSet<Long>(Arrays.asList(new Long[] {1L, 2L}))).getName());
        assertEquals("Expect value on 8 ids", "1-2-3-4-d28f2f51ffe4b6d42b3e46a7cd7a72da-cache", multiFileBasedCache
                .getCacheFile(new HashSet<Long>(Arrays.asList(new Long[] {1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L}))).getName());
        assertEquals("Expect value on 9 ids", "1-2-3-4-ef8be5d11d2348e3cf689ae6bf0dd6ef-cache", multiFileBasedCache
                .getCacheFile(new HashSet<Long>(Arrays.asList(new Long[] {1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L})))
                .getName());
    }
}
