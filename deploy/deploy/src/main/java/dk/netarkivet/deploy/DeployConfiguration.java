/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *   USA
 */
package dk.netarkivet.deploy;

import java.io.File;
import java.io.IOException;
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
    /** The name of the archive database.*/
    private File arcDatabaseFileName;
    /** The optional choice for resetting tempDir.*/
    private boolean resetDirectory;

    /**
     * Constructor. Initialise everything.
     * 
     * @param deployConfigFileName Name of configuration file.
     * @param netarchiveSuiteFileName Name of installation file.
     * @param secPolicyFileName Name of security policy file.
     * @param logPropFileName Name of the log property file.
     * @param outputDirName Directory for the output.
     * @param dbFileName Name of the database.
     * @param arcdbFileName The name of the archive database.
     * @param resetDir Whether the temporary directory should be reset.
     * @throws ArgumentNotValid If one of the following arguments is null:
     * deployConfigFileName netarchiveSuiteFileName, secPolicyFileName,
     * logPropFileName.
     */
    public DeployConfiguration(File deployConfigFileName, 
            File netarchiveSuiteFileName,  File secPolicyFileName, 
            File logPropFileName, String outputDirName, File dbFileName, 
            File arcdbFileName, boolean resetDir) throws ArgumentNotValid {
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
        arcDatabaseFileName = arcdbFileName;
        resetDirectory = resetDir;

        // get configuration tree, settings and parameters
        config = new XmlStructure(deployConfigFile);
        settings = new XmlStructure(
                config.getChild(Constants.COMPLETE_SETTINGS_BRANCH));
        machineParam = new Parameters(config);
        
        String environmentName = config.getSubChildValue(
                Constants.COMPLETE_ENVIRONMENT_NAME_LEAF);

        // vaildate the environment name.
        if(!Constants.validEnvironmentName(environmentName)) {
            System.err.print(Constants.MSG_ERROR_INVALID_ENVIRONMENT_NAME
                    + environmentName);
            System.out.println();
            System.exit(1);
        }

        // if a outputDir has not been given as argument, 
        // it is the output directory
        if(outputDirName == null) {
            // Load output directory from config file
            outputDirName = Constants.DOT + Constants.SLASH 
                + environmentName + Constants.SLASH;
        }
        outputDir = new File(outputDirName);
        // make sure that directory outputDir exists
        FileUtils.createDir(outputDir);
        extractElements();
        
        copyNetarchiveSuiteFile();
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
                    secPolicyFile, databaseFileName, arcDatabaseFileName, 
                    resetDirectory));
        }
    }
    
    /**
     * Method for copying the NetarchiveSuite file to output directory.
     * This handles the following three scenarios: 
     * 
     * <ol>
     * <li> outputDir == directory of zip file </li> 
     *  Do nothing.  <br>
     * <li> outputDir != directory of zip file, but another zip file with same 
     * name exists in the directory </li> 
     * Remove the existing file before copy of the given file. <br>
     * <li> outputDir != directory of zip file and no other zip file </li>
     *    Copy file to output directory.
     * </ol>
     */
    private void copyNetarchiveSuiteFile() {
        try {
            // initialise the new file. This should have the same name 
            // as the original file, but be placed in the output directory.
            File newNetarchiveSuiteFile = new File(outputDir, 
                    netarchiveSuiteFile.getName());

            // check first scenario
            if(newNetarchiveSuiteFile.getCanonicalPath().equals(
                    netarchiveSuiteFile.getCanonicalPath())) {
                // thus first scenario is true, and nothing should be done.
                return;
            }

            // If the file exists = second scenario, and thus issue warning and
            // delete file before the given netarchiveSuiteFile is copied.
            if(newNetarchiveSuiteFile.exists()) { 
                // issue warning
                System.out.println(Constants.MSG_WARN_ZIPFILE_ALREADY_EXISTS
                        + newNetarchiveSuiteFile.getCanonicalPath());
                newNetarchiveSuiteFile.delete();
            }
            
            // copy the file.
            FileUtils.copyFile(netarchiveSuiteFile, newNetarchiveSuiteFile);
        } catch (IOException e) {
            // handle exceptions
            System.out.println(Constants.MSG_ERROR_ZIP_CANNONICAL_PATH 
                    + netarchiveSuiteFile.getAbsolutePath());
            e.printStackTrace();
            System.exit(1);
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
