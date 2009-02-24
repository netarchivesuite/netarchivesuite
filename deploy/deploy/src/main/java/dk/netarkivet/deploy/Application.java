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
import java.io.PrintWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Element;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;

/**
 * The application entity in the deploy hierarchy.
 */
public class Application {
    /** the log, for logging stuff instead of displaying them directly.*/ 
    private final Log log = LogFactory.getLog(getClass().getName());

    /** the root-branch for this application in the XML tree.*/
    private Element applicationRoot;
    /** The specific settings for this instance, inherited and overwritten.*/
    private XmlStructure settings;
    /** parameters.*/
    private Parameters machineParameters;
    /** Name of this instance.*/
    private String name;
    /** The total name of this instance.*/
    private String nameWithNamePath;
    /** application instance id 
     * (optional, used when two application has same name).
     */
    private String applicationInstanceId;

    /**
     * A application is the program to be run on a machine.
     * 
     * @param subTreeRoot The root of this instance in the XML document.
     * @param parentSettings The setting inherited by the parent.
     * @param param The machine parameters inherited by the parent.
     */
    public Application(Element subTreeRoot, XmlStructure parentSettings, 
            Parameters param) {
        ArgumentNotValid.checkNotNull(subTreeRoot, "Element e");
        ArgumentNotValid.checkNotNull(parentSettings, 
                "XmlStructure parentSettings");
        ArgumentNotValid.checkNotNull(param, "Parameters param");
        settings = new XmlStructure(parentSettings.getRoot());
        applicationRoot = subTreeRoot;
        machineParameters = new Parameters(param);
        // retrieve the specific settings for this instance 
        Element tmpSet = applicationRoot
            .element(Constants.COMPLETE_SETTINGS_BRANCH);
        // Generate the specific settings by combining the general settings 
        // and the specific, (only if this instance has specific settings)
        if(tmpSet != null) {
            settings.overWrite(tmpSet);
        }
        // check if new machine parameters
        machineParameters.newParameters(applicationRoot);
        // Retrieve the variables for this instance.
        extractVariables();
    }

    /**
     * Extract the local variables from the root.
     * 
     * Currently, this is the name and the optional applicationId.
     */
    private void extractVariables() {
        try {
            // retrieve name
            Attribute at = applicationRoot.attribute(
                    Constants.APPLICATION_NAME_ATTRIBUTE);
            if(at != null) {
                // the name is actually the classpath, so the specific class is
                // set as the name. It is the last element in the classpath.
                nameWithNamePath = at.getText();
                // the classpath is is separated by '.'
                String[] stlist = nameWithNamePath.split(
                        Constants.REGEX_DOT_CHARACTER);
                // take the last part of the application class path as name. 
                // e.g.
                // dk.netarkivet.archive.bitarhcive.BitarchiveMonitorApplication
                // gets the name BitarchiveMonitorApplication.
                name = stlist[stlist.length -1];
                
                // overwriting the name, if it exists already; 
                // otherwise it is inserted. 
                String xmlName = XmlStructure.pathAndContentToXML(
                        nameWithNamePath, 
                        Constants.COMPLETE_APPLICATION_NAME_LEAF);
                Element appXmlName = XmlStructure.makeElementFromString(
                    xmlName);
                settings.overWrite(appXmlName);
            } else {
                String msg = "Application has no name!"; 
                log.warn(msg);
                throw new IllegalState(msg);
            }
            // look for the optional application instance id
            Element elem = settings.getSubChild(
                    Constants.SETTINGS_APPLICATION_INSTANCE_ID_LEAF);
            if(elem != null && !elem.getText().isEmpty()) {
                applicationInstanceId = elem.getText();
            } 
        } catch(Exception e) {
            String msg = "Application variables not extractable: " + e; 
            log.debug(msg);
            throw new IOFailure(msg);
        }
    }

     /**
     * Uses the name and the optional applicationId to create
     * an unique identification for this application.
     *  
     * @return The unique identification of this application.
     */
    public String getIdentification() {
        StringBuilder res = new StringBuilder(name);
        // use only applicationInstanceId if it exists and has content
        if(applicationInstanceId != null && !applicationInstanceId.isEmpty()) {
            res.append(Constants.UNDERSCORE);
            res.append(applicationInstanceId);
        }
        return res.toString();
    }

    /**
     * @return the total name with directory path.
     */
    public String getTotalName() {
        return nameWithNamePath;
    }

     /**
     * Creates the settings file for this application.
     * This is extracted from the XMLStructure and put into a specific file.
     * The name of the settings file for this application is:
     * "settings_" + identification + ".xml".
     * 
     * @param directory The directory where the settings file should be placed.
     */
    public void createSettingsFile(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");

        // make file
        File settingsFile = new File(directory, 
                Constants.PREFIX_SETTINGS + getIdentification()
                + Constants.EXTENSION_XML_FILES);
        try {
            // initiate writer
            PrintWriter pw = new PrintWriter(settingsFile);
            try {
                // Extract the XML content of the branch for this application
                pw.println(settings.getXML());
            } finally {
                pw.close();
            }
        } catch (Exception e) {
            log.debug("Error in creating settings file for application: " + e);
            throw new IOFailure("Cannot create settings file: " + e);
        }
    }

    /**
     * Makes the install path with linux syntax.
     * 
     * @return The path in linux syntax.
     */
    public String installPathLinux() {
        return machineParameters.getInstallDirValue() + Constants.SLASH
            + settings.getSubChildValue(
                    Constants.SETTINGS_ENVIRONMENT_NAME_LEAF);
    }

    /**
     * Makes the install path with windows syntax.
     * 
     * @return The path with windows syntax.
     */
    public String installPathWindows() {
        return machineParameters.getInstallDirValue() + Constants.BACKSLASH
            + settings.getSubChildValue(
                    Constants.SETTINGS_ENVIRONMENT_NAME_LEAF);
    }

    /** 
     * For acquiring the machine parameter variable.
     * @return The machine parameter variable.
     */
    public Parameters getMachineParameters() {
        return machineParameters;
    }
    
    /**
     * For acquiring all the values of the leafs at the end of the path.
     * 
     * @param path The path to the branches.
     * @return The values of the leafs.
     */
    public String[] getSettingsValues(String[] path) {
        ArgumentNotValid.checkNotNull(path, "String[] path");
        ArgumentNotValid.checkNotNegative(path.length, 
                "Length of String[] path");

        return settings.getLeafValues(path);
    }
}
