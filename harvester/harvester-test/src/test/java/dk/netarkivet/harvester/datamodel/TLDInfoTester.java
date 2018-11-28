/*
 * #%L
 * Netarchivesuite - harvester - test
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
package dk.netarkivet.harvester.datamodel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Unit tests for the TLDInfo class.
 */
public class TLDInfoTester {

    @Test
    public void testConstructor() {
        TLDInfo info = new TLDInfo("dk");
        assertEquals("dk", info.getName());
        assertEquals(0, info.getCount());
        assertFalse(info.isIP());
        info = new TLDInfo("IP Address");
        assertTrue(info.isIP());
        assertEquals("IP Address", info.getName());
        assertEquals(0, info.getCount());
    }

    @Test
    public void testAddSubdomain() {
        TLDInfo i = new TLDInfo("um");
        assertEquals("Should know no subdomains to start with", 0, i.getCount());
        i.addSubdomain("foo.um");
        assertEquals("Should have one after adding one", 1, i.getCount());
        i.addSubdomain("foo.bar.um");
        assertEquals("Should have two after adding a subdomain", 2, i.getCount());
        i.addSubdomain("foo.um");
        assertEquals("Should have three after adding the same domain", 3, i.getCount());

        try {
            i.addSubdomain("bar.dk");
            fail("Should check the ending");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            i.addSubdomain("foo.museum");
            fail("Should check the ending");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            i.addSubdomain("um");
            fail("Should check the ending");
        } catch (ArgumentNotValid e) {
            // expected
        }

        try {
            i.addSubdomain(null);
            fail("Should check the ending");
        } catch (ArgumentNotValid e) {
            // expected
        }

        assertEquals("Should have three after failures", 3, i.getCount());
    }
}
