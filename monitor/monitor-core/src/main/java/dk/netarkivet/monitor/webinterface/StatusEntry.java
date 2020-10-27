/*
 * #%L
 * Netarchivesuite - monitor
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

package dk.netarkivet.monitor.webinterface;

import java.util.Locale;

/**
 * An interface that specifies the information available in our JMX log mbeans.
 */
public interface StatusEntry extends Comparable<StatusEntry> {
    /**
     * Get the (physical) location this status entry is from, e.g. EAST.
     *
     * @return the (physical) location this status entry is from, e.g. EAST
     */
    String getPhysicalLocation();

    /**
     * Get the name of the host (machine) this status entry is from.
     *
     * @return the name of the host (machine) this status entry is from.
     */
    String getMachineName();

    /**
     * Get the HTTP port used by the application this status entry is from. Used for HTTP and self-identification.
     *
     * @return the HTTP port that the application that this status entry is from
     */
    String getHTTPPort();

    /**
     * Get the name of the application that this status entry is from.
     *
     * @return the name of the application that this status entry is from.
     */
    String getApplicationName();

    /**
     * Get the instance id of the application that this status entry is from.
     *
     * @return the instance id of the application that this status entry is from.
     */
    String getApplicationInstanceID();

    /**
     * Get the priority of the harvest queue that this status entry is from.
     *
     * @return the priority of the harvest queue that this status entry is from.
     */
    String getHarvestPriority();

    /**
     * Get the replica id of the application that this status entry is represents.
     *
     * @return the replica id of the application that this status entry is represents.
     */
    String getArchiveReplicaName();

    /**
     * Get the index in the list of most recent log messages that this status entry is from.
     *
     * @return the index in the list of most recent log messages that this status entry is from.
     */
    String getIndex();

    /**
     * Get the actual message.
     *
     * @param l the current locale (only used to translate errormessages)
     * @return the actual status message
     */
    String getLogMessage(Locale l);
}
