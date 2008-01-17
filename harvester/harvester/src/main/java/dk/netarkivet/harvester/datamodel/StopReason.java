/*File:    $Id$
* Version: $Revision$
* Author:  $Author$
* Date:    $Date$
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

import java.util.Locale;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.I18n;

/**
 * Class for containing a reason for stopping the harvesting of a domain.
 * There are five possible reasons:
 *  1) We have now harvested the whole domain (DOWNLOAD_COMPLETE)
 *  2)  We have now harvested the number of objects allowed from this domain
 *      in this iteration (OBJECT_LIMIT)
 *  3) We have now harvested the the number of bytes allowed from this domain
 *      in this iteration (SIZE_LIMIT)
 *  4) We stopped harvesting because we hit the per-configuration limit
 *      (CONFIG_SIZE_LIMIT)
 *  5) We don't know whether or not the harvesting is completed,
 *     because the crawler did not finish in an orderly way (DOWNLOAD_UNFINISHED)
 *
 * Note: This enum is serialized to database using the order in which these
 * are defined. Thus the order of stop reasons MUST NOT BE CHANGED!
 *
 *
 */
public enum StopReason {
    /** We have now harvested the whole domain. */
    DOWNLOAD_COMPLETE,
    /**
     * We have now harvested the number of objects allowed from this domain
     *  in this harvest.
     */
    OBJECT_LIMIT,
    /**
     * We have now harvested the the number of bytes allowed from this domain
     *  in this harvest.
     */
    SIZE_LIMIT,
    /**
     *  We stopped harvesting because we hit the per-configuration limit.
     */
    CONFIG_SIZE_LIMIT,
    /** We don't know whether or not the harvesting is completed,
     *  because the crawler did not finish in an orderly way.
     */
    DOWNLOAD_UNFINISHED;

    /** Internationalisation object. */
    private static final I18n I18N
          = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
    /**
     * Get the StopReason corresponding to the given positive integer.
     * @param stopreasonNum a given positive integer
     * @return the StopReason corresponding to the given positive integer
     */
    static StopReason getStopReason(final int stopreasonNum) {
        switch (stopreasonNum) {
            case 0:
                return DOWNLOAD_COMPLETE;
            case 1:
                return OBJECT_LIMIT;
            case 2:
                return SIZE_LIMIT;
            case 3:
                return CONFIG_SIZE_LIMIT;
            case 4:
                return DOWNLOAD_UNFINISHED;
            default:
                throw new UnknownID("No stop reason assigned to "
                                    + stopreasonNum);
        }
    }
    
   /**
    * Return a localized string describing a stopreason.
    * @param l the locale
    * @return a localized string describing a stopreason.
    */
   public String getLocalizedString(Locale l) {
       ArgumentNotValid.checkNotNull(l, "l");
       switch (this) {
           case DOWNLOAD_COMPLETE :
               return I18N.getString(l, "stopreason.complete");
           case OBJECT_LIMIT :
               return I18N.getString(l, "stopreason.max.objects.limit.reached");
           case SIZE_LIMIT :
               return I18N.getString(l, "stopreason.max.bytes.limit.reached");
           case CONFIG_SIZE_LIMIT :
               return I18N.getString(l, "stopreason.max.domainconfig.limit.reached");
           case DOWNLOAD_UNFINISHED:
               return I18N.getString(l, "stopreason.download.unfinished");
       }
       return I18N.getString(l, "stopreason.unknown.0", this);
   }
}
