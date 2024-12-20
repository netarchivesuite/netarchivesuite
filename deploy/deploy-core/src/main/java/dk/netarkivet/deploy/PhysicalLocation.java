/*
 * #%L
 * Netarchivesuite - deploy
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
package dk.netarkivet.deploy;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;

/**
 * The physical location class.
 */
public class PhysicalLocation {

    /** the log, for logging stuff instead of displaying them directly. */
    private static final Logger log = LoggerFactory.getLogger(PhysicalLocation.class);
    /** The root for the branch of this element in the XML-tree. */
    private Element physLocRoot;
    /** The settings structure. */
    private XmlStructure settings;
    /** The parameters for java. */
    private Parameters machineParameters;
    /** The list of the machines. */
    private List<Machine> machines;
    /** The name of this physical location. */
    private String name;
    /** The inherited name for the NetarchiveSuite file. */
    private String netarchiveSuiteFileName;
    /** The inherited SLF4J config file. */
    private File slf4JConfigFile;
    /** The inherited security file. */
    private File securityPolicyFile;
    /** The inherited database file name. */
    private File databaseFile;
    /** The inherited archive database file name. */
    private File arcDatabaseFile;
    /** The optional choice for resetting tempDir. */
    private boolean resetDirectory;
    /** The folder for the external jar libraries. */
    private File jarFolder;
    /** The folder for the source of the bitmagasin configuration. */
    private File bitmagFolder;
    /** user specific logo png file */
    protected File logoFile;
    /** user specific menulogo png file */
    protected File menulogoFile;

    private final DeployConfiguration deployConfiguration;

    /**
     * The physical locations is referring to the position in the real world where the computers are located. One
     * physical location can contain many machines.
     *
     * @param subTreeRoot The root of this branch in the XML structure.
     * @param parentSettings The settings of the parent (deploy-config).
     * @param param The parameters of the parent (deploy-config).
     * @param netarchiveSuiteSource The name of the NetarchiveSuite file.
     * @param securityPolicy The security policy file.
     * @param dbFile The harvest definition database.
     * @param arcdbFile The archive database.
     * @param resetDir Whether the temporary directory should be reset.
     * @param externalJarFolder The folder containing the external jar library files.
     * @throws ArgumentNotValid If one of the following arguments are null: subTreeRoot, parentSettings, param, logProp,
     * securityPolicy; or if the netarchiveSuiteSource if either null or empty.
     */
    public PhysicalLocation(Element subTreeRoot, XmlStructure parentSettings, Parameters param,
            String netarchiveSuiteSource, File slf4JConfig, File securityPolicy, File dbFile,
            File arcdbFile, boolean resetDir, File externalJarFolder, File aLogoFile, File aMenulogoFile, File bitmagFolder, DeployConfiguration deployConfiguration) throws ArgumentNotValid {
        // test if valid arguments
        ArgumentNotValid.checkNotNull(subTreeRoot, "Element elem (physLocRoot)");
        ArgumentNotValid.checkNotNull(parentSettings, "XmlStructure parentSettings");
        ArgumentNotValid.checkNotNull(param, "Parameters param");
        ArgumentNotValid.checkNotNullOrEmpty(netarchiveSuiteSource, "String netarchiveSuite");
        // ArgumentNotValid.checkNotNull(slf4JConfig, "File slf4JConfig");
        ArgumentNotValid.checkNotNull(securityPolicy, "File securityPolicy");

        // make a copy of parent, don't use it directly.
        settings = new XmlStructure(parentSettings.getRoot());
        physLocRoot = subTreeRoot;
        machineParameters = new Parameters(param);
        netarchiveSuiteFileName = netarchiveSuiteSource;
        slf4JConfigFile = slf4JConfig;
        securityPolicyFile = securityPolicy;
        databaseFile = dbFile;
        arcDatabaseFile = arcdbFile;
        resetDirectory = resetDir;
        jarFolder = externalJarFolder;
        logoFile = aLogoFile;
        menulogoFile = aMenulogoFile;
        this.bitmagFolder = bitmagFolder;
        this.deployConfiguration = deployConfiguration;

        // retrieve the specific settings for this instance
        Element tmpSet = physLocRoot.element(Constants.COMPLETE_SETTINGS_BRANCH);
        // Generate the specific settings by combining the general settings
        // and the specific, (only if this instance has specific settings)
        if (tmpSet != null) {
            settings.overWrite(tmpSet);
        }
        // check if new machine parameters
        machineParameters.newParameters(physLocRoot);
        // Retrieve the variables for this instance.
        extractVariables();
        // generate the machines on this instance
        extractMachines();
    }

    /**
     * Extract the local variables from the root.
     * <p>
     * It is only the name for this instance. This is then set in settings.
     */
    private void extractVariables() {
        // retrieve name
        Attribute at = physLocRoot.attribute(Constants.PHYSICAL_LOCATION_NAME_ATTRIBUTE);
        if (at != null) {
            name = at.getText().trim();
            // insert the name in settings.
            String xmlName = XmlStructure.pathAndContentToXML(name, Constants.COMPLETE_THIS_PHYSICAL_LOCATION_LEAF);
            Element physLocName = XmlStructure.makeElementFromString(xmlName);
            settings.overWrite(physLocName);
        } else {
            throw new IllegalState(Constants.MSG_ERROR_PHYSICAL_LOCATION_NO_NAME);
        }
    }

    /**
     * Extracts the XML for machines from the root, creates the machines, and puts them into the list.
     */
    @SuppressWarnings("unchecked")
    private void extractMachines() {
        machines = new ArrayList<Machine>();
        List<Element> le = physLocRoot.elements(Constants.DEPLOY_MACHINE);
        for (Element e : le) {
            String os = getTrimmedAttributeValue(e, Constants.MACHINE_OPERATING_SYSTEM_ATTRIBUTE);
            // only a windows machine, if the 'os' attribute exists and
            // equals (not case-sensitive) 'windows'. Else linux machine
            if (os != null && os.equalsIgnoreCase(Constants.OPERATING_SYSTEM_WINDOWS_ATTRIBUTE)) {
                machines.add(new WindowsMachine(e, settings, machineParameters, netarchiveSuiteFileName,
                        slf4JConfigFile, securityPolicyFile, databaseFile, arcDatabaseFile, resetDirectory, jarFolder,
                        logoFile, menulogoFile, bitmagFolder));
            } else {
                machines.add(new LinuxMachine(e, settings, machineParameters, netarchiveSuiteFileName,
                        slf4JConfigFile, securityPolicyFile, databaseFile, arcDatabaseFile, resetDirectory, jarFolder, logoFile, menulogoFile,
                        deployConfiguration, bitmagFolder));
            }
        }
    }

    private String getTrimmedAttributeValue(Element e, String attributeName) {
        String value = e.attributeValue(attributeName);
        return (value != null ? value.trim() : null);
    }

    /**
     * Initiate the creation of global scripts and machine scripts.
     *
     * @param directory The directory where the files are to be placed.
     * @throws ArgumentNotValid If the directory is null.
     */
    public void write(File directory) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        // make the script in the directory!
        makeScripts(directory);
        // write all machine at this location
        for (Machine mac : machines) {
            mac.write(directory);
        }
    }

    /**
     * Creates the following scripts for this physical location. * killall. * install. * startall.
     * <p>
     * The scripts for a physical location will only work from Linux/Unix.
     *
     * @param directory The directory where the scripts are to be placed.
     * @throws ArgumentNotValid If the directory is null.
     * @throws IOFailure If an error occurs during the creation of the scripts.
     */
    private void makeScripts(File directory) throws ArgumentNotValid, IOFailure {
        ArgumentNotValid.checkNotNull(directory, "File directory");
        // make extension (e.g. '_kb.sh' in the script 'killall_kb.sh')
        String ext = Constants.UNDERSCORE + name + Constants.SCRIPT_EXTENSION_LINUX;
        // make script files
        File killall = new File(directory, Constants.SCRIPT_NAME_KILL_ALL + ext);
        File install = new File(directory, Constants.SCRIPT_NAME_INSTALL_ALL + ext);
        File startall = new File(directory, Constants.SCRIPT_NAME_START_ALL + ext);
        try {
            // Make the killall script for the physical location
            PrintWriter kWriter = new PrintWriter(killall);
            try {
                kWriter.println(ScriptConstants.BIN_BASH_COMMENT);
                // insert machine data
                for (Machine mac : machines) {
                    // write the call to the kill script of each machines
                    kWriter.println(ScriptConstants.writeDashLine());
                    kWriter.print(mac.writeToGlobalKillScript());
                }
                // write an extra line of dashes.
                kWriter.println(ScriptConstants.writeDashLine());
            } finally {
                // close writer
                kWriter.flush();
                kWriter.close();
            }

            // Make the install script for the physical location
            PrintWriter iWriter = new PrintWriter(install);
            try {
                iWriter.println(ScriptConstants.BIN_BASH_COMMENT);
                // insert machine data
                for (Machine mac : machines) {
                    // write install script from machines
                    iWriter.println(ScriptConstants.writeDashLine());
                    iWriter.print(mac.writeToGlobalInstallScript());
                }
                // write an extra line of dashes.
                iWriter.println(ScriptConstants.writeDashLine());
            } finally {
                // close writer
                iWriter.flush();
                iWriter.close();
            }

            // Make the startall script for the physical location
            PrintWriter sWriter = new PrintWriter(startall);
            try {
                sWriter.println(ScriptConstants.BIN_BASH_COMMENT);
                // insert machine data
                for (Machine mac : machines) {
                    // write start script from machines
                    sWriter.println(ScriptConstants.writeDashLine());
                    sWriter.print(mac.writeToGlobalStartScript());
                }
                sWriter.println(ScriptConstants.writeDashLine());
            } finally {
                // close writer
                sWriter.flush();
                sWriter.close();
            }
        } catch (IOException e) {
            String msg = "Cannot create the scripts for the physical " + "location: '" + name + "'.";
            log.trace(msg, e);
            throw new IOFailure(msg, e);
        }
    }

}
