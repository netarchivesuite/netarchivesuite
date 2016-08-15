package dk.netarkivet.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.experimental.categories.Category;

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
	
    @Test
    public void testDomainNameFromHostname() throws Exception {
        Map<String, String> hostnameToDomainname = new HashMap<String, String>();
        // Normal hostnames
        hostnameToDomainname.put("foo.dk", "foo.dk");
        hostnameToDomainname.put("smurf.bar.com", "bar.com");
        hostnameToDomainname.put("x.y.baz.aero", "baz.aero");
        hostnameToDomainname.put("a.dk", "a.dk");
        // Two part host names
        hostnameToDomainname.put("bbc.co.uk", "bbc.co.uk");
        hostnameToDomainname.put("news.bbc.co.uk", "bbc.co.uk");
        hostnameToDomainname.put("bl.uk", "bl.uk");
        hostnameToDomainname.put("www.bl.uk", "bl.uk");
        // IP-addresses and IP-like hostnames
        hostnameToDomainname.put("1.dk", "1.dk");
        hostnameToDomainname.put("192.168.0.dk", "0.dk");
        hostnameToDomainname.put("192.160.1.2.dk", "2.dk");
        hostnameToDomainname.put("192.168.0.3", "192.168.0.3");
        // Illegal hostnames
        hostnameToDomainname.put("foo.d", null);
        hostnameToDomainname.put("dk", null);
        hostnameToDomainname.put(".dk", null);
        hostnameToDomainname.put("dk.", null);
        hostnameToDomainname.put("[].dk", null);
        hostnameToDomainname.put("192.168.0", null);
        hostnameToDomainname.put("192.168.0.", null);
        hostnameToDomainname.put("3.192.168.0.5", null);

        for (Map.Entry<String, String> entry : hostnameToDomainname.entrySet()) {
            String domainName = DomainUtils.domainNameFromHostname(entry.getKey());
            assertEquals("Domain name should be correctly calculated for " + entry.getKey(), entry.getValue(),
                    domainName);
            if (entry.getValue() != null) {
                assertTrue("Domain name calculated from " + entry.getKey() + " must be a legal domain name",
                        DomainUtils.isValidDomainName(domainName));
            } else {
                assertFalse("Should not get null domain name from legal domainname " + entry.getKey(),
                        DomainUtils.isValidDomainName(entry.getKey()));
            }
        }
    }
    
    
    /**
     * Test that we have a sensible regexp for checking validity of domain names.
     */
    @Category(SlowTest.class)
    @Test
    public void testIsValidDomainName() throws Exception {
        assertFalse("Multidot should not be valid", DomainUtils.isValidDomainName("foo.bar.dk"));
        assertFalse("Multidot should not be valid", DomainUtils.isValidDomainName(".bar.dk"));
        assertFalse("Multidot should not be valid", DomainUtils.isValidDomainName("foo.bar."));
        assertFalse("Strange TLDs should not be valid", DomainUtils.isValidDomainName("foo.foo.bar"));
        assertFalse("Singledot should not be valid", DomainUtils.isValidDomainName(".dk"));
        assertFalse("Ending in dot should not be valid", DomainUtils.isValidDomainName("foo."));
        assertFalse("Nodot should not be valid", DomainUtils.isValidDomainName("dk"));
        assertTrue("Danish characters should be valid", DomainUtils.isValidDomainName("æøåÆØÅëËüÜéÉ.dk"));
        // The following command will extract all non-LDH chars from
        // a domain list:
        // sed 's/\(.\)/\1\n/g;' <dk-domains-10102005.utf-8.txt | grep -v
        // '[abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-]' | sort -u
        assertTrue("Characters from the domain list should be legal", DomainUtils.isValidDomainName("åäæéöøü.dk"));
        assertTrue("Raw IP numbers should be legal", DomainUtils.isValidDomainName("192.168.0.1"));
        assertFalse("Mixed IP/DNS names should not be legal", DomainUtils.isValidDomainName("foo.1"));
        assertTrue("DNS names starting with numbers should be legal", DomainUtils.isValidDomainName("1.dk"));
        assertFalse("Temporarily enabled domain names should eventually not be valid",
                DomainUtils.isValidDomainName("foo.aspx"));
        assertFalse("Temporarily enabled domain names should eventually not be valid",
                DomainUtils.isValidDomainName("bar.d"));
    }

    

} 
