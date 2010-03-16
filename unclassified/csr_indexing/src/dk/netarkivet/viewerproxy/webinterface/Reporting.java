/*
* File:     $Id$
* Revision: $Revision$
* Author:   $Author$
* Date:     $Date$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.viewerproxy.webinterface;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dk.netarkivet.archive.arcrepository.bitpreservation.FileListJob;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.cdx.CDXRecord;
import dk.netarkivet.common.utils.cdx.ExtractCDXJob;
import dk.netarkivet.viewerproxy.reporting.HarvestedUrlsForDomainBatchJob;

/**
 * Methods for generating the batch results needed by the QA pages. 
 */
public class Reporting {
    /**
     * Utility class, do not initialise.
     */
    private Reporting() {}

    /**
     * Submit a batch job to list all files for a job, and report result in a
     * sorted list.
     * @param jobid The job to get files for.
     * @return A sorted list of files.
     * @throws ArgumentNotValid If jobid is 0 or negative. 
     * @throws IOFailure On trouble generating the file list
     */
    public static List<String> getFilesForJob(int jobid) {
        ArgumentNotValid.checkPositive(jobid, "jobid");
        FileBatchJob fileListJob = new FileListJob();
        fileListJob.processOnlyFilesMatching(jobid + "-.*\\.arc(\\.gz)?");
        File f;
        try {
            f = File.createTempFile(jobid + "-files", ".txt",
                                    FileUtils.getTempDir());
        } catch (IOException e) {
            throw new IOFailure("Could not create temorary file", e);
        }
        BatchStatus status
                = ArcRepositoryClientFactory.getViewerInstance().batch(
                fileListJob,
                Settings.get(CommonSettings.USE_REPLICA_ID));
        status.getResultFile().copyTo(f);
        List<String> lines = new ArrayList<String>(
                FileUtils.readListFromFile(f));
        FileUtils.remove(f);
        Collections.sort(lines);
        return lines;
    }

    /**
     * Submit a batch job to generate cdx for all metadata files for a job, and
     * report result in a list.
     * @param jobid The job to get cdx for.
     * @return A list of cdx records.
     * @throws ArgumentNotValid If jobid is 0 or negative.
     * @throws IOFailure On trouble generating the cdx
     */
    public static List<CDXRecord> getMetdataCDXRecordsForJob(int jobid) {
        ArgumentNotValid.checkPositive(jobid, "jobid");
        FileBatchJob cdxJob = new ExtractCDXJob(false);
        cdxJob.processOnlyFilesMatching(jobid + "-metadata-[0-9]+\\.arc(\\.gz)?");
        File f;
        try {
            f = File.createTempFile(jobid + "-reports", ".cdx",
                                    FileUtils.getTempDir());
        } catch (IOException e) {
            throw new IOFailure("Could not create temporary file", e);
        }
        BatchStatus status
                = ArcRepositoryClientFactory.getViewerInstance().batch(
                cdxJob, Settings.get(CommonSettings.USE_REPLICA_ID));
        status.getResultFile().copyTo(f);
        List<CDXRecord> records;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(f));
            records = new ArrayList<CDXRecord>();
            for (String line = reader.readLine();
                 line != null; line = reader.readLine()) {
                String[] parts = line.split("\\s+");
                CDXRecord record = new CDXRecord(parts);
                records.add(record);
            }
        } catch (IOException e) {
            throw new IOFailure("Unable to read results from file '" + f
                                + "'", e);
        } finally {
            FileUtils.remove(f);
        }
        return records;
    }

    /**
     * Submit a batch job to extract the part of a crawl log that is associated
     * with the given domain and job.
     * @param domain The domain to get crawl.log-lines for.
     * @param jobid The jobid to get the crawl.log-lines for.
     * @return A file containing the crawl.log lines. This file is temporary,
     * and should be deleted after use.
     * @throws ArgumentNotValid On negtaive jobids, or if domain is null or the
     * empty string.
     */
    public static File getCrawlLogForDomainInJob(String domain, int jobid) {
        ArgumentNotValid.checkPositive(jobid, "jobid");
        ArgumentNotValid.checkNotNullOrEmpty(domain, "String domain");
        FileBatchJob urlsForDomainBatchJob
                = new HarvestedUrlsForDomainBatchJob(domain);
        urlsForDomainBatchJob.processOnlyFilesMatching(
                jobid + "-metadata-[0-9]+\\.arc(\\.gz)?");
        File f;
        File fsorted;
        try {
            f = File.createTempFile(jobid + "-crawllog-" + domain,
                                    ".txt", FileUtils.getTempDir());
            f.deleteOnExit();
            fsorted = File.createTempFile(jobid + "-crawllog-" + domain + "-",
                                         "-sorted.txt", FileUtils.getTempDir());
            fsorted.deleteOnExit();
        } catch (IOException e) {
            throw new IOFailure("Unable to create temporary file", e);
        }
        BatchStatus status
                = ArcRepositoryClientFactory.getViewerInstance().batch(
                urlsForDomainBatchJob,
                Settings.get(CommonSettings.USE_REPLICA_ID));
        status.getResultFile().copyTo(f);
        FileUtils.sortCrawlLog(f, fsorted);
        FileUtils.remove(f);
        return fsorted;
    }
}
