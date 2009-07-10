/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * Copyright Det Kongelige Bibliotek og Statsbiblioteket, Danmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package dk.netarkivet.wayback;

import java.io.OutputStream;
import java.io.IOException;
import java.io.File;

import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.resourceindex.cdx.SearchResultToCDXLineAdapter;
import org.archive.wayback.resourcestore.indexer.ARCRecordToSearchResultAdapter;
import org.archive.wayback.util.url.AggressiveUrlCanonicalizer;
import org.archive.wayback.util.url.IdentityUrlCanonicalizer;
import org.archive.wayback.util.Adapter;
import org.archive.wayback.UrlCanonicalizer;
import org.archive.wayback.WaybackConstants;
import org.archive.net.UURI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.Header;

import dk.netarkivet.common.utils.arc.ARCBatchJob;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Returns a cdx file using the appropriate format for wayback, including
 * canonicalisation of urls. The returned files are unsorted.
 *
 * @author csr
 * @since Jul 1, 2009
 */

public class ExtractWaybackCDXBatchJob extends ARCBatchJob {
   /**
     * Logger for this class.
     */
    private final Log log = LogFactory.getLog(getClass().getName());
    private MyARCRecordToSearchResultAdapter aToSAdapter;
    private SearchResultToCDXLineAdapter srToCDXAdapter;

    public void initialize(OutputStream os) {
        aToSAdapter = new MyARCRecordToSearchResultAdapter();
        AggressiveUrlCanonicalizer auc = new MyAggressiveUrlCanonicalizer();
        aToSAdapter.setCanonicalizer(auc);
        srToCDXAdapter = new  SearchResultToCDXLineAdapter();
    }

    public void processRecord(ARCRecord record, OutputStream os) {
       CaptureSearchResult csr = null;
        try {
            csr = aToSAdapter.adapt(record);
        } catch (Exception e) {
            log.warn(e);
        }
        try {
            if (csr != null) {
                os.write(srToCDXAdapter.adapt(csr).getBytes());
                os.write("\n".getBytes());
            }
        } catch (IOException e) {
            throw new IOFailure("Write error in batch job", e);
        } catch (Exception e) {
            log.warn(e);
        }

    }

    public void finish(OutputStream os) {
        //No cleanup required
    }

    /**
     * This class overrides the standard wayback canonicalizer in order to use our
     * version of UURIFactory (see Bug 1719).
     */
    public static class MyAggressiveUrlCanonicalizer extends AggressiveUrlCanonicalizer {
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
		UURI tmpURI = MyUURIFactory.getInstance(searchUrl);
		tmpURI.setPath(tmpURI.getPath());

		// convert to UURI to perform required URI fixup:
		UURI searchURI = MyUURIFactory.getInstance(tmpURI.getURI());

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

    public class MyARCRecordToSearchResultAdapter implements Adapter<ARCRecord,CaptureSearchResult> {
        	private UrlCanonicalizer canonicalizer = null;

	public MyARCRecordToSearchResultAdapter() {
		canonicalizer = new IdentityUrlCanonicalizer();
	}
//	public static SearchResult arcRecordToSearchResult(final ARCRecord rec)
//	throws IOException, ParseException {
	/* (non-Javadoc)
	 * @see org.archive.wayback.util.Adapter#adapt(java.lang.Object)
	 */
	public CaptureSearchResult adapt(ARCRecord rec) {
		try {
			return adaptInner(rec);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private CaptureSearchResult adaptInner(ARCRecord rec) throws IOException {
		rec.close();
		ARCRecordMetaData meta = rec.getMetaData();

		CaptureSearchResult result = new CaptureSearchResult();
		String arcName = meta.getArc();
		int index = arcName.lastIndexOf(File.separator);
		if (index > 0 && (index + 1) < arcName.length()) {
		    arcName = arcName.substring(index + 1);
		}
		result.setFile(arcName);
		result.setOffset(meta.getOffset());

		// initialize with default HTTP code...
		result.setHttpCode("-");

		result.setDigest(rec.getDigestStr());
		result.setMimeType(meta.getMimetype());
		result.setCaptureTimestamp(meta.getDate());

		String uriStr = meta.getUrl();
		if (uriStr.startsWith(ARCRecord.ARC_MAGIC_NUMBER)) {
			// skip filedesc record altogether...
			return null;
		}
		if (uriStr.startsWith(WaybackConstants.DNS_URL_PREFIX)) {
			// skip URL + HTTP header processing for dns records...

			result.setOriginalUrl(uriStr);
			result.setRedirectUrl("-");
			result.setUrlKey(uriStr);

		} else {

			result.setOriginalUrl(uriStr);


			String statusCode = (meta.getStatusCode() == null) ? "-" : meta
					.getStatusCode();
			result.setHttpCode(statusCode);

			String redirectUrl = "-";
			Header[] headers = rec.getHttpHeaders();
			if (headers != null) {

				for (int i = 0; i < headers.length; i++) {
					if (headers[i].getName().equals(
							WaybackConstants.LOCATION_HTTP_HEADER)) {

						String locationStr = headers[i].getValue();
						// TODO: "Location" is supposed to be absolute:
						// (http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html)
						// (section 14.30) but Content-Location can be
						// relative.
						// is it correct to resolve a relative Location, as
						// we are?
						// it's also possible to have both in the HTTP
						// headers...
						// should we prefer one over the other?
						// right now, we're ignoring "Content-Location"
						redirectUrl = UrlOperations.resolveUrl(uriStr,
								locationStr);

						break;
					}
				}
				result.setRedirectUrl(redirectUrl);

				String urlKey = canonicalizer.urlStringToKey(meta.getUrl());
				result.setUrlKey(urlKey);
			}
		}
		return result;
	}
	public UrlCanonicalizer getCanonicalizer() {
		return canonicalizer;
	}
	public void setCanonicalizer(UrlCanonicalizer canonicalizer) {
		this.canonicalizer = canonicalizer;
	}
    }

}
