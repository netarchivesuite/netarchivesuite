/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.harvester.webinterface;

import javax.servlet.jsp.JspWriter;
import java.io.File;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.IngestDomainList;

/**
 * This class manages a thread of ingesting domains.
 */

public class DomainIngester extends Thread {
    /** The log. */
    Log log = LogFactory.getLog(DomainIngester.class.getName());
    /** Whether or not the ingesting process is finished yet. */
    private boolean done = false;
    /** If an exception is thrown during ingest, it gets stored here. */
    private Exception e;
    /** The file containg a list of domains to ingest. */
    private File ingestFile;
    /** The JspWriter. */
    private JspWriter out;
    /** The locale used in forwarding errormessages to the user.*/
    private Locale l;

    /** Create a new ingester for a given session and outpout, reading
     * domains from a file.
     *
     * @param out The writer that goes into HTML output
     * @param ingestFile The file of domains to ingest.
     * @param l the given locale
     */
    public DomainIngester(JspWriter out, File ingestFile, Locale l) {
        // TODO Should we validate the JspWriter
        // We currently don't, so we don't do it here either.
        ArgumentNotValid.checkNotNull(ingestFile, "File ingestFile");
        ArgumentNotValid.checkNotNull(l, "Locale l");
        
        this.out = out;
        this.ingestFile = ingestFile;
        this.l = l;
    }

    /** Starts the ingesting thread.  When 'done' is set to true, the thread
     * is finished, and any exceptions are found in 'e'.
     */
    public void run() {
        try {
            new IngestDomainList().updateDomainInfo(ingestFile, out, l);
        } catch (Exception ex) {
            this.e = ex;
            log.warn("Update domains failed", ex);
        } finally {
            done = true;
        }
    }
    
    /**
     * Check whether or not the DomainIngester is finished.
     * @return true if finished; false otherwise
     */
    public boolean isDone() {
        return done;
    }
    
    /**
     * @return any exception catched during ingest
     */
    public Exception getException() {
        return e;
    }
}
