 package dk.netarkivet.common.distribute;

 import java.util.Calendar;

 import junit.framework.TestCase;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.TimeUtils;

/**
 * Testclass for testing the exceptionhandling in JMSConnection.
 */
@SuppressWarnings({ "unused", "serial"})
public class AlternateJMSConnectionTester extends TestCase {

    public void errorcodesTest() {
        Settings.set(JMSConnectionSunMQ.JMS_BROKER_PORT, "7677");
        JMSConnection con = JMSConnectionFactory.getInstance();
        NetarkivetMessage msg;
        int msgNr = 0;
        while (msgNr < 50) {
             msg = new TestMessage(Channels.getError(), Channels.getTheRepos(), "testID" + msgNr);
             System.out.println("Sending message " +  msgNr);
             con.send(msg);
             System.out.println("Message " +  msgNr +  " now sent");
             TimeUtils.exponentialBackoffSleep(1, Calendar.MINUTE);
             msgNr++;
        }
        con.cleanup();
    }

	private static class TestMessage extends NetarkivetMessage {
        private String testID;

        public TestMessage(ChannelID to, ChannelID replyTo, String testID) {
            super(to, replyTo);
            this.testID = testID;
        }

        public String getTestID() {
            return testID;
        }
    }
}
