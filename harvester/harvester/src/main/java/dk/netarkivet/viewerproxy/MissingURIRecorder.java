/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
