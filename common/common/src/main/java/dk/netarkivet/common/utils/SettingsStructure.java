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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
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
 * The settings are retrieved from an xml file.
 */
public class SettingsStructure {
    private Log log = LogFactory.getLog(SettingsStructure.class.getName());
    /** The object representing the contents of the settings xml file. */
    private SimpleXml settingsxml;

    /** The edition of the Settings-object. Each reload increases this edition
     * by 1. */
    public int edition = 1;

    private final String systemProperty;

    private final String defaultSettingsFilepath;
    private long lastModified;

    /**
     * Create new instance, look for settings file in the following order:
     * <ul>
     * <li> If the property given is set, data are loaded from this file</li>
     * <li> Otherwise the file path given as default is used.
     * </ul>
     * If no settings file could be located all subsequent calls will fail.
     *
     * @param systemProperty          The system property specifying the file
     *                                path
     * @param defaultSettingsFilepath The file path if the system property is
     *                                not set.
     * @throws ArgumentNotValid on null or empty parameters
     * @throws IOFailure        if settings cannot be loaded
     */
    public SettingsStructure(String systemProperty,
                             String defaultSettingsFilepath) {
        ArgumentNotValid.checkNotNull(systemProperty, "String systemProperty");
        ArgumentNotValid.checkNotNull(defaultSettingsFilepath,
                                      "String defaultSettingsFilepath");

        this.systemProperty = systemProperty;
        this.defaultSettingsFilepath = defaultSettingsFilepath;
        reload();
    }

    /**
     * Return the file these settings are read from. If the property given in
     * the constructor is set, that will be used to determine the file. If it is
     * not set, the default settings file path given in the constructor will
     * be used.
     *
     * @return The settings file.
     */
    public File getSettingsFile() {
        return new File(
                System.getProperty(systemProperty, defaultSettingsFilepath));
    }

    /**
     * Gets a setting.
     * First System.property is checked, if the key is registered here the
     * registered value is returned, otherwise the data loaded from the
     * settings xml file are checked.
     *
     * @param key name of the setting to retrieve
     * @return the retrieved value
     *
     * @throws ArgumentNotValid if key is null or the empty string
     * @throws UnknownID        if no setting loaded matches key
     * @throws IOFailure        if no settings loaded.
     */
    public String get(String key)
            throws UnknownID, IOFailure, ArgumentNotValid {
        String val = System.getProperty(key);
        if (val != null) {
            return val;
        }

        // Key not in System.properties try loaded data instead
        return settingsxml.getString(key);
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
    public int getInt(String key) {
        String value = get(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            String msg = "Invalid setting. Value:" + value + " for key:" + key
                    + " could not be parsed to int.";
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
    public long getLong(String key)
            throws UnknownID, IOFailure, ArgumentNotValid {
        String value = get(key);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            String msg = "Invalid setting. Value:" + value + " for key:" + key
                    + " could not be parsed to long.";
            throw new ArgumentNotValid(msg, e);
        }
    }

    /**
     * Gets a list of settings.
     * First System.property is checked, if the key is registered here the
     * registered value is returned in a list of length 1, otherwise the data
     * loaded from the settings xml file are checked.
     *
     * @param key name of the setting to retrieve
     * @return the retrieved values
     *
     * @throws ArgumentNotValid if key is null or the empty string
     * @throws UnknownID        if no setting loaded matches key
     * @throws IOFailure        if no settings loaded.
     */
    public String[] getAll(String key)
            throws UnknownID, IOFailure, ArgumentNotValid {
        String val = System.getProperty(key);
        if (val != null) {
            return new String[]{val};
        }

        // Key not in System.properties try loaded data instead
        String[] result = (String[]) settingsxml.getList(key)
                .toArray(new String[0]);
        if (result.length == 0) {
            throw new UnknownID("No such key: " + key);
        }

        return result;

    }

    /**
     * Create a new setting.
     *
     * @param key    the name of the setting
     * @param values the values
     * @throws ArgumentNotValid if key or value is null,
     *                          or if a corresponding key already exists.
     */
    public void create(String key, String... values) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(key, "key");
        ArgumentNotValid.checkNotNull(values, "value");

        if (settingsxml.hasKey(key)) {
            throw new ArgumentNotValid("Key already registered: '" + key + "'");
        }

        settingsxml.add(key, values);
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
    public void set(String key, String... values) {
        ArgumentNotValid.checkNotNullOrEmpty(key, "key");
        ArgumentNotValid.checkNotNull(values, "values");

        settingsxml.update(key, values);
    }

    /**
     * Reload the settings if they have changed on disk. This behaves exactly
     * as forceReload, except it only reloads if the data of the file is
     * different than last time it was loaded.
     * @throws IOFailure if settings cannot be loaded
     */
    public synchronized void conditionalReload() {
        File settingsFile = getSettingsFile();
        if (settingsFile.lastModified() != lastModified) {
            reload();
        }
    }

    /**
     * Reloads the settings. This will reload the settings from disk, and forget
     * all settings that were set with {@link #set} or {@link #create}
     * @throws IOFailure if settings cannot be loaded
     * @see #conditionalReload()
     */
    public synchronized void reload() {
        File settingsFile = getSettingsFile();
        settingsxml = new SimpleXml(settingsFile);
        lastModified = settingsFile.lastModified();
        edition = edition + 1;
    }

    /**
     * Validate that the settings xml file conforms to the XSD.
     *
     * @param xsdFile Schema to check settings against.
     */
    public void validateWithXSD(File xsdFile) {
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder();
            Document document = parser.parse(getSettingsFile());

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
                final String msg = "Settings file '" + getSettingsFile()
                        + "' does not validate using '" + xsdFile + "'";
                log.warn(msg, e);
                throw new ArgumentNotValid(msg, e);
            }
        } catch (IOException e) {
            throw new IOFailure("Error while validating: ", e);
        } catch (ParserConfigurationException e) {
            final String msg = "Error validating settings file '"
                    + getSettingsFile() + "'";
            log.warn(msg, e);
            throw new ArgumentNotValid(msg, e);
        } catch (SAXException e) {
            final String msg = "Error validating settings file '"
                    + getSettingsFile() + "'";
            log.warn(msg, e);
            throw new ArgumentNotValid(msg, e);
        }
    }

    /**
     * Return the current edition of this class. This will increase by one on
     * each reload of settings.
     *
     * @return the current edition of this class
     */
    public int getEdition() {
        return edition;
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
    public void validateStrings(Class classToCheck,
                                List<String> excludedFields) {
        Field[] fields = classToCheck.getDeclaredFields();
        for (Field f : fields) {
            if (!excludedFields.contains(f.getName())
                    && f.getType().equals(String.class) && Modifier
                    .isStatic(f.getModifiers())) {
                String xmlKey = null;
                try {
                    xmlKey = (String) f.get(null);
                } catch (IllegalAccessException e) {
                    final String msg
                            = "Internal error while checking settings: ";
                    log.warn(msg, e);
                    throw new ArgumentNotValid(msg, e);
                }
                try {
                    get(xmlKey);
                } catch (UnknownID e) {
                    final String msg = "Setting '" + xmlKey + "' ('"
                            + f.getName() + "') is undefined in '"
                            + getSettingsFile().getPath() + "'";
                    log.warn(msg);
                    throw new ArgumentNotValid(msg, e);
                }
            }
        }
    }
}
