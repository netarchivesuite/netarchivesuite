/*
 * #%L
 * Netarchivesuite - heritrix 3 monitor
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

package dk.netarkivet.heritrix3.monitor;

import java.util.Locale;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.harvester.HarvesterSettings;

/**
 * Attempting to isolate the NAS dependencies in this class.
 *
 * @author nicl
 */
public class NASJobWrapper {

    /** Meta refresj header used on some pages to automatically reload the page after some interval has passed. */
    public final String metaRefreshHeaderHtml;

    /** Get and cache the versions string from the jar manifest file. */
    public final String versionString;

    /** Get and cache the environment name used for this installation. */
    public final String environmentName;

    public final I18n I18N = new I18n(Constants.TRANSLATIONS_BUNDLE);

    public NASJobWrapper() {
        metaRefreshHeaderHtml = "<meta http-equiv=\"refresh\" content=\""+Settings.get(HarvesterSettings.HARVEST_MONITOR_REFRESH_INTERVAL)+"\"/>\n";
        versionString = Constants.getVersionString(true);
        environmentName = Settings.get(CommonSettings.ENVIRONMENT_NAME);
    }

    public String getMetaRefreshHeaderHtml() {
        return metaRefreshHeaderHtml;
    }

    public String getVersionString() {
        return versionString;
    }

    public String getEnvironmentName() {
        return environmentName;
    }

    /**
     * Get a localized message for a given locale and label, and optionally arguments.
     * @param locale The locale to get the string for
     * @param label The label of the string in the resource bundle
     * @param args Any args required for formatting the label
     * @return The localised string, or the label if the string could not be found or the format is invalid or does not match the args.
     * @throws ArgumentNotValid on null or empty local or label.
     */
    public String getString(Locale locale, String label, Object... args) {
        return I18N.getString(locale, label, args);
    }

}
