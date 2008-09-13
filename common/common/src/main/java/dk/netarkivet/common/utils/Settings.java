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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.common.utils;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * Provides access to general application settings.
 * The settings are retrieved from xml files. XML files may be specified one of
 * two places:
 * 1) Default settings in XML files, specified by class path. These are intended
 * to be packaged in the jar files, to provide a fallback for settings.
 * 2) Overriding settings in XML files in file systems. These are intended to
 * override the necessary values with minimal XML files. The location of these
 * files are either specified by the system property {@link #SYSTEM_PROPERTY},
 * multiple files can be separated by {@link File#pathSeparator}, that is
 * ':' on linux and ';' on windows; or if that property is not set, the
 * default location is {@link #DEFAULT_SETTINGS_FILEPATH}.
 */
public class Settings {
    static {
        reload();
    }

    /** Logger for this class. */
    private static final Log log
            = LogFactory.getLog(Settings.class.getName());
    /** The object representing the contents of the settings xml file. */
    private static List<SimpleXml> fileSettingsXmlList;
    /**
     * The object srepresenting the contents of the default settings xml files
     * in classpath.
     */
    private static List<SimpleXml> defaultClasspathSettingsXmlList
            = new ArrayList<SimpleXml>();
    /**
     * This system property specifies alternative position(s) to look for
     * settings files. If more files are specified, they should be separated by
     * {@link File#pathSeparatorChar}
     */
    public static final String SYSTEM_PROPERTY = "dk.netarkivet.settings.file";

    /**
     * The file path to look for settings in, if the system property above is
     * not set.
     */
    public static final String DEFAULT_SETTINGS_FILEPATH = "conf/settings.xml";
    /**
     * The newest "last modified" date of all settings files.
     */
    private static long lastModified;

    /**
     * Return the file these settings are read from. If the property given in
     * the constructor is set, that will be used to determine the file. If it is
     * not set, the default settings file path given in the constructor will
     * be used.
     *
     * @return The settings file.
     */
    public static List<File> getSettingsFiles() {
        String[] pathList = System.getProperty(SYSTEM_PROPERTY, DEFAULT_SETTINGS_FILEPATH).split(File.pathSeparator);
        List<File> result = new ArrayList<File>();
        for (String path : pathList) {
            if (path.trim().length() != 0) {
                File settingsFile = new File(path);
                if (settingsFile.isFile()) {
                    result.add(settingsFile);
                } 
            }
        }
        return result;
    }

    /**
     * Gets a setting.
     * First System.property is checked, if the key is registered here the
     * registered value is returned, otherwise the data loaded from the
     * settings xml files are checked. If value is there, it is returned,
     * otherwise default settings from classpath are checked.
     *
     * @param key name of the setting to retrieve
     * @return the retrieved value
     *
     * @throws ArgumentNotValid if key is null or the empty string
     * @throws UnknownID        if no setting loaded matches key
     * @throws IOFailure        if no settings loaded.
     */
    public static String get(String key)
            throws UnknownID, IOFailure, ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(key, "String key");
        String val = System.getProperty(key);
        if (val != null) {
            return val;
        }

        // Key not in System.properties try loaded data instead
        for (SimpleXml settingsXml : fileSettingsXmlList) {
            if (settingsXml.hasKey(key)) {
                return settingsXml.getString(key);
            }
        }

        // Key not in file based settings, try classpath settings instead
        for (SimpleXml settingsXml : defaultClasspathSettingsXmlList) {
            if (settingsXml.hasKey(key)) {
                return settingsXml.getString(key);
            }
        }
        throw new UnknownID("No match for key '" + key + "' in settings");
    }

    /**
     * Gets a setting as an int.
     * This method calls get(key) and then parses the value to int.
     *
     * @param key name of the setting to retrieve
     * @return the retrieved int
     *
     * @throws ArgumentNotValid if key is null, the empty string or key is not
     *                          parseable to int
     * @throws UnknownID        if no setting loaded matches key
     * @throws IOFailure        if no settings loaded.
     */
    public static int getInt(String key)
            throws UnknownID, IOFailure, ArgumentNotValid {
        String value = get(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            String msg = "Invalid setting. Value '" + value + "' for key '"
                         + key + "' could not be parsed as int.";
            throw new ArgumentNotValid(msg, e);
        }
    }

    /**
     * Gets a setting as a long.
     * This method calls get(key) and then parses the value to long.
     *
     * @param key name of the setting to retrieve
     * @return the retrieved long
     *
     * @throws ArgumentNotValid if key is null, the empty string or key is not
     *                          parseable to long
     * @throws UnknownID        if no setting loaded matches key
     * @throws IOFailure        if no settings loaded.
     */
    public static long getLong(String key)
            throws UnknownID, IOFailure, ArgumentNotValid {
        String value = get(key);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            String msg = "Invalid setting. Value '" + value + "' for key '"
                         + key + "' could not be parsed as long.";
            throw new ArgumentNotValid(msg, e);
        }
    }

    /**
     * Gets a setting as a file.
     * This method calls get(key) and then returns the value as a file.
     *
     * @param key name of the setting to retrieve
     * @return the retrieved file
     *
     * @throws ArgumentNotValid if key is null, the empty string
     * @throws UnknownID        if no setting loaded matches key
     * @throws IOFailure        if no settings loaded.
     */
    public static File getFile(String key) {
        return new File(get(key));
    }

    /**
     * Gets a list of settings.
     * First System.property is checked, if the key is registered here the
     * registered value is returned in a list of length 1, otherwise the data
     * loaded from the settings xml files are checked. If value is there, it is
     * returned in a list, otherwise default settings from classpath are
     * checked. Note that the values will not be concatenated, the first
     * place with a match will define the entire list.
     *
     * @param key name of the setting to retrieve
     * @return the retrieved values
     *
     * @throws ArgumentNotValid if key is null or the empty string
     * @throws UnknownID        if no setting loaded matches key
     * @throws IOFailure        if no settings loaded.
     */
    public static String[] getAll(String key)
            throws UnknownID, IOFailure, ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(key, "key");
        String val = System.getProperty(key);
        if (val != null) {
            return new String[]{val};
        }

        // Key not in System.properties try loaded data instead
        for (SimpleXml settingsXml : fileSettingsXmlList) {
            List<String> result
                    = settingsXml.getList(key);
            if (result.size() == 0) {
                continue;
            }
            return result.toArray(new String[result.size()]);
        }

        // Key not in file based settings, try settings from classpath
        for (SimpleXml settingsXml : defaultClasspathSettingsXmlList) {
            List<String> result
                    = settingsXml.getList(key);
            if (result.size() == 0) {
                continue;
            }
            return result.toArray(new String[result.size()]);
        }
        throw new UnknownID("No match for key '" + key + "' in settings");
    }

    /**
     * Sets the key to one or more values. The key must exist beforehand.
     *
     * @param key     The settings key to add this under, legal keys are
     *                fields in this class.
     * @param values  The (ordered) list of values to put under this key.
     * @throws ArgumentNotValid if key or values are null
     * @throws UnknownID        if the key does not already exist
     */
    public static void set(String key, String... values) {
        ArgumentNotValid.checkNotNullOrEmpty(key, "key");
        ArgumentNotValid.checkNotNull(values, "values");

        if (fileSettingsXmlList.isEmpty()) {
            fileSettingsXmlList.add(new SimpleXml("settings"));
        }
        SimpleXml simpleXml = fileSettingsXmlList.get(0);
        if (simpleXml.hasKey(key)) {
            simpleXml.update(key, values);
        } else {
            simpleXml.add(key, values);
        }
    }

    /**
     * Reload the settings if they have changed on disk. This behaves exactly
     * as forceReload, except it only reloads if the data of the file is
     * different than last time it was loaded.
     * @throws IOFailure if settings cannot be loaded
     */
    public static synchronized void conditionalReload() {
        List<File> settingsFiles = getSettingsFiles();
        for (File settingsFile : settingsFiles) {
            if (settingsFile.lastModified() > lastModified) {
                reload();
                return;
            }
        }
    }

    /**
     * Reloads the settings. This will reload the settings from disk, and forget
     * all settings that were set with {@link #set}
     * @throws IOFailure if settings cannot be loaded
     * @see #conditionalReload()
     */
    public static synchronized void reload() {
        lastModified = 0;
        List<File> settingsFiles = getSettingsFiles();
        List<SimpleXml> simpleXmlList = new ArrayList<SimpleXml>();
        for (File settingsFile : settingsFiles) {
            if (settingsFile.isFile()) {
                simpleXmlList.add(new SimpleXml(settingsFile));
            }
            if (settingsFile.lastModified() > lastModified) {
                lastModified = settingsFile.lastModified();
            }
        }
        fileSettingsXmlList = simpleXmlList;
    }

    public static void addDefaultClasspathSettings(
            String defaultClasspathSettingsPath) {
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(defaultClasspathSettingsPath);
        if (stream != null) {
            defaultClasspathSettingsXmlList.add(new SimpleXml(stream));
        }
    }

    /**
     * Validate that the settings xml file conforms to the XSD.
     *
     * @param xsdFile Schema to check settings against.
     */
    public static void validateWithXSD(File xsdFile) {
        List<File> settingsFiles = getSettingsFiles();
        for (File settingsFile : settingsFiles) {
            try {
                DocumentBuilderFactory builderFactory
                        = DocumentBuilderFactory.newInstance();
                builderFactory.setNamespaceAware(true);
                DocumentBuilder parser = builderFactory
                        .newDocumentBuilder();
                Document document = parser.parse(settingsFile);

                // create a SchemaFactory capable of understanding WXS schemas
                SchemaFactory factory = SchemaFactory
                        .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

                // load a WXS schema, represented by a Schema instance
                Source schemaFile = new StreamSource(xsdFile);
                Schema schema = factory.newSchema(schemaFile);

                // create a Validator instance, which can be used to validate an
                // instance document
                Validator validator = schema.newValidator();

                // validate the DOM tree
                try {
                    validator.validate(new DOMSource(document));
                } catch (SAXException e) {
                    // instance document is invalid!
                    final String msg = "Settings file '" + settingsFile
                            + "' does not validate using '" + xsdFile + "'";
                    log.warn(msg, e);
                    throw new ArgumentNotValid(msg, e);
                }
            } catch (IOException e) {
                throw new IOFailure("Error while validating: ", e);
            } catch (ParserConfigurationException e) {
                final String msg = "Error validating settings file '"
                        + settingsFile + "'";
                log.warn(msg, e);
                throw new ArgumentNotValid(msg, e);
            } catch (SAXException e) {
                final String msg = "Error validating settings file '"
                        + settingsFile + "'";
                log.warn(msg, e);
                throw new ArgumentNotValid(msg, e);
            }
        }
    }

    /**
     * Validate that the strings defined in the given class are present in
     * the settings xml file.
     * Checks all static String fields that are not explicitly excluded above.
     * This asserts the correspondence between the settings we think we have
     * and those defined in the XSD/.xml file.
     *
     * @param classToCheck   The class defining the constants to check
     * @param excludedFields Fields not to check, even thoug they are constants
     *                       in that class.
     */
    public static void validateStrings(Class classToCheck,
                                List<String> excludedFields) {
        Field[] fields = classToCheck.getDeclaredFields();
        for (Field f : fields) {
            if (!excludedFields.contains(f.getName())
                    && f.getType().equals(String.class) && Modifier
                    .isStatic(f.getModifiers())) {
                String xmlKey;
                try {
                    xmlKey = (String) f.get(null);
                } catch (IllegalAccessException e) {
                    final String msg
                            = "Internal error while checking settings for key '"
                              + f.getName() + "' ";
                    log.warn(msg, e);
                    throw new ArgumentNotValid(msg, e);
                }
                try {
                    get(xmlKey);
                } catch (UnknownID e) {
                    final String msg = "Setting '" + xmlKey + "' ('"
                            + f.getName() + "') is undefined in '"
                            + getSettingsFiles() + "'";
                    log.warn(msg);
                    throw new ArgumentNotValid(msg, e);
                }
            }
        }
    }

    /** Get a tree view of a part of the settings. Note: settings read with
     * this mechanism do not support overriding with system properties!
     *
     * @param path Dotted path to a unique element in the tree.
     * @return The part of the setting structure below the element given.
     */
    public static StringTree<String> getTree(String path) {
        for (SimpleXml settingsXml : fileSettingsXmlList) {
            if (settingsXml.hasKey(path)) {
                return settingsXml.getTree(path);
            }
        }

        // Key not in file based settings, try classpath settings instead
        for (SimpleXml settingsXml : defaultClasspathSettingsXmlList) {
            if (settingsXml.hasKey(path)) {
                return settingsXml.getTree(path);
            }
        }
        throw new UnknownID("No match for key '" + path + "' in settings");
    }
}
