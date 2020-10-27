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
package dk.netarkivet.harvester.harvesting.distribute;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.StopReason;

/**
 * Unit tests for the class dk.netarkivet.harvester.harvesting.distribute.DomainStats.
 */
public class DomainStatsTester {

    private final long negativeInitObjectCount = -100;
    private final long positiveInitObjectCount = 100;
    private final long negativeInitByteCount = -100;
    private final long positiveInitByteCount = 100;
    private final StopReason downloadComplete = StopReason.DOWNLOAD_COMPLETE;
    private final StopReason nullStopreason = null;
    private DomainStats domainstats;

    @Before
    public void setUp() throws Exception {
        domainstats = new DomainStats(positiveInitObjectCount, positiveInitByteCount, downloadComplete);
    }

    /** test the DomainStats constructor. */
    @Test
    public void testDomainStats() {
        try {
            new DomainStats(negativeInitObjectCount, positiveInitByteCount, downloadComplete);
            fail("Should throw ArgumentNotValid exception on negative" + " objectCount");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        try {
            new DomainStats(positiveInitObjectCount, negativeInitByteCount, downloadComplete);
            fail("Should throw ArgumentNotValid exception on negative" + " byteCount");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        try {
            new DomainStats(positiveInitObjectCount, positiveInitByteCount, nullStopreason);
            fail("Should throw ArgumentNotValid exception on null stopreason");
        } catch (ArgumentNotValid e) {
            // Expected
        }

        try {
            new DomainStats(positiveInitObjectCount, positiveInitByteCount, downloadComplete);
        } catch (ArgumentNotValid e) {
            fail("Should not throw ArgumentNotValid exception on " + "correct arguments.");
        }
    }

    /** Test the setByteCount method. */
    @Test
    public void testSetByteCount() {
        final long bytes = 500L;
        domainstats.setByteCount(bytes);
        assertEquals(bytes, domainstats.getByteCount());
    }

    /** Test the setObjectCount method. */
    @Test
    public void testSetObjectCount() {
        final long objects = 200L;
        domainstats.setObjectCount(objects);
        assertEquals(objects, domainstats.getObjectCount());
    }

    /** Test the setStopReason method. */
    @Test
    public void testSetStopReason() {
        StopReason aStopreason = StopReason.DOWNLOAD_UNFINISHED;
        domainstats.setStopReason(aStopreason);
        assertEquals(aStopreason, domainstats.getStopReason());

    }

}
