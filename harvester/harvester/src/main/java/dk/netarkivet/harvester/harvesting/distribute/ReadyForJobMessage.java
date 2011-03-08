/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
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
package dk.netarkivet.harvester.harvesting.distribute;

import java.io.Serializable;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.harvester.datamodel.JobPriority;
import dk.netarkivet.harvester.distribute.HarvesterMessage;
import dk.netarkivet.harvester.distribute.HarvesterMessageVisitor;
import dk.netarkivet.harvester.harvesting.HarvestController;
import dk.netarkivet.harvester.scheduler.HarvestDispatcher;

/**
 * The {@link HarvestController} sends a {@link ReadyForJobMessage} when it is
 * ready to perform another crawl. {@link ReadyForJobMessage}s are processed
 * by the {@link HarvestDispatcher}.
 */
public class ReadyForJobMessage
extends HarvesterMessage
implements Serializable {

    /**
     * The priority of jobs crawled by the sender.
     */
    private final JobPriority jobProprity;

    /**
     * Builds a new message.
     * @param jobPriority the priority of jobs crawled by the sender.
     */
    public ReadyForJobMessage(JobPriority jobPriority) {
        super(Channels.getHarvestDispatcherChannel(), Channels.getError());
        this.jobProprity = jobPriority;
    }

    @Override
    public void accept(HarvesterMessageVisitor v) {
        v.visit(this);
    }

    /**
     * @return the priority of jobs crawled by the sender.
     */
    public JobPriority getJobProprity() {
        return jobProprity;
    }

}
