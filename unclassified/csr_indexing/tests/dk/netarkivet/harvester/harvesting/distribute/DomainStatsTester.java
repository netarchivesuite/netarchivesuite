/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.harvester.harvesting.distribute;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.StopReason;
import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Unit tests for the class
 * dk.netarkivet.harvester.harvesting.distribute.DomainStats.
 */
public class DomainStatsTester extends TestCase {

    private final long negativeInitObjectCount = -100;
    private final long positiveInitObjectCount = 100;
    private final long negativeInitByteCount = -100;
    private final long positiveInitByteCount = 100;
    private final StopReason downloadComplete = StopReason.DOWNLOAD_COMPLETE;
    private final StopReason nullStopreason = null;
    private DomainStats domainstats;
    
    protected void setUp() throws Exception {
        domainstats = new DomainStats(positiveInitObjectCount,
                positiveInitByteCount, downloadComplete);
    }

    /** test the DomainStats constructor. */
    public void testDomainStats() {
        try {
            new DomainStats(negativeInitObjectCount,
                    positiveInitByteCount, downloadComplete);
            fail("Should throw ArgumentNotValid exception on negative"
                    + " objectCount");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
        try {
            new DomainStats(positiveInitObjectCount,
                    negativeInitByteCount, downloadComplete);
            fail("Should throw ArgumentNotValid exception on negative"
                    + " byteCount");
        } catch (ArgumentNotValid e) {
            // Expected
        }
 
        try {
            new DomainStats(positiveInitObjectCount,
                    positiveInitByteCount, nullStopreason);
            fail("Should throw ArgumentNotValid exception on null stopreason");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
        try {
            new DomainStats(positiveInitObjectCount,
                    positiveInitByteCount, downloadComplete);
        } catch (ArgumentNotValid e) {
            fail("Should not throw ArgumentNotValid exception on "
                    + "correct arguments.");
        }
    }
    
    /** Test the setByteCount method. */
    public void testSetByteCount() {
        final long bytes = 500L;
        domainstats.setByteCount(bytes);
        Assert.assertEquals(bytes, domainstats.getByteCount());
    }
    /** Test the setObjectCount method. */
    public void testSetObjectCount() {
        final long objects = 200L;
        domainstats.setObjectCount(objects);
        Assert.assertEquals(objects, domainstats.getObjectCount());
    }
    
    /** Test the setStopReason method. */
    public void testSetStopReason() {
        StopReason aStopreason = StopReason.DOWNLOAD_UNFINISHED;
        domainstats.setStopReason(aStopreason);
        Assert.assertEquals(aStopreason, domainstats.getStopReason());

    }

}
