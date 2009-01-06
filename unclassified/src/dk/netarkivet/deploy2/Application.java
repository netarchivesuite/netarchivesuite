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
import java.io.PrintWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Element;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * The application entity in the deploy structure.
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
    private String applicationId;

    /**
     * A application is the program to be run on a machine.
     * 
     * @param e The root of this instance in the XML document.
     * @param parentSettings The setting inherited by the parent.
     * @param param The machine parameters inherited by the parent.
     */
    public Application(Element e, XmlStructure parentSettings, 
            Parameters param) {
        ArgumentNotValid.checkNotNull(e, "Element e");
        ArgumentNotValid.checkNotNull(parentSettings, 
                "XmlStructure parentSettings");
        ArgumentNotValid.checkNotNull(param, "Parameters param");
        settings = new XmlStructure(parentSettings.getRoot());
        applicationRoot = e;
        machineParameters = new Parameters(param);
        // retrieve the specific settings for this instance 
        Element tmpSet = applicationRoot.element(Constants.SETTINGS_BRANCH);
        // Generate the specific settings by combining the general settings 
        // and the specific, (only if this instance has specific settings)
        if(tmpSet != null) {
            settings.OverWrite(tmpSet);
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
                String[] stlist = nameWithNamePath.split("[.]");
                name = stlist[stlist.length -1];
            } else {
                log.debug("Physical location has no name!");
                name = "";
                nameWithNamePath = "";
            }
            // look for the optional application instance id
            Element elem = applicationRoot.element(
                    Constants.APPLICATION_INSTANCE_ID_BRANCH);
            if(elem != null) {
                applicationId = elem.getText();
            } else {
                applicationId = null;
            }
        } catch(Exception e) {
            log.debug("Application variables not extractable! ");
            throw new IOFailure("Application variables not extractable! ");
        }
    }

     /**
     * Uses the name and the optional applicationId to create
     * an unique identification for this application.
     *  
     * @return The unique identification of this application.
     */
    public String getIdentification() {
        String res = name;
        // apply only applicationId if it exists and has content
        if(applicationId != null && !applicationId.isEmpty()) {
            res += "_";
            res += applicationId;
        }
        return res;
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
        // make file
        File settingsFile = new File(directory, 
                "settings_" + getIdentification() + ".xml");
        try {
            // initiate writer
            PrintWriter pw = new PrintWriter(settingsFile);
            try {
                // Extract the XML content of the branch for this application
                pw.println(settings.GetXML());
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
        return machineParameters.getInstallDir().getText() + "/"
            + settings.GetSubChildValue(
                    Constants.ENVIRONMENT_NAME_SETTING_PATH_BRANCH);
    }

    /**
     * Makes the install path with windows syntax.
     * 
     * @return The path with windows syntax.
     */
    public String installPathWindows() {
        return machineParameters.getInstallDir().getText() + "\\"
            + settings.GetSubChildValue(
                    Constants.ENVIRONMENT_NAME_SETTING_PATH_BRANCH);
    }

    /** 
     * For acquiring the machine parameter variable.
     * @return The machine parameter variable.
     */
    public Parameters getMachineParameters() {
        return machineParameters;
    }
}
