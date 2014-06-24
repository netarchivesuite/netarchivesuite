package dk.netarkivet.archive.bitarchive.distribute;

import junit.framework.TestCase;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Unittests for the HearBeatMessage class.
 */
public class HeartBeatMessageTester extends TestCase {

    private String baID = "BA_ID";
    private ChannelID baMon = Channels.getTheBamon();

    /**
     * Verify that the constructor only fails if it is given
     * a null ChannelID or a null or empty applicationId:
     */
    public void testConstructor() {

        HeartBeatMessage hbm = null;

        try {
            hbm = new HeartBeatMessage(null, baID);
            fail("HeartBeatMessage constructor shouldn't accept null as Channel parameter.");
        } catch (ArgumentNotValid e) {
            //Expected.
        }

        try {
            hbm = new HeartBeatMessage(baMon, null);
            fail("HeartBeatMessage constructor shouldn't accept null as application ID.");
        } catch (ArgumentNotValid e) {
            //Expected.
        }


        try {
            hbm = new HeartBeatMessage(baMon, "");
            fail("HeartBeatMessage constructor shouldn't accept empty string as application ID.");
        } catch (ArgumentNotValid e) {
            //Expected.
        }

        // The OK case:
        hbm = new HeartBeatMessage(baMon, baID);

        assertNotNull(hbm);
    }

    /**
     * Verify that getTimestamp(), getApplicationId() and getLogLevel()
     * behave as expected.
     */
    public void testGetters() {

        long time = System.currentTimeMillis();
        HeartBeatMessage hbm = new HeartBeatMessage(baMon, baID);

        assertTrue("Timestamp of HeartBeatMessage does not make sense.",
                hbm.getTimestamp() >= time);

        assertEquals("ApplicationID of HeartBeatMessage is not as excepted.",
                baID, hbm.getBitarchiveID());
    }

}
