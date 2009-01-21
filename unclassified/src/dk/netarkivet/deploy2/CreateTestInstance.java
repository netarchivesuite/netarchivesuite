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
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.dom4j.Element;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * This class applies the test variables.
 * 
 * It creates a new instance of the settings where the variables are changed, 
 * and then writes it out as a new it-configuration file 
 * (does not overwrite the original, but creates a new in same directory).
 */
public class CreateTestInstance {
    /** The source configuration file.*/
    private File source;
    /** The settings instance. Loaded from the source file, changed and saved.*/
    private XmlStructure set;
    /** The string value of the calculated offset.*/
    private String offsetVal;
    /** The paths to where the positions the offset are to be used.*/
    private OffsetSystem[] offsetPaths;
    /** The new value for the HTTP port.*/
    private String httpPortVal;
    /** The path to the HTTP port.*/
    private String[] httpPortPath;
    /** The new value for the environment name.*/
    private String environmentNameVal;
    /** The path to the environment name.*/
    private String[] environmentNamePath;
    /** The new value of the mail receiver.*/
    private String mailReceiverVal;
    /** The path to the mail receiver.*/
    private String[] mailReceiverPath;
    
    /**
     * The constructor.
     * 
     * @param configSource The source configuration file.
     */
    public CreateTestInstance(File configSource) {
        source = configSource;
        set = new XmlStructure(source);
    }

    /**
     * Function to apply the variables.
     * 
     * @param offset The input offset value (1-9 below httpPort).
     * @param httpPort The new value for the HTTP port.
     * @param environmentName The new value for the environment name.
     * @param mailReceiver The new value for the mailReceiver.
     */
    public void applyTestArguments(String offset, String httpPort, 
            String environmentName, String mailReceiver) {
        ArgumentNotValid.checkNotNullOrEmpty(offset, "String offset");
        ArgumentNotValid.checkNotNullOrEmpty(httpPort, "String httpPort");
        ArgumentNotValid.checkNotNullOrEmpty(environmentName, 
                "String environmentName");
        ArgumentNotValid.checkNotNullOrEmpty(mailReceiver, 
                "String mailReciever");

        // calculate offset
        int offsetInt = (new Integer(httpPort)).intValue() 
                - (new Integer(offset)).intValue();

        if(offsetInt > 9 || offsetInt < 0) {
            System.err.print(Constants.MSG_ERROR_TEST_OFFSET);
            System.out.println();
            System.exit(0);
        }
        // change integer to string (easiest way to change integer to String)
        offsetVal = new String("" + offsetInt);

        // Get values
        httpPortVal = httpPort;
        environmentNameVal = environmentName;
        mailReceiverVal = mailReceiver;

        // make paths
        httpPortPath = Constants.SETTINGS_HTTP_PORT_PATH;
        environmentNamePath = Constants.ENVIRONMENT_NAME_TOTAL_PATH_LEAF;
        mailReceiverPath = Constants.SETTINGS_NOTIFICATION_RECEIVER_PATH;

        // make offset paths
        offsetPaths = new OffsetSystem[] {
                new OffsetSystem(2, Constants.TEXT_JMX_PORT_PATH),
                new OffsetSystem(2, Constants.TEXT_JMX_RMI_PORT_PATH),
                new OffsetSystem(1, Constants.TEXT_HARVEST_HETRIX_GUI_PORT),
                new OffsetSystem(1, Constants.TEXT_HARVEST_HETRIX_JMX_PORT)
                };

        // apply the arguments
        apply();
    }

    /**
     * Applies the new variables.
     * Goes through all element instances and applies the variables.
     */
    @SuppressWarnings("unchecked")
    private void apply() {
        // apply on root
        applyOnElement(set.getRoot());

        List <Element> physLocs = set.getChildren(
                Constants.PHYSICAL_LOCATION_BRANCH);

        for(Element pl : physLocs) {
            // apply on every physical location
            applyOnElement(pl);

            List <Element> machines = pl.elements(Constants.MACHINE_BRANCH);
            for(Element mac : machines) {
                // apply on every machine
                applyOnElement(mac);

                List <Element> applications = 
                    mac.elements(Constants.APPLICATION_BRANCH);
                for(Element app : applications) {
                    // apply on every application
                    applyOnElement(app);
                    
                    applyOnApplication(app);
                }
            }
        }
    }

    /**
     * Applies the new variables on a specific element.
     * 
     * @param e The element where the variables are to be applied.
     */
    private void applyOnElement(Element e) {
        // Check argument valid
        ArgumentNotValid.checkNotNull(e, "Element e");

        // Check the following! 
        set.overWriteOnly(e, httpPortVal, httpPortPath);
        set.overWriteOnly(e, environmentNameVal, environmentNamePath);
        set.overWriteOnly(e, mailReceiverVal, mailReceiverPath);

        for(OffsetSystem ofs : offsetPaths) {
            set.overWriteOnlyInt(e, ofs.index, offsetVal.charAt(0), ofs.path);
        }
    }
    
    /**
     * Applies the environment name on the name of the file-directories.
     * Thus: fileDir -> fileDir/environmentName
     * 
     * @param app The application where this has to be applied.
     */
    @SuppressWarnings("unchecked")
    private void applyOnApplication(Element app) {
        Element current = app.element(Constants.SETTINGS_BRANCH);
        // Go through tree to end branch before leafs
        for(int i = 0; i < Constants.SETTINGS_FILE_DIR_LEAF.length - 1; i++) {
            // stop if branch does not exist
            if(current == null) {
                return;
            }
            // get next string
            String st = Constants.SETTINGS_FILE_DIR_LEAF[i];
            // go to next branch
            current = current.element(st);
        }
        if(current == null) {
            return;
        }

        List<Element> elems = current.elements(Constants.SETTINGS_FILE_DIR_LEAF[
                           Constants.SETTINGS_FILE_DIR_LEAF.length - 1]);
        for(Element el : elems) {
            String content = el.getText();
            // check if windows format has been used
            if(content.contains("\\")) {
                content += "\\" + environmentNameVal;
            } else {
                content += "/" + environmentNameVal;
            }
            el.setText(content);
        }
    }
    
    /**
     * Writing out the XMLcode to a file.
     * 
     * @param filename The name of the file to be written.
     * @throws IOException If anything goes wrong.
     */
    public void createSettingsFile(String filename) 
        throws IOException {
        ArgumentNotValid.checkNotNullOrEmpty(filename, "String filename");
        File f = new File(filename);

        FileWriter fw = new FileWriter(f);
        fw.write(set.getXML());
        fw.close();
    }
    
    /** 
     * Structure for handling where to apply the new offset value.
     */
    private class OffsetSystem {
        /** The index of the decimal to be replaced by the offset.*/
        public int index;
        /** The path to the leaf where the offset are to be applied.*/
        public String[] path;

        /**
         * The constructor.
         * 
         * @param i The index variable.
         * @param p The path variable.
         */
        public OffsetSystem(int i, String[] p) {
            index = i;
            path = p;
        }
    }
}
