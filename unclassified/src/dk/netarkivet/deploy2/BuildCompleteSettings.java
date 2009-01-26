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

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import dk.netarkivet.common.utils.FileUtils;

/**
 * Class for combining the different setting files into a 
 * complete settings file.
 */
public final class BuildCompleteSettings {
    /**
     * Constructor.
     */
    private BuildCompleteSettings()
    {}
    
    /**
     * Run the program.
     * This loads and merges all the setting files into a single file.
     * 
     * @param args Unused arguments.
     * @throws IOException For input/output errors.
     */
    public static void main(String[] args) throws IOException {
        XmlStructure settings = null;
        for (String path : Constants.BUILD_SETTING_FILES) {
            File tmpFile = FileUtils.getResourceFileFromClassPath(path);
            if (settings == null) {
                settings = new XmlStructure(tmpFile);
            } else {
                Element elem = retrieveXmlSettingsTree(tmpFile);
                if (elem != null) {
                    settings.overWrite(elem);
                } else {
                    System.out.println("Cannot overwrite!!");
                }
            }
        }

        // make settings file.
        File completeSettings = new File(
                Constants.BUILD_COMPLETE_SETTINGS_FILE_PATH);
        FileWriter fw = new FileWriter(completeSettings);
        // write settings to file.
        fw.append(settings.getXML());
        fw.append("\n");
        fw.close();
    }
    
    /**
     * Retrieves the main element from the file.
     * 
     * @param settingFile The file to load into an Element.
     * @return The root of the XML structure of the settings file. 
     */
    private static Element retrieveXmlSettingsTree(File settingFile) {
        try {
            Document doc;
            SAXReader reader = new SAXReader();
            if (settingFile.canRead()) {
                doc =  reader.read(settingFile);
                settingFile.deleteOnExit();
                return doc.getRootElement();
            } else {
                System.out.println("Cannot read file: " 
                        + settingFile.getAbsolutePath());
            }
        } catch (DocumentException e) {
            System.err.println("Problems with file: " 
                    + settingFile.getAbsolutePath() + " : " + e);
            
        }
        return null;
    }
}
