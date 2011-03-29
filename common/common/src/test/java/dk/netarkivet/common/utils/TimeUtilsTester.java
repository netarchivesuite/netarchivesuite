/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.common.utils;

import java.util.Calendar;

import junit.framework.TestCase;

/**
 * Unit tests for the TimeUtils class.
 *
 */
public class TimeUtilsTester extends TestCase {
    public TimeUtilsTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testExponentialBackoffSleep() throws Exception {
        long timeStart = System.currentTimeMillis();
        TimeUtils.exponentialBackoffSleep(2, Calendar.MILLISECOND);
        long waited = System.currentTimeMillis() - timeStart;
        assertTrue("Should have waited at least 2^2 millis, but only waited "
                + waited,
                waited >= 4);

        timeStart = System.currentTimeMillis();
        TimeUtils.exponentialBackoffSleep(3, Calendar.MILLISECOND);
        waited = System.currentTimeMillis() - timeStart;
        assertTrue("Should have waited at least 2^3 millis, but only waited "
                + waited,
                waited >= 8);

        timeStart = System.currentTimeMillis();
        TimeUtils.exponentialBackoffSleep(0, Calendar.SECOND);
        waited = System.currentTimeMillis() - timeStart;
        assertTrue("Should have waited at least 2^0 seconds, but only waited "
                + waited,
                waited >= 1000);
    }
}
