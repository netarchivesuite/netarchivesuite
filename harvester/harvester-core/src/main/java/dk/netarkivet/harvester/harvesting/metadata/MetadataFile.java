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
package dk.netarkivet.harvester.harvesting.metadata;

import java.io.File;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;

/**
 * Wraps information for an Heritrix file that should be stored in the metadata ARC.
 * <p>
 * Defines a natural order to sort them.
 */
public class MetadataFile implements Comparable<MetadataFile> {

    /**
     * The available type of metadata records.
     *
     * @author ngiraud
     */
    private enum MetadataType {
        /** Metadata records for the setup of Heritrix. */
        setup,
        /** Metadata records for the reports generated by Heritrix. */
        reports,
        /** Metadata records for the logs generated by Heritrix. */
        logs,
        /** Metadata records for the index. */
        index
    }

    /**
     * A string format that is used to build metadata URLs. Parameters are, in order :
     * <ol>
     * <li>The organization</li>
     * <li>the file type @see {@link MetadataType}</li>
     * <li>the file name</li>
     * <li>the Heritrix version</li>
     * <li>the harvest id</li>
     * <li>the job id</li>
     * </ol>
     */
    private static final String URL_FORMAT = "metadata://%s/crawl/%s/%s?heritrixVersion=%s&harvestid=%s&jobid=%s";
    /**
     * A pattern identifying a CDX metadata entry.
     *
     * @see dk.netarkivet.harvester.indexserver.CDXDataCache#CDXDataCache()
     */
    public static final String CDX_PATTERN = "metadata://[^/]*/crawl/index/cdx.*";

    /**
     * A pattern identifying the crawl log metadata entry.
     *
     * @see dk.netarkivet.harvester.indexserver.CrawlLogDataCache#CrawlLogDataCache()
     */
    public static final String CRAWL_LOG_PATTERN = "metadata://[^/]*/crawl/logs/crawl\\.log.*";

    /**
     * A pattern identifying the recover.gz log metadata entry.
     */
    public static final String RECOVER_LOG_PATTERN = "metadata://[^/]*/crawl/logs/recover\\.gz.*";

    /**
     * The pattern controlling which files in the crawl directory root should be stored in the metadata ARC.
     */
    public static final String HERITRIX_FILE_PATTERN = Settings.get(HarvesterSettings.METADATA_HERITRIX_FILE_PATTERN);

    /**
     * The pattern controlling which files in the crawl directory root should be stored in the metadata ARC as reports.
     */
    public static final String REPORT_FILE_PATTERN = Settings.get(HarvesterSettings.METADATA_REPORT_FILE_PATTERN);

    /**
     * The pattern controlling which files in the logs subdirectory of the crawl directory root should be stored in the
     * metadata ARC as log files.
     */
    public static final String LOG_FILE_PATTERN = Settings.get(HarvesterSettings.METADATA_LOG_FILE_PATTERN);

    /**
     * The name of a domain-specific Heritrix settings file (a.k.a. override).
     */
    public static final String DOMAIN_SETTINGS_FILE = "settings.xml";

    /**
     * The url representing the record in metadata-arc-file for this metadata file.
     */
    private String url;
    /** The Heritrix metadata file. */
    private File heritrixFile;
    /** The type of Heritrix metadata file. */
    private MetadataType type;

    /**
     * Creates a metadata file and finds which metadata type it belongs to. First the name of a heritrixfile is tested
     * against the reportfile pattern, then again the logfile pattern. If the name matches neither of these, it is
     * considered a setup file.
     */
    public MetadataFile(File heritrixFile, Long harvestId, Long jobId, String heritrixVersion) {
        this.heritrixFile = heritrixFile;

        type = MetadataType.setup;
        String name = heritrixFile.getName();
        if (name.matches(REPORT_FILE_PATTERN)) {
            type = MetadataType.reports;
            url = makeMetadataURL(MetadataType.reports, heritrixFile.getName(), harvestId, jobId, heritrixVersion);
        } else if (name.matches(LOG_FILE_PATTERN)) {
            type = MetadataType.logs;
            url = makeMetadataURL(MetadataType.logs, heritrixFile.getName(), harvestId, jobId, heritrixVersion);
        } else {
            url = makeMetadataURL(MetadataType.setup, heritrixFile.getName(), harvestId, jobId, heritrixVersion);
        }
    }

    /**
     * Creates a metadata file for a domain-specific override file.
     *
     * @param heritrixFile a given heritrix metadata file.
     * @param harvestId The harvestID that the job generating this file is part of.
     * @param jobId The Id of the job generating this file
     * @param heritrixVersion the version of Heritrix generating the file
     * @param domain The name of the domain, this metadata belongs to
     */
    public MetadataFile(File heritrixFile, Long harvestId, Long jobId, String heritrixVersion, String domain) {
        this(heritrixFile, harvestId, jobId, heritrixVersion);
        url += "&domain=" + domain;
    }

    /**
     * @return the metadata URL associated to this file.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the actual file.
     *
     * @return the actual file.
     */
    public File getHeritrixFile() {
        return heritrixFile;
    }

    /** First we compare the type ordinals, then the URLs. */
    public int compareTo(MetadataFile other) {
        Integer thisOrdinal = this.type.ordinal();
        Integer otherOrdinal = other.type.ordinal();

        int ordinalCompare = thisOrdinal.compareTo(otherOrdinal);
        if (ordinalCompare != 0) {
            return ordinalCompare;
        }
        return this.url.compareTo(other.url);
    }

    /**
     * Creates a metadata URL for this file. Metadata URLs are used to retrieve records in the metadata ARC file.
     *
     * @param theType The type of metadata for this file.
     * @param name The name of the file.
     * @param harvestID The harvestID that the job is part of.
     * @param jobID The jobID that this file belongs to.
     * @param heritrixVersion The version of Heritrix generating the file.
     * @return the metadata URL for this file
     */
    private String makeMetadataURL(MetadataType theType, String name, long harvestID, long jobID, String heritrixVersion) {
        return String.format(URL_FORMAT, Settings.get(CommonSettings.ORGANIZATION), theType.name(), name,
                heritrixVersion, Long.toString(harvestID), Long.toString(jobID));
    }

}
