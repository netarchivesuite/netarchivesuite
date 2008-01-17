/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Internationalization class.
 */
public class I18n {
    /** Logger for this class. */
    private static Log log = LogFactory.getLog(I18n.class);

    /** Name of the resource bundle. */
    private final String bundleName;

    /** Make an internationalisation object with the given bundle. */
    public I18n(String translationsBundle) {
        ArgumentNotValid.checkNotNullOrEmpty(translationsBundle,
                                             "String translationsBundle");
        bundleName = translationsBundle;
    }

    /**
     * Get a localized message for a given locale and label, and optionally
     * arguments.
     *
     * E.g.
     *
     * I18N.getString(Locale.default, "job.unknown.id", 17)
     *
     * In contrast to {@link java.util.ResourceBundle#getString}, this method is
     * forgiving on errors
     *
     * @param locale The locale to get the string for
     * @param label The label of the string in the resource bundle
     * @param args Any args required for formatting the label
     * @return The localised string, or the label if the string could not be
     * found or the format is invalid or does not match the args.
     * @throws ArgumentNotValid on null or empty local or label.
     */
    public String getString(Locale locale,
                                   String label, Object... args) {
        //Arguments checked in helper method.
        return getString(bundleName, locale, label, args);
    }

    /**
     * Get a localized message for a given resource bundle, locale and label.
     *
     * In contrast to {@link java.util.ResourceBundle#getString}, this method is
     * forgiving on errors
     *
     * I18n.getString("dk.netarkivet.common.Translations",
     * Locale.default, "job.unknown.id", 17)
     *
     * @param bundleName The name of the resource bundle, fully qualified, but
     * without the properties.
     * See {@link java.util.ResourceBundle#getBundle(String)}
     * @param locale The locale to get the string for
     * @param label The label of the string in the resource bundle
     * @param args Any args required for formatting the label
     * @return The localised string, or the label if the string could not be
     * found or the format is invalid or does not match the args.
     * @throws ArgumentNotValid on null bundleName, locale or label.
     */
    public static String getString(String bundleName, Locale locale,
                                   String label, Object... args) {
        ArgumentNotValid.checkNotNullOrEmpty(bundleName, "String bundleName");
        ArgumentNotValid.checkNotNull(locale, "Locale locale");
        ArgumentNotValid.checkNotNullOrEmpty(label, "String label");
        try {
            ResourceBundle bundle =
                    ResourceBundle.getBundle(bundleName, locale);
            String message = bundle.getString(label);
            try {
                return new MessageFormat(message, locale).format(args);
            } catch (IllegalArgumentException e) {
                log.warn("I18n bundle '" + bundleName
                        + "' has wrong format '" + message + "' for label '"
                        + label + "'", e);
                return label;
            }
        } catch (MissingResourceException e) {
            log.warn("I18n bundle '" + bundleName
                    + "' is missing label '" + label + "'", e);
            return label;
        }
    }
}
