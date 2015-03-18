/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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

package dk.netarkivet.harvester.heritrix3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.ArcRepositoryClientFactory;
import dk.netarkivet.common.distribute.arcrepository.BatchStatus;
import dk.netarkivet.common.distribute.arcrepository.BitarchiveRecord;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.cdx.ArchiveExtractCDXJob;
import dk.netarkivet.common.utils.cdx.CDXRecord;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.harvesting.metadata.MetadataFile;

/**
 * This class handles all the things in a single harvest that are not related directly related either to launching
 * Heritrix3 or to handling JMS messages.
 */
public class HarvestController {

    /** The instance logger. */
    private static final Logger log = LoggerFactory.getLogger(HarvestJob.class);

    /**
     * This method attempts to retrieve the Heritrix recover log from the job which this job tries to continue. If
     * successful, the Heritrix template is updated accordingly.
     *
     * @param job The harvest Job object containing various harvest setup data.
     * @param files Heritrix files related to this harvestjob.
     */
    private static void tryToRetrieveRecoverLog(Job job, Heritrix3Files files) {
        Long previousJob = job.getContinuationOf();
        List<CDXRecord> metaCDXes = null;
        try {
            metaCDXes = getMetadataCDXRecordsForJob(previousJob);
        } catch (IOFailure e) {
            log.debug("Failed to retrive CDX of metatadata records. "
                    + "Maybe the metadata arcfile for job {} does not exist in repository", previousJob, e);
        }

        CDXRecord recoverlogCDX = null;
        if (metaCDXes != null) {
            for (CDXRecord cdx : metaCDXes) {
                if (cdx.getURL().matches(MetadataFile.RECOVER_LOG_PATTERN)) {
                    recoverlogCDX = cdx;
                }
            }
            if (recoverlogCDX == null) {
                log.debug("A recover.gz log file was not found in metadata-arcfile");
            } else {
                log.debug("recover.gz log found in metadata-arcfile");
            }
        }

        BitarchiveRecord br = null;
        if (recoverlogCDX != null) { // Retrieve recover.gz from metadata.arc file
            br = ArcRepositoryClientFactory.getViewerInstance().get(recoverlogCDX.getArcfile(),
                    recoverlogCDX.getOffset());
            if (br != null) {
                log.debug("recover.gz log retrieved from metadata-arcfile");
                if (files.writeRecoverBackupfile(br.getData())) {
                    // modify order.xml, so Heritrix recover-path points
                    // to the retrieved recoverlog
                    insertHeritrixRecoverPathInOrderXML(job, files);
                } else {
                    log.warn("Failed to retrieve and write recoverlog to disk.");
                }
            } else {
                log.debug("recover.gz log not retrieved from metadata-arcfile");
            }
        }
    }

    /**
     * Insert the correct recoverpath in the order.xml for the given harvestjob.
     *
     * @param job A harvestjob
     * @param files Heritrix files related to this harvestjob.
     */
    private static void insertHeritrixRecoverPathInOrderXML(Job job, Heritrix3Files files) {    	
        HeritrixTemplate temp = job.getOrderXMLdoc(); 
    	temp.setRecoverlogNode(files.getRecoverBackupGzFile());
    	job.setOrderXMLDoc(temp); // Update template associated with job
    }

    /**
     * Submit a batch job to generate cdx for all metadata files for a job, and report result in a list.
     *
     * @param jobid The job to get cdx for.
     * @return A list of cdx records.
     * @throws ArgumentNotValid If jobid is 0 or negative.
     * @throws IOFailure On trouble generating the cdx
     */
    public static List<CDXRecord> getMetadataCDXRecordsForJob(long jobid) {
        ArgumentNotValid.checkPositive(jobid, "jobid");
        FileBatchJob cdxJob = new ArchiveExtractCDXJob(false);
        cdxJob.processOnlyFilesMatching(jobid + "-metadata-[0-9]+\\.(w)?arc(\\.gz)?");
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

}
