/*
 * #%L
 * Netarchivesuite - wayback
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
package dk.netarkivet.wayback.batch;

import org.archive.wayback.UrlCanonicalizer;
import org.archive.wayback.util.url.AggressiveUrlCanonicalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.wayback.WaybackSettings;

/**
 * A factory for returning a UrlCanonicalizer.
 */
public class UrlCanonicalizerFactory extends SettingsFactory<UrlCanonicalizer> {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(UrlCanonicalizerFactory.class);

    /**
     * This method returns an instance of the UrlCanonicalizer class specified in the settings.xml for the
     * dk.netarkivet.wayback module. In the event that reading this file generates a SecurityException, as may occur in
     * batch operation if security does not allow System properties to be read, the method will fall back on returning
     * an instance of the class org.archive.wayback.util.url.AggressiveUrlCanonicalizer.
     *
     * @return a canonicalizer for urls
     */
    public static UrlCanonicalizer getDefaultUrlCanonicalizer() {
        try {
            return SettingsFactory.getInstance(WaybackSettings.URL_CANONICALIZER_CLASSNAME);
        } catch (SecurityException e) {
            logger.debug("The requested canoncializer could not be loaded. Falling back to {}",
                    AggressiveUrlCanonicalizer.class.toString(), e);
            return new AggressiveUrlCanonicalizer();
        }
    }

}
