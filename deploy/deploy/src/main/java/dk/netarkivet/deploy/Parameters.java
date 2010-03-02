/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import java.util.ArrayList;
import java.util.List;

import org.dom4j.Element;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * The Parameters class contains the machine parameters.
 * These are the user name, the install directory and the parameters for running
 * the Java applications.
 * These are inherited from the parent entity (e.g. the machine inherits the 
 * Parameters from the PhysicalLocation), then overwrites its own specified 
 * variables.
 */
public class Parameters {
    /** The class paths.*/
    private List<Element> classPaths;
    /** The options for java.*/
    private List<Element> javaOptions;
    /** Install directory.*/
    private Element installDir;
    /** The machine user name.*/
    private Element machineUserName;
    /** The directory for the database.*/
    private Element databaseDir;
    /** The directory for the archive database.*/
    private Element arcDatabaseDir;

    /**
     * Constructor.
     * Retrieves the parameters from the XML tree.
     * 
     * @param root The root of the branch for the parent instance.
     * This retrieves the variables in the branch.
     * @throws ArgumentNotValid If the root is null.
     */
    public Parameters(XmlStructure root) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(root, "XmlStructure root");
        // initialise variables
        classPaths = root.getChildren(Constants.DEPLOY_CLASS_PATH);
        javaOptions = root.getChildren(Constants.DEPLOY_JAVA_OPTIONS);
        installDir = root.getChild(Constants.DEPLOY_INSTALL_DIR);
        machineUserName = root.getChild(
                Constants.DEPLOY_MACHINE_USER_NAME);
        databaseDir = root.getChild(Constants.DEPLOY_DATABASE_DIR);
        arcDatabaseDir = root.getChild(
                Constants.DEPLOY_ARCHIVE_DATABASE_DIR);
    }

    /**
     * Constructor.
     * Inherits the parameters of the parent instance.
     * 
     * @param parent The parameters of the parent instance.
     * @throws ArgumentNotValid If the parent is null.
     */
    public Parameters(Parameters parent) throws ArgumentNotValid {
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
        } 
        // copy parent install dir (if any)
        if(parent.machineUserName != null) {
            machineUserName = parent.machineUserName.createCopy();
        } 
        // copy parent harvest database dir (if any)
        if(parent.databaseDir != null) {
            databaseDir = parent.databaseDir.createCopy();
        }
        // copy the parent archive database dir (if any)
        if(parent.arcDatabaseDir != null) {
            arcDatabaseDir = parent.arcDatabaseDir.createCopy();
        }
    }

    /**
     * Overwrites the inherited parameters, if the root has new specified.
     * 
     * @param root The root of the current instance.
     * @throws ArgumentNotValid If the root is null.
     */
    @SuppressWarnings("unchecked")
    public void newParameters(Element root) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNull(root, "Element root");
        List<Element> tmp;
        // check if root contains any class paths to overwrite inherited ones.
        tmp = root.elements(Constants.DEPLOY_CLASS_PATH);
        if(tmp.size() > 0) {
            classPaths = tmp;
        }
        // check if root contains any java options to overwrite inherited ones.
        tmp = root.elements(Constants.DEPLOY_JAVA_OPTIONS);
        if(tmp.size() > 0) {
            javaOptions = tmp;
        }
        // check if root contains an install dir to overwrite inherited one.
        tmp = root.elements(Constants.DEPLOY_INSTALL_DIR);
        if(tmp.size() > 0) {
            installDir = tmp.get(0);
            // log if more than one install directory.
            if(tmp.size() > 1) {
                System.out.println("Maximum 1 value expected at: "
                        + Constants.DEPLOY_INSTALL_DIR
                        + " but " + tmp.size() + " received.");
            }
        }
        // check if root contains machine user name to overwrite inherited ones.
        tmp = root.elements(Constants.DEPLOY_MACHINE_USER_NAME);
        if(tmp.size() > 0) {
            machineUserName = tmp.get(0);
            // log if more than one machine user name.
            if(tmp.size() > 1) {
                System.out.println("Maximum 1 value expected at: "
                        + Constants.DEPLOY_MACHINE_USER_NAME
                        + " but " + tmp.size() + " received.");
            }
        }
        // check if root contains a database dir to overwrite inherited ones.
        tmp = root.elements(Constants.DEPLOY_DATABASE_DIR);
        if(tmp.size() > 0) {
            databaseDir = tmp.get(0);
            // log if more than one database directory.
            if(tmp.size() > 1) {
                System.out.println("Maximum 1 value expected at: "
                        + Constants.DEPLOY_DATABASE_DIR
                        + " but " + tmp.size() + " received.");
            }
        }
        // check if root contains a archive database dir to overwrite 
        // inherited ones.
        tmp = root.elements(Constants.DEPLOY_ARCHIVE_DATABASE_DIR);
        if(tmp.size() > 0) {
            arcDatabaseDir = tmp.get(0);
            // log if more than one database directory.
            if(tmp.size() > 1) {
                System.out.println("Maximum 1 value expected at: "
                        + Constants.DEPLOY_ARCHIVE_DATABASE_DIR
                        + " but " + tmp.size() + " received.");
            }
        }
    }

    /**
     * Makes all the java options into a single String.
     * 
     * @return All the java options.
     */
    public String writeJavaOptions() {
        StringBuilder res = new StringBuilder();
        // apply the java options
        for(Element e : javaOptions) {
            res.append(e.getText());
            res.append(Constants.SPACE);
        }
        return res.toString();
    }
    
    /**
     * For retrieving the install directory parameter.
     * 
     * @return The install directory element, or empty string if install dir 
     * is null.
     */
    public String getInstallDirValue() {
        if(installDir != null) {
            return installDir.getText();
        } else {
            return "";
        }
    }
    
    /** 
     * For retrieving the directory for the database.
     * 
     * @return The database directory element, or empty string if install dir 
     * is null.
     */
    public String getDatabaseDirValue() {
        if(databaseDir != null) {
            return databaseDir.getText();
        } else {
            return "";
        }
    }
    
    /**
     * For retrieving the directory for the archive database.
     * 
     * @return The archive database install directory element, or 
     * empty string if install dir is null.
     */
    public String getArchiveDatabaseDirValue() {
        if(arcDatabaseDir != null) {
            return arcDatabaseDir.getText();
        } else {
            return "";
        }
    }
    
    /**
     * For retrieving the machine user name parameter.
     * 
     * @return The machine user name.
     */
    public Element getMachineUserName() {
        return machineUserName;
    }
    
    /**
     * For retrieving the list of class paths.
     * 
     * @return The list of class paths.
     */
    public List<Element> getClassPaths() {
        return classPaths;
    }
}
