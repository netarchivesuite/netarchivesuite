/* File:             $Id$
 * Revision:         $Revision$
 * Author:           $Author$
 * Date:             $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
package dk.netarkivet.harvester.scheduler.jobgen;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.DomainConfiguration;
import dk.netarkivet.harvester.datamodel.HarvestDefinition;
import dk.netarkivet.harvester.datamodel.Job;

/**
 * This interface defines the core methods that should be provided by a job generator.
 * It is designed to allow alternate implementations of job generation, depending on
 * curators and/or production engineers specific needs.
 */
public interface JobGenerator {

    /**
     * Generates a series of jobs for the given harvest definition.
     * Note that a job generator is expected to follow the singleton pattern,
     * so implementations of this method should be thread-safe.
     * @param harvest the harvest definition to process.
     * @return the number of jobs that were generated.
     */
    int generateJobs(HarvestDefinition harvest);

    /**
     * Tests if a configuration fits into this Job.
     * First tests if it's the right type of order-template and bytelimit, and
     * whether the bytelimit is right for the job.
     * The Job limits are compared against the configuration
     * estimates and if no limits are exceeded true is returned
     * otherwise false is returned.
     *
     * @param job the job being built.
     * @param cfg the configuration to check
     * @return true if adding the configuration to this Job does
     *         not exceed any of the Job limits.
     * @throws ArgumentNotValid if cfg is null
     */
    boolean canAccept(Job job, DomainConfiguration cfg);

}
