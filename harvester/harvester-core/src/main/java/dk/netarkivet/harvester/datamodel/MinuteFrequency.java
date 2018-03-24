/*
 * #%L
 * Netarchivesuite - harvester
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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Allows specification of a schedule with a frequency measured in minutes. This is an "anyTime" frequency, meaning that
 * only the frequency of of the scheduling is specified, without any constraint on the actual walltime. (This is because
 * the additional constraints don't make sense for minute frequencies. E.g. you can have a frequency "Every day at 6pm"
 * but how would you complete "Every 17 minutes at ???"?)
 */
public class MinuteFrequency extends Frequency {

    /**
     * Constructor specifying the number of minutes between runs scheduled with this frequency.
     *
     * @param numMinutes
     */
    public MinuteFrequency(int numMinutes) {
        super(numMinutes, true);
    }

    /**
     * This method returns the time of the next event, which is just the value of lastEvent+(numMinutes)Minutes
     *
     * @param lastEvent A time from which the next event should be calculated.
     * @return the time of the next event.
     * @throws ArgumentNotValid if lastEvent is null.
     */
    @Override
    public Date getNextEvent(Date lastEvent) {
        ArgumentNotValid.checkNotNull(lastEvent, "lastEvent");
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(getFirstEvent(lastEvent));
        calendar.add(Calendar.MINUTE, getNumUnits());
        return calendar.getTime();
    }

    /**
     * As this is an "anyTime" frequency, this method just returns its argument (so long as it is not null).
     *
     * @param startTime The earliest time the event can happen.
     * @return the startTime for the first event of this frequency.
     * @throws ArgumentNotValid if startTime is null.
     */
    @Override
    public Date getFirstEvent(Date startTime) {
        ArgumentNotValid.checkNotNull(startTime, "startTime");
        return startTime;
    }

    @Override
    public Integer getOnMinute() {
        return null;
    }

    @Override
    public Integer getOnHour() {
        return null;
    }

    @Override
    public Integer getOnDayOfWeek() {
        return null;
    }

    @Override
    public Integer getOnDayOfMonth() {
        return null;
    }

    @Override
    public int ordinal() {
        return TimeUnit.MINUTE.ordinal();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        MinuteFrequency that = (MinuteFrequency) o;

        if (getNumUnits() != that.getNumUnits()) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + getNumUnits();
        return result;
    }

}
