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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.UrlValidator;
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
            f = Files.createTempFile(FileUtils.getTempDir().toPath(), jobid + "-files", ".txt").toFile();
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
               log.info("Collecting hadoop output from {} to {}", job.getJobOutputDir(), cacheFile.getAbsolutePath());
               try (OutputStream os = new FileOutputStream(cacheFile)) {
                   HadoopJobUtils.collectOutputLines(fileSystem, job.getJobOutputDir(), os);
               }
               log.info("Collected {} bytes output to {}", cacheFile.length(), cacheFile.getAbsolutePath());
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
            f = Files.createTempFile(FileUtils.getTempDir().toPath(), jobid + "-reports", ".cdx").toFile();
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
        return createSortedResultFile(urlsForDomainBatchJob);
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
            tempFile = Files.createTempFile(FileUtils.getTempDir().toPath(), "temp", uuidSuffix + ".txt").toFile();
            tempFile.deleteOnExit();
        } catch (IOException e) {
            throw new IOFailure("Unable to create temporary file", e);
        }
        return tempFile;
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
            return createSortedResultFile(crawlLogBatchJob);
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
        File cacheFile = getCrawlLogFromCacheOrHdfs(jobID);
        Pattern regexp = Pattern.compile(regex);
        log.info("Filtering cache file {} with regexp {}", cacheFile.getAbsolutePath(), regex);
        Predicate<String> stringPredicate = s -> regexp.matcher(s).matches();
        return getFilteredFile(cacheFile, stringPredicate);
    }

    //Called from .jsp
    public static File getCrawlLogLinesMatchingDomain(long jobID, String domain) {
        log.info("Finding matching crawl log lines for {} in job {}", domain, jobID);
        File cacheFile = getCrawlLogFromCacheOrHdfs(jobID);
        log.info("Finding matching crawl log lines for {} in job {} in file {}", domain, jobID, cacheFile.getAbsoluteFile());
        Predicate<String> domainFilteringPredicate = s -> lineMatchesDomain(s, domain);
        return getFilteredFile(cacheFile, domainFilteringPredicate);
    }

    private static File getFilteredFile(File cacheFile, Predicate<String> stringPredicate) {
        final String uuid = UUID.randomUUID().toString();
        File tempFile = createTempResultFile(uuid);
        log.info("Unsorted results in {}." + tempFile.getAbsolutePath());
        File sortedTempFile = createTempResultFile(uuid + "-sorted");
        log.info("Sorted results in {}.", sortedTempFile.getAbsolutePath());
        try (BufferedWriter writer = Files.newBufferedWriter(tempFile.toPath())) {
            try (BufferedReader reader = Files.newBufferedReader(cacheFile.toPath())) {
                String line;
                while ((line = reader.readLine()) != null ) {
                    Optional<String> result = Stream.of(line).filter(stringPredicate).findAny();
                    if (result.isPresent()) {
                        writer.write(result.get());
                        writer.newLine();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException("Error reading file " + cacheFile.getAbsolutePath(), e);
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing to file " + tempFile.getAbsolutePath());
        }
        FileUtils.sortCrawlLogOnTimestamp(tempFile, sortedTempFile);
        FileUtils.remove(tempFile);
        return sortedTempFile;
    }

    private static List<String> getMatchingStringsFromFile(File cacheFile,
            String regex) {
        List<String> matches = null;
        Pattern regexp = Pattern.compile(regex);
        try {
            matches = org.apache.commons.io.FileUtils.readLines(cacheFile).stream().filter(s -> regexp.matcher(s).matches() ).collect(
                    Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return matches;
    }

    /**
     * Helper method to get sorted File of crawllog lines.
     *
     * @param crawlLogLines The crawllog lines output from a job.
     * @return A File containing the sorted lines.
     */
    private static File createSortedResultFile(List<String> crawlLogLines) {
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
    private static File createSortedResultFile(FileBatchJob batchJob) {
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



    //TODO this is also a walking oom
    private static List<String> getMatchingDomainStringsFromFile(File cacheFile, String domain) {
        try {
            return org.apache.commons.io.FileUtils.readLines(cacheFile).stream()
                    .filter(line -> lineMatchesDomain(line, domain)).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static boolean lineMatchesDomain(String crawlLine, String domain) {
        int urlElement = 10;
        String urlS = crawlLine.split("\\s+")[urlElement];
        String[] schemes = {"http","https"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        try {
            URL url = null;
            if (urlValidator.isValid(urlS)) {
                url = new URL(urlS);
            } else if (urlValidator.isValid("http://" + urlS)) {
                url = new URL("http://" + urlS);
            }
            if (url.getHost().equals(domain) || url.getHost().endsWith("."+domain)) {
                log.debug("Domain {} found in crawlline {}", domain, crawlLine);
                return true;
            } else {
                log.debug("Domain {} not found in crawlline {}", domain, crawlLine);
                return false;
            }
        } catch (Exception e) {
            log.warn("Exception finding seed domain. No domain to match found in element {} of '{}' which is '{}'", urlElement, crawlLine, urlS, e);
            return false;
        }
    }



    private static File getCrawlLogFromCacheOrHdfs(long jobID) {
        File cacheFile = getCrawlLogCache(jobID);
        if (cacheFile.exists() && cacheFile.length() == 0) {
            log.info("Overwriting empty cache file {}.", cacheFile.getAbsolutePath());
        }
        if (cacheFile.length()==0 || !cacheFile.exists()) { //The || part of this is strictly unnecessary
            File outputFile = getCrawlLogUsingHadoop(jobID);
            try {
                log.info("Copying {} to {}", outputFile.getAbsolutePath(), cacheFile.getAbsolutePath());
                org.apache.commons.io.FileUtils.copyFile(outputFile, cacheFile);
                if (outputFile.delete()) {
                    log.info("Deleted {}", outputFile.getAbsolutePath());
                } else {
                    log.warn("Could not delete {}", outputFile.getAbsolutePath());
                }
            } catch (IOException e) {
                throw new RuntimeException((e));
            }
        }
        return cacheFile;
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
            File tempOutputFile1 = Files.createTempFile("unsorted_crawl", "log").toFile();
            File tempOutputFile2 = Files.createTempFile("sorted_crawl", "log").toFile();
            log.info("Collecting output from {} to {}", job.getJobOutputDir(), tempOutputFile1.getAbsolutePath());
            try (OutputStream os = new FileOutputStream(tempOutputFile1)) {
                HadoopJobUtils.collectOutputLines(fileSystem, job.getJobOutputDir(), os);
            }
            log.info("Collected {} bytes to {}", tempOutputFile1.length(), tempOutputFile1.getAbsolutePath());
            log.info("Sorting {} to {}", tempOutputFile1.getAbsolutePath(), tempOutputFile2.getAbsolutePath());
            FileUtils.sortCrawlLogOnTimestamp(tempOutputFile1, tempOutputFile2);
            log.info("Collected {} bytes to {}", tempOutputFile2.length(), tempOutputFile2.getAbsolutePath());
            if (tempOutputFile1.delete()) {
                log.info("Deleted {}", tempOutputFile1.getAbsolutePath());
            } else {
                log.warn("Could not delete {}", tempOutputFile1.getAbsolutePath());
            }
            return tempOutputFile2;
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
