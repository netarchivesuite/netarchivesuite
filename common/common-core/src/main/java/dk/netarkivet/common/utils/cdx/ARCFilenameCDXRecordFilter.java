/*
 * #%L
 * Netarchivesuite - common
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

package dk.netarkivet.common.utils.cdx;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * A filter to use in CDXReader when finding CDXRecords matching a filename-pattern.
 */
@SuppressWarnings({"serial"})
public class ARCFilenameCDXRecordFilter extends SimpleCDXRecordFilter {

    private String arcfilenamepattern;
    private Pattern p;

    /**
     * Class constructor.
     *
     * @param arcfilenamepattern The filename pattern to be used by this filter
     * @param filtername The name of this filter
     * @throws ArgumentNotValid If any argument are null or an empty string.
     */
    public ARCFilenameCDXRecordFilter(String arcfilenamepattern, String filtername) throws ArgumentNotValid {
        super(filtername);
        ArgumentNotValid.checkNotNullOrEmpty(arcfilenamepattern, "arcfilenamepattern");
        this.arcfilenamepattern = arcfilenamepattern;
        this.p = Pattern.compile(arcfilenamepattern);
    }

    /**
     * Get the filename pattern used by this filter.
     *
     * @return the filename pattern used by this filter.
     */
    public String getFilenamePattern() {
        return this.arcfilenamepattern;
    }

    /*
     * (non-Javadoc)
     * 
     * @see dk.netarkivet.common.utils.cdx.SimpleCDXRecordFilter#process( dk.netarkivet.common.utils.cdx.CDXRecord)
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
