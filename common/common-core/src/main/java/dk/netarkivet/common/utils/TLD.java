package dk.netarkivet.common.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.UnknownID;
import static dk.netarkivet.common.utils.DomainUtils.DOMAINNAME_CHAR_REGEX_STRING;

/**
 * Encapsulate the reading of Top level domains from settings and the embedded public_suffix.dat file.
 *
 */
public class TLD {

	/** The class logger. */
    private static final Logger log = LoggerFactory.getLogger(TLD.class);
	private static TLD tld;
	
	/**
     * A regular expression matching hostnames, and remembering the hostname in group 1 and the domain in group 2.
     */
    private final Pattern HOSTNAME_REGEX; 
    
    /** A string for a regexp recognising a TLD  */
    private final String TLD_REGEX_STRING; 
	
    /**
     * Regexp for matching a valid domain, that is a single domainnamepart followed by a TLD from settings, or an IP
     * address.
     */
    private final Pattern VALID_DOMAIN_MATCHER;
    
    
	public static TLD getInstance() {
		if (tld == null) {
			tld = new TLD();
		}
		return tld;
	}
	
	public static void reset() {
		tld = null;
	}
	
	private final List<String> tldListQuoted;
	private final List<String> tldList;
	
	public TLD() {
		tldListQuoted = readTldsFromPublicSuffixFile(true);
		tldListQuoted.addAll(readTldsFromSettings(true));
		
		tldList = readTldsFromPublicSuffixFile(false);
		tldList.addAll(readTldsFromSettings(false));

		TLD_REGEX_STRING = "\\.(" + StringUtils.conjoin("|", tldListQuoted) + ")";
		HOSTNAME_REGEX = Pattern.compile("^(|.*?\\.)(" + DOMAINNAME_CHAR_REGEX_STRING + "+"
	            + TLD_REGEX_STRING + ")");
		VALID_DOMAIN_MATCHER = Pattern.compile("^(" + Constants.IP_REGEX_STRING + "|"
	    		+ DOMAINNAME_CHAR_REGEX_STRING + "+" + TLD_REGEX_STRING + ")$");
	}
	
	/**
     * Helper method for reading TLDs from settings. Will read all settings, validate them as legal TLDs and warn and
     * ignore them if any are invalid. Settings may be with or without prefix "."
     *
     * @return a List of TLDs as Strings
     */
    protected static List<String> readTldsFromSettings(boolean asPattern) {
        List<String> tlds = new ArrayList<String>();
        try {
        	String[] settingsTlds = Settings.getAll(CommonSettings.TLDS);
        	for (String tld : settingsTlds) {
                if (tld.startsWith(".")) {
                    tld = tld.substring(1);
                }
                if (!tld.matches(DOMAINNAME_CHAR_REGEX_STRING + "(" + DOMAINNAME_CHAR_REGEX_STRING + "|\\.)*")) {
                    log.warn("Invalid tld '{}', ignoring", tld);
                    continue;
                }
                if (asPattern) {
                	tlds.add(Pattern.quote(tld));
                } else {
                	tlds.add(tld);
                }
            }
        } catch (UnknownID e) {
        	log.debug("No tlds found in settingsfiles " + StringUtils.conjoin(",", Settings.getSettingsFiles()));
        } 
        return tlds;
    }
        
    /**
     * Helper method for reading TLDs from the embedded public suffix file. Will read all entries, validate them as legal TLDs and warn and
     * ignore them if any are invalid.
     * @param asPattern if true, return a list of quoted Strings using Pattern.quote
     * @return a List of TLDs as Strings
     */
    protected static List<String> readTldsFromPublicSuffixFile(boolean asPattern) {
        List<String> tlds = new ArrayList<String>();
        String filePath = "dk/netarkivet/common/utils/public_suffix_list.dat";
        InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(filePath);
        
        if (stream != null) {
        	BufferedReader br = null;
        	try {
        		br = new BufferedReader(new InputStreamReader(stream));
        		String line;
        		while ((line = br.readLine()) != null) {
        			String tld = line.trim();
        			if (tld.isEmpty() || tld.startsWith("//")) {
        				continue;
        			} else {
        	            if (!tld.matches(DOMAINNAME_CHAR_REGEX_STRING + "(" + DOMAINNAME_CHAR_REGEX_STRING + "|\\.)*")) {
        	                log.warn("Invalid tld '{}', ignoring", tld);
        	                continue;
        	            }
        	            if (asPattern) {
        	            	tlds.add(Pattern.quote(tld));
        	            } else {
        	            	tlds.add(tld);
        	            }
        			}
        		}
        	} catch(IOException e) {
        		e.printStackTrace();
        	} finally {
        		IOUtils.closeQuietly(br);
        	}
        } else {
        	log.warn("Filepath '{}' to public suffix_list incorrect", filePath);
        }        
        return tlds;
    }

	public Pattern getValidDomainMatcher() {
		return VALID_DOMAIN_MATCHER;
	}

	public Pattern getHostnamePattern() {
		return HOSTNAME_REGEX;
	}
	
	public List<String> getAllTlds(boolean quoted) {
		if (quoted) {
			return tldListQuoted; 
		} else {
			return tldList;
		}
	}
}
