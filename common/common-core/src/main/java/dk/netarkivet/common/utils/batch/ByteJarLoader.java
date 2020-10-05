/*
 * #%L
 * Netarchivesuite - common
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
package dk.netarkivet.common.utils.batch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.StreamUtils;

/**
 * ByteJarLoader is a ClassLoader that stores java classes in a map where the key to the map is the class name, and the
 * value is the class stored as a byte array.
 */
@SuppressWarnings("serial")
public class ByteJarLoader extends ClassLoader implements Serializable {

    /** The log. */
    private static final transient Logger log = LoggerFactory.getLogger(ByteJarLoader.class);

    /** The map, that holds the class data. */
    Map<String, byte[]> binaryData = new HashMap<String, byte[]>();

    /** Java package separator. */
    private static final String JAVA_PACKAGE_SEPARATOR = ".";

    /** Directory separator. */
    private static final String DIRECTOR_SEPARATOR = "/";

    /**
     * Constructor for the ByteLoader.
     *
     * @param files An array of files, which are assumed to be jar-files, but they need not have the extension .jar
     */
    public ByteJarLoader(File... files) {
        ArgumentNotValid.checkNotNull(files, "File ... files");
        ArgumentNotValid.checkTrue(files.length != 0, "Should not be empty array");
        for (File file : files) {
            try {
                JarFile jarFile = new JarFile(file);
                for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
                    JarEntry entry = e.nextElement();
                    String name = entry.getName();
                    InputStream in = jarFile.getInputStream(entry);
                    ByteArrayOutputStream out = new ByteArrayOutputStream((int) entry.getSize());
                    StreamUtils.copyInputStreamToOutputStream(in, out);
                    log.trace("Entering data for class '{}'", name);
                    binaryData.put(name, out.toByteArray());
                }
            } catch (IOException e) {
                throw new IOFailure("Failed to load jar file '" + file.getAbsolutePath() + "': " + e);
            }
        }
    }

    /**
     * Lookup and return the Class with the given className. This method overrides the ClassLoader.findClass method.
     *
     * @param className The name of the class to lookup
     * @return the Class with the given className.
     * @throws ClassNotFoundException If the class could not be found
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Class findClass(String className) throws ClassNotFoundException {
        ArgumentNotValid.checkNotNullOrEmpty(className, "String className");
        // replace all dots with '/' in the className before looking it up
        // in the
        // hashmap
        // Note: The class is stored in the hashmap with a .class extension
        String realClassName = className.replace(JAVA_PACKAGE_SEPARATOR, DIRECTOR_SEPARATOR) + ".class";

        if (binaryData.isEmpty()) {
            log.warn("No data loaded for class with name '{}'", className);
        }
        if (binaryData.containsKey(realClassName)) {
            final byte[] bytes = binaryData.get(realClassName);
            return defineClass(className, bytes, 0, bytes.length);
        } else {
            return super.findClass(className);
        }
    }

}
