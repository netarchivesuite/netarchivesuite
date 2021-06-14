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
package dk.netarkivet.viewerproxy.webinterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.common.utils.batch.ArchiveBatchFilter;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.FileListJob;
import dk.netarkivet.common.utils.cdx.ArchiveExtractCDXJob;
import dk.netarkivet.common.utils.cdx.CDXRecord;
import dk.netarkivet.common.utils.hadoop.HadoopJob;
import dk.netarkivet.common.utils.hadoop.HadoopJobStrategy;
import dk.netarkivet.common.utils.hadoop.HadoopJobUtils;
import dk.netarkivet.common.utils.service.FileResolver;
import dk.netarkivet.viewerproxy.webinterface.hadoop.CrawlLogExtractionStrategy;
import dk.netarkivet.viewerproxy.webinterface.hadoop.MetadataCDXExtractionStrategy;

/**
 * Methods for generating the batch results needed by the QA pages.
 */
@SuppressWarnings({"serial"})
public class Reporting {
    /**
     * Utility class, do not initialise.
     */
    private Reporting() {
    }

    /** Logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(Reporting.class);

    /** The suffix for the data arc/warc files produced by Heritrix. 
     * TODO This should be configurable 
     */
    static final String archivefile_suffix = ".*\\.(w)?arc(\\.gz)?";

    /** The suffix for the data arc/warc metadata file created by NetarchiveSuite. 
     * should probably replaced by: Settings.get(CommonSettings.METADATAFILE_REGEX_SUFFIX);
     */
    static final String metadatafile_suffix = "-metadata-[0-9]+\\.(w)?arc(\\.gz)?";

    /**
     * Retrieve a list of all files uploaded for a given harvest job. For installations that use batch, this is
     * done via a batch job, and for hadoop-based implementations it is done via an implementation of
     * dk.netarkivet.common.utils.service.FileResolver
     * @param jobid the job for which files are required
     * @param harvestprefix the prefix for the (w)arc datafiles for this job as determined by the implementation of
     *                      ArchiveFileNaming used in the installation
     * @return a list of filenames
     */
    public static List<String> getFilesForJob(long jobid, String harvestprefix) {
        if (!Settings.getBoolean(CommonSettings.USE_BITMAG_HADOOP_BACKEND)) {
            return getFilesForJobBatch(jobid, harvestprefix);
        } else {
            return getFilesForJobFileResolver(jobid, harvestprefix);
        }
    }

    private static List<String> getFilesForJobFileResolver(long jobid, String harvestprefix) {
        FileResolver fileResolver = SettingsFactory.getInstance(CommonSettings.FILE_RESOLVER_CLASS);
        String metadataFilePatternForJobId = getMetadataFilePatternForJobId(jobid);
        log.debug("Looking for metadata files matching {}.", metadataFilePatternForJobId);
        List<Path> metadataPaths =  fileResolver.getPaths(Pattern.compile(metadataFilePatternForJobId));
        log.debug("Initial found metadata files: {}", metadataPaths);
        String archiveFilePatternForJobId = harvestprefix + archivefile_suffix;
        log.debug("Looking for archive files matching {}.", archiveFilePatternForJobId);
        List<Path> archivePaths = fileResolver.getPaths(Pattern.compile(archiveFilePatternForJobId));
        log.debug("Initial found archive files {}.", archivePaths);
        //What is this? When using getPaths() with a pattern we get all files in the installation matching the pattern.
        //When using getPath() with an exact filename we include filtering by collectionId. This should only make a
        //difference in the case of test installations where we have multiple collections with overlapping filenames. It's
        //irritating to have to do this but the overhead should be low.
        List<String> filteredFiles = Stream.concat(metadataPaths.stream(), archivePaths.stream())
                .filter(path -> fileResolver.getPath(path.getFileName().toString())!=null)
                .map(path -> path.getFileName().toString()).distinct().sorted().collect(Collectors.toList());
        log.debug("After filtering by collection we have the following files: {}", filteredFiles);
        return filteredFiles;
    }

    private static List<String> getFilesForJobBatch(long jobid, String harvestprefix) {
        ArgumentNotValid.checkPositive(jobid, "jobid");
        FileBatchJob fileListJob = new FileListJob();
        List<String> acceptedPatterns = new ArrayList<String>();
        acceptedPatterns.add(getMetadataFilePatternForJobId(jobid));
        acceptedPatterns.add(harvestprefix + archivefile_suffix);
        fileListJob.processOnlyFilesMatching(acceptedPatterns);
        File f;
        try {
            f = File.createTempFile(jobid + "-files", ".txt", FileUtils.getTempDir());
        } catch (IOException e) {
            throw new IOFailure("Could not create temporary file", e);
        }
        BatchStatus status = ArcRepositoryClientFactory.getViewerInstance().batch(fileListJob,
                Settings.get(CommonSettings.USE_REPLICA_ID));
        status.getResultFile().copyTo(f);
        List<String> lines = new ArrayList<String>(FileUtils.readListFromFile(f));
        FileUtils.remove(f);
        Set<String> linesAsSet = new HashSet<String>();
        linesAsSet.addAll(lines);
        lines = new ArrayList<String>();
        lines.addAll(linesAsSet);
        Collections.sort(lines);
        return lines;
    }

    /**
     * Depending on settings, submits either a Hadoop job or batch job to generate cdx for all metadata files for a job,
     * and returns the results in a list.
     *
     * @param jobid The job to get cdx for.
     * @throws ArgumentNotValid If jobid is 0 or negative.
     * @return A list of cdx records.
     */
    public static List<CDXRecord> getMetadataCDXRecordsForJob(long jobid) {
        ArgumentNotValid.checkPositive(jobid, "jobid");
        if (Settings.getBoolean(CommonSettings.USE_BITMAG_HADOOP_BACKEND)) {
            return getRecordsUsingHadoop(jobid);
        } else {
            return getRecordsUsingBatch(jobid);
        }
    }

    private static File getCDXCacheFile(long jobid) {
        String cacheDir = Settings.get(CommonSettings.METADATA_CACHE);
        String cdxcache = "cdxcache";
        File cdxdir = new File(new File(cacheDir), cdxcache);
        cdxdir.mkdirs();
        File cacheFile = new File(cdxdir, "" + jobid);
        return cacheFile;
    }

    private static List<CDXRecord> getCachedCDXRecords(long jobid) {
        List<String> cdxLines;
        File cacheFile = getCDXCacheFile(jobid);
        if (cacheFile.exists() && cacheFile.length() != 0) {
            try {
                cdxLines = org.apache.commons.io.FileUtils.readLines(cacheFile);
                return HadoopJobUtils.getCDXRecordListFromCDXLines(cdxLines);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            return null;
        }
    }

    /**
     * Submits a Hadoop job to generate cdx for all metadata files for a jobID and returns the resulting list of records.
     *
     * @param jobid The job to get CDX for.
     * @return A list of CDX records.
     */
    private static List<CDXRecord> getRecordsUsingHadoop(long jobid) {
       log.info("Getting records for jobid {}.", jobid);
       List<CDXRecord> cdxRecords = getCachedCDXRecords(jobid);
       if (cdxRecords != null) {
           log.info("Found {} cached records for jobid {}.", cdxRecords.size(), jobid);
           return cdxRecords;
       } else {
           File cacheFile = getCDXCacheFile(jobid);
           log.info("Cached records not found for jobid {} so fetching them to {} via hadoop.", jobid, cacheFile.getAbsolutePath());
           Configuration hadoopConf = HadoopJobUtils.getConf();
           String metadataFileSearchPattern = getMetadataFilePatternForJobId(jobid);

           try (FileSystem fileSystem = FileSystem.newInstance(hadoopConf)) {
               HadoopJobStrategy jobStrategy = new MetadataCDXExtractionStrategy(jobid, fileSystem);
               HadoopJob job = new HadoopJob(jobid, jobStrategy);
               job.processOnlyFilesMatching(metadataFileSearchPattern);
               job.prepareJobInputOutput(fileSystem);
               job.run();

               try {
                   List<String> cdxLines = HadoopJobUtils.collectOutputLines(fileSystem, job.getJobOutputDir());
                   org.apache.commons.io.FileUtils.writeLines(cacheFile, cdxLines);
               } catch (IOException e) {
                   log.error("Failed getting CDX lines output for Hadoop job with ID: {}", jobid);
                   throw new IOFailure("Failed getting " + job.getJobType() + " job results");
               }
               return getCachedCDXRecords(jobid);
           } catch (IOException e) {
               log.error("Error instantiating Hadoop filesystem for job {}.", jobid, e);
               throw new IOFailure("Failed instantiating Hadoop filesystem.");
           }
       }
    }

    /**
     * Submit a job to generate cdx for all metadata files for a job, and report result in a list.
     *
     * @param jobid The job to get cdx for.
     * @return A list of cdx records.
     * @throws IOFailure On trouble generating the cdx
     */
    private static List<CDXRecord> getRecordsUsingBatch(long jobid) {
        FileBatchJob cdxJob = new ArchiveExtractCDXJob(false) {
            @Override
            public ArchiveBatchFilter getFilter() {
                return ArchiveBatchFilter.EXCLUDE_NON_WARCINFO_RECORDS;
            }
        };
        String metadataFileSearchPattern = getMetadataFilePatternForJobId(jobid);
        cdxJob.processOnlyFilesMatching(metadataFileSearchPattern);

        File f;
        try {
            f = File.createTempFile(jobid + "-reports", ".cdx", FileUtils.getTempDir());
        } catch (IOException e) {
            throw new IOFailure("Could not create temporary file", e);
        }
        BatchStatus status = ArcRepositoryClientFactory.getViewerInstance().batch(cdxJob,
                Settings.get(CommonSettings.USE_REPLICA_ID));
        status.getResultFile().copyTo(f);
        List<CDXRecord> records;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(f));
            records = new ArrayList<CDXRecord>();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                System.out.println(line);
                String[] parts = line.split("\\s+");
                CDXRecord record = new CDXRecord(parts);
                records.add(record);
            }
        } catch (IOException e) {
            throw new IOFailure("Unable to read results from file '" + f + "'", e);
        } finally {
            IOUtils.closeQuietly(reader);
            FileUtils.remove(f);
        }
        return records;
    }

    /**
     * Submit a batch job to extract the part of a crawl log that is associated with the given domain and job.
     *
     * @param domain The domain to get crawl.log-lines for.
     * @param jobid The jobid to get the crawl.log-lines for.
     * @return A file containing the crawl.log lines. This file is temporary, and should be deleted after use.
     * @throws ArgumentNotValid On negative jobids, or if domain is null or the empty string.
     */
    public static File getCrawlLogForDomainInJob(String domain, long jobid) {
        ArgumentNotValid.checkPositive(jobid, "jobid");
        ArgumentNotValid.checkNotNullOrEmpty(domain, "String domain");
        FileBatchJob urlsForDomainBatchJob = new HarvestedUrlsForDomainBatchJob(domain);
        urlsForDomainBatchJob.processOnlyFilesMatching(getMetadataFilePatternForJobId(jobid));
        return getResultFile(urlsForDomainBatchJob);
    }

    /**
     * Helper method to create temp file for storage of result
     *
     * @param uuidSuffix Suffix of temp file.
     * @return a new temp File.
     */
    private static File createTempResultFile(String uuidSuffix) {
        File tempFile;
        try {
            tempFile = File.createTempFile("temp", uuidSuffix + ".txt", FileUtils.getTempDir());
            tempFile.deleteOnExit();
        } catch (IOException e) {
            throw new IOFailure("Unable to create temporary file", e);
        }
        return tempFile;
    }

    /**
     * Helper method to get sorted File of crawllog lines.
     *
     * @param crawlLogLines The crawllog lines output from a job.
     * @return A File containing the sorted lines.
     */
    private static File getResultFile(List<String> crawlLogLines) {
        final String uuid = UUID.randomUUID().toString();
        File tempFile = createTempResultFile(uuid);
        File sortedTempFile = createTempResultFile(uuid + "-sorted");
        FileUtils.writeCollectionToFile(tempFile, crawlLogLines);
        FileUtils.sortCrawlLogOnTimestamp(tempFile, sortedTempFile);
        FileUtils.remove(tempFile);
        return sortedTempFile;
    }

    /**
     * Helper method to get result from a batchjob.
     *
     * @param batchJob a certain FileBatchJob
     * @return a file with the result.
     */
    private static File getResultFile(FileBatchJob batchJob) {
        final String uuid = UUID.randomUUID().toString();
        File tempFile = createTempResultFile(uuid);
        File sortedTempFile = createTempResultFile(uuid);
        BatchStatus status = ArcRepositoryClientFactory.getViewerInstance().batch(batchJob,
                Settings.get(CommonSettings.USE_REPLICA_ID));
        status.getResultFile().copyTo(tempFile);
        FileUtils.sortCrawlLogOnTimestamp(tempFile, sortedTempFile);
        FileUtils.remove(tempFile);
        return sortedTempFile;
    }

    /**
     * Return any crawllog lines for a given jobid matching the given regular expression.
     *
     * @param jobid The jobid
     * @param regexp A regular expression
     * @return a File with the matching lines.
     */
    public static File getCrawlLoglinesMatchingRegexp(long jobid, String regexp) {
        ArgumentNotValid.checkPositive(jobid, "jobid");
        ArgumentNotValid.checkNotNullOrEmpty(regexp, "String regexp");
        if (Settings.getBoolean(CommonSettings.USE_BITMAG_HADOOP_BACKEND)) {
            return getCrawlLogLinesUsingHadoop(jobid, regexp);
        } else {
            FileBatchJob crawlLogBatchJob = new CrawlLogLinesMatchingRegexp(regexp);
            crawlLogBatchJob.processOnlyFilesMatching(getMetadataFilePatternForJobId(jobid));
            return getResultFile(crawlLogBatchJob);
        }
    }

    private static File getCrawlLogCache(long jobid) {
        String cacheDir = Settings.get(CommonSettings.METADATA_CACHE);
        String crawllog_cache = "crawllog_cache";
        File crawllog_dir = new File(new File(cacheDir), crawllog_cache);
        crawllog_dir.mkdirs();
        File cacheFile = new File(crawllog_dir, "" + jobid);
        return cacheFile;
    }



    /**
     * Using Hadoop, gets crawllog lines for a given jobID matching a given regular expression.
     *
     * @param jobID The ID for the job.
     * @param regex The regular expression specifying files to process.
     * @return a File with the matching lines.
     */
    private static File getCrawlLogLinesUsingHadoop(long jobID, String regex) {
        File cacheFile = getCrawlLogCache(jobID);
        if (cacheFile.exists() && cacheFile.length() == 0) {
            log.info("Overwriting empty cache file {}.", cacheFile.getAbsolutePath());
        }
        if (cacheFile.length()==0 || !cacheFile.exists()) { //The || part of this is strictly unnecessary
            File outputFile = getCrawlLogUsingHadoop(jobID);
            try {
                org.apache.commons.io.FileUtils.copyFile(outputFile, cacheFile);
            } catch (IOException e) {
                throw new RuntimeException((e));
            }
        }
        List<String> matches = null;
        Pattern regexp = Pattern.compile(regex);
        try {
            matches = org.apache.commons.io.FileUtils.readLines(cacheFile).stream().filter(s -> regexp.matcher(s).matches() ).collect(
                    Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return getResultFile(matches);
    }

    private static File getCrawlLogUsingHadoop(long jobID) {
        String metadataFileSearchPattern = getMetadataFilePatternForJobId(jobID);
        Configuration hadoopConf = HadoopJobUtils.getConf();
        hadoopConf.setPattern("regex", Pattern.compile(".*"));
        try (FileSystem fileSystem = FileSystem.newInstance(hadoopConf)) {
            HadoopJobStrategy jobStrategy = new CrawlLogExtractionStrategy(jobID, fileSystem);
            HadoopJob job = new HadoopJob(jobID, jobStrategy);
            job.processOnlyFilesMatching(metadataFileSearchPattern);
            job.prepareJobInputOutput(fileSystem);
            job.run();
            List<String> crawlLogLines;
            try {
                crawlLogLines = HadoopJobUtils.collectOutputLines(fileSystem, job.getJobOutputDir());
            } catch (IOException e) {
                log.error("Failed getting crawl log lines output for job with ID: {}", jobID);
                throw new IOFailure("Failed getting " + job.getJobType() + " job results");
            }
            return getResultFile(crawlLogLines);
        } catch (IOException e) {
            log.error("Error instantiating Hadoop filesystem for job {}.", jobID, e);
            throw new IOFailure("Failed instantiating Hadoop filesystem.");
        }
    }
    
    /**
     * Construct the correct metadatafilepattern for a given jobID.
     * @param jobid a given harvest jobID
     * @return metadatafilePattern for the given jobid
     */
    private static String getMetadataFilePatternForJobId(long jobid) {
    	return "(.*-)?" + jobid + "(-.*)?" + metadatafile_suffix;
    }
}
