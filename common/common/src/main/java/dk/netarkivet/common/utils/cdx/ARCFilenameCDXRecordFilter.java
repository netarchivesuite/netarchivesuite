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

package dk.netarkivet.common.utils.cdx;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * A filter to use in CDXReader when finding CDXRecords matching a
 * filename-pattern.
 *
 */

public class ARCFilenameCDXRecordFilter extends SimpleCDXRecordFilter {
    private String arcfilenamepattern;
    private Pattern p;

    /**
     * Class constructor.
     * @param arcfilenamepattern The filename pattern to be used by this filter
     * @param filtername The name of this filter
     * @throws ArgumentNotValid If any argument are null or an empty string.
     */
    public ARCFilenameCDXRecordFilter(String arcfilenamepattern,
            String filtername) throws ArgumentNotValid {
        super(filtername);
        ArgumentNotValid.checkNotNullOrEmpty(arcfilenamepattern,
                "arcfilenamepattern");
        this.arcfilenamepattern = arcfilenamepattern;
        this.p = Pattern.compile(arcfilenamepattern);
    }

    /**
     * Get the filename pattern used by this filter.
     * @return the filename pattern used by this filter.
     */
    public String getFilenamePattern() {
         return this.arcfilenamepattern;
    }
    
    /*
     * (non-Javadoc)
     * @see dk.netarkivet.common.utils.cdx.SimpleCDXRecordFilter#process(
     * dk.netarkivet.common.utils.cdx.CDXRecord)
     */
    public boolean process(CDXRecord cdxrec) {
        ArgumentNotValid.checkNotNull(cdxrec, "CDXRecord cdxrec");
        Matcher m = p.matcher(cdxrec.getArcfile());
        if (m.matches()) {
            return true;
        }
        return false;
    }
}
