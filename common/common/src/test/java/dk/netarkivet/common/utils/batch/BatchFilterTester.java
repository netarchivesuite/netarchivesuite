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
package dk.netarkivet.common.utils.batch;

import junit.framework.TestCase;

import java.awt.datatransfer.MimeTypeParseException;

import dk.netarkivet.common.utils.batch.ARCBatchFilter;


public class BatchFilterTester extends TestCase {
    //Our main instance of BatchFilter:
    ARCBatchFilter bf;

    /**
     * Verify that we can ask for a no-action filter.
     */
    public void testNoFilter() {
        bf = ARCBatchFilter.NO_FILTER;
    }

    /**
     * Verify that we can ask for a filter that exludes ARC file headers.
     */
    public void testExcludeFileHeaders() {
        bf = ARCBatchFilter.EXCLUDE_FILE_HEADERS;
    }

    /**
     * Verify that each possible filter can be identified for what it is.
     */
    public void testIdentifiable() {
        bf = ARCBatchFilter.NO_FILTER;
        assertFalse(bf.equals(ARCBatchFilter.EXCLUDE_FILE_HEADERS));
    }

    /**
    * Test the dk.netarkivet.common.utils.arc.BatchFilter.getMimetypeBatchFilter
    * Test the validity of the given mimetype
    */
    public void testGetMimetype() {
        String invalidMimetype = new String("test");
        String validMimetype = "text/html";
        ARCBatchFilter cfilter = null;

        try {
            cfilter = ARCBatchFilter.getMimetypeBatchFilter(invalidMimetype);
            fail(
                "MimeTypeParseException expected because of invalid mimetype: " +
                invalidMimetype);
        } catch (MimeTypeParseException e) {
            // Expected behaviour
        }

        try {
            cfilter = ARCBatchFilter.getMimetypeBatchFilter(validMimetype);
        } catch (MimeTypeParseException e) {
            fail(
                "MimeTypeParseException not expected here with valid mimetype as argument: " +
                validMimetype);
        } catch (Exception e) {
            fail("Exception " + e +
                " not expected here with valid mimetype as argument: " +
                validMimetype);
        }
        
        assertTrue("The return batchfilter should not be null", cfilter != null);
        
        assertEquals(cfilter.getName(), "MimetypeBatchFilter-text/html");
    }
}
