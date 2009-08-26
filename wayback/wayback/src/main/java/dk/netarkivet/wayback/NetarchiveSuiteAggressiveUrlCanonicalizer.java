package dk.netarkivet.wayback;

import org.archive.wayback.util.url.AggressiveUrlCanonicalizer;
import org.archive.net.UURI;
import org.apache.commons.httpclient.URIException;

/**
     * This class overrides the standard wayback canonicalizer in order to use our
 * version of UURIFactory (see Bug 1719).
 */
public class NetarchiveSuiteAggressiveUrlCanonicalizer extends
                                                       AggressiveUrlCanonicalizer {
    public String urlStringToKey(String urlString) throws URIException {

      if(urlString.startsWith("dns:")) {
        return urlString;
    }
    String searchUrl = canonicalize(urlString);
    String scheme = UrlOperations.urlToScheme(urlString);
    if(scheme != null) {
        searchUrl = searchUrl.substring(scheme.length());
    } else {
        scheme = UrlOperations.HTTP_SCHEME;
    }

    if (-1 == searchUrl.indexOf("/")) {
        searchUrl = scheme + searchUrl + "/";
    } else {
        searchUrl = scheme + searchUrl;
    }

    // TODO: These next few lines look crazy -- need to be reworked.. This
    // was the only easy way I could find to get the correct unescaping
    // out of UURIs, possible a bug. Definitely needs some TLC in any case,
    // as building UURIs is *not* a cheap operation.

    // unescape anything that can be:
    UURI tmpURI = NetarchiveSuiteUURIFactory.getInstance(searchUrl);
    tmpURI.setPath(tmpURI.getPath());

    // convert to UURI to perform required URI fixup:
    UURI searchURI = NetarchiveSuiteUURIFactory.getInstance(tmpURI.getURI());

    // replace ' ' with '+' (this is only to match Alexa's canonicalization)
    String newPath = searchURI.getEscapedPath().replace("%20","+");

    // replace multiple consecutive '/'s in the path.
    while(newPath.contains("//")) {
        newPath = newPath.replace("//","/");
    }

    // this would remove trailing a '/' character, unless the path is empty
    // but we're not going to do this just yet..
//		if((newPath.length() > 1) && newPath.endsWith("/")) {
//			newPath = newPath.substring(0,newPath.length()-1);
//		}

    StringBuilder sb = new StringBuilder(searchUrl.length());
    sb.append(searchURI.getHostBasename());

    // omit port if scheme default:
    int defaultSchemePort = UrlOperations.schemeToDefaultPort(scheme);
    if(searchURI.getPort() != defaultSchemePort
            && searchURI.getPort() != -1) {

        sb.append(":").append(searchURI.getPort());
    }

    sb.append(newPath);
    if(searchURI.getEscapedQuery() != null) {
        sb.append("?").append(searchURI.getEscapedQuery());
    }

    return sb.toString();
    }
}
