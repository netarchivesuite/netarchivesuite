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

import edu.harvard.hul.ois.jhove.Module;
import edu.harvard.hul.ois.jhove.module.*;
import edu.harvard.hul.ois.jhove.RepInfo;

/**
 * 
 * @author jolf
 *
 */
public class JhoveBatchJob extends FileBatchJob{

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
    public boolean processFile(File file, OutputStream os) {
	try {
	    // load file, initialize representation tool and an output handler
	    FileInputStream fis = new FileInputStream(file);
	    RepInfo repinfo = new RepInfo(DUMMY_URI);

	    // get module based on extension
	    Module extMod = FindModule(file.getName());

	    // check if module was loaded
	    if(extMod == null) {
		os.write(new String("Unsupported file extension: " 
			+ file.getName()).getBytes());
		return false;
	    }

	    // Is this necessary?
	    extMod.setVerbosity(Module.MAXIMUM_VERBOSITY);

	    // parse file
	    int pars = extMod.parse(fis, repinfo, 0);

	    // write out results 
	    os.write(new String(
		    file.getName() + STREAM_SEPARATOR +
		    pars + STREAM_SEPARATOR + 
		    repinfo.getFormat() + STREAM_SEPARATOR + 
		    repinfo.getVersion() + STREAM_SEPARATOR + 
		    repinfo.getMimeType() + STREAM_DONE
	    ).getBytes());

	    return true;
	} catch (FileNotFoundException e) {
	    System.out.println("File not found: " + file.getName());
	    return false;
	} catch (IOException e) {
	    System.out.println("Input/output error: " + file.getName());
	    return false;
	} catch (Exception e) {
	    System.out.println("Unknown error: " + file.getName() + " : " + e);
	    return false;
	}
    }

    /**
     * This function makes a module corresponding to the extension of a file.
     * If unknown extension, return null.
     * 
     * Modules 		|   Extensions
     * AiffModule: 		aiff, aif, aifc
     * GifModule: 		gif, gfa
     * HtmlModule: 		htm, html
     * JpegModule: 		jpg, jpeg, jpe, jif, jfif, jfi
     * Jpeg2000Module: 	jp2, j2c, jpc, j2k, jpx
     * PdfModule:		pdf, epdf
     * TiffModule: 		tiff, tif
     * WaveModule:		wav
     * XmlModule:		xml
     * 
     * @param name The name of the file
     * @return The module corresponding to the extension of the filename
     */
    private Module FindModule(String name){

	if(name.endsWith(".aiff") 
		|| name.endsWith(".aif") 
		|| name.endsWith(".aifc")) {
	    return new AiffModule();
	} else if(name.endsWith(".gif") 
		|| name.endsWith(".gfa")) {
	    return new GifModule();			
	} else if(name.endsWith(".html") 
		|| name.endsWith(".htm")) {
	    return new HtmlModule();
	} else if(name.endsWith(".jpg") 
		|| name.endsWith(".jpeg") 
		|| name.endsWith(".jpe") 
		|| name.endsWith(".jif") 
		|| name.endsWith(".jfif") 
		|| name.endsWith(".jfi")) {
	    return new JpegModule();
	} else if(name.endsWith(".jp2") 
		|| name.endsWith(".j2c") 
		|| name.endsWith(".jpc") 
		|| name.endsWith(".j2k") 
		|| name.endsWith(".jpx")) {
	    return new Jpeg2000Module();
	} else if(name.endsWith(".pdf") 
		|| name.endsWith(".epdf")) {			
	    return new PdfModule();
	} else if(name.endsWith(".tiff") 
		|| name.endsWith(".tif")) {
	    return new TiffModule();
	} else if(name.endsWith(".wav")) {
	    return new WaveModule();
	} else if(name.endsWith(".xml")) {
	    return new XmlModule();
	}

	return null;
    }
}
