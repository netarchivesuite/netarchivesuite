package dk.netarkivet.harvester.harvesting;

import java.util.logging.Logger;

import org.archive.modules.CrawlURI;
import org.archive.modules.deciderules.surt.SurtPrefixedDecideRule;

/**
 * Extended <code>SurtPrefixedDecideRule</code> class.
 * Modifies a small subset of SURT seeds so it is possible to define http(s)//tld,host,..., seeds.
 * Only http(s)//www.host.tld are converted to http(s)//(tld,host,www, instead of http(s)//(tld,host,www,)/ 
 *
 * @author nicl
 */
public class NASSurtPrefixedDecideRule extends SurtPrefixedDecideRule {

	/**
	 * UUID.
	 */
	private static final long serialVersionUID = 3334790462876505839L;

    private static final Logger logger = Logger.getLogger(NASSurtPrefixedDecideRule.class.getName());

	@Override
    public void addedSeed(final CrawlURI curi) {
        if(getSeedsAsSurtPrefixes()) {
        	addedSeedImpl(curi);
        }
    }

	/**
	 * <code>addedSeed</code iImplementation method to facilitate unit testing. 
	 * @param curi <code>CrawlURI</code> object to convert
	 * @return URI converted to SURT string
	 */
	protected String addedSeedImpl(final CrawlURI curi) {
    	String originalUri = curi.getSourceTag();
    	String surt = prefixFrom(curi.getURI());
        int idx;
        int idx2;
        String scheme;
    	String surtHost;
    	String path;
        if (surt != null) {
            idx = surt.indexOf("://");
            if (idx != -1) {
                scheme = surt.substring(0, idx);
                idx += "://".length();
                idx2 = surt.indexOf(')', idx);
                if (idx2 != -1 && surt.charAt(idx++) == '(') {
                	surtHost = surt.substring(idx, idx2);
                	path = surt.substring(idx2 + 1);
                	if ("/".compareTo(path) == 0) {
                		if (originalUri != null) {
                    		idx = originalUri.indexOf("://");
                    		if (idx != -1) {
                                idx += "://".length();
                                idx = originalUri.indexOf('/', idx);
                                if (idx == -1) {
                            		surt = scheme + "://(" + surtHost; 
                                }
                    		}
                		} else {
                			logger.warning("originalUri not available");
                		}
                	}
                }
            }
        }
    	surtPrefixes.add(surt);
        return surt;
	}

}
