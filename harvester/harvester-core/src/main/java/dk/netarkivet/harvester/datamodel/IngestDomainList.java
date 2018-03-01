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
package dk.netarkivet.harvester.datamodel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.servlet.jsp.JspWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.DomainUtils;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.StringUtils;
import dk.netarkivet.harvester.Constants;

/**
 * Utility class for ingesting new domains into the database.
 */
public class IngestDomainList {

    /** The logger. */
    protected static final Logger log = LoggerFactory.getLogger(IngestDomainList.class);

    /** I18n bundle used by this class. */
    private static final I18n I18N = new dk.netarkivet.common.utils.I18n(Constants.TRANSLATIONS_BUNDLE);

    /** How often to log progress. */
    private static final int PRINT_INTERVAL = 10000;

    /** Connection to the persistent store of Domains. */
    private DomainDAO dao;

    /**
     * Constructor for the IngestDomainList class. It makes a connection to the Domains store.
     */
    public IngestDomainList() {
        dao = DomainDAO.getInstance();
    }

    /**
     * Adds all new domains from a newline-separated file of domain names. The file is assumed to be in the UTF-8
     * format. For large files, a line is printed to the log, and to the out variable (if not set to null), every
     * PRINT_INTERVAL lines.
     *
     * @param domainList the file containing the domain names.
     * @param out a stream to which output can be sent. May be null.
     * @param theLocale the given Locale
     */
    public void updateDomainInfo(File domainList, JspWriter out, Locale theLocale) {
        ArgumentNotValid.checkNotNull(domainList, "File domainList");
        ArgumentNotValid.checkNotNull(theLocale, "Locale theLocale");
        Domain myDomain;
        String domainName;
        BufferedReader in = null;
        int countDomains = 0;
        List<String> invalidDomains = new ArrayList<String>();
        int countCreatedDomains = 0;
        boolean print = (out != null);
        try {
            in = new BufferedReader(new InputStreamReader(new FileInputStream(domainList), "UTF-8"));

            while ((domainName = in.readLine()) != null) {
                domainName = domainName.trim();
                if (domainName.isEmpty()) {
                	continue; // Skip empty lines
                }
                try {
                    countDomains++;
                    if ((countDomains % PRINT_INTERVAL) == 0) {
                        Date d = new Date();
                        String msg = "Domain #" + countDomains + ": " + domainName + " added at " + d;
                        log.info(msg);
                        if (print) {
                            out.print(I18N.getString(theLocale, "domain.number.0.1.added.at.2", countDomains,
                                    domainName, d));
                            out.print("<br/>");
                            out.flush();
                        }
                    }

                    if (DomainUtils.isValidDomainName(domainName)) {
                        if (!dao.exists(domainName)) {
                            myDomain = Domain.getDefaultDomain(domainName);
                            dao.create(myDomain);
                            countCreatedDomains++;
                        }
                    } else {
                        log.debug("domain '{}' is not a valid domain Name", domainName);
                        invalidDomains.add(domainName);
                        if (print) {
                            out.print(I18N.getString(theLocale, "errormsg;domain.0.is.not.a.valid" + ".domainname",
                                    domainName));
                            out.print("<br/>");
                            out.flush();
                        }
                    }
                } catch (Exception e) {
                    log.debug("Could not create domain '{}'", domainName, e);
                    if (print) {
                        out.print(I18N.getString(theLocale, "errormsg;unable.to.create" + ".domain.0.due.to.error.1",
                                domainName, e.getMessage()));
                        out.print("<br/>\n");
                        out.flush();
                    }
                }
            }
            log.info("Looked at {} domains, created {} new domains and found {} invalid domains", countDomains, countCreatedDomains, invalidDomains.size());
            if (!invalidDomains.isEmpty()) {
                log.warn("Found the following {} invalid domains during ingest", invalidDomains.size(), StringUtils.conjoin(",", invalidDomains));
            }
        } catch (FileNotFoundException e) {
            String msg = "File '" + domainList.getAbsolutePath() + "' not found";
            log.debug(msg);
            throw new IOFailure(msg, e);
        } catch (IOException e) {
            String msg = " Can't read the domain-file '" + domainList.getAbsolutePath() + "'.";
            log.debug(msg);
            throw new IOFailure(msg, e);
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
