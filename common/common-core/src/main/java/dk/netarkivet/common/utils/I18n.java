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
package dk.netarkivet.common.utils;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Internationalization class.
 */
public class I18n {

    /** Logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(I18n.class);

    /** Name of the resource bundle. */
    private final String bundleName;

    /** Make an internationalisation object with the given bundle. */
    public I18n(String translationsBundle) {
        ArgumentNotValid.checkNotNullOrEmpty(translationsBundle, "String translationsBundle");
        bundleName = translationsBundle;
    }

    /**
     * Get a localized message for a given locale and label, and optionally arguments.
     * <p>
     * E.g.
     * <p>
     * I18N.getString(Locale.default, "job.unknown.id", 17)
     * <p>
     * In contrast to {@link java.util.ResourceBundle#getString}, this method is forgiving on errors
     *
     * @param locale The locale to get the string for
     * @param label The label of the string in the resource bundle
     * @param args Any args required for formatting the label
     * @return The localised string, or the label if the string could not be found or the format is invalid or does not
     * match the args.
     * @throws ArgumentNotValid on null or empty local or label.
     */
    public String getString(Locale locale, String label, Object... args) {
        // Arguments checked in helper method.
        return getString(bundleName, locale, label, args);
    }

    /**
     * Get a localized message for a given resource bundle, locale and label.
     * <p>
     * In contrast to {@link java.util.ResourceBundle#getString}, this method is forgiving on errors
     * <p>
     * I18n.getString("dk.netarkivet.common.Translations", Locale.default, "job.unknown.id", 17)
     *
     * @param bundleName The name of the resource bundle, fully qualified, but without the properties. See
     * {@link java.util.ResourceBundle#getBundle(String)}
     * @param locale The locale to get the string for
     * @param label The label of the string in the resource bundle
     * @param args Any args required for formatting the label
     * @return The localised string, or the label if the string could not be found or the format is invalid or does not
     * match the args.
     * @throws ArgumentNotValid on null bundleName, locale or label.
     */
    public static String getString(String bundleName, Locale locale, String label, Object... args) {
        ArgumentNotValid.checkNotNullOrEmpty(bundleName, "String bundleName");
        ArgumentNotValid.checkNotNull(locale, "Locale locale");
        ArgumentNotValid.checkNotNullOrEmpty(label, "String label");
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(bundleName, locale);
            String message = bundle.getString(label);
            try {
                return new MessageFormat(message, locale).format(args);
            } catch (IllegalArgumentException e) {
                log.warn("I18n bundle '{}' has wrong format '{}' for label '{}'", bundleName, message, label, e);
                return label;
            }
        } catch (MissingResourceException e) {
            log.warn("I18n bundle '{}' is missing label '{}'", bundleName, label, e);
            return label;
        }
    }

}
