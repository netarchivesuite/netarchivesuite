package dk.netarkivet.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import junit.framework.TestCase;

/**
 * Unit tests for methods in class dk.netarkivet.common.Constants.
 */
public class ConstantsTester {

    @Test
    public void is_getHeritrixVersionString_sameAsConstant() {
        Assert.assertEquals("HeritrixVersionString is wrong", "1.14.4",
                Constants.getHeritrixVersionString());
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
        final boolean[] failed = new boolean[] { false };

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
        WAITLOOP: do {
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