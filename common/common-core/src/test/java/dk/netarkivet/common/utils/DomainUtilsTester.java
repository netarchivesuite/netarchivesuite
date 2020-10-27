package dk.netarkivet.common.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DomainUtilsTester {
	@Test
	public void canRetrieveTLDsFromPublisuffixFile() {
		final int tldcount = 8381;
		List<String> tlds = new ArrayList<String>();
		List<String> tldsQuoted = new ArrayList<String>();
		TLD.readTldsFromPublicSuffixFile(tlds, tldsQuoted);
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
    
    @Test
    public void testExtraTLDInSettingsFiles() {
    	String oldprop = System.getProperty(Settings.SETTINGS_FILE_PROPERTY);
    	if (oldprop == null) {
    		oldprop = "";
    	}
    	int count = TLD.getInstance().getAllTlds(false).size();
    	System.setProperty(Settings.SETTINGS_FILE_PROPERTY, 
    			getTestResourceFile("settings_with_extra_tlds.xml").getAbsolutePath());
    	Settings.reload();
    	TLD.reset();
    	List<String> tlds = new ArrayList<String>();
		List<String> tldsQuoted = new ArrayList<String>();
    	TLD.readTldsFromSettings(tlds, tldsQuoted);
    	assertTrue(tlds.size() == 2);
        int newcount = TLD.getInstance().getAllTlds(false).size();
        assertTrue(newcount == (count + 2));
    	System.setProperty(Settings.SETTINGS_FILE_PROPERTY, oldprop);
 
    	Settings.reload();
    	TLD.reset();
    }
    
    /**
     * Find a test resource for a given path.
     * @param path the path relative to resources directory eg. path is mypackage/YourFile.csv 
     * if file is <project>/src/test/resources/mypackage/YourFile.csv
     * @return the file corresponding to the given path 
     */
    public static File getTestResourceFile(String path) {
            URL url = Thread.currentThread().getContextClassLoader().getResource(path);
            File file = new File(url.getPath());
            return file;
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
