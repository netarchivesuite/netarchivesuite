/*$Id$
* $Revision$
* $Date$
* $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.wayback.batch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.archive.wayback.UrlCanonicalizer;

import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.wayback.WaybackSettings;
import dk.netarkivet.wayback.batch.copycode.NetarchiveSuiteAggressiveUrlCanonicalizer;

/**
 * A factory for returning a UrlCanonicalizer
 */
public class UrlCanonicalizerFactory extends SettingsFactory<UrlCanonicalizer> {

    private static Log logger = LogFactory.getLog(UrlCanonicalizerFactory.class);

    /**
     * This method returns an instance of the UrlCanonicalizer class specified
     * in the settings.xml for the dk.netarkivet.wayback module. In the event
     * that reading this file generates a SecurityException, as may occur in
     * batch operation if security does not allow System properties to be read,
     * the method will fall back on returning an instance of the class
     * dk.netarkivet.wayback.batch.copycode.NetarchiveSuiteAggressiveUrlCanonicalizer
     * @return a canonicalizer for urls
     */
    public static UrlCanonicalizer getDefaultUrlCanonicalizer()  {
        try {
            return SettingsFactory.getInstance(WaybackSettings.URL_CANONICALIZER_CLASSNAME);
        } catch (SecurityException e) {
            logger.debug("The requested canoncializer could not be loaded. "
                         + "Falling back to "
                         + NetarchiveSuiteAggressiveUrlCanonicalizer
                    .class.toString(), e);
            return new NetarchiveSuiteAggressiveUrlCanonicalizer();
        }
    }

}
