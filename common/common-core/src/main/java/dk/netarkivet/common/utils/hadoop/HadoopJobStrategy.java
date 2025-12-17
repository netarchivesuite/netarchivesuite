package dk.netarkivet.common.utils.hadoop;

import java.util.UUID;

import org.apache.hadoop.fs.Path;

/**
 * Interface for a HadoopJob's strategy of how to perform the job.
 */
public interface HadoopJobStrategy {

    /**
     * Runs a Hadoop job (HadoopJobTool) according to the specification of the used strategy.
     *
     * @param jobInputFile The Path specifying the job's input file.
     * @param jobOutputDir The Path specifying the job's output directory.
     * @return An exit code for the job.
     */
    void runJob(Path jobInputFile, Path jobOutputDir) throws HadoopException;

    /**
     * Create the job input file with name from a uuid.
     *
     * @param uuid The UUID to create a unique name from.
     * @return Path specifying where the input file is located.
     */
    Path createJobInputFile(UUID uuid);

    /**
     * Create the job output directory with name from a uuid.
     *
     * @param uuid The UUID to create a unique name from.
     * @return Path specifying where the output directory is located.
     */
    Path createJobOutputDir(UUID uuid);

    /**
     * Return a string specifying which kind of job is being run.
     *
     * @return String specifying the job's type.
     */
    String getJobType();
}
