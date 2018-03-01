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

/**
 * Common interface for an Heritrix frontier report wrapper.
 */
public interface FrontierReport {

    /**
     * @return the jobName
     */
    String getJobName();

    /**
     * @return the creation timestamp
     */
    long getTimestamp();

    /**
     * Add a line to the report.
     *
     * @param line line to add.
     */
    void addLine(FrontierReportLine line);

    /**
     * Returns the line of the frontier report corresponding to the queue for the given domain name.
     *
     * @param domainName the domain name.
     * @return null if no queue for this domain name exists, otherwise the line of the frontier report corresponding to
     * the queue for the given domain name.
     */
    FrontierReportLine getLineForDomain(String domainName);

}
