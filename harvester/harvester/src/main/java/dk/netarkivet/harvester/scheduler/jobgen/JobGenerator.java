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
