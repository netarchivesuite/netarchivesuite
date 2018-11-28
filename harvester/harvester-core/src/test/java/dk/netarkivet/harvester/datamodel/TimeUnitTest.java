/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *       the National Library of France and the Austrian National Library.
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
import static org.junit.Assert.fail;

import org.junit.Test;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Unit tests for the {@link TimeUnit} class.
 */
public class TimeUnitTest {

    @Test
    public void testFromOrdinal() {
        assertEquals(TimeUnit.HOURLY, TimeUnit.fromOrdinal(1));
        assertEquals(TimeUnit.DAILY, TimeUnit.fromOrdinal(2));
        assertEquals(TimeUnit.WEEKLY, TimeUnit.fromOrdinal(3));
        assertEquals(TimeUnit.MONTHLY, TimeUnit.fromOrdinal(4));
        assertEquals(TimeUnit.MINUTE, TimeUnit.fromOrdinal(5));
        try {
            TimeUnit.fromOrdinal(0);
            fail("Should throw ArgumentNotValid when giving arg 0");
        } catch (ArgumentNotValid e) {}
        try {
            TimeUnit.fromOrdinal(6);
            fail("Should throw ArgumentNotValid when giving arg 5");
        } catch (ArgumentNotValid e) {}
    }
}
