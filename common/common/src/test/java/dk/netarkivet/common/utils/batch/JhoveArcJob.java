/* $Id$
 * $Date$
 * $Revision$
 * $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.common.utils.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCReader;
import org.archive.io.arc.ARCReaderFactory;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.arc.ARCBatchJob;

import edu.harvard.hul.ois.jhove.Module;
import edu.harvard.hul.ois.jhove.module.*;
import edu.harvard.hul.ois.jhove.RepInfo;

/**
 * 
 * @author jolf
 *
 */

public class JhoveArcJob extends ARCBatchJob {
    private static final String DUMMY_URI = "doms://" 
	+ JhoveBatchJob.class.getCanonicalName() + "/DUMMY";
    private static final String STREAM_SEPARATOR = ",";
    private static final String STREAM_DONE = "\n";

    @Override
    public void finish(OutputStream os) {
	// TODO Auto-generated method stub

    }

    @Override
    public void initialize(OutputStream os) {
	// TODO Auto-generated method stub

    }

    @Override
    public void processRecord(ARCRecord record, OutputStream os) {
	// TODO Auto-generated method stub

	try {

	    ARCRecordMetaData arcRMD = record.getMetaData();
	    RepInfo repinfo = new RepInfo(DUMMY_URI);

	    // find file format from url-extension.
	    Module mod = FindModuleFromUrl(arcRMD.getUrl());

	    // if no proper url-extension, try mimetype
	    if(mod == null) {

		mod = FindModuleFromMimetype(arcRMD.getMimetype());

		if(mod == null) {
		    os.write(new String(
			    "null" + STREAM_SEPARATOR +
			    "null" + STREAM_SEPARATOR +
			    "null" + STREAM_SEPARATOR +
			    arcRMD.getMimetype() + STREAM_DONE
		    ).getBytes());
		    return;
		}

	    } 

	    int pars = mod.parse(record, repinfo, 0);

	    // write out results
	    os.write(new String(
		    pars + STREAM_SEPARATOR + 
		    repinfo.getFormat() + STREAM_SEPARATOR + 
		    repinfo.getVersion() + STREAM_SEPARATOR + 
		    repinfo.getMimeType() + STREAM_DONE
	    ).getBytes());

	} catch (Exception e) {
	    // HANDLE EXCEPTION ??
	    return;
	}
    }


    /**
     * This function makes a module corresponding to the extension of a url.
     * If unknown extension, return null.
     * 
     * Modules 		|   Extensions
     * AiffModule: 		aiff, aif, aifc
     * GifModule: 		gif, gfa
     * HtmlModule: 		htm, html
     * JpegModule: 		jpg, jpeg, jpe, jif, jfif, jfi
     * Jpeg2000Module: 		jp2, j2c, jpc, j2k, jpx
     * PdfModule:		pdf, epdf
     * TiffModule: 		tiff, tif
     * WaveModule:		wav
     * XmlModule:		xml
     * 
     * @param url The url of the file
     * @return The module corresponding to the extension of the filename
     */
    private Module FindModuleFromUrl(String url){

	if(url.endsWith(".aiff") 
		|| url.endsWith(".aif") 
		|| url.endsWith(".aifc")) {
	    return new AiffModule();
	} else if(url.endsWith(".gif") 
		|| url.endsWith(".gfa")) {
	    return new GifModule();			
	} else if(url.endsWith(".html") 
		|| url.endsWith(".htm")) {
	    return new HtmlModule();
	} else if(url.endsWith(".jpg") 
		|| url.endsWith(".jpeg") 
		|| url.endsWith(".jpe") 
		|| url.endsWith(".jif") 
		|| url.endsWith(".jfif") 
		|| url.endsWith(".jfi")) {
	    return new JpegModule();
	} else if(url.endsWith(".jp2") 
		|| url.endsWith(".j2c") 
		|| url.endsWith(".jpc") 
		|| url.endsWith(".j2k") 
		|| url.endsWith(".jpx")) {
	    return new Jpeg2000Module();
	} else if(url.endsWith(".pdf") 
		|| url.endsWith(".epdf")) {			
	    return new PdfModule();
	} else if(url.endsWith(".tiff") 
		|| url.endsWith(".tif")) {
	    return new TiffModule();
	} else if(url.endsWith(".wav")) {
	    return new WaveModule();
	} else if(url.endsWith(".xml")) {
	    return new XmlModule();
	}

	return null;
    }

    /**
     * This function makes a module corresponding to the mimetype.
     * If unknown extension, return null.
     * 
     * Modules 		|   Mimetype
     * AiffModule: 		N/A
     * GifModule: 		image/gif
     * HtmlModule: 		text/html
     * JpegModule: 		image/jpeg
     * Jpeg2000Module: 	N/A
     * PdfModule:		application/pdf
     * TiffModule: 		N/A
     * WaveModule:		audio/x-wav
     * XmlModule:		text/xml, application/xml
     * 		suffix: '-xml'
     * 		* (application/atom+xml, application/rss+xml, 
     * 			application/xml...)
     * 
     * Mimetype found but not handled:
     * text/plain
     * text/dns
     * text/css
     * text/javascript
     * image/png
     * image/x-icon
     * application/x-javascript
     * application/vnd.ms-powerpoint
     * application/x-cdx
     * application/x-shockwave-flash
     * application/x-component
     * application/json
     * application/x-gzip
     * application/x-java-archive
     * 
     * @param name The name of the file
     * @return The module corresponding to the extension of the filename
     */
    private Module FindModuleFromMimetype(String mimetype){

	if(mimetype.equalsIgnoreCase("text/html")) {
	    return new HtmlModule();
	} else if(mimetype.equalsIgnoreCase("text/xml") ||
		mimetype.equalsIgnoreCase("application/xml")) {
	    return new XmlModule();
	} else if(mimetype.equalsIgnoreCase("image/jpeg")) {
	    return new JpegModule();
	} else if(mimetype.equalsIgnoreCase("image/gif")) {
	    return new GifModule();
	} else if(mimetype.equalsIgnoreCase("application/pdf")) {
	    return new PdfModule();
	} else if(mimetype.endsWith("audio/x-wav")) {
	    return new WaveModule();
	} else if(mimetype.endsWith("-xml")) {
	    return new XmlModule();
	}

	return null;
    }
}
