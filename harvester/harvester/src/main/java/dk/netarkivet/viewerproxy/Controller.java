
package dk.netarkivet.viewerproxy;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * The API for controlling the viewerproxy.
 *
 */
public interface Controller {
    /** Start URI collection. */
    void startRecordingURIs();

    /** Stop URI collection. */
    void stopRecordingURIs();

    /** Clear collected URIs. */
    void clearRecordedURIs();

    /** Get collected URIs.
     *
     * @return The collected URIs.
     */
    Set<URI> getRecordedURIs();

    /** Change current index to work on these jobs.
     *
     * @param jobList The list of jobs.
     * @param label A label this index should be known as
     */
    void changeIndex(Set<Long> jobList, String label);

    /** Get current status of viewerproxy. The status is not supposed to be
     * machine parsable. Do not base anything on the content of this status
     * message, the format may change without notice.
     *
     * @return A human-readable string with current status of the viewerproxy.
     * @param locale Indication of which locale to use for generating 
     * the string.
     */
    String getStatus(Locale locale);
}
