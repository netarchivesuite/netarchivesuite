/* File:       $Id$
* Revision:    $Revision$
* Author:      $Author$
* Date:        $Date$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.harvester.harvesting.report;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.StopReason;
import dk.netarkivet.harvester.harvesting.HeritrixFiles;
import dk.netarkivet.harvester.harvesting.distribute.DomainStats;

/**
 * Base implementation for a harvest report.
 *
 */
public abstract class AbstractHarvestReport implements HarvestReport {

    /**
     * Strings found in the progress-statistics.log, used to devise the
     * default stop reason for domains.
     */
    public static enum ProgressStatisticsConstants {

        /**
         * String in crawl.log, that Heritrix writes
         *  as the last entry in the progress-statistics.log.
         */
        ORDERLY_FINISH("CRAWL ENDED"),

        /**
         * String which shows that the harvest was deliberately aborted from
         * the Heritrix GUI or forcibly stopped by the Netarchive Suite
         * software due to an inactivity timeout.
         */
        HARVEST_ABORTED("Ended by operator");

        private final String pattern;

        ProgressStatisticsConstants(String pattern) {
            this.pattern = pattern;
        }
    }

    /** Datastructure holding the domain-information contained in one
     *  harvest.
     */
    private final Map<String, DomainStats> domainstats =
        new HashMap<String, DomainStats>();

    // Used at construction tile only, does not need to be serialized.
    private transient HeritrixFiles heritrixFiles;

    /**
     * The default reason why we stopped harvesting this domain.
     * This value is set by looking for a CRAWL ENDED in the crawl.log.
     */
    private StopReason defaultStopReason;


    /**
     * Default constructor that does nothing.
     * The real construction is supposed to be done
     * in the subclasses by filling out the domainStats map with crawl results.
     */
    public AbstractHarvestReport() {

    }

    /**
     * Constructor from Heritrix report files. Subclasses might use a different
     * set of Heritrix reports.
     * @param files
     * @throws IOFailure
     */
    public AbstractHarvestReport(HeritrixFiles files) throws IOFailure {
        ArgumentNotValid.checkNotNull(files, "files");
        this.heritrixFiles = files;
        this.defaultStopReason = findDefaultStopReason(
                heritrixFiles.getProgressStatisticsLog());
        preProcess(heritrixFiles);
    }

    /**
     * Pre-processing happens when the report is built just at the end of the
     * crawl, before the ARC files upload.
     */
    @Override
    public abstract void preProcess(HeritrixFiles files);

    /**
     * Post-processing happens on the scheduler side when ARC files
     * have been uploaded.
     */
    @Override
    public abstract void postProcess(Job job);

    @Override
    public StopReason getDefaultStopReason() {
        return defaultStopReason;
    }

    /**
     * Returns the set of domain names
     * that are contained in hosts-report.txt
     * (i.e. host names mapped to domains)
     *
     * @return a Set of Strings
     */
    public final Set<String> getDomainNames() {
        return Collections.unmodifiableSet(domainstats.keySet());
    }

    /**
     * Get the number of objects found for the given domain.
     *
     * @param domainName A domain name (as given by getDomainNames())
     * @return How many objects were collected for that domain
     * @throws ArgumentNotValid if null or empty domainName
     */
    public final Long getObjectCount(String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        final DomainStats domainStats = domainstats.get(domainName);
        if (domainStats != null) {
            return domainStats.getObjectCount();
        }
        return null;
    }

    /**
     * Get the number of bytes downloaded for the given domain.
     *
     * @param domainName A domain name (as given by getDomainNames())
     * @return How many bytes were collected for that domain
     * @throws ArgumentNotValid if null or empty domainName
     */
    public final Long getByteCount(String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        final DomainStats domainStats = domainstats.get(domainName);
        if (domainStats != null) {
            return domainStats.getByteCount();
        }
        return null;
    }

    /**
     * Get the StopReason for the given domain.
     * @param domainName A domain name (as given by getDomainNames())
     * @return the StopReason for the given domain.
     * @throws ArgumentNotValid if null or empty domainName
     */
    public final StopReason getStopReason(String domainName) {
        ArgumentNotValid.checkNotNullOrEmpty(domainName, "domainName");
        final DomainStats domainStats = domainstats.get(domainName);
        if (domainStats != null) {
            return domainStats.getStopReason();
        }
        return null;
    }

    /**
     * @return the heritrixFiles
     */
    protected HeritrixFiles getHeritrixFiles() {
        return heritrixFiles;
    }

    /**
     * Attempts to get an already existing {@link DomainStats} object for that
     * domain, and if not found creates one with zero values.
     */
    protected DomainStats getOrCreateDomainStats(String domainName) {
        DomainStats dhi = domainstats.get(domainName);
        if (dhi == null) {
            dhi = new DomainStats(0L, 0L, defaultStopReason);
            domainstats.put(domainName, dhi);
        }

        return dhi;
    }

    /**
     * Find out whether we stopped normally in progress statistics log.
     * @param logFile A progress-statistics.log file.
     * @return StopReason.DOWNLOAD_COMPLETE for progress statistics ending with
     * CRAWL ENDED, StopReason.DOWNLOAD_UNFINISHED otherwise or if file does
     * not exist.
     * @throws ArgumentNotValid on null argument.
     */
    public static StopReason findDefaultStopReason(File logFile)
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(logFile, "File logFile");
        if (!logFile.exists()) {
            return StopReason.DOWNLOAD_UNFINISHED;
        }
        String lastLine = FileUtils.readLastLine(logFile);
        if (lastLine.contains(
                ProgressStatisticsConstants.ORDERLY_FINISH.pattern)) {
            if (lastLine.contains(
                    ProgressStatisticsConstants.HARVEST_ABORTED.pattern)) {
               return StopReason.DOWNLOAD_UNFINISHED;
            } else {
               return StopReason.DOWNLOAD_COMPLETE;
            }
        } else {
            return StopReason.DOWNLOAD_UNFINISHED;
        }
    }

}
