/* $Id$
 * $Revision$
 * $Date$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.FileUtils;

/**
 * Class for combining the different setting files into a 
 * complete settings file.
 * The different settings are listed here: {@link Constants#BUILD_SETTING_FILES}
 *
 export NAS_SRC=$HOME/workspace/netarchivesuite
 cd $NAS_SRC
 ant jarfiles
 export CLASSPATH=$NAS_SRC/lib/dk.netarkivet.harvester.jar:$NAS_SRC/lib/dk.netarkivet.archive.jar:\
 $NAS_SRC/lib/dk.netarkivet.wayback.jar:$NAS_SRC/lib/dk.netarkivet.deploy.jar:
 cd src
 java dk.netarkivet.deploy.BuildCompleteSettings
 *
 */
public final class BuildCompleteSettings {
    /**
     * Private constructor to disallow instantiation of this class.
     */
    private BuildCompleteSettings() {}

    /**
     * Run the program.
     * This loads and merges all the setting files into a single file.
     *
     * @param args Optional argument for name of complete settings file.
     * E.g. /home/myUser/myDir/default_settings.xml
     * @throws IOException For input/output errors.
     */
    public static void main(String[] args) {
        if(args.length < 1) {
            buildCompleteSettings(Constants.BUILD_COMPLETE_SETTINGS_FILE_PATH);
        } else {
            buildCompleteSettings(args[0]);
        }
    }

    public static void buildCompleteSettings(String completeSettingsPath) {
        ArgumentNotValid.checkNotNullOrEmpty(
            completeSettingsPath, "completeSettingsPath");
        XmlStructure settings = null;
        for (String path : Constants.BUILD_SETTING_FILES) {
            File tmpFile = FileUtils.getResourceFileFromClassPath(path);
            if (settings == null) {
                settings = new XmlStructure(tmpFile, Charset.defaultCharset().name());
            } else {
                Element elem = retrieveXmlSettingsTree(tmpFile);
                if (elem != null) {
                    settings.overWrite(elem);
                } else {
                    throw new ArgumentNotValid("No settings found at: "
                        + tmpFile.getAbsolutePath());
                }
            }
        }

        try {
            FileWriter fw = new FileWriter( new File(completeSettingsPath));
            fw.append(settings.getXML());
            fw.append(Constants.NEWLINE);
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write new settings");
        }
    }

    /**
     * Retrieves the main element from the file.
     *
     * @param settingFile The file to load into an Element.
     * This has to be a temporary file, since it is deleted afterwards.
     * @return The root of the XML structure of the settings file. 
     * Returns null if problems occurred during reading.
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
