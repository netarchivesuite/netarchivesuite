package dk.netarkivet.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class DomainUtilsTester {
	@Test
	public void canRetrieveTLDsFromPublisuffixFile() {
		final int tldcount = 7975;
		List<String> tlds = DomainUtils.readTldsFromPublicSuffixFile(true);
		assertEquals(tlds.size(), tldcount);
	}
	
	@Test
	public void canValidatePreviouslyInvalidTlds() {
		String[] previouslyTldsInvalidatedbyNAS = new String[] { 
				"moonburn.rocks",
				"sunde.tips",
				"spaceagent.agency",
				"gardinbussen.gratis",
				"lesson.one"
		};
		for (String tld: previouslyTldsInvalidatedbyNAS) {
			assertTrue(DomainUtils.isValidDomainName(tld));
		}
	}
} 
