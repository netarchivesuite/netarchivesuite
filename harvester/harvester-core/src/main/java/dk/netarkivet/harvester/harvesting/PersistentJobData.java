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
package dk.netarkivet.harvester.harvesting;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SimpleXml;
import dk.netarkivet.common.utils.archive.ArchiveDateConverter;
import dk.netarkivet.harvester.HarvesterSettings;
import dk.netarkivet.harvester.datamodel.H3HeritrixTemplate;
import dk.netarkivet.harvester.datamodel.H3HeritrixTemplate.MetadataInfo;
import dk.netarkivet.harvester.datamodel.HarvestDefinitionInfo;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.harvesting.PersistentJobData.XmlState.OKSTATE;

/**
 * Class PersistentJobData holds information about an ongoing harvest. Presently the information is stored in a
 * XML-file.
 */
public class PersistentJobData implements JobInfo {

    /** The logger to use. */
    private static final Logger log = LoggerFactory.getLogger(PersistentJobData.class);

    /** the crawlDir. */
    private final File crawlDir;

    /**
     * The filename for the file containing the persistent job data, stored in crawlDir.
     */
    private static final String HARVEST_INFO_FILENAME = "harvestInfo.xml";
    /** XML-root element for the persistent Job Data. */
    private static final String ROOT_ELEMENT = "harvestInfo";
    /** Key in harvestinfo file for the ID of the job. */
    private static final String JOBID_KEY = ROOT_ELEMENT + ".jobId";
    /** Key in harvestinfo file for the harvestNum of the job. */
    private static final String HARVESTNUM_KEY = ROOT_ELEMENT + ".harvestNum";
    /** Key in harvestinfo file for the maxBytesPerDomain value for the job. */
    private static final String MAXBYTESPERDOMAIN_KEY = ROOT_ELEMENT + ".maxBytesPerDomain";
    /**
     * Key in harvestinfo file for the maxObjectsPerDomain value for the job.
     */
    private static final String MAXOBJECTSPERDOMAIN_KEY = ROOT_ELEMENT + ".maxObjectsPerDomain";
    /** Key in harvestinfo file for the orderXMLName of the job. */
    private static final String TEMPLATENAME_KEY = ROOT_ELEMENT + ".templateName";
    /** Key in harvestinfo file for the orderXMLName of the job. */
    private static final String OLDORDERXMLNAME_KEY = ROOT_ELEMENT + ".orderXMLName";
    /** Key in harvestinfo file for the orderXMLName update date. */
    private static final String ORDERXML_UPDATE_DATE_KEY = ROOT_ELEMENT + ".templateLastUpdateDate";
    /** Key in harvestinfo file for the orderXMLName description. */
    private static final String ORDERXML_DESCRIPTION_KEY = ROOT_ELEMENT + ".templateDescription";
    /** Key in harvestinfo file for the harvestID of the job. */
    private static final String ORIGHARVESTDEFINITIONID_KEY = ROOT_ELEMENT + ".origHarvestDefinitionID";
    /** Key in harvestinfo file for the harvest channel of the job. */
    private static final String CHANNEL_KEY = ROOT_ELEMENT + ".channel";

    private static final String PRIORITY_KEY = ROOT_ELEMENT + ".priority";

    /** Key in harvestinfo file for the original harvest definition name. */
    private static final String HARVEST_NAME_KEY = ROOT_ELEMENT + ".origHarvestDefinitionName";

    /**
     * Key in harvestinfo file for the original harvest definition description.
     */
    private static final String HARVEST_DESC_KEY = ROOT_ELEMENT + ".origHarvestDefinitionComments";

    /**
     * Key in harvestinfo file for the original harvest definition schedule, will be empty for broad crawls.
     */
    private static final String HARVEST_SCHED_KEY = ROOT_ELEMENT + ".scheduleName";
    /** The harvestfilename prefix used by this job set in the Job class. */
    private static final String HARVEST_FILENAME_PREFIX_KEY = ROOT_ELEMENT + ".harvestFilenamePrefix";
    /** The submitted date of this job. */
    private static final String JOB_SUBMIT_DATE_KEY = ROOT_ELEMENT + ".jobSubmitDate";
    /** The performer of this harvest. */
    private static final String HARVEST_PERFORMER_KEY = ROOT_ELEMENT + ".performer";
    /** The audience of this harvest. */
    private static final String HARVEST_AUDIENCE_KEY = ROOT_ELEMENT + ".audience";
    /** The operator of this harvest. */
    private static final String HARVEST_OPERATOR_KEY = ROOT_ELEMENT + ".operator";
    

    /** Key in harvestinfo file for the file version. */
    private static final String HARVESTINFO_VERSION_KEY = "harvestInfo.version";
    /** Value for current version number. */
    private static final String HARVESTINFO_VERSION_NUMBER = "0.6";

    /**
     * Also support for version 0.4 of harvestInfo xml. In the previous format the channel and snapshot keys were
     * absent. Instead there was the priority key.
     */
    private static final String OLD_HARVESTINFO_VERSION_NUMBER_4 = "0.4";
    
    /**
     * Also support for version 0.5 of harvestInfo xml. In this previous format, templateName was named orderXMLName
     * and fields templateLastUpdateDate & templateDescription didn't exist.
     */
    private static final String OLD_HARVESTINFO_VERSION_NUMBER_5 = "0.5";

    /** String array containing all mandatory keys contained in valid version 0.6 xml.
     *  ORDERXML_UPDATE_DATE_KEY, ORDERXML_DESCRIPTION_KEY are optionals 
     */
    private static final String[] ALL_KEYS = {JOBID_KEY, HARVESTNUM_KEY, MAXBYTESPERDOMAIN_KEY,
            MAXOBJECTSPERDOMAIN_KEY, TEMPLATENAME_KEY,  ORIGHARVESTDEFINITIONID_KEY, CHANNEL_KEY,
            HARVESTINFO_VERSION_KEY, HARVEST_NAME_KEY, HARVEST_FILENAME_PREFIX_KEY, JOB_SUBMIT_DATE_KEY};

    /**
     * Optional keys are HARVEST_DESC_KEY representing harvest comments, and HARVEST_SCHED_KEY representing the
     * scheduleName behind the harvest, only applicable for selective harvests.
     */
    
    /** String array containing all mandatory keys contained in valid version 0.5 xml. */
    private static final String[] ALL_KEYS_5 = {JOBID_KEY, HARVESTNUM_KEY, MAXBYTESPERDOMAIN_KEY,
            MAXOBJECTSPERDOMAIN_KEY, OLDORDERXMLNAME_KEY, ORIGHARVESTDEFINITIONID_KEY, CHANNEL_KEY,
            HARVESTINFO_VERSION_KEY, HARVEST_NAME_KEY, HARVEST_FILENAME_PREFIX_KEY, JOB_SUBMIT_DATE_KEY};

    /**
     * String array containing all mandatory keys contained in old valid version 0.4 xml.
     */
    private static final String[] ALL_KEYS_4 = {JOBID_KEY, HARVESTNUM_KEY, MAXBYTESPERDOMAIN_KEY,
            MAXOBJECTSPERDOMAIN_KEY, OLDORDERXMLNAME_KEY, ORIGHARVESTDEFINITIONID_KEY, PRIORITY_KEY,
            HARVESTINFO_VERSION_KEY, HARVEST_NAME_KEY, HARVEST_FILENAME_PREFIX_KEY, JOB_SUBMIT_DATE_KEY};

    /** the SimpleXml object, that contains the XML in HARVEST_INFO_FILENAME. */
    private SimpleXml theXML = null;

    /**
     * Constructor for class PersistentJobData.
     *
     * @param crawlDir The directory where the harvestInfo can be found
     * @throws ArgumentNotValid if crawlDir is null or does not exist.
     */
    public PersistentJobData(File crawlDir) {
        ArgumentNotValid.checkExistsDirectory(crawlDir, "crawlDir");

        this.crawlDir = crawlDir;
    }

    /**
     * Returns true, if harvestInfo exists in crawDir, otherwise false.
     *
     * @return true, if harvestInfo exists, otherwise false
     */
    public boolean exists() {
        return getHarvestInfoFile().isFile();
    }

    /**
     * Returns true if the given directory exists and contains a harvestInfo file.
     *
     * @param crawlDir A directory that may contain harvestInfo file.
     * @return True if the harvestInfo file exists.
     */
    public static boolean existsIn(File crawlDir) {
        return new File(crawlDir, HARVEST_INFO_FILENAME).exists();
    }

    /**
     * @return the location of the harvestInfo File in the crawlDir.
     */
    public static File getHarvestInfoFile(File crawlDir) {
        return new File(crawlDir, HARVEST_INFO_FILENAME);
    }
    

    /**
     * Read harvestInfo into SimpleXML object.
     *
     * @return SimpleXml object for harvestInfo
     * @throws IOFailure if HarvestInfoFile does not exist or if HarvestInfoFile is invalid
     */
    private synchronized SimpleXml read() {
        if (theXML != null) {
            return theXML;
        }
        if (!exists()) {
            throw new IOFailure("The harvestInfo file '" + getHarvestInfoFile().getAbsolutePath() + "' does not exist!");
        }
        SimpleXml sx = new SimpleXml(getHarvestInfoFile());
        XmlState validationResult = validateHarvestInfo(sx);
        if (validationResult.getOkState().equals(XmlState.OKSTATE.NOTOK)) {
            try {
                String errorMsg = "The harvestInfoFile '" + getHarvestInfoFile().getAbsolutePath() + "' is invalid: "
                        + validationResult.getError() + ". The contents of the file is this: "
                        + FileUtils.readFile(getHarvestInfoFile());
                throw new IOFailure(errorMsg);
            } catch (IOException e) {
                String errorMsg = "Unable to read HarvestInfoFile: '" + getHarvestInfoFile().getAbsolutePath() + "'";
                throw new IOFailure(errorMsg);
            }
        } else { // The xml is valid
            theXML = sx;
            return sx;
        }
    }

    /**
     * Write information about given Job to XML-structure.
     *
     * @param harvestJob the given Job
     * @param hdi Information about the harvestJob.
     * @throws IOFailure if any failure occurs while persisting data, or if the file has already been written.
     */
    public synchronized void write(Job harvestJob, HarvestDefinitionInfo hdi) {
        ArgumentNotValid.checkNotNull(harvestJob, "Job harvestJob");
        ArgumentNotValid.checkNotNull(hdi, "HarvestDefinitionInfo hdi");
        if (exists()) {
            String errorMsg = "Persistent Job data already exists in '" + crawlDir + "'. Aborting";
            log.warn(errorMsg);
            throw new IOFailure(errorMsg);
        }

        SimpleXml sx = new SimpleXml(ROOT_ELEMENT);
        sx.add(HARVESTINFO_VERSION_KEY, HARVESTINFO_VERSION_NUMBER);
        sx.add(JOBID_KEY, harvestJob.getJobID().toString());
        sx.add(CHANNEL_KEY, harvestJob.getChannel());
        sx.add(HARVESTNUM_KEY, Integer.toString(harvestJob.getHarvestNum()));
        sx.add(ORIGHARVESTDEFINITIONID_KEY, Long.toString(harvestJob.getOrigHarvestDefinitionID()));
        sx.add(MAXBYTESPERDOMAIN_KEY, Long.toString(harvestJob.getMaxBytesPerDomain()));
        sx.add(MAXOBJECTSPERDOMAIN_KEY, Long.toString(harvestJob.getMaxObjectsPerDomain()));
        sx.add(TEMPLATENAME_KEY, harvestJob.getOrderXMLName());
        // insert fields got from crawler-beans.cxml and add them into
        // harvestInfo.xml for preservation purpose
        if(harvestJob.getOrderXMLdoc() instanceof H3HeritrixTemplate) {
        	H3HeritrixTemplate template = (H3HeritrixTemplate) harvestJob.getOrderXMLdoc();
        	String tmp = null;
        	tmp = template.getMetadataInfo(MetadataInfo.TEMPLATE_UPDATE_DATE);
        	if (tmp != null && !tmp.isEmpty()) {
                sx.add(ORDERXML_UPDATE_DATE_KEY, tmp);
            }
        	tmp = template.getMetadataInfo(MetadataInfo.TEMPLATE_DESCRIPTION);
        	if (tmp != null && !tmp.isEmpty()) {
                sx.add(ORDERXML_DESCRIPTION_KEY, tmp);
            }
        }

        sx.add(HARVEST_NAME_KEY, hdi.getOrigHarvestName());

        String comments = hdi.getOrigHarvestDesc();
        if (!comments.isEmpty()) {
            sx.add(HARVEST_DESC_KEY, comments);
        }

        String schedName = hdi.getScheduleName();
        if (!schedName.isEmpty()) {
            sx.add(HARVEST_SCHED_KEY, schedName);
        }
        // Store the harvestname prefix selected by the used Naming Strategy.
        sx.add(HARVEST_FILENAME_PREFIX_KEY, harvestJob.getHarvestFilenamePrefix());

        // store the submitted date in WARC Date format
        sx.add(JOB_SUBMIT_DATE_KEY, ArchiveDateConverter.getWarcDateFormat().format(harvestJob.getSubmittedDate()));
        // if performer set to something different from the empty String
        if (!Settings.get(HarvesterSettings.PERFORMER).isEmpty()) {
            sx.add(HARVEST_PERFORMER_KEY, Settings.get(HarvesterSettings.PERFORMER));
        }
        // insert fields got from crawler-beans.cxml and add them into
        // harvestInfo.xml for preservation purpose
        if(harvestJob.getOrderXMLdoc() instanceof H3HeritrixTemplate) {
        	H3HeritrixTemplate template = (H3HeritrixTemplate) harvestJob.getOrderXMLdoc();
        	String temp = null;
        	temp = template.getMetadataInfo(MetadataInfo.OPERATOR);
        	if (temp != null && !temp.isEmpty()) {
                sx.add(HARVEST_OPERATOR_KEY, temp);
            }
        }
        if (harvestJob.getHarvestAudience() != null && !harvestJob.getHarvestAudience().isEmpty()) {
            sx.add(HARVEST_AUDIENCE_KEY, harvestJob.getHarvestAudience());
        }

        XmlState validationResult = validateHarvestInfo(sx);
        if (validationResult.getOkState().equals(XmlState.OKSTATE.NOTOK)) {
            String msg = "Could not create a valid harvestinfo file for job " + harvestJob.getJobID() + ": "
                    + validationResult.getError();
            throw new IOFailure(msg);
        } else {
            sx.save(getHarvestInfoFile());
        }
    }

    /**
     * Checks that the xml data in the persistent job data file is valid.
     *
     * @param sx the SimpleXml object containing the persistent job data
     * @return empty string, if valid persistent job data, otherwise a string containing the problem.
     */
    private static XmlState validateHarvestInfo(SimpleXml sx) {
        final String version;
        if (sx.hasKey(HARVESTINFO_VERSION_KEY)) {
            version = sx.getString(HARVESTINFO_VERSION_KEY);
        } else {
            final String errMsg = "Missing version information";
            return new XmlState(OKSTATE.NOTOK, errMsg);
        }

        String[] keysToCheck = new String[0];
        if (version.equals(HARVESTINFO_VERSION_NUMBER)) {
            keysToCheck = ALL_KEYS;
        } else if (version.equals(OLD_HARVESTINFO_VERSION_NUMBER_5)) {
            keysToCheck = ALL_KEYS_5;
        } else if(version.equals(OLD_HARVESTINFO_VERSION_NUMBER_4)) {
        	version.equals(OLD_HARVESTINFO_VERSION_NUMBER_4);
        } else {
            final String errMsg = "Invalid version: " + version;
            return new XmlState(OKSTATE.NOTOK, errMsg);
        }

        /* Check, if all necessary components exist in the SimpleXml */

        for (String key : keysToCheck) {
            if (!sx.hasKey(key)) {
                final String errMsg = "Could not find key " + key + " in harvestInfoFile, version " + version;
                return new XmlState(OKSTATE.NOTOK, errMsg);
            }
        }

        /* Check, if the jobId element contains a long value */
        final String jobidAsString = sx.getString(JOBID_KEY);
        try {
            Long.valueOf(jobidAsString);
        } catch (Throwable t) {
            final String errMsg = "The id '" + jobidAsString + "' in harvestInfoFile must be a long value";
            return new XmlState(OKSTATE.NOTOK, errMsg);
        }

        // Verify, that the job channel and snapshot elements are not the empty String (version 0.5+)
        if ((version.equals(HARVESTINFO_VERSION_NUMBER) || version.equals(OLD_HARVESTINFO_VERSION_NUMBER_5))
        		&& sx.getString(CHANNEL_KEY).isEmpty()) {
            final String errMsg = "The channel and/or the snapshot value of the job is undefined";
            return new XmlState(OKSTATE.NOTOK, errMsg);
        }

        if (version.equals(OLD_HARVESTINFO_VERSION_NUMBER_4) && sx.getString(PRIORITY_KEY).isEmpty()) {
            final String errMsg = "The priority value of the job is undefined";
            return new XmlState(OKSTATE.NOTOK, errMsg);
        }

        // Verify, that the job channel element is not the empty String
        if ((version.equals(HARVESTINFO_VERSION_NUMBER) || version.equals(OLD_HARVESTINFO_VERSION_NUMBER_5))
        		&& sx.getString(CHANNEL_KEY).isEmpty()) {
            final String errMsg = "The channel and/or the snapshot value of the job is undefined";
            return new XmlState(OKSTATE.NOTOK, errMsg);
        }

        // Verify, that the ORDERXMLNAME element is not the empty String (V.0.6)
        if (version.equals(HARVESTINFO_VERSION_NUMBER) && sx.getString(TEMPLATENAME_KEY).isEmpty()) {
            final String errMsg = "The orderxmlname of the job is undefined";
            return new XmlState(OKSTATE.NOTOK, errMsg);
        }
        
        // Verify, that the ORDERXMLNAME element is not the empty String (V.0.5)
        if (version.equals(OLD_HARVESTINFO_VERSION_NUMBER_5) && sx.getString(OLDORDERXMLNAME_KEY).isEmpty()) {
            final String errMsg = "The orderxmlname of the job is undefined";
            return new XmlState(OKSTATE.NOTOK, errMsg);
        }

        // Verify that the HARVESTNUM element is an integer
        final String harvestNumAsString = sx.getString(HARVESTNUM_KEY);
        try {
            Integer.valueOf(harvestNumAsString);
        } catch (Throwable t) {
            final String errMsg = "The HARVESTNUM in harvestInfoFile must be a Integer "
                    + "value. The value given is '" + harvestNumAsString + "'.";
            return new XmlState(OKSTATE.NOTOK, errMsg);
        }

        /*
         * Check, if the OrigHarvestDefinitionID element contains a long value.
         */
        final String origHarvestDefinitionIDAsString = sx.getString(ORIGHARVESTDEFINITIONID_KEY);
        try {
            Long.valueOf(origHarvestDefinitionIDAsString);
        } catch (Throwable t) {
            final String errMsg = "The OrigHarvestDefinitionID in harvestInfoFile must be a long value. "
                    + "The value given is: '" + origHarvestDefinitionIDAsString + "'.";
            return new XmlState(OKSTATE.NOTOK, errMsg);
        }

        /* Check, if the MaxBytesPerDomain element contains a long value */
        final String maxBytesPerDomainAsString = sx.getString(MAXBYTESPERDOMAIN_KEY);
        try {
            Long.valueOf(maxBytesPerDomainAsString);
        } catch (Throwable t) {
            final String errMsg = "The MaxBytesPerDomain element in harvestInfoFile must be a long value. "
                    + "The value given is: '" + maxBytesPerDomainAsString + "'.";
            return new XmlState(OKSTATE.NOTOK, errMsg);
        }

        /* Check, if the MaxObjectsPerDomain element contains a long value */
        final String maxObjectsPerDomainAsString = sx.getString(MAXOBJECTSPERDOMAIN_KEY);
        try {
            Long.valueOf(maxObjectsPerDomainAsString);
        } catch (Throwable t) {
            final String errMsg = "The MaxObjectsPerDomain element in harvestInfoFile must be a long value. "
                    + "The value given is: '" + maxObjectsPerDomainAsString + "'.";
            return new XmlState(OKSTATE.NOTOK, errMsg);
        }

        return new XmlState(OKSTATE.OK, "");
    }

    /**
     * @return the harvestInfoFile.
     */
    private File getHarvestInfoFile() {
        return new File(crawlDir, HARVEST_INFO_FILENAME);
    }

    /**
     * Return the harvestInfo jobID.
     *
     * @return the harvestInfo JobID
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public Long getJobID() {
        SimpleXml sx = read(); // reads and validates XML
        String jobIDString = sx.getString(JOBID_KEY);
        return Long.parseLong(jobIDString);
    }

    /**
     * Return the job's harvest channel name.
     *
     * @return the job's harvest channel name
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public String getChannel() {
        SimpleXml sx = read(); // reads and validates XML
        return sx.getString(CHANNEL_KEY);
    }

    /**
     * Return the job harvestNum.
     *
     * @return the job harvestNum
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public int getJobHarvestNum() {
        SimpleXml sx = read(); // reads and validates XML
        String harvestNumString = sx.getString(HARVESTNUM_KEY);
        return Integer.parseInt(harvestNumString);
    }

    /**
     * Return the job origHarvestDefinitionID.
     *
     * @return the job origHarvestDefinitionID
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public Long getOrigHarvestDefinitionID() {
        SimpleXml sx = read(); // reads and validates XML
        String origHarvestDefinitionIDString = sx.getString(ORIGHARVESTDEFINITIONID_KEY);
        return Long.parseLong(origHarvestDefinitionIDString);
    }

    /**
     * Return the job maxBytesPerDomain value.
     *
     * @return the job maxBytesPerDomain value.
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public long getMaxBytesPerDomain() {
        SimpleXml sx = read(); // reads and validates XML
        String maxBytesPerDomainString = sx.getString(MAXBYTESPERDOMAIN_KEY);
        return Long.parseLong(maxBytesPerDomainString);
    }

    /**
     * Return the job maxObjectsPerDomain value.
     *
     * @return the job maxObjectsPerDomain value.
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public long getMaxObjectsPerDomain() {
        SimpleXml sx = read(); // reads and validates XML
        String maxObjectsPerDomainString = sx.getString(MAXOBJECTSPERDOMAIN_KEY);
        return Long.parseLong(maxObjectsPerDomainString);
    }

    /**
     * Return the job orderXMLName.
     *
     * @return the job orderXMLName.
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public String getOrderXMLName() {
        SimpleXml sx = read(); // reads and validates XML
        return sx.getString(TEMPLATENAME_KEY);
    }

    /**
     * Return the version of the xml.
     *
     * @return the version of the xml
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public String getVersion() {
        SimpleXml sx = read(); // reads and validates XML
        return sx.getString(HARVESTINFO_VERSION_KEY);
    }

    /**
     * Helper class for returning the OK-state back to the caller.
     */
    protected static class XmlState {
        /** enum for holding OK/NOTOK values. */
        public enum OKSTATE {
            OK, NOTOK
        }

        /** the state of the XML. */
        private OKSTATE ok;
        /** The error coming from an xml-validation. */
        private String error;;

        /**
         * Constructor of an XmlState object.
         *
         * @param ok Is the XML OK or not OKAY?
         * @param error The error found during validation, if any.
         */
        public XmlState(OKSTATE ok, String error) {
            this.ok = ok;
            this.error = error;
        }

        /**
         * @return the OK value of this object.
         */
        public OKSTATE getOkState() {
            return ok;
        }

        /**
         * @return the error value of this object (maybe null).
         */
        public String getError() {
            return error;
        }
    }

    /**
     * If not set in persistentJobData, fall back to the standard way.
     * jobid-harvestid.
     */
    public String getHarvestFilenamePrefix() {
        SimpleXml sx = read(); // reads and validates XML
        String prefix = null;
        if (!sx.hasKey(HARVEST_FILENAME_PREFIX_KEY)) {
            prefix = this.getJobID() + "-" + this.getOrigHarvestDefinitionID();
            log.warn("harvestFilenamePrefix not part of persistentJobData. Using old standard naming: {}", prefix);
        } else {
            prefix = sx.getString(HARVEST_FILENAME_PREFIX_KEY);
        }
        return prefix;
    }

    /**
     * Return the harvestname in this xml.
     *
     * @return the harvestname in this xml.
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public String getharvestName() {
        SimpleXml sx = read(); // reads and validates XML
        return sx.getString(HARVEST_NAME_KEY);
    }

    /**
     * Return the schedulename in this xml.
     *
     * @return the schedulename in this xml (or null, if undefined for this job)
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public String getScheduleName() {
        SimpleXml sx = read(); // reads and validates XML
        if (sx.hasKey(HARVEST_SCHED_KEY)) {
            return sx.getString(HARVEST_SCHED_KEY);
        } else {
            return null;
        }
    }

    /**
     * Return the submit date of the job in this xml.
     *
     * @return the submit date of the job in this xml.
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public String getJobSubmitDate() {
        SimpleXml sx = read(); // reads and validates XML
        return sx.getString(JOB_SUBMIT_DATE_KEY);
    }

    /**
     * Return the performer information in this xml.
     *
     * @return the performer information in this xml or null if value undefined
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public String getPerformer() {
        SimpleXml sx = read(); // reads and validates XML
        if (sx.hasKey(HARVEST_PERFORMER_KEY)) {
            return sx.getString(HARVEST_PERFORMER_KEY);
        } else {
            return null;
        }
    }

    /**
     * Return the audience information in this xml.
     *
     * @return the audience information in this xml or null if value undefined
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public String getAudience() {
        SimpleXml sx = read(); // reads and validates XML
        if (sx.hasKey(HARVEST_AUDIENCE_KEY)) {
            return sx.getString(HARVEST_AUDIENCE_KEY);
        } else {
            return null;
        }
    }

}
