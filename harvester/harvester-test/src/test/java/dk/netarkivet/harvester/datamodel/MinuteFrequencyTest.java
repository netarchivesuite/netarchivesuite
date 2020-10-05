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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Test;

public class MinuteFrequencyTest {

    @Test
    public void testGetFirstEvent() throws Exception {
        MinuteFrequency minuteFrequency = new MinuteFrequency(5);
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.set(2010, Calendar.FEBRUARY, 12, 8, 14);
        Date calendarDate = calendar.getTime();
        Date firstEvent = minuteFrequency.getFirstEvent(calendarDate);
        assertEquals("First event should happen at once.", calendarDate, firstEvent);
    }

    @Test
    public void testGetNextEvent() throws Exception {
        MinuteFrequency minuteFrequency = new MinuteFrequency(5);
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.set(2010, Calendar.FEBRUARY, 12, 8, 14);
        Date calendarDate = calendar.getTime();
        Date nextEvent = minuteFrequency.getNextEvent(calendarDate);
        calendar.add(Calendar.MINUTE, 5);
        assertEquals("Next event should be five minutes later.", calendar.getTime(), nextEvent);
    }
}
