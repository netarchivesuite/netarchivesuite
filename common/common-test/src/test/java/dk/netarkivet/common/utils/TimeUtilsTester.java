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
package dk.netarkivet.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;

import org.junit.Test;

/**
 * Unit tests for the TimeUtils class.
 */
public class TimeUtilsTester {

    @Test
    public void testExponentialBackoffSleep() throws Exception {
        long timeStart = System.currentTimeMillis();
        TimeUtils.exponentialBackoffSleep(2, Calendar.MILLISECOND);
        long waited = System.currentTimeMillis() - timeStart;
        assertTrue("Should have waited at least 2^2 millis, but only waited " + waited, waited >= 4);

        timeStart = System.currentTimeMillis();
        TimeUtils.exponentialBackoffSleep(3, Calendar.MILLISECOND);
        waited = System.currentTimeMillis() - timeStart;
        assertTrue("Should have waited at least 2^3 millis, but only waited " + waited, waited >= 8);

        timeStart = System.currentTimeMillis();
        TimeUtils.exponentialBackoffSleep(0, Calendar.SECOND);
        waited = System.currentTimeMillis() - timeStart;
        assertTrue("Should have waited at least 2^0 seconds, but only waited " + waited, waited >= 1000);
    }

    /**
     * Tests whether the readableTimeInterval function converts numbers correctly.
     */
    @Test
    public void testReadability() {
        // Test whether it works.
        assertEquals(TimeUtils.readableTimeInterval(1L), "1 millisecond");

        // Test conversion between millisecond and seconds.
        assertEquals(TimeUtils.readableTimeInterval(5000L), "5 seconds");

        // Test conversion between millisecond and minutes.
        assertEquals(TimeUtils.readableTimeInterval(900000L), "15 minutes");

        // Test conversion between millisecond and hours.
        assertEquals(TimeUtils.readableTimeInterval(50400000), "14 hours");

        // Test conversion between millisecond and days.
        assertEquals(TimeUtils.readableTimeInterval(172800000L), "2 days");

        // Test conversion between millisecond and weeks.
        assertEquals(TimeUtils.readableTimeInterval(604800000L), "1 week");

        // 2,5 hours should be returned in minutes
        assertEquals(TimeUtils.readableTimeInterval(9000000L), "150 minutes");
    }
}
