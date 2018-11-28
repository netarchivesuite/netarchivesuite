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
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.FileUtils;

/**
 * Class for evaluating a config file. Tests the settings in the config file against default settings to test for
 * wrongly assigned elements.
 */
public class EvaluateConfigFile {

    /** The elements to check the settings against. */
    private Element completeSettings;
    /** The root element in the xml tree. */
    private XmlStructure root;
    /** the log, for logging stuff instead of displaying them directly. */
    private static final Logger log = LoggerFactory.getLogger(EvaluateConfigFile.class);

    /**
     * Constructor. Only initialises the config file and settings list.
     *
     * @param deployConfigFile The file to evaluate.
     * @param encoding the encoding to use to read from file
     */
    public EvaluateConfigFile(File deployConfigFile, String encoding) {
        ArgumentNotValid.checkNotNull(deployConfigFile, "File deployConfigFile");
        initLoadDefaultSettings();
        root = new XmlStructure(deployConfigFile, encoding);
    }

    /**
     * Evaluates the config file. This is done by evaluating the settings branch for all the instances in the XML-tree
     * (global, physical locaiton, machine and application)
     */
    @SuppressWarnings("unchecked")
    public void evaluate() {
        try {
            // check global settings
            evaluateElement(root.getChild(Constants.COMPLETE_SETTINGS_BRANCH));
            List<Element> physLocs = root.getChildren(Constants.DEPLOY_PHYSICAL_LOCATION);
            for (Element pl : physLocs) {
                // check physical location settings
                evaluateElement(pl.element(Constants.COMPLETE_SETTINGS_BRANCH));
                List<Element> macs = pl.elements(Constants.DEPLOY_MACHINE);
                for (Element mac : macs) {
                    // check machine settings
                    evaluateElement(mac.element(Constants.COMPLETE_SETTINGS_BRANCH));
                    List<Element> apps = mac.elements(Constants.DEPLOY_APPLICATION_NAME);
                    for (Element app : apps) {
                        // check application settings
                        evaluateElement(app.element(Constants.COMPLETE_SETTINGS_BRANCH));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error occured during evaluation: ", e);
        }
    }

    /**
     * Load the default settings files as reference trees. These are used for testing whether the branches in the
     * settings file are to be used or not.
     */
    private void initLoadDefaultSettings() {
        File f = FileUtils.getResourceFileFromClassPath(Constants.BUILD_COMPLETE_SETTINGS_FILE_PATH);
        try {
            Document doc;
            SAXReader reader = new SAXReader();
            if (f.canRead()) {
                doc = reader.read(f);
                completeSettings = doc.getRootElement();
            } else {
                log.warn("Cannot read file: '{}'", f.getAbsolutePath());
            }
        } catch (DocumentException e) {
            log.error("Cannot handle complete settings file.", e);
            throw new IOFailure("Cannot handle complete settings file.", e);
        }
    }

    /**
     * Evaluates a element (has to called with the settings branch). Then tries to evaluate all the branches to the
     * element. The method is called recursively for the children of curElem.
     *
     * @param curElem The current element to evaluate. Null element represents in this context that no settings branch
     * exists for the current instance.
     */
    @SuppressWarnings("unchecked")
    private void evaluateElement(Element curElem) {
        // make sure to catch null-pointers
        if (curElem == null) {
            return;
        }
        List<Element> elList = curElem.elements();
        for (Element el : elList) {
            boolean valid = false;
            // get path
            String path = getSettingsPath(el);

            // check if path exists in any default setting.
            valid = existBranch(completeSettings, path.split(Constants.SLASH));

            if (valid) {
                if (!el.isTextOnly()) {
                    evaluateElement(el);
                }
            } else {
                // Print out the 'illegal' branches.
                System.out.println("Branch in settings not found: " + path.replace(Constants.SLASH, Constants.DOT));
            }
        }
    }

    /**
     * For testing whether a branch with the current path exists.
     *
     * @param settings The root of the default settings XML-tree.
     * @param path The path to the branch to test.
     * @return Whether the branch at the end of the path in the root exists.
     */
    private boolean existBranch(Element settings, String[] path) {
        Element curE = settings;
        for (String st : path) {
            if (curE == null) {
                return false;
            }
            curE = curE.element(st);
        }

        // return whether the final branch exists.
        return (curE != null);
    }

    /**
     * Gets the path from settings of an element.
     *
     * @param el The element to get the settings path.
     * @return The path from settings to the element, in the XML-tree.
     */
    private String getSettingsPath(Element el) {
        String[] elList = el.getPath().split(Constants.SLASH);

        StringBuilder res = new StringBuilder();
        int i = 0;
        // find the index for settings
        while (i < elList.length && !elList[i].equalsIgnoreCase(Constants.COMPLETE_SETTINGS_BRANCH)) {
            ++i;
        }

        // TODO WTF?!
        for (i++; i < elList.length; i++) {
            res.append(elList[i]);
            res.append(Constants.SLASH);
        }

        // remove last '/'
        res.deleteCharAt(res.length() - 1);

        return res.toString();
    }

}
