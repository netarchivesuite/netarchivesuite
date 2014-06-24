package dk.netarkivet.systemtest;

import java.util.HashSet;
import java.util.Set;
import org.testng.Assert;

public class NASAssert extends Assert {

	// FIXME: http://stackoverflow.com/q/2313229/53897
	
    /**
     * Improved assert method for set, which prints the difference between the two sets.
     * @param expectedSet
     * @param resultSet
     */
  public static void assertEquals(Set<?> expectedSet, Set<?> resultSet) {
    Set<Object> disjunctInExpectedSet = new HashSet<Object>(expectedSet);
    disjunctInExpectedSet.removeAll(resultSet);

    Set<Object> disjunctInResultSet =
      new HashSet<Object>(resultSet);
    disjunctInResultSet.removeAll(expectedSet);

    if (!disjunctInExpectedSet.isEmpty() || !disjunctInResultSet.isEmpty()) {
            fail("Sets not equal, Expected sets contained the following "
                    + disjunctInExpectedSet.size() + " elements "
                    + " not found in the result set:\n" + disjunctInExpectedSet
                    + "\nand the following " +  disjunctInResultSet.size()
                    + " elements in the result set where not"
                    + " found in the expected set\n" + disjunctInResultSet);
        }
  }
}
