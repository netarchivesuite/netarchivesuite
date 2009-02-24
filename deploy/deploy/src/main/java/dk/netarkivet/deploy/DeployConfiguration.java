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
package dk.netarkivet.deploy;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;

/**
 * The structure for the deploy-config.
 * Loads the deploy-configuration from an XML file into a XmlStructure.
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
    /** The file containing the deploy-configuration.*/
    private File deployConfigFile;
    /** The NetarchiveSuite file (in .zip).*/
    private File netarchiveSuiteFile;
    /** The security policy file.*/
    private File secPolicyFile;
    /** The log property file.*/
    private File logPropFile;
    /** The directory for output.*/
    private File outputDir;
    /** The name of the database.*/
    private File databaseFileName;
    /** The optional choice for resetting tempDir.*/
    private boolean resetDirectory;

    /**
     *  Initialise everything.
     * 
     * @param deployConfigFileName Name of configuration file.
     * @param netarchiveSuiteFileName Name of installation file.
     * @param secPolicyFileName Name of security policy file.
     * @param logPropFileName Name of the log property file.
     * @param outputDirName Directory for the output.
     * @param dbFileName Name of the database.
     * @param resetDir Whether the temporary directory should be reset.
     */
    public DeployConfiguration(File deployConfigFileName, 
            File netarchiveSuiteFileName, 
            File secPolicyFileName,
            File logPropFileName,
            String outputDirName,
            File dbFileName,
            boolean resetDir) {
        ArgumentNotValid.checkNotNull(
                deployConfigFileName, "No config file");
        ArgumentNotValid.checkNotNull(
                netarchiveSuiteFileName, "No installation file");
        ArgumentNotValid.checkNotNull(
                secPolicyFileName, "No security file");
        ArgumentNotValid.checkNotNull(
                logPropFileName, "No log file");

        deployConfigFile = deployConfigFileName;
        netarchiveSuiteFile = netarchiveSuiteFileName;
        secPolicyFile = secPolicyFileName;
        logPropFile = logPropFileName;
        databaseFileName = dbFileName;
        resetDirectory = resetDir;

        // get configuration tree, settings and parameters
        config = new XmlStructure(deployConfigFile);
        settings = new XmlStructure(
                config.getChild(Constants.COMPLETE_SETTINGS_BRANCH));
        machineParam = new Parameters(config);

        // if a outputDir has not been given as argument, 
        // it is the output directory
        if(outputDirName == null) {
            // Load output directory from config file
            outputDirName = Constants.DOT + Constants.SLASH 
                + config.getSubChildValue(
                        Constants.COMPLETE_ENVIRONMENT_NAME_LEAF)
                        + Constants.SLASH;
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
                Constants.DEPLOY_PHYSICAL_LOCATION);
        // get all physical locations into the list
        for(Element elem : physList) {
            physLocs.add(new PhysicalLocation(elem, settings, machineParam,
                    netarchiveSuiteFile.getName(), logPropFile, 
                    secPolicyFile, databaseFileName, resetDirectory));
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
