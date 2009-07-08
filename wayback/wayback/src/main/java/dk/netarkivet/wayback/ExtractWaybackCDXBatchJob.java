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
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.archive.io.arc.ARCRecord;
import org.archive.wayback.resourcestore.indexer.ARCRecordToSearchResultAdapter;
import org.archive.wayback.util.url.AggressiveUrlCanonicalizer;
import org.archive.wayback.core.CaptureSearchResult;
import org.archive.wayback.resourceindex.cdx.SearchResultToCDXLineAdapter;
import org.archive.net.UURIFactory;
import org.archive.net.UURI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.httpclient.URIException;

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
    private ARCRecordToSearchResultAdapter aToSAdapter;
    private SearchResultToCDXLineAdapter srToCDXAdapter;

    public void initialize(OutputStream os) {
        try {
            //Thread.currentThread().getContextClassLoader().loadClass("org.archive.net.UURIFactory");
            Class<?> urifactory = this.getClass().getClassLoader().loadClass(
                    "org.archive.net.UURIFactory");
            Method method = urifactory.getMethod("getInstance", String.class);
            Object o = method.invoke(null, "http://foo.bar");
            try {
                os.write(("Output was: '" + o.toString() + "'\n").getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            this.getClass().getClassLoader().loadClass("org.archive.net.UURI");
            this.getClass().getClassLoader().loadClass("gnu.inet.encoding.IDNA");
            this.getClass().getClassLoader().loadClass("gnu.inet.encoding.IDNAException");
            this.getClass().getClassLoader().loadClass("it.unimi.dsi.mg4j.util.MutableString");
            this.getClass().getClassLoader().loadClass("org.apache.commons.httpclient.URI");
            this.getClass().getClassLoader().loadClass("org.apache.commons.httpclient.URIException");
            this.getClass().getClassLoader().loadClass("org.archive.util.TextUtils");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        try {
            UURI uurif = UURIFactory.getInstance("http://nosuch.dummy");
        } catch (URIException e) {
           throw new RuntimeException(e);
        }

        aToSAdapter = new ARCRecordToSearchResultAdapter();
        AggressiveUrlCanonicalizer auc = new AggressiveUrlCanonicalizer();
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
}
