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
package dk.netarkivet.harvester.harvesting.frontier;

import java.io.Serializable;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Base abstract implementation of an Heritrix 1 frontier report wrapper.
 */
@SuppressWarnings({"serial"})
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
     * Default empty constructor.
     */
    AbstractFrontierReport() {

    }

    /**
     * Initializes an empty Heritrix frontier report wrapper object.
     *
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

}
