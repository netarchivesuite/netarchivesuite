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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.deploy2;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Element;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * The Parameters class contains the machine parameters.
 * This is the user name, the install directory and the parameters for running
 * the Java applications.
 * This is inherited from the parent entity (e.g. the machine inherits the 
 * Parameters from the PhysicalLocation), then overwrites its own specified 
 * variables.
 */
public class Parameters {
    /** the log, for logging stuff instead of displaying them directly.*/
    protected final Log log = LogFactory.getLog(getClass().getName());
    /** The class paths.*/
    protected List<Element> classPaths;
    /** The options for java.*/
    protected List<Element> javaOptions;
    /** Install directory.*/
    protected Element installDir;
    /** The machine user name.*/
    protected Element machineUserName;

    /**
     * Constructor.
     * Retrieves the parameters from the XML tree.
     * 
     * @param root The root of the branch for the parent instance.
     * This retrieves the variables in the branch.
     */
    public Parameters(XmlStructure root) {
        ArgumentNotValid.checkNotNull(root, "XmlStructure root");
        // initialise variables
        classPaths = root.GetChildren(Constants.CLASS_PATH_BRANCH);
        javaOptions = root.GetChildren(Constants.JAVA_OPTIONS_BRANCH);
        installDir = root.GetChild(Constants.PARAMETER_INSTALL_DIR_BRANCH);
        machineUserName = root.GetChild(
                Constants.PARAMETER_MACHINE_USER_NAME_BRANCH);
    }

    /**
     * Constructor.
     * Inherits the parameters of the parent instance.
     * 
     * @param parent The parameters of the parent instance.
     */
    public Parameters(Parameters parent) {
        ArgumentNotValid.checkNotNull(parent, "Parameter parent");
        // copy parent class paths
        classPaths = new ArrayList<Element>();
        for(Element e : parent.classPaths) {
                classPaths.add(e.createCopy());
        }
        // copy parent java options
        javaOptions = new ArrayList<Element>();
        for(Element e : parent.javaOptions) {
            javaOptions.add(e.createCopy());
        }
        // copy parent install dir (if any)
        if(parent.installDir != null) {
            installDir = parent.installDir.createCopy();
        } else {
            installDir = null;
        }
        // copy parent install dir (if any)
        if(parent.machineUserName != null) {
            machineUserName = parent.machineUserName.createCopy();
        } else {
            machineUserName = null;
        }
    }

    /**
     * Overwrites the inherited parameters, if the root has new specified.
     * 
     * @param root The root of the current instance.
     */
    @SuppressWarnings("unchecked")
    public void newParameters(Element root) {
        ArgumentNotValid.checkNotNull(root, "Element root");
        List<Element> tmp;
        // check if any class paths to overwrite existing
        tmp = root.elements(Constants.CLASS_PATH_BRANCH);
        if(tmp.size() > 0) {
            classPaths = tmp;
        }
        // check if any java options to overwrite existing
        tmp = root.elements(Constants.JAVA_OPTIONS_BRANCH);
        if(tmp.size() > 0) {
            javaOptions = tmp;
        }
        // check if new install dir to overwrite existing
        tmp = root.elements(Constants.PARAMETER_INSTALL_DIR_BRANCH);
        if(tmp.size() > 0) {
            installDir = tmp.get(0);
        }
        // check if new install dir to overwrite existing
        tmp = root.elements(Constants.PARAMETER_MACHINE_USER_NAME_BRANCH);
        if(tmp.size() > 0) {
            machineUserName = tmp.get(0);
        }
    }

    /**
     * Makes all the java options into a single String.
     * 
     * @return All the java options.
     */
    public String writeJavaOptions() {
        String res = "";
        // apply the java options
        for(Element e : javaOptions) {
            res += e.getText() + " ";
        }
        return res;
    }
}
