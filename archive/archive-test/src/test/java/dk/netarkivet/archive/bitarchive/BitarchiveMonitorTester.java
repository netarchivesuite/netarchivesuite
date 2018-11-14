/*
 * #%L
 * Netarchivesuite - archive - test
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
package dk.netarkivet.archive.bitarchive;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.archive.ArchiveSettings;
import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.testutils.ClassAsserts;
import dk.netarkivet.testutils.preconfigured.MockupJMS;
import dk.netarkivet.testutils.preconfigured.ReloadSettings;

/** Unit test for BitarchiveMonitorTester */
@SuppressWarnings({"rawtypes", "unused"})
public class BitarchiveMonitorTester {
    private ReloadSettings rs = new ReloadSettings();

    private MockupJMS mj = new MockupJMS();

    private static final Replica ONE = Replica.getReplicaFromId("ONE");

    @Before
    public void setUp() {
        rs.setUp();
        mj.setUp();
    }

    @After
    public void tearDown() {
        mj.tearDown();
        rs.tearDown();
    }

    /**
     * Test for singleton.
     */
    @Test
    public void testSingleton() {
        ClassAsserts.assertSingleton(BitarchiveMonitor.class);
    }

    /**
     * Tests the timeout of batchjobs.
     */
    @Test
    public void testBatchJobTimout() {
    	TimeoutBatch timeoutBatch = new TimeoutBatch();
    	Channels.reset();
        // jmsARClient.batch(timeoutBatch, "ONE");
        BatchMessage msg = new BatchMessage(Channels.getTheBamon(), Channels.getError(), timeoutBatch,
                Settings.get(CommonSettings.USE_REPLICA_ID));

        long batchTimeout = msg.getJob().getBatchJobTimeout();
        /*
         * bitarchiveMonitor.registerBatch("asdfsdf", msg.getReplyTo(), "asdfsdf", batchTimeout > 0 ? batchTimeout :
         * Settings.getLong( ArchiveSettings.BITARCHIVE_BATCH_JOB_TIMEOUT) );
         */

        assertEquals(batchTimeout, batchTimeout > 0 ? batchTimeout : TestInfo.BITARCHIVE_BATCH_JOB_TIMEOUT);

        EvilBatch eb = new EvilBatch();

        // jmsARClient.batch(timeoutBatch, "ONE");
        msg = new BatchMessage(Channels.getTheBamon(), Channels.getError(), eb,
                Settings.get(CommonSettings.USE_REPLICA_ID));

        batchTimeout = msg.getJob().getBatchJobTimeout();
        /*
         * bitarchiveMonitor.registerBatch("asdfsdf", msg.getReplyTo(), "asdfsdf", batchTimeout > 0 ? batchTimeout :
         * TestInfo.BITARCHIVE_BATCH_JOB_TIMEOUT );
         */
        assertEquals(TestInfo.BITARCHIVE_BATCH_JOB_TIMEOUT, batchTimeout > 0 ? batchTimeout
                : TestInfo.BITARCHIVE_BATCH_JOB_TIMEOUT);
    }

    /**
     * Checks that a bitarchive correctly is removed when its sign of life expires.
     *
     * @throws NoSuchFieldException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public void testBitarchiveLifeTimeout() throws SecurityException, NoSuchFieldException, NoSuchMethodException,
            IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        Settings.set(ArchiveSettings.BITARCHIVE_ACCEPTABLE_HEARTBEAT_DELAY, "0");
        BitarchiveMonitor bamon = BitarchiveMonitor.getInstance();

        Field acceptableDelay = bamon.getClass().getDeclaredField("acceptableSignOfLifeDelay");
        acceptableDelay.setAccessible(true);
        assertEquals("The acceptable sign of life delay should be set to 0.", 0L, acceptableDelay.get(bamon));

        Field bitarchiveLife = bamon.getClass().getDeclaredField("bitarchiveSignsOfLife");
        bitarchiveLife.setAccessible(true);
        Map bl = (Map) bitarchiveLife.get(bamon);
        assertTrue("The bitarchiveSignsOfLife map should be empty", bl.isEmpty());
        bamon.signOfLife("appID");
        assertFalse("The bitarchiveSignsOfLife map should not be empty", bl.isEmpty());

        Method getBAs = bamon.getClass().getDeclaredMethod("getRunningBitarchiveIDs");
        getBAs.setAccessible(true);
        Set bas = (Set) getBAs.invoke(bamon);
        assertTrue("No running bitarchive ids should be found", bas.isEmpty());

        bl = (Map) bitarchiveLife.get(bamon);
        assertTrue("The map should have been cleaned.", bl.isEmpty());
        bamon.cleanup();
    }
}
