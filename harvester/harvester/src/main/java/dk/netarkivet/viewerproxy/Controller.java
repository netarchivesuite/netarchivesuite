/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
import java.util.Locale;
import java.util.Set;

/**
 * The API for controlling the viewerproxy.
 *
 */
public interface Controller {
    /** Start URI collection. */
    public void startRecordingURIs();

    /** Stop URI collection. */
    public void stopRecordingURIs();

    /** Clear collected URIs. */
    public void clearRecordedURIs();

    /** Get collected URIs.
     *
     * @return The collected URIs.
     */
    public Set<URI> getRecordedURIs();

    /** Change current index to work on these jobs.
     *
     * @param jobList The list of jobs.
     * @param label A label this index should be known as
     */
    public void changeIndex(Set<Long> jobList, String label);

    /** Get current status of viewerproxy. The status is not supposed to be
     * machine parsable. Do not base anything on the content of this status
     * message, the format may change without notice.
     *
     * @return A human-readable string with current status of the viewerproxy.
     * @param locale Indication of which locale to use for generating the string.
     */
    public String getStatus(Locale locale);
}
