package is.hi.bok.deduplicator;

import static org.junit.Assert.*;
import is.hi.bok.deduplicator.DeDuplicator;
import is.hi.bok.deduplicator.DeDuplicator.FilterMode;

import org.junit.Test;

public class DeDuplicatorTester {

	@Test
	public void testMatchingMethod() {
		/*
		DeDuplicator.MatchingMethod method1 = DeDuplicator.MatchingMethod.URL;
		DeDuplicator.MatchingMethod method2 = DeDuplicator.MatchingMethod.DIGEST;
		String correctMethod1 = "URL";
		String correctMethod2 = "DIGEST";
		
		String unknownMethod = "DIGET";
		DeDuplicator d = new DeDuplicator();
		try {
			d.setMatchingMethod(correctMethod1);
			DeDuplicator.MatchingMethod m = d.getMatchingMethod();
			assertTrue(m.equals(method1));
			d.setMatchingMethod(correctMethod2);
			m = d.getMatchingMethod();
			assertTrue(m.equals(method2));
		} catch (IllegalArgumentException e) {
			fail("Should not throw IllegalArgumentException on valid MatchingMethod");
		}
		try {
			d.setMatchingMethod(unknownMethod);
			fail("Should throw IllegalArgumentException on invalid MatchingMethod");
		} catch (IllegalArgumentException e) {
			// OK to throw Exception
		}
		*/
	}
	
	public void testFilterMethod() {
		/*
		FilterMode correct1 = FilterMode.BLACKLIST;
		FilterMode correct2 = FilterMode.WHITELIST;
		String correct1AsString = "BLACKLIST";
		String correct2AsString = "WHITELIST";
		String unknown = "BAD";
		DeDuplicator d = new DeDuplicator();
		try {
			d.setfilterMode(correct1AsString);
			FilterMode m = d.getFilterMode();
			assertTrue(m.equals(correct1));
			d.setfilterMode(correct2AsString);
			m = d.getFilterMode();
			assertTrue(m.equals(correct2));
		} catch (IllegalArgumentException e) {
			fail("Should not throw IllegalArgumentException on valid FilterMode");
		}
		try {
			
			d.setfilterMode(unknown);
			fail("Should throw IllegalArgumentException on invalid FilterMode");
		} catch (IllegalArgumentException e) {
			// OK to throw Exception
		}
		
	}
	
	*/
	}	

}
