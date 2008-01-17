/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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
package dk.netarkivet.harvester.datamodel;

import java.util.Date;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Class encapsulating domain alias information.
 * The information is used to prevent harvesting the domains which
 * are aliases of other domains.
 *
 */
public class AliasInfo {

    /** the domain. */
    final private String domain;
    /** the domain which this domain is an alias of. */
    final private String aliasOf;
    /** the domain was (re)registered as an alias on this date. */
    final private Date lastChange;

    /**
     * Constructor for the AliasInfo class.
     *
     * @param domain a given domain
     * @param aliasOf the given domain is an alias of this domain
     * @param lastChange the alias was (re-)registered on this date.
     * @throws dk.netarkivet.common.exceptions.ArgumentNotValid in the following cases:
     *     1. domain is null or empty
     *     2. aliasOf is null or empty
     *     3. lastChange is null
     *     4. domain equals aliasOf
     */
    public AliasInfo(String domain, String aliasOf, Date lastChange)
    throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(domain, "domain");
        ArgumentNotValid.checkNotNullOrEmpty(aliasOf, "aliasOf");
        ArgumentNotValid.checkNotNull(lastChange, "lastChange");
        if (domain.equals(aliasOf)) {
            throw new
                ArgumentNotValid(
                        "the aliasOf argument must not be equal to the domain");
        }
        this.domain = domain;
        this.aliasOf = aliasOf;
        this.lastChange = (Date) lastChange.clone();
    }

    /**
     * @return Returns the aliasOf.
     */
    public String getAliasOf() {
        return aliasOf;
    }

    /**
     * @return Returns the domain.
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @return Returns the lastChange.
     */
    public Date getLastChange() {
        return (Date) lastChange.clone();
    }


    /**
     * Is this alias expired?
     * This method depends upon the Constant: dk.netarkivet.harvester.webinterface.Constants.ALIAS_TIMEOUT_IN_MILLISECONDS
     * @return true, if alias is expired
     */
    public boolean isExpired() {
        Date aliasTimeoutDate = getExpirationDate();
        Date now = new Date();
        return aliasTimeoutDate.before(now);
    }

    /** The date when this alias will expire (or has expired)
     *
     * @return The expiration date for this alias.  May be in the past or
     * in the future.
     */
    public Date getExpirationDate() {
        Date aliasTimeoutDate = new Date (this.lastChange.getTime() +
                Constants.ALIAS_TIMEOUT_IN_MILLISECONDS);
        return aliasTimeoutDate;
    }

    /**
     * @see java.lang.Object#toString
     * @return String representation of this AliasInfo object.
     */
    public String toString() {
        return "Domain '" + getDomain() + "' is an alias of '" + getAliasOf()
               + "', last updated " + getLastChange();
    }
}
