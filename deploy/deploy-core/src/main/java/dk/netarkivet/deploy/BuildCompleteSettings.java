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
 * Class for combining the different setting files into a complete settings file. The different settings are listed
 * here: {@link Constants#BUILD_SETTING_FILES}
 * <p>
 * This is used when updating the deploy/deploy-core/src/main/resources/dk/netarkivet/deploy/complete_settings.xml before a new release of Netarchivesuite.
 * From your IDE, run this program (dk.netarkivet.deploy.BuildCompleteSettings) with no arguments.
 * And then copy then output-file 'complete_settings.xml' to deploy/deploy-core/src/main/resources/dk/netarkivet/deploy/complete_settings.xml
 * Finally commit the changes to github.
 */
public final class BuildCompleteSettings {
    /**
     * Private constructor to disallow instantiation of this class.
     */
    private BuildCompleteSettings() {
    }
    /** The default path to the output file. */
    public static final String defaultCompleteSettingsPath = "complete_settings.xml";
    
    /**
     * Run the program. This loads and merges all the different settings files into a single outputfile.
     *
     * @param args Optional argument for name of complete settings file. E.g. /home/myUser/myDir/default_settings.xml, otherwise the default "complete_settings.xml" is used
     * @throws IOException For input/output errors.
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            buildCompleteSettings(defaultCompleteSettingsPath);
        } else {
            buildCompleteSettings(args[0]);
        }
    }

    public static void buildCompleteSettings(String completeSettingsPath) {
        ArgumentNotValid.checkNotNullOrEmpty(completeSettingsPath, "completeSettingsPath");
        File completeSettingsFile = new File(completeSettingsPath);
        System.out.println("Writing complete settings to file: " + completeSettingsFile.getAbsolutePath());
        XmlStructure settings = null;
        for (String path : Constants.BUILD_SETTING_FILES) {
            System.out.println("Adding settingfile '" + path + "' to completesettings file");
            File tmpFile = FileUtils.getResourceFileFromClassPath(path);
            if (settings == null) {
                settings = new XmlStructure(tmpFile, Charset.defaultCharset().name());
            } else {
                Element elem = retrieveXmlSettingsTree(tmpFile);
                if (elem != null) {
                    settings.overWrite(elem);
                } else {
                    throw new ArgumentNotValid("No settings found at: " + tmpFile.getAbsolutePath());
                }
            }
        }

        try {
            FileWriter fw = new FileWriter(new File(completeSettingsPath));
            fw.append(settings.getXML());
            fw.append(Constants.NEWLINE);
            fw.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write new settings", e);
        }
        System.out.println("Complete settings successfully written to file: " + completeSettingsFile.getAbsolutePath());
    }

    /**
     * Retrieves the main element from the file.
     *
     * @param settingFile The file to load into an Element. This has to be a temporary file, since it is deleted
     * afterwards.
     * @return The root of the XML structure of the settings file. Returns null if problems occurred during reading.
     */
    private static Element retrieveXmlSettingsTree(File settingFile) {
        try {
            Document doc;
            SAXReader reader = new SAXReader();
            if (settingFile.canRead()) {
                doc = reader.read(settingFile);
                settingFile.deleteOnExit();
                return doc.getRootElement();
            } else {
                System.out.println("Cannot read file: " + settingFile.getAbsolutePath());
            }
        } catch (DocumentException e) {
            System.err.println("Problems with file: " + settingFile.getAbsolutePath() + " : " + e);

        }
        return null;
    }
}
