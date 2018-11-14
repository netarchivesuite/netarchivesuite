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
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;

/**
 * The application entity in the deploy hierarchy.
 */
public class Application {

    /** the log, for logging stuff instead of displaying them directly. */
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    /** the root-branch for this application in the XML tree. */
    private Element applicationRoot;
    /** The specific settings for this instance, inherited and overwritten. */
    private XmlStructure settings;
    /** parameters. */
    private Parameters machineParameters;
    /** Name of this instance. */
    private String name;
    /** The total name of this instance. */
    private String nameWithNamePath;
    /** application instance id (optional, used when two application has same name). */
    private String applicationInstanceId;

    /** The encoding to use when writing files. */
    private final String targetEncoding;

    /**
     * A application is the program to be run on a machine.
     *
     * @param subTreeRoot The root of this instance in the XML document.
     * @param parentSettings The setting inherited by the parent.
     * @param param The machine parameters inherited by the parent.
     * @param targetEncoding the encoding to use when writing files.
     */
    public Application(Element subTreeRoot, XmlStructure parentSettings, Parameters param, String targetEncoding) {
        ArgumentNotValid.checkNotNull(subTreeRoot, "Element e");
        ArgumentNotValid.checkNotNull(parentSettings, "XmlStructure parentSettings");
        ArgumentNotValid.checkNotNull(param, "Parameters param");
        ArgumentNotValid.checkNotNullOrEmpty(targetEncoding, "targetEncoding");
        this.targetEncoding = targetEncoding;
        settings = new XmlStructure(parentSettings.getRoot());
        applicationRoot = subTreeRoot;
        machineParameters = new Parameters(param);
        // retrieve the specific settings for this instance
        Element tmpSet = applicationRoot.element(Constants.COMPLETE_SETTINGS_BRANCH);
        // Generate the specific settings by combining the general settings
        // and the specific, (only if this instance has specific settings)
        if (tmpSet != null) {
            settings.overWrite(tmpSet);
        }
        // check if new machine parameters
        machineParameters.newParameters(applicationRoot);
        // Retrieve the variables for this instance.
        extractVariables();
    }

    /**
     * Extract the local variables from the root.
     * <p>
     * Currently, this is the name and the optional applicationId.
     */
    private void extractVariables() {
        try {
            // retrieve name
            Attribute at = applicationRoot.attribute(Constants.APPLICATION_NAME_ATTRIBUTE);
            if (at != null) {
                // the name is actually the classpath, so the specific class is
                // set as the name. It is the last element in the classpath.
                nameWithNamePath = at.getText().trim();
                // the classpath is is separated by '.'
                String[] stlist = nameWithNamePath.split(Constants.REGEX_DOT_CHARACTER);
                // take the last part of the application class path as name.
                // e.g.
                // dk.netarkivet.archive.bitarhcive.BitarchiveMonitorApplication
                // gets the name BitarchiveMonitorApplication.
                name = stlist[stlist.length - 1];

                // overwriting the name, if it exists already;
                // otherwise it is inserted.
                String xmlName = XmlStructure.pathAndContentToXML(nameWithNamePath,
                        Constants.COMPLETE_APPLICATION_NAME_LEAF);
                Element appXmlName = XmlStructure.makeElementFromString(xmlName);
                settings.overWrite(appXmlName);
            } else {
                log.warn("Application has no name!");
                throw new IllegalState("Application has no name!");
            }
            // look for the optional application instance id
            Element elem = settings.getSubChild(Constants.SETTINGS_APPLICATION_INSTANCE_ID_LEAF);
            if (elem != null && !elem.getText().trim().isEmpty()) {
                applicationInstanceId = elem.getText().trim();
            }
        } catch (Exception e) {
            log.debug("Application variables not extractable.", e);
            throw new IOFailure("Application variables not extractable.", e);
        }
    }

    /**
     * Uses the name and the optional applicationId to create an unique identification for this application.
     *
     * @return The unique identification of this application.
     */
    public String getIdentification() {
        StringBuilder res = new StringBuilder(name);
        // use only applicationInstanceId if it exists and has content
        if (applicationInstanceId != null && !applicationInstanceId.isEmpty()) {
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
     * Creates the settings file for this application. This is extracted from the XMLStructure and put into a specific
     * file. The name of the settings file for this application is: "settings_" + identification + ".xml".
     *
     * @param directory The directory where the settings file should be placed.
     */
    public void createSettingsFile(File directory) {
        ArgumentNotValid.checkNotNull(directory, "File directory");

        // make file
        File settingsFile = new File(directory, Constants.PREFIX_SETTINGS + getIdentification()
                + Constants.EXTENSION_XML_FILES);
        try {
            // initiate writer
            PrintWriter pw = new PrintWriter(settingsFile, targetEncoding);
            try {
                // Extract the XML content of the branch for this application
                pw.println(settings.getXML());
            } finally {
                pw.close();
            }
        } catch (FileNotFoundException e) {
            log.debug("Cannot create settings file for an application.", e);
            throw new IOFailure("Cannot create settings file for an application.", e);
        } catch (UnsupportedEncodingException e) {
            log.debug("Unsupported encoding '{}'", targetEncoding, e);
            throw new IOFailure("Unsupported encoding '" + targetEncoding + "'", e);
        }
    }

    /**
     * Makes the install path with linux syntax.
     *
     * @return The path in linux syntax.
     */
    public String installPathLinux() {
        return machineParameters.getInstallDirValue() + Constants.SLASH
                + settings.getSubChildValue(Constants.SETTINGS_ENVIRONMENT_NAME_LEAF);
    }

    /**
     * Makes the install path with windows syntax.
     *
     * @return The path with windows syntax.
     */
    public String installPathWindows() {
        return machineParameters.getInstallDirValue() + Constants.BACKSLASH
                + settings.getSubChildValue(Constants.SETTINGS_ENVIRONMENT_NAME_LEAF);
    }

    /**
     * For acquiring the machine parameter variable.
     *
     * @return The machine parameter variable.
     */
    public Parameters getMachineParameters() {
        return machineParameters;
    }

    /**
     * For acquiring all the values of the leafs at the end of the path.
     *
     * @param path The path to the branches.
     * @return The values of the leafs. If no values were found, then an empty collection of strings are returned.
     */
    public String[] getSettingsValues(String[] path) {
        ArgumentNotValid.checkNotNull(path, "String[] path");
        ArgumentNotValid.checkNotNegative(path.length, "Length of String[] path");
        return settings.getLeafValues(path);
    }

    /**
     * Returns the settings XML subtree for the application.
     * @return the settings XML subtree for the application
     */
    public XmlStructure getSettings() {
    	return settings;
    }

    /**
     * Detects whether this is a Harvester app, which requires a harvester bundle to be deployed.
     * @return <code>true if the is a harvester requiring a harvester bundle, else <code>false</code>.</code>
     */
    public boolean isBundledHarvester() {
        List<Element> classPaths = getMachineParameters().getClassPaths();
        for (Element classPathElement : classPaths) {
            if (classPathElement.getText().contains("heritrix3")) {
                return true;
            }
        }
        return false;
    }

}
