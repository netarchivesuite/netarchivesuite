package dk.netarkivet;

import java.util.HashSet;
import java.util.Set;

import org.testng.Assert;

public class NASAssert extends Assert {

  static public void assertEquals(Set<Object> expectedSet, Set<Object> resultSet) {
    Set<Object> disjunctInExpectedSet = 
      new HashSet<Object>(expectedSet);
    disjunctInExpectedSet.removeAll(resultSet);

    Set<Object> disjunctInResultSet = 
      new HashSet<Object>(resultSet);
    disjunctInResultSet.removeAll(expectedSet);

    if (!disjunctInExpectedSet.isEmpty() || !disjunctInResultSet.isEmpty()) {
      fail("Set not equal, Expected set contained the following elements not found in the result set:\n" +
          disjunctInExpectedSet + 
          "\nand the following elements in the result set where not found in the expected set\n" +
          disjunctInResultSet);
    }
  }
}
