/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.harvester.datamodel;

import javax.servlet.jsp.JspWriter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.DomainUtils;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.harvester.Constants;

/**
 * Utility class for ingesting new domains into the database.
 */
public class IngestDomainList {
    /** I18n bundle used by this class. */
    private static final I18n I18N = new dk.netarkivet.common.utils.I18n(
            Constants.TRANSLATIONS_BUNDLE);

    /** How often to log progress. */
    private static final int PRINT_INTERVAL = 10000;
    
    /** The logger. */
    protected final Log log = LogFactory.getLog(getClass());

    /** Connection to the persistent store of Domains. */
    private DomainDAO dao;
    
    /**
     * Constructor for the IngestDomainList class.
     * It makes a connection to the Domains store.
     */
    public IngestDomainList() {
        dao = DomainDAO.getInstance();
    }

    /**
     * Adds all new domains from a newline-separated file of domain names. The
     * file is assumed to be in the UTF-8 format. For large files, a line is
     * printed to the log, and to the out variable (if not set to null), every
     * PRINT_INTERVAL lines.
     * 
     * @param domainList
     *            the file containing the domain names.
     * @param out
     *            a stream to which output can be sent. May be null.
     * @param theLocale
     *            the given Locale
     */
    public void updateDomainInfo(File domainList, JspWriter out,
            Locale theLocale) {
        ArgumentNotValid.checkNotNull(domainList, "File domainList");
        ArgumentNotValid.checkNotNull(theLocale, "Locale theLocale");
        Domain myDomain;
        String domainName;
        BufferedReader in = null;
        int countDomains = 0;
        boolean print = (out != null);
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(
                    domainList), "UTF-8"));

            while ((domainName = in.readLine()) != null) {
                try {
                    countDomains++;
                    if ((countDomains % PRINT_INTERVAL) == 0) {
                        Date d = new Date();
                        String msg = "Domain #" + countDomains + ": "
                                + domainName + " added at " + d;
                        log.info(msg);
                        if (print) {
                            out.print(I18N.getString(theLocale,
                                    "domain.number.0.1.added.at.2",
                                    countDomains, domainName, d));
                            out.print("<br/>");
                            out.flush();
                        }
                    }

                    if (DomainUtils.isValidDomainName(domainName)) {
                        if (!dao.exists(domainName)) {
                            myDomain = Domain.getDefaultDomain(domainName);
                            dao.create(myDomain);
                        }
                    } else {
                        log.debug("domain '" + domainName
                                + "' is not a valid domain Name");
                        if (print) {
                            out.print(I18N.getString(theLocale,
                                    "errormsg;domain.0.is.not.a.valid"
                                            + ".domainname", domainName));
                            out.print("<br/>");
                            out.flush();
                        }
                    }
                } catch (Exception e) {
                    log
                            .debug("Could not create domain '" + domainName
                                    + "'", e);
                    if (print) {
                        out.print(I18N.getString(theLocale,
                                "errormsg;unable.to.create"
                                        + ".domain.0.due.to.error.1",
                                domainName, e.getMessage()));
                        out.print("<br/>\n");
                        out.flush();
                    }
                }
            }
        } catch (FileNotFoundException fnf) {
            String msg = "File '" + domainList.getAbsolutePath()
                    + "' not found";
            log.debug(msg);
            throw new IOFailure(msg, fnf);
        } catch (IOException io) {
            String msg = " Can't read the domain-file '"
                    + domainList.getAbsolutePath() + "'.";
            log.debug(msg);
            throw new IOFailure(msg, io);
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                throw new IOFailure("Problem closing input stream", e);
            }
        }
    }
}
