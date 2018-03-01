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
package dk.netarkivet.harvester.harvesting.distribute;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.harvester.distribute.HarvesterMessage;
import dk.netarkivet.harvester.distribute.HarvesterMessageVisitor;
import dk.netarkivet.harvester.harvesting.frontier.FrontierReportFilter;
import dk.netarkivet.harvester.harvesting.frontier.InMemoryFrontierReport;
import dk.netarkivet.harvester.harvesting.monitor.HarvestMonitor;

/**
 * Sends a frontier report to the {@link HarvestMonitor}.
 */
@SuppressWarnings({"serial"})
public class FrontierReportMessage extends HarvesterMessage {

    /** The id of the filter that generated this report. */
    private String filterId;

    /** The report. */
    private InMemoryFrontierReport report;

    /** The ID of the job, this message represents. */
    private Long jobID;

    /**
     * Builds a frontier report wrapper message.
     *
     * @param filter the filter that generated the report.
     * @param report the report to wrap.
     */
    public FrontierReportMessage(FrontierReportFilter filter, InMemoryFrontierReport report, Long jobID) {
        super(HarvestMonitor.HARVEST_MONITOR_CHANNEL_ID, Channels.getError());
        this.filterId = filter.getFilterId();
        this.report = report;
        this.jobID = jobID;
    }

    @Override
    public void accept(HarvesterMessageVisitor v) {
        v.visit(this);
    }

    /**
     * @return the filter id
     */
    public String getFilterId() {
        return filterId;
    }

    /**
     * @return the report
     */
    public InMemoryFrontierReport getReport() {
        return report;
    }

    public Long getJobID() {
        return jobID;
    }

}
