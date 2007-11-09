/* File:        $Id$
 * Revision:    $Revision$
 * Date:        $Date$
 * Author:      $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

    /** the crawlDir. */
    private final File crawlDir;
    /** The filename for the file containing the persistent job data, stored in crawlDir. */
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
    /** Key in harvestinfo file for the maxObjectsPerDomain value for the job. */
    private static final String MAXOBJECTSPERDOMAIN_KEY = ROOT_ELEMENT
                        + ".maxObjectsPerDomain";
    /** Key in harvestinfo file for the orderXMLName of the job. */
    private static final String ORDERXMLNAME_KEY = ROOT_ELEMENT + ".orderXMLName";
    /** Key in harvestinfo file for the harvestID of the job. */
    private static final String ORIGHARVESTDEFINITIONID_KEY = ROOT_ELEMENT
                        + ".origHarvestDefinitionID";
    /** Key in harvestinfo file for the priority of the job. */
    private static final String PRIORITY_KEY = ROOT_ELEMENT + ".priority";

    /** Key in harvestinfo file for the file version. */
    private static final String HARVESTVERSION_KEY = "harvestInfo.version";
    /** Value for current version number. */
    private static final String HARVESTVERSION_NUMBER = "0.2";

    /** String array containing all keys contained in valid xml. */
    private static final String[] ALL_KEYS = {JOBID_KEY, HARVESTNUM_KEY,   MAXBYTESPERDOMAIN_KEY,
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
            throw new ArgumentNotValid("Given crawldir '" + crawlDir.getAbsolutePath()
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
    private SimpleXml read() {
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
     * @throws IOFailure if any failure occurs while persisting data, or if
     * the file has already been written.
     */
    public void write(Job harvestJob) {
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

        boolean validVersion = version.equals(HARVESTVERSION_NUMBER);
        if (!validVersion) {
            log.warn("Invalid version: " + version);
            return false;
        }

        /* Check, if all necessary components exist in the SimpleXml */

        for (String key: ALL_KEYS) {
            if (!sx.hasKey(key)) {
                log.debug("Could not find key " + key + " in harvestInfoFile ");
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
        if (sx.getString(PRIORITY_KEY).equals("")) {
            return false;
        }

        // Verify, that the ORDERXMLNAME element is not the empty String
        if (sx.getString(ORDERXMLNAME_KEY).equals("")) {
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

        /* Check, if the OrigHarvestDefinitionID element contains a long value */
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
