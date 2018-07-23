/*
 * #%L
 * Netarchivesuite - common - test
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
package dk.netarkivet.common.utils.cdx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Performs unit tests of the ARCFilenameCDXRecordFilter. Implicitly tests both SimpleCDXRecordFilter and
 * CDXRecordFilter
 */
public class ARCFilenameCDXRecordFilterTester {

    @Test
    public void testConstructor() {
        new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "filter1");
    }

    @Test
    public void testGetFiltername() {
        SimpleCDXRecordFilter cdxfil = new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "filter1");
        assertEquals("Filtername are not the same !", cdxfil.getFilterName(), "filter1");
    }

    @Test(expected = ArgumentNotValid.class)
    public void testNullFiltername() {
        new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", null);
        fail("ArgumentNotValid should have been thrown !");
    }

    @Test(expected = ArgumentNotValid.class)
    public void testEmptyFiltername() {
        new ARCFilenameCDXRecordFilter("NETARKIVET_00001.*", "");
        fail("ArgumentNotValid should have been thrown !");
    }

    @Test(expected = ArgumentNotValid.class)
    public void testEmptyFilenamePattern() {
        new ARCFilenameCDXRecordFilter("", "filter1");
        fail("ArgumentNotValid should have been thrown !");
    }

    @Test(expected = ArgumentNotValid.class)
    public void testNullFilenamePattern() {
        new ARCFilenameCDXRecordFilter(null, "filter1");
        fail("ArgumentNotValid should have been thrown !");
    }
}
