/*
 * #%L
 * Netarchivesuite - archive - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
package dk.netarkivet.archive.arcrepositoryadmin;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import junit.framework.TestCase;

public class ChecksumStatusTester extends TestCase {

    public void testFromOrdinal() {
        assertEquals(ChecksumStatus.UNKNOWN, ChecksumStatus.fromOrdinal(0));
        assertEquals(ChecksumStatus.CORRUPT, ChecksumStatus.fromOrdinal(1));
        assertEquals(ChecksumStatus.OK, ChecksumStatus.fromOrdinal(2));
        try {
            ChecksumStatus.fromOrdinal(3);
            fail("Should throw ArgumentNotValid with argument > 2");
        } catch (ArgumentNotValid e) {
            // Expected
        }
        
      }
}
