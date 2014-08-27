/*
 * #%L
 * Netarchivesuite - common
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
package dk.netarkivet.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for methods in class dk.netarkivet.common.Constants.
 */
public class ConstantsTester {

    @Test
    public void is_getHeritrixVersionString_sameAsConstant() {
        Assert.assertEquals("HeritrixVersionString is wrong", "1.14.4", Constants.getHeritrixVersionString());
    }

    /**
     * Try to see if getIsoDateFormatter is thread safe.
     */
    @Test
    public void is_getIsoDateFormatter_threadsafe() throws Exception {

        final String date = "2005-12-24 13:42:07 +0100";
        final Date time = Constants.getIsoDateFormatter().parse(date);

        List<Thread> threads = new ArrayList<Thread>();

        // FIXME: What do we actually want to do here?

        // This is a latch, so we don't need to synchronize
        final boolean[] failed = new boolean[] {false};

        for (int i = 0; i < 30; i++) {
            threads.add(new Thread() {
                public void run() {
                    // yield();
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
        for (Thread t : threads) {
            t.start();
        }
        WAITLOOP:
        do {
            Thread.sleep(10);
            if (failed[0]) {
                break;
            }
            for (Thread t : threads) {
                if (t.isAlive()) {
                    continue WAITLOOP;
                }
            }
            // If we get here, no thread was still alive, we can go on.
            break;
        } while (true);

        if (failed[0]) {
            Assert.fail("Failed to handle parallel use of SimpleDateFormat");
        }
    }
}
