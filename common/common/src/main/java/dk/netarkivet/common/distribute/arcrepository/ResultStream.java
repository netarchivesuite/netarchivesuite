/* File:                $Id$
 * Revision:            $Revision$
 * Author:              $Author$
 * Date:                $Date$
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
package dk.netarkivet.common.distribute.arcrepository;

import java.io.InputStream;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/** Simple helper class to store the fact, whether we have a stream which 
 * contains a header or a stream, which does not.
 */
public class ResultStream {
	
	private final InputStream resultstream;
	
	private final boolean containsHeader;
	
	/**
	 * Create a ResultStream with the given inputStream and information
	 * of whether or not the inputStream contains a header.
	 * @param resultStream an inputStream w/ the data for a stored URI
	 * @param containsHeader true, if the stream contains a header, otherwise
	 * false
	 */
	public ResultStream(InputStream resultStream, boolean containsHeader) {
		ArgumentNotValid.checkNotNull(resultStream, "InputStream resultStream");
		this.resultstream = resultStream;
		this.containsHeader = containsHeader;
	}
	
	/**
	 * 
	 * @return the resultStream
	 */
	public InputStream getResultStream(){
		return this.resultstream;
	}
	
	/**
	 * 
	 * @return true, if the resultStream contains a header; otherwise false.
	 */
	public boolean containsHeader(){
		return this.containsHeader;
	}
	
}
