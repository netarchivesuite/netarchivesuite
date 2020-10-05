/*
 * #%L
 * Netarchivesuite - harvester
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

package dk.netarkivet.viewerproxy;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

/**
 * The API for controlling the viewerproxy.
 */
public interface Controller {
    /** Start URI collection. */
    void startRecordingURIs();

    /** Stop URI collection. */
    void stopRecordingURIs();

    /** Clear collected URIs. */
    void clearRecordedURIs();

    /**
     * Get collected URIs.
     *
     * @return The collected URIs.
     */
    Set<URI> getRecordedURIs();

    /**
     * Change current index to work on these jobs.
     *
     * @param jobList The list of jobs.
     * @param label A label this index should be known as
     */
    void changeIndex(Set<Long> jobList, String label);

    /**
     * Get current status of viewerproxy. The status is not supposed to be machine parsable. Do not base anything on the
     * content of this status message, the format may change without notice.
     *
     * @param locale Indication of which locale to use for generating the string.
     * @return A human-readable string with current status of the viewerproxy.
     */
    String getStatus(Locale locale);
}
