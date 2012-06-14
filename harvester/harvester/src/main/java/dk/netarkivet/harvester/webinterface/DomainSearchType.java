/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2012 The Royal Danish Library, the Danish State and
 * University Library, the National Library of France and the Austrian
 * National Library.
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

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * Enumeration of the possible ways to search for domains.
 *
 */
public enum DomainSearchType {

        /** search in the crawlertraps associated with a domain. */
        CRAWLERTRAPS,
        /** search the name associated with a domain. */
        NAME,
        /** search the comments associated with a domain. */
        COMMENTS;
                
        /** Helper method that tries to convert a string to
         * a DomainSearchType.
         *
         * @param type a DomainSearchType as string
         * @return the DomainSearchType related to a string
         * @throws ArgumentNotValid
        */
       public static DomainSearchType parse(String type) {
            for (DomainSearchType s : values()) {
                if (s.name().equalsIgnoreCase(type)) {
                    return s;
                }
            }
            throw new ArgumentNotValid("Invalid Domain Search Type '" + type + "'");
        }
 
        
       /**
        * Return the localized key related to this value.
        *
        * @return The localized key for this value.
        */
       public String getLocalizedKey() {
           switch (this) {
               case NAME:
                   return "domain.search.name";
               case CRAWLERTRAPS:
                   return "domain.search.crawlertraps";
               case COMMENTS:
                   return "domain.search.comments";
               default:
                   throw new ArgumentNotValid("Invalid Domain Search Type '" 
                           + this + "'");
           }
       }
}
