/*
 * #%L
 * Netarchivesuite - wayback - test
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
package dk.netarkivet.wayback;

import static org.junit.Assert.assertEquals;

import org.archive.wayback.UrlCanonicalizer;
import org.archive.wayback.util.url.AggressiveUrlCanonicalizer;
import org.archive.wayback.util.url.IdentityUrlCanonicalizer;
import org.junit.Test;

import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.wayback.batch.UrlCanonicalizerFactory;

@SuppressWarnings({"deprecation"})
public class UrlCanonicalizerFactoryTest {

    @Test
    public void testGetDefaultUrlCanonicalizer() {
        UrlCanonicalizer uc1 = UrlCanonicalizerFactory.getDefaultUrlCanonicalizer();
        assertEquals(
                "Expect default to return and instance of " + "NetarchiveSuiteAggressiveUrlCanonicalizer class",
                AggressiveUrlCanonicalizer.class, uc1.getClass());
        Settings.set(WaybackSettings.URL_CANONICALIZER_CLASSNAME,
                "org.archive.wayback.util.url.IdentityUrlCanonicalizer");
        uc1 = UrlCanonicalizerFactory.getDefaultUrlCanonicalizer();
        assertEquals("Expect to get IdentityUrlCanonicalizer", IdentityUrlCanonicalizer.class, uc1.getClass());
    }

}
