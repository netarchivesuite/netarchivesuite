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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.datamodel.AliasInfo;

/**
 * Class used to carry metadata in DoOneCrawl messages, including the URL and mimetype necessary to write the metadata
 * to metadata (W)ARC files.
 */
@SuppressWarnings({"serial"})
public class MetadataEntry implements Serializable {

    /** The instance logger. */
    private static final Logger log = LoggerFactory.getLogger(MetadataEntry.class);
    /** The URL for this metadataEntry: Used as the unique identifier for this bit of metadata in the Netarchive. */
    private String url;
    /** The mimetype for this metadataEntry: Identifies which type of document this bit of metadata is. */
    private String mimeType;

    /** the metadata itself as byte array. */
    private byte[] data;

    /** Regular expression for a valid mimetype. */
    private static final String MIMETYPE_REGEXP = "\\w+/\\w+";
    /** The corresponding pattern for the regexp MIMETYPE_REGEXP. */
    private static final Pattern MIMETYPE_PATTERN = Pattern.compile(MIMETYPE_REGEXP);

    /**
     * The url should be valid according to RFC 2396 This URL_REGEXP is taken from org.archive.util.SURT v. 1.12 1:
     * scheme:// 2: userinfo (if present) 3: @ (if present) 4: host 5: :port 6: path
     */
    private static String URL_REGEXP = "^(\\w+://)(?:([-\\w\\.!~\\*'\\(\\)%;:&=+$,]+?)(@))?(\\S+?)(:\\d+)?(/\\settingsStructure*)?$";
    // 1 2 3 4 5 6
    /** The corresponding pattern for the regexp URL_REGEXP. */
    private static final Pattern URL_PATTERN = Pattern.compile(URL_REGEXP);

    /** Mimetype for metadata url. */
    private static final String MIMETYPE_TEXT_PLAIN = "text/plain";

    /** Suffix for both metadata URLs. */
    private static final String METADATA_URL_SUFFIX = "?majorversion=1&minorversion=0&harvestid=%s&harvestnum=%s&jobid=%s";

    /** Metadata URL template for aliases. */
    private static final String ALIAS_METADATA_URL_TEMPLATE = "metadata://%s/crawl/setup/aliases" + METADATA_URL_SUFFIX;

    /** Common template prefix for all deduplication metadata URLs. */
    private static final String DUPLICATEREDUCTION_METADATA_URL_PREFIX_TEMPLATE = "metadata://%s/crawl/setup/duplicatereductionjobs";

    /**
     * Constructor for this class.
     *
     * @param url the URL assigned to this metadata (needed for it to be searchable)
     * @param mimeType the mimeType for this metadata (normally text/plain or text/xml)
     * @param data the metadata itself
     * @throws ArgumentNotValid if arguments are null or empty strings, or if argument url is not valid URL or if
     * argument mimeType is not valid MimeType
     */
    public MetadataEntry(String url, String mimeType, String data) {
        ArgumentNotValid.checkNotNullOrEmpty(url, "url");
        ArgumentNotValid.checkNotNullOrEmpty(mimeType, "mimetype");
        ArgumentNotValid.checkNotNull(data, "data");
        setURL(url); // Ensures this is a valid url
        setMimetype(mimeType); // Ensures this is a valid mimetype
        this.mimeType = mimeType;
        this.data = data.getBytes();
    }

    /**
     * Generate a MetadataEntry from a list of AliasInfo objects (VERSION 2) Expired aliases is skipped by this method.
     *
     * @param aliases the list of aliases (possibly empty)
     * @param origHarvestDefinitionID The harvestdefinition that is behind the job with the given jobId
     * @param harvestNum The number of the harvest that the job with the given jobid belongs to
     * @param jobId The id of the Job, which this metadata belongs to
     * @return null, if the list if empty (or only consists of expired aliases), otherwise returns a MetadataEntry from
     * a list of AliasInfo objects containing unexpired aliases.
     */
    public static MetadataEntry makeAliasMetadataEntry(List<AliasInfo> aliases, Long origHarvestDefinitionID,
            int harvestNum, Long jobId) {
        ArgumentNotValid.checkNotNull(aliases, "aliases");
        ArgumentNotValid.checkNotNull(origHarvestDefinitionID, "Long origHarvestDefinitionID");
        ArgumentNotValid.checkNotNegative(harvestNum, "int harvestNum");
        ArgumentNotValid.checkNotNull(jobId, "Long jobId");
        if (aliases.isEmpty()) {
            return null;
        }
        // Remove any expired aliases from the aliases collection
        List<AliasInfo> nonExpiredAliases = new ArrayList<AliasInfo>();
        for (AliasInfo alias : aliases) {
            if (!alias.isExpired()) {
                nonExpiredAliases.add(alias);
            }
        }
        if (nonExpiredAliases.isEmpty()) {
            log.warn("All the aliases for the domains in job {} are expired. Aborting creation of an alias MetadataEntry", jobId);
            return null;
        }

        String organization = Settings.get(CommonSettings.ORGANIZATION);
        // construct metadata-URL for AliasMetadataEntry
        String metadataUrl = String.format(ALIAS_METADATA_URL_TEMPLATE, organization, origHarvestDefinitionID,
                harvestNum, jobId);

        StringBuffer sb = new StringBuffer();
        for (AliasInfo alias : nonExpiredAliases) {
            sb.append(alias.getDomain()).append(" is an alias for ").append(alias.getAliasOf()).append("\n");
        }
        return new MetadataEntry(metadataUrl, MIMETYPE_TEXT_PLAIN, sb.toString());
    }

    /**
     * Generate a MetadataEntry from a list of job ids for duplicate reduction.
     *
     * @param jobIDsForDuplicateReduction the list of jobids (possibly empty)
     * @param origHarvestDefinitionID The harvestdefinition that is behind the job with the given jobId
     * @param harvestNum The number of the harvest that the job with the given jobid belongs to
     * @param jobId The id of the Job, which this metadata belongs to
     * @return null, if the list is empty, otherwise returns a MetadataEntry from the list of jobids.
     */
    public static MetadataEntry makeDuplicateReductionMetadataEntry(List<Long> jobIDsForDuplicateReduction,
            Long origHarvestDefinitionID, int harvestNum, Long jobId) {
        ArgumentNotValid.checkNotNull(jobIDsForDuplicateReduction, "List<Long> jobIDsForDuplicateReduction");
        ArgumentNotValid.checkNotNull(origHarvestDefinitionID, "Long origHarvestDefinitionID");
        ArgumentNotValid.checkNotNegative(harvestNum, "int harvestNum");
        ArgumentNotValid.checkNotNull(jobId, "Long jobId");

        String organization = Settings.get(CommonSettings.ORGANIZATION);
        // construct a metadata-URL for this MetadataEntry
        String metadataUrl = String.format(DUPLICATEREDUCTION_METADATA_URL_PREFIX_TEMPLATE + METADATA_URL_SUFFIX,
                organization, origHarvestDefinitionID, harvestNum, jobId);

        return new MetadataEntry(metadataUrl, MIMETYPE_TEXT_PLAIN,
                StringUtils.conjoin(",", jobIDsForDuplicateReduction));
    }

    /**
     * @return Returns the data.
     */
    public byte[] getData() {
        return data;
    }

    /**
     * @return Returns the mimeType.
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Set the mimetype for this object.
     *
     * @param mimetype a given mimetype
     * @throws ArgumentNotValid if the mimetype is not valid
     */
    private void setMimetype(String mimetype) {
        if (isMimetypeValid(mimetype)) {
            this.mimeType = mimetype;
        } else {
            throw new ArgumentNotValid("The given MimeType is not valid: " + mimetype);
        }
    }

    /**
     * @return Returns the URL
     */
    public String getURL() {
        return url;
    }

    /**
     * Set the url for this object.
     *
     * @param aUrl a given URL
     * @throws ArgumentNotValid if the URL is not valid
     */
    private void setURL(String aUrl) {
        if (isURLValid(aUrl)) {
            this.url = aUrl;
        } else {
            throw new ArgumentNotValid("The given URL is not valid: " + aUrl);
        }
    }

    /**
     * Method needed to de-serializable an object of this class.
     *
     * @param s the given ObjectInputStream
     * @throws ClassNotFoundException If the class of the serialized object could not be found
     * @throws IOException If an I/O error occurred while reading the serialized object
     */
    private void readObject(ObjectInputStream s) throws ClassNotFoundException, IOException {
        s.defaultReadObject();
    }

    /**
     * Method needed to serializable an object of this class.
     *
     * @param s the given ObjectOutputStream
     * @throws IOException If an I/O error occurred while writing to the outputstream
     */
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
    }

    /**
     * Utility method for testing the validity of the mimetype. We need do this, because the ARCWriter does not do this
     * check properly
     *
     * @param mimetype the given mimetype
     * @return true, if the mimetype match the pattern: \\w+/\\w+
     */
    private static boolean isMimetypeValid(String mimetype) {
        return MIMETYPE_PATTERN.matcher(mimetype).matches();
    }

    /**
     * Utility method for testing the validity of the URL. We need do this, because the ARCWriter does not do this check
     * properly.
     *
     * @param url the given URL
     * @return true, if the URL match the pattern:
     */
    private static boolean isURLValid(String url) {
        return URL_PATTERN.matcher(url).matches();
    }

    /**
     * Checks, if this is a duplicate reduction MetadataEntry.
     *
     * @return true, if this is a duplicate reduction MetadataEntry, otherwise false.
     */
    public boolean isDuplicateReductionMetadataEntry() {
        return this.getURL().startsWith(MetadataEntry.getDuplicatereductionMetadataURLPrefix());
    }

    private static String getDuplicatereductionMetadataURLPrefix() {
        String organization = Settings.get(CommonSettings.ORGANIZATION);
        return String.format(DUPLICATEREDUCTION_METADATA_URL_PREFIX_TEMPLATE, organization);
    }

    /**
     * @return a string representation of this object
     */
    public String toString() {
        return "URL= " + getURL() + " ; mimetype= " + getMimeType() + " ; data= " + new String(getData());
    }

    /**
     * Store a list of metadata entries to disk.
     *
     * @param metadata the given metadata
     * @param destinationDir the directory to store the metadata.
     */
    public static void storeMetadataToDisk(List<MetadataEntry> metadata, File destinationDir) {
        try {
            for (MetadataEntry m : metadata) {
                File mFile = new File(destinationDir, UUID.randomUUID().toString() + ".ser");
                FileOutputStream fos = new FileOutputStream(mFile);
                ObjectOutputStream out = new ObjectOutputStream(fos);
                out.writeObject(m);
                fos.close();
            }
        } catch (IOException e) {
            throw new IOFailure("Unable to store metadata temporarily in directory ' "
                    + destinationDir.getAbsolutePath() + "'", e);
        }
    }

    /**
     * Retrieve a list of serialized metadata entries on disk.
     *
     * @param sourceDir the directory where the metadata is stored.
     * @return the list of deserialized MetadataEntry object.
     */
    public static List<MetadataEntry> getMetadataFromDisk(File sourceDir) {
        List<MetadataEntry> metadata = new ArrayList<MetadataEntry>();
        FilenameFilter filter = new FilenameFilter() {

            @Override
            public boolean accept(File dir, String name) {
                if (name.endsWith(".ser")) {
                    return true;
                }
                return false;
            }
        };

        for (String file : sourceDir.list(filter)) {
            File metadataEntryFile = new File(sourceDir, file);
            try {
                FileInputStream fileIn = new FileInputStream(metadataEntryFile);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                MetadataEntry o = (MetadataEntry) in.readObject();
                metadata.add(o);
                in.close();
                fileIn.close();
            } catch (IOException e) {
                throw new IOFailure("Unable to read the serialized metadata", e);
            } catch (ClassNotFoundException e) {
                throw new IllegalState("Unable to read the serialized metadata", e);
            }
        }
        return metadata;
    }

}
