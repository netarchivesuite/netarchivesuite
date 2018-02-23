/*
 * #%L
 * Netarchivesuite - common
 * %%
 * Copyright (C) 2005 - 2017 The Royal Danish Library, 
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

package dk.netarkivet.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Onb Free Space Provider returns the number of bytes free
 * Returning 0 if a given path is not writable or if free space is lower than a given minimum free space percentage
 */

public class OnbFreeSpaceProvider implements FreeSpaceProvider {

    /** The error logger we notify about error messages on. */
    private static final Logger log = LoggerFactory.getLogger(OnbFreeSpaceProvider.class);

    /** The default place in classpath where the settings file can be found. */
    private static String DEFAULT_SETTINGS_CLASSPATH = "dk/netarkivet/common/utils/OnbFreeSpaceProviderSettings.xml";

    /*
     * The static initialiser is called when the class is loaded. It will add default values for all settings defined in
     * this class, by loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(DEFAULT_SETTINGS_CLASSPATH);
    }

    /**
     * <b>settings.common.freespaceprovider.minfreespacepercentage</b>: <br>
     * The setting for minimum free space percentage
     */
    public static final String FREESPACEPROVIDER_MINFREESPACEPERCENTAGE_SETTING = "settings.common.freespaceprovider.minfreespacepercentage";

    /**
     * The minimum free space percentage
     * e.g. 5 or 5.55
     **/
    private static final Double FREESPACEPROVIDER_MINFREESPACEPERCENTAGE = Double.parseDouble(Settings.get(FREESPACEPROVIDER_MINFREESPACEPERCENTAGE_SETTING));

    /**
     * Returns the number of bytes free on the file system that the given file resides on. Will return 0 on non-existing
     * files, on read only files and if free space is lower than given freespacepercentage in settings.
     *
     * @param f a given file
     * @return the number of bytes free.
     */
    public long getBytesFree(File f) {
        ArgumentNotValid.checkNotNull(f, "File f");
        if (!f.exists()) {
            log.warn("The file '{}' does not exist. The value 0 returned.", f.getAbsolutePath());
            return 0;
        }

        if (!f.canWrite()) {
            log.warn("The file '{}' is not writeable. The value 0 returned.", f.getAbsolutePath());
            return 0;
        }

        long totalspace;
        long usable;

        totalspace = f.getTotalSpace();
        usable = f.getUsableSpace();

        double freeSpaceInPercent =  100.0 / totalspace * usable;

        if (freeSpaceInPercent <= FREESPACEPROVIDER_MINFREESPACEPERCENTAGE) {
            log.warn("Free space on '{}' is lower than '{}' percent. The value 0 returned.", f.getAbsolutePath(), FREESPACEPROVIDER_MINFREESPACEPERCENTAGE);
            return 0;
        }

        return usable;
    }
}
