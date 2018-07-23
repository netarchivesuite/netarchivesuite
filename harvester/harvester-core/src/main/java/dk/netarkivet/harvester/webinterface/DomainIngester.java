/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
package dk.netarkivet.harvester.webinterface;

import java.io.File;
import java.util.Locale;

import javax.servlet.jsp.JspWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.harvester.datamodel.IngestDomainList;


/**
 * This class manages a thread that ingests (i.e. creates) new domains.
 */
public class DomainIngester extends Thread {
    /** The log. */
    private static final Logger log = LoggerFactory.getLogger(DomainIngester.class);
    /** Whether or not the ingesting process is finished yet. */
    private boolean done = false;
    /** If an exception is thrown during ingest, it gets stored here. */
    private Exception e;
    /** The file containg a list of domains to ingest. */
    private File ingestFile;
    /** The JspWriter. */
    private JspWriter out;
    /** The locale used in forwarding errormessages to the user. */
    private Locale l;

    /**
     * Create a new ingester for a given session and output, reading domains from a file.
     *
     * @param out The writer that goes into HTML output
     * @param ingestFile The file with a list of domains to ingest.
     * @param l the given locale
     */
    public DomainIngester(JspWriter out, File ingestFile, Locale l) {
        ArgumentNotValid.checkNotNull(ingestFile, "File ingestFile");
        ArgumentNotValid.checkNotNull(l, "Locale l");

        this.out = out;
        this.ingestFile = ingestFile;
        this.l = l;
    }

    /**
     * Starts the ingesting thread. When 'done' is set to true, the thread is finished, and any exceptions are found in
     * 'e'.
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
     *
     * @return true if finished; false otherwise
     */
    public boolean isDone() {
        return done;
    }

    /**
     * @return any exception caught during ingest
     */
    public Exception getException() {
        return e;
    }
}
