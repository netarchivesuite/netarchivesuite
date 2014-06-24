
package dk.netarkivet.testutils;

import junit.framework.TestCase;

import dk.netarkivet.common.distribute.NetarkivetMessage;

/**
 * Assertions on JMS/Netarkivet messages
 *
 */

public class MessageAsserts {
    /** Assert that a message is ok, and if not, print out the error message.
     *
     * @param s A string explaining what was expected to happen, e.g.
     * "Get message on existing file should reply ok"
     * @param msg The message to check the status of
     */
    public static void assertMessageOk(String s, NetarkivetMessage msg) {
        if (!msg.isOk()) {
            TestCase.fail(s + ": " + msg.getErrMsg());
        }
    }
}
