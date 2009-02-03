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
import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import dk.netarkivet.common.utils.FileUtils;

/**
 * Class for evaluating a config file.
 * Tests the settings in the config file against default settings to
 * test for wrongly assigned elements. 
 * 
 */
public class EvaluateConfigFile {
    /** The elements to check the settings against.*/
    private Element completeSettings;
    /** The root element in the xml tree.*/
    private XmlStructure root;
    /** the log, for logging stuff instead of displaying them directly.*/
    private final Log log = LogFactory.getLog(getClass().getName());
    
    /**
     * Constructor.
     * Only initialises the config file and settings list.
     * 
     * @param itConfigFile The file to evaluate.
     */
    public EvaluateConfigFile(File itConfigFile) {
        try {
            initLoadDefaultSettings();
            root = new XmlStructure(itConfigFile);
        } catch (Exception e) {
            System.err.println("Evaluation error!");
        }
    }
    
    /**
     * Evaluates the config file.
     * This is done by evaluating the settings branch for all the instances in 
     * the XML-tree (global, physical locaiton, machine and application)
     */
    @SuppressWarnings("unchecked")
    public void evaluate() {
	try {
            // check global settings
            evaluateElement(root.getChild(Constants.COMPLETE_SETTINGS_BRANCH));
            List<Element> physLocs = root.getChildren(
                    Constants.DEPLOY_PHYSICAL_LOCATION);
            for(Element pl : physLocs) {
                // check physical location settings
                evaluateElement(pl.element(Constants.COMPLETE_SETTINGS_BRANCH));
                List<Element> macs = pl.elements(Constants.DEPLOY_MACHINE);
                for(Element mac : macs) {
                    // check machine settings 
                    evaluateElement(mac
                            .element(Constants.COMPLETE_SETTINGS_BRANCH));
                    List<Element> apps = mac
                            .elements(Constants.DEPLOY_APPLICATION_NAME);
                    for(Element app : apps) {
                        // check application settings
                        evaluateElement(app
                                .element(Constants.COMPLETE_SETTINGS_BRANCH));
                    }
                }
            }
	} catch (Exception e) {
	    System.err.println("Unknown evaluation error: " + e);
	    log.trace("Error occured during evaluation: " + e);
	}
    }
    
    /**
     * Load the default settings files as reference trees.
     * These are used for testing whether the branches in the settings file
     * are to be used or not.
     * @throws IOException If file not found.
     */
    private void initLoadDefaultSettings() throws IOException {
        File f = FileUtils.getResourceFileFromClassPath(
                Constants.BUILD_COMPLETE_SETTINGS_FILE_PATH);
        try {
            Document doc;
            SAXReader reader = new SAXReader();
            if (f.canRead()) {
                doc =  reader.read(f);
                completeSettings = doc.getRootElement();
            } else {
                System.out.println("Cannot read file: " 
                        + f.getAbsolutePath());
            }
        } catch (DocumentException e) {
            System.err.println("Problems with file: " 
                    + f.getAbsolutePath());
        }
    }
    
    /**
     * Evaluates a element (has to called with the settings branch).
     * Then tries to evaluate all the branches to the element. 
     * The method is called recursively for the children of curElem.
     * 
     * @param curElem The current element to evaluate. 
     */
    @SuppressWarnings("unchecked")
    private void evaluateElement(Element curElem) {
        // make sure to catch null-pointers
        if(curElem == null) {
            return;
        }
        List<Element> elList = curElem.elements();
        for(Element el : elList) {
            boolean valid = false;
            // get path
            String path = getSettingsPath(el);

            // check if path exists in any default setting.
                valid = existBranch(completeSettings, path.split("/"));

            if(valid) {
                if(!el.isTextOnly()) {
                    evaluateElement(el);
                }
            } else {
                System.out.println("Branch in settings not found: " 
                        + path.replace("/", "."));
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
        for(String st : path) {
            if(curE == null) {
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
        String[] elList = el.getPath().split("/");

        StringBuilder res = new StringBuilder();
        int i = 0;
        // find the index for settings
        while(i < elList.length && !elList[i].equalsIgnoreCase("settings")) {
            i++;
        }

        for(i++; i<elList.length; i++) {
            res.append(elList[i]);
            res.append("/");
        }

        // remove last '/'
       res.deleteCharAt(res.length()-1);

       return res.toString();
    }
}
