/* $Id: Deploy.java 470 2008-08-20 16:08:30Z svc $
 * $Revision: 470 $
 * $Date: 2008-08-20 18:08:30 +0200 (Wed, 20 Aug 2008) $
 * $Author: svc $
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

    /**
     *  Initialise everything.
     * 
     * @param itConfigFileName Name of configuration file.
     * @param netarchiveSuiteFileName Name of installation file.
     * @param secPolicyFileName Name of security policy file.
     * @param logPropFileName Name of the log file.
     * @param outputDirName Directory for the output.
     */
    public DeployConfiguration(String itConfigFileName, 
            String netarchiveSuiteFileName, 
            String secPolicyFileName, 
            String logPropFileName,
            String outputDirName) {
        ArgumentNotValid.checkNotNullOrEmpty(
                itConfigFileName, "No config file");
        ArgumentNotValid.checkNotNullOrEmpty(
                netarchiveSuiteFileName, "No installation file");
        ArgumentNotValid.checkNotNullOrEmpty(
                secPolicyFileName, "No security file");
        ArgumentNotValid.checkNotNullOrEmpty(
                logPropFileName, "No log file");

        itConfigFile = new File(itConfigFileName);
        netarchiveSuiteFile = new File(netarchiveSuiteFileName);
        secPolicyFile = new File(secPolicyFileName);
        logPropFile = new File(logPropFileName);

        // get configuration tree, settings and parameters
        config = new XmlStructure(itConfigFile);
        settings = new XmlStructure(
                config.GetChild(Constants.SETTINGS_BRANCH));
        machineParam = new Parameters(config);

        // if a outputDir has not been given as argument, 
        // it is the output directory
        if(outputDirName == null) {
            // Load output directory from config file
            outputDirName = "./" 
                + config.GetSubChildValue(
                        Constants.ENVIRONMENT_NAME_TOTAL_PATH_BRANCH)
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
        List<Element> physList = config.GetChildren(
                Constants.PHYSICAL_LOCATION_BRANCH);
        // get all physical locations into the list
        for(Element elem : physList) {
            physLocs.add(new PhysicalLocation(elem, settings, machineParam,
                    netarchiveSuiteFile.getName(), logPropFile, 
                    secPolicyFile));
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
