/*
 * #%L
 * Netarchivesuite - common - test
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
package dk.netarkivet.common.utils.batch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.datatransfer.MimeTypeParseException;

import org.junit.Test;

public class BatchFilterTester {
    // Our main instance of BatchFilter:
    ARCBatchFilter bf;

    /**
     * Verify that we can ask for a no-action filter.
     */
    @Test
    public void testNoFilter() {
        bf = ARCBatchFilter.NO_FILTER;
    }

    /**
     * Verify that we can ask for a filter that exludes ARC file headers.
     */
    @Test
    public void testExcludeFileHeaders() {
        bf = ARCBatchFilter.EXCLUDE_FILE_HEADERS;
    }

    /**
     * Verify that each possible filter can be identified for what it is.
     */
    @Test
    public void testIdentifiable() {
        bf = ARCBatchFilter.NO_FILTER;
        assertFalse(bf.equals(ARCBatchFilter.EXCLUDE_FILE_HEADERS));
    }

    /**
     * Test the dk.netarkivet.common.utils.arc.BatchFilter.getMimetypeBatchFilter Test the validity of the given
     * mimetype
     */
    @Test
    public void testGetMimetype() {
        String invalidMimetype = "test";
        String validMimetype = "text/html";
        ARCBatchFilter cfilter = null;

        try {
            cfilter = ARCBatchFilter.getMimetypeBatchFilter(invalidMimetype);
            fail("MimeTypeParseException expected because of invalid mimetype: " + invalidMimetype);
        } catch (MimeTypeParseException e) {
            // Expected behaviour
        }

        try {
            cfilter = ARCBatchFilter.getMimetypeBatchFilter(validMimetype);
        } catch (MimeTypeParseException e) {
            fail("MimeTypeParseException not expected here with valid mimetype as argument: " + validMimetype);
        } catch (Exception e) {
            fail("Exception " + e + " not expected here with valid mimetype as argument: " + validMimetype);
        }

        assertTrue("The return batchfilter should not be null", cfilter != null);

        assertEquals(cfilter.getName(), "MimetypeBatchFilter-text/html");
    }
}
