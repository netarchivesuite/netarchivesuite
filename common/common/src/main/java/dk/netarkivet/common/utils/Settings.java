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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.common.utils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.UnknownID;

/**
 * Provides access to general application settings. The settings are retrieved
 * from xml files. XML files may be specified one of two places: 1) Default
 * settings in XML files, specified by class path. These are intended to be
 * packaged in the jar files, to provide a fallback for settings. 2) Overriding
 * settings in XML files in file systems. These are intended to override the
 * necessary values with minimal XML files. The location of these files are
 * either specified by the system property {@link #SETTINGS_FILE_PROPERTY},
 * multiple files can be separated by {@link File#pathSeparator}, that is ':' on
 * linux and ';' on windows; or if that property is not set, the default
 * location is {@link #DEFAULT_SETTINGS_FILEPATH}.
 */
public class Settings {

    /**
     * The objects representing the contents of the settings xml files. For
     * handling multithreaded instances this list must be initialised through
     * the method Collections.synchronizedList().
     */
    private static final List<SimpleXml> fileSettingsXmlList
            = Collections.synchronizedList(new ArrayList<SimpleXml>());
    static {

        // Perform an initial loading of the settings.
        reload();
    }

    /** Logger for this class. */
    private static final Log log
            = LogFactory.getLog(Settings.class.getName());


    /**
     * The objects representing the contents of the default settings xml files
     * in classpath. For handling multithreaded instances this list must be
     * initialised through the method Collections.synchronizedList().
     */
    private static final List<SimpleXml> defaultClasspathSettingsXmlList
            = Collections.synchronizedList(new ArrayList<SimpleXml>());
    /**
     * This system property specifies alternative position(s) to look for
     * settings files. If more files are specified, they should be separated by
     * {@link File#pathSeparatorChar}
     */
    public static final String SETTINGS_FILE_PROPERTY
            = "dk.netarkivet.settings.file";

    /**
     * The file path to look for settings in, if the system property {@link
     * #SETTINGS_FILE_PROPERTY} is not set.
     */
    public static final String DEFAULT_SETTINGS_FILEPATH = "conf/settings.xml";
    /** The newest "last modified" date of all settings files. */
    private static long lastModified;

    /**
     * Return the file these settings are read from. If the property given in
     * the constructor is set, that will be used to determine the file. If it is
     * not set, the default settings file path given in the constructor will be
     * used.
     *
     * @return The settings file.
     */
    public static List<File> getSettingsFiles() {
        String[] pathList = System.getProperty(SETTINGS_FILE_PROPERTY,
                                               DEFAULT_SETTINGS_FILEPATH).split(
                File.pathSeparator);
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
     * Gets a setting. The search order for a given setting is as follows:
     *
     * First it is checked, if the argument key is set as a System property. If
     * yes, return this value. If no, we continue the search.
     *
     * Secondly, we check, if the setting is in one of the loaded settings xml
     * files. If the value is there, it is returned. If no, we continue the
     * search.
     *
     * Finally, we check if the setting is in one of default settings files from
     * classpath. If the value is there, it is returned. Otherwise an UnknownId
     * exception is thrown.
     *
     * Note: The retrieved value can be the empty string
     *
     * @param key name of the setting to retrieve
     *
     * @return the retrieved value
     *
     * @throws ArgumentNotValid if key is null or the empty string
     * @throws UnknownID        if no setting loaded matches key
     * @throws IOFailure        if IO Failure
     */
    public static String get(String key)
            throws UnknownID, IOFailure, ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(key, "String key");
        String val = System.getProperty(key);
        if (val != null) {
            return val;
        }

        // Key not in System.properties try loaded data instead
        synchronized (fileSettingsXmlList) {
            for (SimpleXml settingsXml : fileSettingsXmlList) {
                if (settingsXml.hasKey(key)) {
                    return settingsXml.getString(key);
                }
            }
        }

        // Key not in file based settings, try classpath settings instead
        synchronized (defaultClasspathSettingsXmlList) {
            for (SimpleXml settingsXml : defaultClasspathSettingsXmlList) {
                if (settingsXml.hasKey(key)) {
                    return settingsXml.getString(key);
                }
            }
        }
        throw new UnknownID("No match for key '" + key + "' in settings");
    }

    /**
     * Gets a setting as an int. This method calls get(key) and then parses the
     * value as integer.
     *
     * @param key name of the setting to retrieve
     *
     * @return the retrieved int
     *
     * @throws ArgumentNotValid if key is null, the empty string or key is not
     *                          parseable as an integer
     * @throws UnknownID        if no setting loaded matches key
     */
    public static int getInt(String key)
            throws UnknownID, ArgumentNotValid {
        String value = get(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            String msg = "Invalid setting. Value '" + value + "' for key '"
                         + key + "' could not be parsed as an integer.";
            throw new ArgumentNotValid(msg, e);
        }
    }

    /**
     * Gets a setting as a long. This method calls get(key) and then parses the
     * value as a long.
     *
     * @param key name of the setting to retrieve
     *
     * @return the retrieved long
     *
     * @throws ArgumentNotValid if key is null, the empty string or key is not
     *                          parseable as a long
     * @throws UnknownID        if no setting loaded matches key
     */
    public static long getLong(String key)
            throws UnknownID, ArgumentNotValid {
        String value = get(key);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            String msg = "Invalid setting. Value '" + value + "' for key '"
                         + key + "' could not be parsed as a long.";
            throw new ArgumentNotValid(msg, e);
        }
    }

    /**
     * Gets a setting as a file. This method calls get(key) and then returns the
     * value as a file.
     *
     * @param key name of the setting to retrieve
     *
     * @return the retrieved file
     *
     * @throws ArgumentNotValid if key is null, the empty string
     * @throws UnknownID        if no setting loaded matches ke
     */
    public static File getFile(String key) {
        ArgumentNotValid.checkNotNullOrEmpty(key, "String key");
        return new File(get(key));
    }

    /**
     * Gets a setting as a boolean. This method calls get(key) and then parses
     * the value as a boolean.
     *
     * @param key name of the setting to retrieve
     *
     * @return the retrieved boolean
     *
     * @throws ArgumentNotValid if key is null or the empty string
     * @throws UnknownID        if no setting loaded matches key
     */
    public static boolean getBoolean(String key)
            throws UnknownID, ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(key, "String key");
        String value = get(key);
        return Boolean.parseBoolean(value);
    }

    /**
     * Gets a list of settings. First it is checked, if the key is registered as
     * a System property. If yes, registered value is returned in a list of
     * length 1. If no, the data loaded from the settings xml files are
     * examined. If value is there, it is returned in a list. If not, the
     * default settings from classpath are examined. If values for this setting
     * are found here, they are returned. Otherwise, an UnknownId exception is
     * thrown.
     *
     * Note that the values will not be concatenated, the first place with a
     * match will define the entire list. Furthemore the list cannot be empty.
     *
     * @param key name of the setting to retrieve
     *
     * @return the retrieved values (as a non-empty String array)
     *
     * @throws ArgumentNotValid if key is null or the empty string
     * @throws UnknownID        if no setting loaded matches key
     */
    public static String[] getAll(String key)
            throws UnknownID, ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(key, "key");
        log.debug("Searching for a setting for key: " + key);
        String val = System.getProperty(key);
        if (val != null) {
            log.debug("value for key found in property:" + val);
            return new String[]{val};
        }
        if (fileSettingsXmlList.isEmpty()) {
            log.warn("The list of loaded data settings is empty."
                     + "Is this OK?");
        }
        // Key not in System.properties try loaded data instead
        synchronized (fileSettingsXmlList) {
            for (SimpleXml settingsXml : fileSettingsXmlList) {
                List<String> result
                        = settingsXml.getList(key);
                if (result.size() == 0) {
                    continue;
                }
                log.debug("Value found in loaded data: "
                          + StringUtils.conjoin(",", result));
                return result.toArray(new String[result.size()]);
            }
        }

        // Key not in file based settings, try settings from classpath
        synchronized (defaultClasspathSettingsXmlList) {
            for (SimpleXml settingsXml : defaultClasspathSettingsXmlList) {
                List<String> result
                        = settingsXml.getList(key);
                if (result.size() == 0) {
                    continue;
                }
                log.debug("Value found in classpath data: "
                          + StringUtils.conjoin(",", result));
                return result.toArray(new String[result.size()]);
            }
        }
        throw new UnknownID("No match for key '" + key + "' in settings");
    }

    /**
     * Sets the key to one or more values. Calls to this method are forgotten
     * whenever the {@link #reload()} is executed.
     *
     * TODO write these values to its own simpleXml structure, that are not
     * reset during reload.
     *
     * @param key    The settings key to add this under, legal keys are fields
     *               in this class.
     * @param values The (ordered) list of values to put under this key.
     *
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
     * Reload the settings if they have changed on disk. This behaves exactly as
     * forceReload, except it only reloads if the data of the file is different
     * than last time it was loaded.
     *
     * @throws IOFailure if settings cannot be loaded
     */
    public static synchronized void conditionalReload() {
        List<File> settingsFiles = getSettingsFiles();
        for (File settingsFile : settingsFiles) {
            if (settingsFile.lastModified() > lastModified) {
                log.info("Do reload of settings, as the file '"
                         + settingsFile.getAbsolutePath()
                         + "' has changed since last reload");
                reload();
                return;
            }
        }
    }

    /**
     * Reloads the settings. This will reload the settings from disk, and forget
     * all settings that were set with {@link #set}
     *
     * The field {@link #lastModified} is updated to timestamp of the settings
     * file that has been changed most recently.
     *
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
            } else {
                log.warn("The file '"
                         + settingsFile.getAbsolutePath()
                         + "' is not a file, and therefore not loaded");
            }
            if (settingsFile.lastModified() > lastModified) {
                lastModified = settingsFile.lastModified();
            }
        }
        synchronized (fileSettingsXmlList) {
            fileSettingsXmlList.clear();
            fileSettingsXmlList.addAll(simpleXmlList);
        }
    }

    /**
     * Add the settings file represented by this path to the list of default
     * classpath settings.
     *
     * @param defaultClasspathSettingsPath the given default classpath setting.
     */
    public static void addDefaultClasspathSettings(
            String defaultClasspathSettingsPath) {
        ArgumentNotValid.checkNotNullOrEmpty(
                defaultClasspathSettingsPath,
                "String defaultClasspathSettingsPath");
        InputStream stream = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(defaultClasspathSettingsPath);
        if (stream != null) {
            defaultClasspathSettingsXmlList.add(new SimpleXml(stream));
        } else {
            log.warn("Unable to read the settings file represented by path: '"
                     + defaultClasspathSettingsPath + "'");
        }
    }

    /**
     * Get a tree view of a part of the settings. Note: settings read with this
     * mechanism do not support overriding with system properties!
     *
     * @param path Dotted path to a unique element in the tree.
     *
     * @return The part of the setting structure below the element given.
     */
    public static StringTree<String> getTree(String path) {
        synchronized (fileSettingsXmlList) {
            for (SimpleXml settingsXml : fileSettingsXmlList) {
                if (settingsXml.hasKey(path)) {
                    return settingsXml.getTree(path);
                }
            }
        }

        // Key not in file based settings, try classpath settings instead
        synchronized (defaultClasspathSettingsXmlList) {
            for (SimpleXml settingsXml : defaultClasspathSettingsXmlList) {
                if (settingsXml.hasKey(path)) {
                    return settingsXml.getTree(path);
                }
            }
        }
        throw new UnknownID("No match for key '" + path + "' in settings");
    }

}
