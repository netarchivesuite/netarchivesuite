/*
 * #%L
 * Netarchivesuite - deploy - test
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

import static org.junit.Assert.fail;

import org.apache.commons.httpclient.URIException;
import org.archive.url.UsableURI;
import org.junit.Test;

public class FixedUUURITest {

    /**
     * Verify, that FixedUURI solves bug 820, and that the class org.archive.net.UURI still has the problem. When bug
     * 820 is resolved in the Heritrix class, this test will fail, and FixedUURI can be removed.
     */
    @SuppressWarnings("serial")
    @Test
    public void testBug820() throws URIException {
        String troublesomeURL = "http/www.test.foo";
        try {
            new FixedUURI(troublesomeURL, false).getReferencedHost();
        } catch (NullPointerException e) {
            fail("Should not throw an NullPointerException here: " + e);
        }

        try {
            new UsableURI(troublesomeURL, false) {
            }.getReferencedHost();
            fail("Bug 820 seems to be solved now. We can now remove FixedUURI");
        } catch (NullPointerException e) {
            // Expected
        }
    }

}
