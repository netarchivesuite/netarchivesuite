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
package dk.netarkivet.harvester.harvesting.frontier;

import java.io.Serializable;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Base abstract implementation of an Heritrix frontier report wrapper.
 *
 */
abstract class AbstractFrontierReport implements FrontierReport, Serializable {

    /**
     * The Heritrix job name.
     */
    private String jobName;

    /**
     * The report generation timestamp.
     */
    private long timestamp;

    /**
     * Default empty contrcutor.
     */
    AbstractFrontierReport() {

    }

    /**
     * Initializes an empty Heritrix frontier report wrapper object.
     * @param jobName the Heritrix job name
     */
    public AbstractFrontierReport(String jobName) {
        ArgumentNotValid.checkNotNullOrEmpty(jobName, "jobName");
        this.jobName = jobName;
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String getJobName() {
        return jobName;
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @param jobName the jobName to set
     */
    protected void setJobName(String jobName) {
        this.jobName = jobName;
    }

    /**
     * @param timestamp the timestamp to set
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public abstract void addLine(FrontierReportLine line);

    @Override
    public abstract FrontierReportLine getLineForDomain(String domainName);

    @Override
    public abstract FrontierReportLine[] getRetiredQueues(int maxSize);

    @Override
    public abstract FrontierReportLine[] getExhaustedQueues(int maxSize);

}
