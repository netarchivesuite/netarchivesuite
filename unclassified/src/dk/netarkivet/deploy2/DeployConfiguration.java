/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 */
package dk.netarkivet.deploy2;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;

/**
 * The structure for the it-config.
 * Loads the IT configuration from an XML file into a XmlStructure.
 */
public class DeployConfiguration {
    /** The configuration structure (deployGlobal).*/
    private XmlStructure config;
    /** The settings branch of the config.*/
    private XmlStructure settings;
    /** The parameters for running java.*/
    private Parameters machineParam;
    /** The list of the physical locations.*/
    private List<PhysicalLocation> physLocs;
    /** The file containing the it-configuration.*/
    private File itConfigFile;
    /** The NetarchiveSuite file (in .zip).*/
    private File netarchiveSuiteFile;
    /** The security policy file.*/
    private File secPolicyFile;
    /** The log property file.*/
    private File logPropFile;
    /** The directory for output.*/
    private File outputDir;
    /** The name of the database.*/
    private String databaseFileName;

    /**
     *  Initialise everything.
     * 
     * @param itConfigFileName Name of configuration file.
     * @param netarchiveSuiteFileName Name of installation file.
     * @param secPolicyFileName Name of security policy file.
     * @param logPropFileName Name of the log property file.
     * @param outputDirName Directory for the output.
     * @param dbFileName Name of the database.
     */
    public DeployConfiguration(File itConfigFileName, 
            File netarchiveSuiteFileName, 
            File secPolicyFileName,
            File logPropFileName,
            String outputDirName,
            String dbFileName) {
        ArgumentNotValid.checkNotNull(
                itConfigFileName, "No config file");
        ArgumentNotValid.checkNotNull(
                netarchiveSuiteFileName, "No installation file");
        ArgumentNotValid.checkNotNull(
                secPolicyFileName, "No security file");
        ArgumentNotValid.checkNotNull(
                logPropFileName, "No log file");

        itConfigFile = itConfigFileName;
        netarchiveSuiteFile = netarchiveSuiteFileName;
        secPolicyFile = secPolicyFileName;
        logPropFile = logPropFileName;
        databaseFileName = dbFileName;
        
        // get configuration tree, settings and parameters
        config = new XmlStructure(itConfigFile);
        settings = new XmlStructure(
                config.getChild(Constants.SETTINGS_BRANCH));
        machineParam = new Parameters(config);

        // if a outputDir has not been given as argument, 
        // it is the output directory
        if(outputDirName == null) {
            // Load output directory from config file
            outputDirName = "./" 
                + config.getSubChildValue(
                        Constants.ENVIRONMENT_NAME_TOTAL_PATH_LEAF)
                        + "/";
        }
        outputDir = new File(outputDirName);
        // make sure that directory outputDir exists
        FileUtils.createDir(outputDir);
        extractElements();
    }

    /**
     * Extracts the physical locations and put them into the list.
     */
    private void extractElements() {
        // initialise physical location array
        physLocs = new ArrayList<PhysicalLocation>();
        // get the list from the XML tree
        List<Element> physList = config.getChildren(
                Constants.PHYSICAL_LOCATION_BRANCH);
        // get all physical locations into the list
        for(Element elem : physList) {
            physLocs.add(new PhysicalLocation(elem, settings, machineParam,
                    netarchiveSuiteFile.getName(), logPropFile, 
                    secPolicyFile, databaseFileName));
        }
    }

    /**
     * Makes every physical location create their scripts.
     */
    public void write() {
        // write all physical locations
        for(PhysicalLocation pl : physLocs) {
            pl.write(outputDir);
        }
    }
}
