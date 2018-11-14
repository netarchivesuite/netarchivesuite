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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;

import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Unit tests for the DomainOwner class.
 */
public class DomainOwnerInfoTester {

    @Test
    public void testCompareTo() throws Exception {
        DomainOwnerInfo i1 = new DomainOwnerInfo(new Date(1), "foo");
        DomainOwnerInfo i2 = new DomainOwnerInfo(new Date(2), "bar");
        DomainOwnerInfo i3 = new DomainOwnerInfo(new Date(0), "baz");

        try {
            i1.compareTo(null);
            fail("Failed to throw ArgumentNotValid");
        } catch (ArgumentNotValid e) {
            // expected
        }

        assertTrue("Earlier domain owner info should compare less", i1.compareTo(i2) < 0);
        assertTrue("Later domain owner info should compare greater", i2.compareTo(i3) > 0);
        assertTrue("Same domain owner info should compare equals", i2.compareTo(i2) == 0);
    }
}
