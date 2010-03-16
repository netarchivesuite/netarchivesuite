/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package dk.netarkivet.wayback;

import junit.framework.TestCase;
import org.archive.wayback.UrlCanonicalizer;
import org.archive.wayback.util.url.IdentityUrlCanonicalizer;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.wayback.batch.copycode.NetarchiveSuiteAggressiveUrlCanonicalizer;
import dk.netarkivet.wayback.batch.UrlCanonicalizerFactory;

/**
 *
 */
public class UrlCanonicalizerFactoryTester extends TestCase {

    public void testGetDefaultUrlCanonicalizer() {
        UrlCanonicalizer uc1 = UrlCanonicalizerFactory.getDefaultUrlCanonicalizer();
        assertEquals("Expect default to return and instance of "
                     + "NetarchiveSuiteAggressiveUrlCanonicalizer class",
                     NetarchiveSuiteAggressiveUrlCanonicalizer.class,
                     uc1.getClass());
        Settings.set(WaybackSettings.URL_CANONICALIZER_CLASSNAME, "org.archive.wayback.util.url.IdentityUrlCanonicalizer");
        uc1 = UrlCanonicalizerFactory.getDefaultUrlCanonicalizer();
        assertEquals("Expect to get IdentityUrlCanonicalizer", IdentityUrlCanonicalizer.class, uc1.getClass());
    }


}
