/*$Id$
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
package dk.netarkivet.wayback.batch;

import org.archive.wayback.UrlCanonicalizer;

import dk.netarkivet.common.utils.SettingsFactory;
import dk.netarkivet.wayback.batch.copycode.NetarchiveSuiteAggressiveUrlCanonicalizer;

/**
 * Created by IntelliJ IDEA. User: csr Date: Aug 26, 2009 Time: 10:15:15 AM To
 * change this template use File | Settings | File Templates.
 */
public class UrlCanonicalizerFactory extends SettingsFactory<UrlCanonicalizer> {

    //TODO the correct thing to do here is to load the class specified in
    //WaybackSettings. However this requires changing our security policy to allow
    //batch jobs to read system Properties, so for the time being we always
    //return a NetarchiveSuiteAggressiveUrlCanonicalizer
    public static UrlCanonicalizer getDefaultUrlCanonicalizer() {
        //return SettingsFactory.getInstance(WaybackSettings.URL_CANONICALIZER_CLASSNAME);
        return new NetarchiveSuiteAggressiveUrlCanonicalizer();
    }

}
