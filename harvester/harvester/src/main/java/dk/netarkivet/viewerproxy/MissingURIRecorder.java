
package dk.netarkivet.viewerproxy;

import java.net.URI;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * This class handles recordings of URIs not found during URI lookup.
 *
 */

public class MissingURIRecorder extends URIObserver {
    /** The recorded list of URIs. We use TreeSet which removes duplicates
     * and sorts the entries. */
    private Set<URI> uriSet = Collections
            .synchronizedSortedSet(new TreeSet<URI>());

    /** Indicates whether we are actively recording reported URIs
     *  at the moment. */
    private boolean recordingURIs;

    /**
     * Start recording missing URIs.
     */
    public void startRecordingURIs() {
        recordingURIs = true;
    }

    /**
     * Stop recording missing URIs.
     */
    public void stopRecordingURIs() {
        recordingURIs = false;
    }

    /**
     * Clear list of missing URIs.
     */
    public void clearRecordedURIs() {
        uriSet = Collections.synchronizedSortedSet(new TreeSet<URI>());
    }

    /**
     * Getter for the recorded missing URIs.
     * @return the recorded URIs, as a sorted set. Note that this is the primary
     * copy, so don't modify it!
     */
    public Set<URI> getRecordedURIs() {
        return uriSet;
    }

    /** If we are recording URIs, and the response code is NOT_FOUND, then
     * add URI to the list of missing URIs.
     * @param uri The URI observed.
     * @param responseCode The responsecode of the uri.
     */
    public void notify(URI uri, int responseCode) {
        if (recordingURIs && responseCode == URIResolver.NOT_FOUND) {
            uriSet.add(uri);
        }
    }

    /** Returns whether we are currently collecting URIs.
     *
     * @return True if we are currently collecting URIs.
     */
    public boolean isRecordingURIs() {
        return recordingURIs;
    }
}
