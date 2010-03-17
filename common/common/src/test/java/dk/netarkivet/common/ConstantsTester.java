/*$Id$
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
package dk.netarkivet.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.TestCase;

/**
 * Unitests for methods in class dk.netarkivet.common.Constants.
 */
public class ConstantsTester extends TestCase {
    public ConstantsTester(String s) {
        super(s);
    }

    public void setUp() {
    }

    public void tearDown() {
    }

    public void testgetHeritrixVersionString() {
        assertEquals("HeritrixVersionString is wrong",
                "1.14.3",
                Constants.getHeritrixVersionString()
                );
    }

    public void testGetIsoDateFormatter() throws Exception {
        final String date = "2005-12-24 13:42:07 +0100";
        final Date time = Constants.getIsoDateFormatter().parse(date);
        List<Thread> threads = new ArrayList<Thread>();
        // This is a latch, so we don't need to synchronize
        final boolean[] failed = new boolean[] { false };
        for (int i = 0; i < 30; i++) {
            threads.add(new Thread() {
                public void run() {
                    //yield();
                    SimpleDateFormat format = Constants.getIsoDateFormatter();
                    for (int tries = 0; tries < 10; tries++) {
                        if (failed[0]) {
                            break;
                        }
                        try {
                            Date t = format.parse(date);
                            if (!t.equals(time)) {
                                System.out.println("Time " + time + " != " + t);
                                failed[0] = true;
                            }
                        } catch (ParseException e) {
                            System.out.println("ParseException " + e);
                            failed[0] = true;
                        }
                    }
                }
            });
        }
        for (Thread t: threads) {
            t.start();
        }
        WAITLOOP: do {
            Thread.sleep(10);
            if (failed[0]) {
                break;
            }
            for (Thread t: threads) {
                if (t.isAlive()) {
                    continue WAITLOOP;
                }
            }
            // If we get here, no thread was still alive, we can go on.
            break;
        } while (true);
        if (failed[0]) {
            fail("Failed to handle parallel use of SimpleDateFormat");
        }
    }

}