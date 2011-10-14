/* File:        $Id$
 * Revision:    $Revision$
 * Date:        $Date$
 * Author:      $Author$
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
package dk.netarkivet.harvester.harvesting.distribute;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.SimpleXml;
import dk.netarkivet.harvester.datamodel.Job;
import dk.netarkivet.harvester.datamodel.JobPriority;


/**
 * Class PersistentJobData holds information about an ongoing harvest.
 * Presently the information is stored in a XML-file.
 */
public class PersistentJobData {
    
    /** Innerclass containing Info about a harvestjob. */
    public static class HarvestDefinitionInfo implements Serializable {

        /**
         * The original harvest name.
         */
        private final String origHarvestName;

        /**
         * The original harvest description.
         */
        private final String origHarvestDesc;

        /**
         * The name of the schedule for the original harvest definition.
         */
        private final String scheduleName;

        /**
         * Builds a harvest definition info object.
         * @param origHarvestName the harvest definition's name
         * @param origHarvestDesc the harvest definition's comments
         * @param scheduleName the harvest definition's schedule name
         */
        public HarvestDefinitionInfo(
                String origHarvestName,
                String origHarvestDesc,
                String scheduleName) {
            super();
            ArgumentNotValid.checkNotNullOrEmpty(
                    origHarvestName, "origHarvestName");
            ArgumentNotValid.checkNotNull(
                    origHarvestDesc, "origHarvestDesc");
            ArgumentNotValid.checkNotNull(
                    scheduleName, "scheduleName");
            this.origHarvestName = origHarvestName;
            this.origHarvestDesc = origHarvestDesc;
            this.scheduleName = scheduleName;
        }

        /**
         * @return the origHarvestName
         */
        public String getOrigHarvestName() {
            return origHarvestName;
        }

        /**
         * @return the origHarvestDesc
         */
        public String getOrigHarvestDesc() {
            return origHarvestDesc;
        }

        /**
         * @return the origHarvestScheduleName
         */
        public String getScheduleName() {
            return scheduleName;
        }

    }

    /** the crawlDir. */
    private final File crawlDir;
    /** The filename for the file containing the persistent job data, 
     * stored in crawlDir. */
    private static final String HARVEST_INFO_FILENAME = "harvestInfo.xml";
    /** XML-root element for the persistent Job Data. */
    private static final String ROOT_ELEMENT = "harvestInfo";
    /** Key in harvestinfo file for the ID of the job. */
    private static final String JOBID_KEY = ROOT_ELEMENT + ".jobId";
    /** Key in harvestinfo file for the harvestNum of the job. */
    private static final String HARVESTNUM_KEY = ROOT_ELEMENT + ".harvestNum";
    /** Key in harvestinfo file for the maxBytesPerDomain value for the job. */
    private static final String MAXBYTESPERDOMAIN_KEY = ROOT_ELEMENT
                        + ".maxBytesPerDomain";
    /** Key in harvestinfo file for the maxObjectsPerDomain value for 
     * the job. */
    private static final String MAXOBJECTSPERDOMAIN_KEY = ROOT_ELEMENT
                        + ".maxObjectsPerDomain";
    /** Key in harvestinfo file for the orderXMLName of the job. */
    private static final String ORDERXMLNAME_KEY = ROOT_ELEMENT 
        + ".orderXMLName";
    /** Key in harvestinfo file for the harvestID of the job. */
    private static final String ORIGHARVESTDEFINITIONID_KEY = ROOT_ELEMENT
                        + ".origHarvestDefinitionID";
    /** Key in harvestinfo file for the priority of the job. */
    private static final String PRIORITY_KEY = ROOT_ELEMENT + ".priority";

    /** Key in harvestinfo file for the original harvest definition name. */
    private static final String HARVEST_NAME_KEY =
        ROOT_ELEMENT + ".origHarvestDefinitionName";

    /** Key in harvestinfo file for the original harvest definition
     * description. */
    private static final String HARVEST_DESC_KEY =
        ROOT_ELEMENT + ".origHarvestDefinitionComments";

    /** Key in harvestinfo file for the original harvest definition
     * schedule, will be empty for broad crawls. */
    private static final String HARVEST_SCHED_KEY =
        ROOT_ELEMENT + ".scheduleName";

    /** Key in harvestinfo file for the file version. */
    private static final String HARVESTVERSION_KEY = "harvestInfo.version";
    /** Value for current version number. */
    private static final String HARVESTVERSION_NUMBER = "0.3";
    
    /** Also support for version 0.2 of harvestInfo xml. 
     * In the previous format the field origHarvestDefinitionName
     * did not exist. However version 0.2 of harvestInfo.xml will contain
     * this field as well when running 3.16.* of NetarchiveSuite.
     */
    private static final String OLD_HARVESTVERSION_NUMBER = "0.2";

    /** String array containing all keys contained in valid version 0.3 xml.  */
    private static final String[] ALL_KEYS = {JOBID_KEY, HARVESTNUM_KEY, 
        MAXBYTESPERDOMAIN_KEY,
        MAXOBJECTSPERDOMAIN_KEY, ORDERXMLNAME_KEY,
        ORIGHARVESTDEFINITIONID_KEY, PRIORITY_KEY, HARVESTVERSION_KEY,
        HARVEST_NAME_KEY};
    
    /** String array containing all keys contained in old valid version 
     * 0.2 xml.  */
    private static final String[] ALL_KEYS_OLD = {JOBID_KEY, HARVESTNUM_KEY, 
        MAXBYTESPERDOMAIN_KEY,
        MAXOBJECTSPERDOMAIN_KEY, ORDERXMLNAME_KEY,
        ORIGHARVESTDEFINITIONID_KEY, PRIORITY_KEY, HARVESTVERSION_KEY};
    
    
    /** The logger to use. */
    private static final Log log
            = LogFactory.getLog(PersistentJobData.class);

    /** the SimpleXml object, that contains the XML in HARVEST_INFO_FILENAME. */
    private SimpleXml theXML = null;

    /**
     * Constructor for class PersistentJobData.
     * @param crawlDir The directory where the harvestInfo can be found
     * @throws ArgumentNotValid if crawlDir is null or does not exist.
     */
    public PersistentJobData(File crawlDir) {
        ArgumentNotValid.checkNotNull(crawlDir, "crawlDir");
        if (!crawlDir.isDirectory()) {
            throw new ArgumentNotValid("Given crawldir '" 
                    + crawlDir.getAbsolutePath()
                    + "' does not exist!");
        }
        this.crawlDir = crawlDir;
    }

    /**
     * Returns true, if harvestInfo exists in crawDir, otherwise false.
     * @return true, if harvestInfo exists, otherwise false
     */
    public boolean exists() {
        return getHarvestInfoFile().isFile();
    }

    /** Returns true if the given directory exists and contains a harvestInfo
     * file.
     *
     * @param crawlDir A directory that may contain harvestInfo file.
     * @return True if the harvestInfo file exists.
     */
    public static boolean existsIn(File crawlDir) {
        return new File(crawlDir, HARVEST_INFO_FILENAME).exists();
    }

    /**
     * Read harvestInfo into SimpleXML object.
     * @return SimpleXml object for harvestInfo
     * @throws IOFailure if HarvestInfoFile does not exist
     *                    if HarvestInfoFile is invalid
     */
    private synchronized SimpleXml read() {
        if (theXML != null) {
            return theXML;
        }
        if (!exists()) {
            throw new IOFailure("The harvestInfo file '"
                                + getHarvestInfoFile().getAbsolutePath()
                                + "' does not exist!");
        }
        SimpleXml sx = new SimpleXml(getHarvestInfoFile());
        if (!validHarvestInfo(sx)) {
            try {
                String errorMsg = "Invalid data found in harvestInfoFile '"
                    + getHarvestInfoFile().getAbsolutePath()
                    + "': " + FileUtils.readFile(getHarvestInfoFile());
                log.warn(errorMsg);
                throw new IOFailure(errorMsg);
            } catch (IOException e) {
                String errorMsg = "Unable to read HarvestInfoFile: '"
                                  + getHarvestInfoFile().getAbsolutePath()
                                  + "'";
                log.warn(errorMsg);
                throw new IOFailure(errorMsg);
            }
        }
        theXML = sx;
        return sx;
    }

    /**
     * Write information about given Job to XML-structure.
     * @param harvestJob the given Job
     * @param hdi Information about the harvestJob.
     * @throws IOFailure if any failure occurs while persisting data, or if
     * the file has already been written.
     */
    public void write(Job harvestJob, HarvestDefinitionInfo hdi) {
        if (exists()) {
            String errorMsg = "Persistent Job data already exists in '"
                    + crawlDir + "'. Aborting";
            log.warn(errorMsg);
            throw new IOFailure(errorMsg);
        }

        SimpleXml sx = new SimpleXml(ROOT_ELEMENT);
        sx.add(HARVESTVERSION_KEY, HARVESTVERSION_NUMBER);
        sx.add(JOBID_KEY, harvestJob.getJobID().toString());
        sx.add(PRIORITY_KEY, harvestJob.getPriority().toString());
        sx.add(HARVESTNUM_KEY,
                Integer.toString(harvestJob.getHarvestNum()));
        sx.add(ORIGHARVESTDEFINITIONID_KEY,
                Long.toString(harvestJob.getOrigHarvestDefinitionID()));
        sx.add(MAXBYTESPERDOMAIN_KEY,
                Long.toString(harvestJob.getMaxBytesPerDomain()));
        sx.add(MAXOBJECTSPERDOMAIN_KEY,
                Long.toString(harvestJob.getMaxObjectsPerDomain()));
        sx.add(ORDERXMLNAME_KEY,
                harvestJob.getOrderXMLName());

        sx.add(HARVEST_NAME_KEY, hdi.getOrigHarvestName());

        String comments = hdi.getOrigHarvestDesc();
        if (!comments.isEmpty()) {
            sx.add(HARVEST_DESC_KEY, comments);
        }

        String schedName = hdi.getScheduleName();
        if (!schedName.isEmpty()) {
            sx.add(HARVEST_SCHED_KEY, schedName);
        }

        if (!validHarvestInfo(sx)) {
            String msg = "Could not create a valid harvestinfo file for job "
                    + harvestJob.getJobID();
            log.warn(msg);
            throw new IOFailure(msg);
        }
        sx.save(getHarvestInfoFile());
    }

    /**
     * Checks that the xml data in the persistent job data file is valid.
     * @param sx  the SimpleXml object containing the persistent job data
     * @return true if valid persistent job data, otherwise false
     */
    private static boolean validHarvestInfo(SimpleXml sx) {
        String version = "invalid";
        if (sx.hasKey(HARVESTVERSION_KEY)) {
            version = sx.getString(HARVESTVERSION_KEY);
        }
        final String[] keysToCheck;
        if (version.equals(HARVESTVERSION_NUMBER)) {
            keysToCheck = ALL_KEYS;
        } else if (version.equals(OLD_HARVESTVERSION_NUMBER)) {
            keysToCheck = ALL_KEYS_OLD;
        } else {
            log.warn("Invalid version: " + version);
            return false;
        }

        /* Check, if all necessary components exist in the SimpleXml */
        
        for (String key: keysToCheck) {
            if (!sx.hasKey(key)) {
                log.debug("Could not find key " + key 
                        + " in harvestInfoFile version " + version);
                return false;
            }
        }

        /* Check, if the jobId element contains a long value */
        try {
            Long.valueOf(sx.getString(JOBID_KEY));
        } catch(Throwable t) {
            log.debug("The id in harvestInfoFile must be a long value");
            return false;
        }

        // Verify, that the job priority element is not the empty String
        if (sx.getString(PRIORITY_KEY).isEmpty()) {
            return false;
        }

        // Verify, that the ORDERXMLNAME element is not the empty String
        if (sx.getString(ORDERXMLNAME_KEY).isEmpty()) {
            return false;
        }

        // Verify that the HARVESTNUM element is an integer
        try {
            Integer.valueOf(sx.getString(HARVESTNUM_KEY));
        } catch(Throwable t) {
            log.debug("The HARVESTNUM in harvestInfoFile must be a Integer "
                     + "value");
            return false;
        }

        // Verify that the HARVESTNUM element is an integer
        try {
            Integer.valueOf(sx.getString(HARVESTNUM_KEY));
        } catch(Throwable t) {
            log.debug("The HARVESTNUM in harvestInfoFile must be a Integer "
                      + "value");
            return false;
        }

        /* Check, if the OrigHarvestDefinitionID element contains 
         * a long value.
         */
        try {
            Long.valueOf(sx.getString(ORIGHARVESTDEFINITIONID_KEY));
        } catch(Throwable t) {
            log.debug("The OrigHarvestDefinitionID in harvestInfoFile must be a"
                      + " long value");
            return false;
        }

        /* Check, if the MaxBytesPerDomain element contains a long value */
        try {
            Long.valueOf(sx.getString(MAXBYTESPERDOMAIN_KEY));
        } catch(Throwable t) {
            log.debug("The MaxBytesPerDomain element in harvestInfoFile must be"
                      + " a long value");
            return false;
        }

        /* Check, if the MaxObjectsPerDomain element contains a long value */
        try {
            Long.valueOf(sx.getString(MAXOBJECTSPERDOMAIN_KEY));
        } catch(Throwable t) {
            log.debug("The MaxObjectsPerDomain element in harvestInfoFile must"
                      + " be a long value");
            return false;
        }

        return true;
    }
    
    /**
     * @return the harvestInfoFile.
     */
    private File getHarvestInfoFile() {
        return new File(crawlDir, HARVEST_INFO_FILENAME);
    }

    /**
     * Return the harvestInfo jobID.
     * @return the harvestInfo JobID
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public Long getJobID() {
        SimpleXml sx = read(); // reads and validates XML
        String jobIDString = sx.getString(JOBID_KEY);
        return Long.parseLong(jobIDString);
    }

    /**
     * Return the job priority.
     * @return the job priority
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public JobPriority getJobPriority() {
        SimpleXml sx = read(); // reads and validates XML
        return JobPriority.valueOf(sx.getString(PRIORITY_KEY));
    }

    /**
     * Return the job harvestNum.
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
     * @return the job origHarvestDefinitionID
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public Long getOrigHarvestDefinitionID() {
        SimpleXml sx = read(); // reads and validates XML
        String origHarvestDefinitionIDString =
            sx.getString(ORIGHARVESTDEFINITIONID_KEY);
        return Long.parseLong(origHarvestDefinitionIDString);
    }

    /**
     * Return the job maxBytesPerDomain value.
     * @return the job maxBytesPerDomain value.
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public long getMaxBytesPerDomain() {
        SimpleXml sx = read(); // reads and validates XML
        String maxBytesPerDomainString =
            sx.getString(MAXBYTESPERDOMAIN_KEY);
        return Long.parseLong(maxBytesPerDomainString);
    }

    /**
     * Return the job maxObjectsPerDomain value.
     * @return the job maxObjectsPerDomain value.
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public long getMaxObjectsPerDomain() {
        SimpleXml sx = read(); // reads and validates XML
        String maxObjectsPerDomainString =
            sx.getString(MAXOBJECTSPERDOMAIN_KEY);
        return Long.parseLong(maxObjectsPerDomainString);
    }

    /**
     * Return the job orderXMLName.
     * @return the job orderXMLName.
     * @throws IOFailure if no harvestInfo exists or it is invalid.
     */
    public String getOrderXMLName() {
        SimpleXml sx = read(); // reads and validates XML
        return sx.getString(ORDERXMLNAME_KEY);
    }
}
