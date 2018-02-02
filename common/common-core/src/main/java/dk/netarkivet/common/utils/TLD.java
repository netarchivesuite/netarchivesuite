package dk.netarkivet.common.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
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
	
	public final static String PUBLIC_SUFFIX_LIST_EMBEDDED_PATH = "dk/netarkivet/common/utils/public_suffix_list.dat";
	public final static String PUBLIC_SUFFIX_LIST_EXTERNAL_FILE_PATH = "conf/public_suffix_list.dat";
	
	/**
     * A regular expression matching hostnames, and remembering the hostname in group 1 and the domain in group 2.
     */
    private final Pattern HOSTNAME_REGEX; 
    
    /** A string for a regexp recognising a TLD  */
    private final String TLD_REGEX_STRING; 
	
    /**
     * Regexp for matching a valid domain, that is a single domain-name part followed by a TLD from settings, or an IP
     * address.
     */
    private final Pattern VALID_DOMAIN_MATCHER;

    /**
     * GetInstance method for the TLD. Ensures singleton usage of the TLD class.
     * @return the current instance of the TLD class.
     */
	public static synchronized TLD getInstance() {
		if (tld == null) {
			tld = new TLD();
		}
		return tld;
	}
	
	/**
	 * Reset TLD instance. primarily used for testing.
	 */
	public static void reset() {
		tld = null;
	}
	/**
	 * List of quoted TLD read from both settings and public suffix file.
	 */
	private final List<String> tldListQuoted;
	
	/**
	 * List of TLD read from both settings and public suffix file.
	 */
	private final List<String> tldList;
	
	/**
	 * Private constructor of the TLD class. This constructor reads the TLDs from both settings and public suffix file.
	 * both quoted and unquoted. Sets the TLD_REGEX_STRING,HOSTNAME_REGEX, and  VALID_DOMAIN_MATCHER.
	 */
	private TLD() {	
		tldListQuoted = new ArrayList<String>();
		tldList = new ArrayList<String>();
		readTldsFromPublicSuffixFile(tldList, tldListQuoted);
		readTldsFromSettings(tldList, tldListQuoted);

		TLD_REGEX_STRING = "\\.(" + StringUtils.conjoin("|", tldListQuoted) + ")";
		HOSTNAME_REGEX = Pattern.compile("^(|.*?\\.)(" + DOMAINNAME_CHAR_REGEX_STRING + "+"
	            + TLD_REGEX_STRING + ")");
		VALID_DOMAIN_MATCHER = Pattern.compile("^(" + Constants.IP_REGEX_STRING + "|"
	    		+ DOMAINNAME_CHAR_REGEX_STRING + "+" + TLD_REGEX_STRING + ")$");
	}
	
	/**
     * Helper method for reading TLDs from settings. Will read all settings, validate them as legal TLDs and warn and
     * ignore them if any are invalid. Settings may be with or without prefix "."
     * @param tldList the list to add all the tlds found in the settings
     * @param quotedTldList the list to add all the tlds found in the settings - as a pattern  
     */
    protected static void readTldsFromSettings(List<String> tldList, List<String> quotedTldList) {
    	int count=0;
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
                tldList.add(tld);
                quotedTldList.add(Pattern.quote(tld));
                count++;
            }
        	log.info("Read {} TLDs from settings", count);
        } catch (UnknownID e) {
        	log.debug("No tlds found in settingsfiles " + StringUtils.conjoin(",", Settings.getSettingsFiles()));
        } 
    }
        
    /**
     * Helper method for reading TLDs from the embedded public suffix file. Will read all entries, validate them as legal TLDs and warn and
     * ignore them if any are invalid.
     * Now silently ignores starred tld's in public suffix file (e.g "*.kw") and exclusion rules (e.g. !metro.tokyo.jp)
 	 * @param tldList the list to add all the tlds found in the public suffix file 
     * @param quotedTldList the list to add all the tlds found in the public suffix file - as a pattern  
     */
    protected static void readTldsFromPublicSuffixFile(List<String> tldList, List<String> quotedTldList) {
        InputStream stream = getPublicSuffixListDataStream();
        boolean silentlyIgnoringStarTldsInPublicSuffixFile = Settings.getBoolean(CommonSettings.TLD_SILENTLY_IGNORE_STARRED_TLDS);
        int count=0;
        if (stream != null) {
        	BufferedReader br = null;
        	try {
        		br = new BufferedReader(new InputStreamReader(stream));
        		String line;
        		while ((line = br.readLine()) != null) {
        			String tld = line.trim();
        			if (tld.isEmpty() || tld.startsWith("//")) {
        				continue;
        			} else if (silentlyIgnoringStarTldsInPublicSuffixFile && (tld.startsWith("*.") || tld.startsWith("!"))) {
        				continue;
        			} else {
        	            if (!tld.matches(DOMAINNAME_CHAR_REGEX_STRING + "(" + DOMAINNAME_CHAR_REGEX_STRING + "|\\.)*")) {
        	                log.warn("Invalid tld '{}', ignoring", tld);
        	                continue; 
        	            }
        	            tldList.add(tld);
                        quotedTldList.add(Pattern.quote(tld));
        			}
        		}
        		log.info("Read {} TLDs from public suffix file", count);
        	} catch(IOException e) {
        		e.printStackTrace();
        	} finally {
        		IOUtils.closeQuietly(br);
        	}
        } else {
        	log.warn("Unable to retrieve public suffix_list failed. No tlds added!");
        }        
    }

    
    private static InputStream getPublicSuffixListDataStream() {
    	InputStream stream = null;
    	File alternateExternalFile = new File(PUBLIC_SUFFIX_LIST_EXTERNAL_FILE_PATH);
    	if (alternateExternalFile.isFile()) {
    		try {
    			stream = new FileInputStream(alternateExternalFile);
    		} catch (FileNotFoundException e) {
    			// Will never happen!
    			e.printStackTrace();
    		}
    		log.info("Reading public suffixes list from external file '{}'", alternateExternalFile.getAbsolutePath());
    	} else { // Read embedded copy
    		log.info("Did not found external public suffix list at '{}'! Reading instead the public suffixes list from embedded file '{}' in common-core.jar-VERSION.jar.", 
    				alternateExternalFile.getAbsolutePath(), PUBLIC_SUFFIX_LIST_EMBEDDED_PATH); 
    		stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(PUBLIC_SUFFIX_LIST_EMBEDDED_PATH);
    		
    	}

    	return stream;
    }

	/**
     * @return the VALID_DOMAIN_MATCHER pattern.
     */
	public Pattern getValidDomainMatcher() {
		return VALID_DOMAIN_MATCHER;
	}

	/**
	 * 
	 * @return the HOSTNAME_REGEX pattern.
	 */
	public Pattern getHostnamePattern() {
		return HOSTNAME_REGEX;
	}
	
	/**
	 * GetAllTlds method.
	 * @param quoted do you want the quoted, or unquoted list.
	 * @return the quoted list (if quoted=true), else the unquoted list.
	 */
	public List<String> getAllTlds(boolean quoted) {
		if (quoted) {
			return tldListQuoted; 
		} else {
			return tldList;
		}
	}
}
