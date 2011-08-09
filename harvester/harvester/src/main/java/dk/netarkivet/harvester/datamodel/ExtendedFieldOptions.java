/* File:        $Id$Id$
 * Revision:    $Revision$Revision$
 * Author:      $Author$Author$
 * Date:        $Date$Date$
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;

public class ExtendedFieldOptions {
	public static final String KEYVALUESEPARATOR = "=";
	public static final String NEWLINE = System.getProperty("line.separator");
	
	String lines;
	boolean valid = false;
	LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
	
	public ExtendedFieldOptions(String aLines) {
		lines = aLines;
		
		parsing();
	}
	
	private void parsing() {
		if (lines == null) {
			return;
		}
		
		StringTokenizer st = new StringTokenizer(lines.trim(), System.getProperty("line.separator"));
		
		String key = null;
		String value = null;
		
		while(st.hasMoreElements()) {
			String line = st.nextToken();
			StringTokenizer st2 = new StringTokenizer(line, KEYVALUESEPARATOR);
			
			try {
				key = st2.nextToken();
				value = st2.nextToken();
				if (key.length() == 0 || value.length() == 0) {
					continue;
				}
				options.put(key, value);
			}
			catch (NoSuchElementException e) {
				// invalid line, ignoring
				continue;
			}
		}
		
		if (!options.isEmpty()) {
			valid = true;
		}
	}
	
	public boolean isValid() {
		return valid;
	}
	
	public HashMap<String, String> getOptions() {
		return options;
	}
	
	public String getOptionsString() {
		String str = "";
		
		if (isValid()) {
			Set<String> set = options.keySet();
			
			Iterator<String> it = set.iterator();
			while(it.hasNext()) {
				String key = it.next();
				str += key + KEYVALUESEPARATOR + options.get(key) + NEWLINE;
			}
		}
		
		
		return str;
	}
	
	public boolean isKeyValid(String aKey) {
		if (isValid()) {
			Set<String> set = options.keySet();
			
			Iterator<String> it = set.iterator();
			while(it.hasNext()) {
				String key = it.next();
				if (key.equals(aKey)) {
					return true;
				}
			}
		}
		return false;
	}
}
