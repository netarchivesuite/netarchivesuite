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
/** This code is taken from http://www.incava.org/projects/java/java-diff/
 * It was there released under the LGPL as of Jan 31. 2007.
 */
package dk.netarkivet.testutils;

/**
 * Represents a difference, as used in <code>Diff</code>. A difference consists of two pairs of starting and ending
 * points, each pair representing either the "from" or the "to" collection passed to <code>Diff</code>. If an ending
 * point is -1, then the difference was either a deletion or an addition. For example, if <code>getDeletedEnd()</code>
 * returns -1, then the difference represents an addition.
 */
public class Difference {
    public static final int NONE = -1;

    /**
     * The point at which the deletion starts.
     */
    private int delStart = NONE;

    /**
     * The point at which the deletion ends.
     */
    private int delEnd = NONE;

    /**
     * The point at which the addition starts.
     */
    private int addStart = NONE;

    /**
     * The point at which the addition ends.
     */
    private int addEnd = NONE;

    /**
     * Creates the difference for the given start and end points for the deletion and addition.
     */
    public Difference(int delStart, int delEnd, int addStart, int addEnd) {
        this.delStart = delStart;
        this.delEnd = delEnd;
        this.addStart = addStart;
        this.addEnd = addEnd;
    }

    /**
     * The point at which the deletion starts, if any. A value equal to <code>NONE</code> means this is an addition.
     */
    public int getDeletedStart() {
        return delStart;
    }

    /**
     * The point at which the deletion ends, if any. A value equal to <code>NONE</code> means this is an addition.
     */
    public int getDeletedEnd() {
        return delEnd;
    }

    /**
     * The point at which the addition starts, if any. A value equal to <code>NONE</code> means this must be an
     * addition.
     */
    public int getAddedStart() {
        return addStart;
    }

    /**
     * The point at which the addition ends, if any. A value equal to <code>NONE</code> means this must be an addition.
     */
    public int getAddedEnd() {
        return addEnd;
    }

    /**
     * Sets the point as deleted. The start and end points will be modified to include the given line.
     */
    public void setDeleted(int line) {
        delStart = Math.min(line, delStart);
        delEnd = Math.max(line, delEnd);
    }

    /**
     * Sets the point as added. The start and end points will be modified to include the given line.
     */
    public void setAdded(int line) {
        addStart = Math.min(line, addStart);
        addEnd = Math.max(line, addEnd);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + addEnd;
        result = prime * result + addStart;
        result = prime * result + delEnd;
        result = prime * result + delStart;
        return result;
    }

    /**
     * Compares this object to the other for equality. Both objects must be of type Difference, with the same starting
     * and ending points.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Difference other = (Difference) obj;
        if (addEnd != other.addEnd) {
            return false;
        }
        if (addStart != other.addStart) {
            return false;
        }
        if (delEnd != other.delEnd) {
            return false;
        }
        if (delStart != other.delStart) {
            return false;
        }
        return true;
    }

    /**
     * Returns a string representation of this difference.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("del: [" + delStart + ", " + delEnd + "]");
        buf.append(" ");
        buf.append("add: [" + addStart + ", " + addEnd + "]");
        return buf.toString();
    }

}
