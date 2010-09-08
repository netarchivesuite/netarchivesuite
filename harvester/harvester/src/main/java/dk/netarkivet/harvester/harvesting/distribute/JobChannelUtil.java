/* File:    $Id: $
* Revision: $Revision: $
* Author:   $Author: $
* Date:     $Date: $
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.harvester.harvesting.distribute;

import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.harvester.datamodel.JobPriority;

/**
 * Provides functionality specific to the harvest job channels.
 */
public class JobChannelUtil {

	/**
	 * Hides constructor to avoid instantiation of this class (all method should
	 * be accessed statically)
	 */
	private JobChannelUtil() {
	}

	/**
	 * Finds channels based on the priority of jobs.
	 * 
	 * @param jobPriority
	 *            The job priority the find a channel for
	 * @return The channel used to send the jobs for the indicated priorities
	 */
	public static ChannelID getChannel(JobPriority jobPriority) {
		switch (jobPriority) {
		case LOWPRIORITY:
			return Channels.getAnyLowpriorityHaco();
		case HIGHPRIORITY:
			return Channels.getAnyHighpriorityHaco();
		default:
			throw new UnknownID("Unable to find channel for job priority "
					+ jobPriority);
		}
	}
}