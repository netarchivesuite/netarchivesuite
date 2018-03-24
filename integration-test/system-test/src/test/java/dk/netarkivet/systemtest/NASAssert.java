/*
 * #%L
 * NetarchiveSuite System test
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
package dk.netarkivet.systemtest;

import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;

public class NASAssert extends Assert {

    // FIXME: http://stackoverflow.com/q/2313229/53897

    /**
     * Improved assert method for set, which prints the difference between the two sets.
     *
     * @param expectedSet
     * @param resultSet
     */
    public static void assertEquals(Set<?> expectedSet, Set<?> resultSet) {
        Set<Object> disjunctInExpectedSet = new HashSet<Object>(expectedSet);
        disjunctInExpectedSet.removeAll(resultSet);

        Set<Object> disjunctInResultSet = new HashSet<Object>(resultSet);
        disjunctInResultSet.removeAll(expectedSet);

        if (!disjunctInExpectedSet.isEmpty() || !disjunctInResultSet.isEmpty()) {
            fail("Sets not equal, Expected sets contained the following " + disjunctInExpectedSet.size() + " elements "
                    + " not found in the result set:\n" + disjunctInExpectedSet + "\nand the following "
                    + disjunctInResultSet.size() + " elements in the result set where not"
                    + " found in the expected set\n" + disjunctInResultSet);
        }
    }
}
