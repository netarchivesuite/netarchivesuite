/* File:    $Id: ParameterMap.java 2716 2013-07-10 17:15:34Z mss $
 * Version: $Revision: 2716 $
 * Date:    $Date: 2013-07-10 19:15:34 +0200 (Wed, 10 Jul 2013) $
 * Author:  $Author: mss $
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
package dk.netarkivet.harvester.dao;

import java.util.HashMap;

/**
 * @author ngiraud
 *
 */
@SuppressWarnings("serial")
public final class ParameterMap extends HashMap<String, Object> {
	
	/**
	 * Final constant denoting the empty parameter map
	 */
	public static final ParameterMap EMPTY = new ParameterMap(); 
	
	/**
	 * Default empty constructor.
	 */
	public ParameterMap() {
		super();
	}
	
	/**
	 * Builds a map from an array containing key-value arrays. 
	 * This method is intended to enhance code readability when building
	 * parameter maps to feed the template.
	 * @param baseMap a base map that will be copied to the new one. 
	 * @param pairs the parameters in the form [key1, value1, ... , keyN, valueN], overwrites any 
	 * key defined in baseMap  
	 * @return the equivalent map
	 */
	public ParameterMap(
			final ParameterMap baseMap, 
			final Object... pairs) {
		super();
		putAll(baseMap);
		putAll(pairs);
	}
	
	/**
	 * Builds a map from an array containing key-value arrays. 
	 * This method is intended to enhance code readability when building
	 * parameter maps to feed the template. 
	 * @param pairs the parameters in the form [key1, value1, ... , keyN, valueN] 
	 * @return the equivalent map
	 */
	public ParameterMap(final Object... pairs) {
		super();
		putAll(pairs);
	}
	
	/**
	 * Builds a singleton map.
	 * @param key the key
	 * @param value the value
	 */
	public ParameterMap(String key, Object value) {
		super();
		put(key, value);
	}
	
	public void putAll(final Object... pairs) {
		for (int i = 0; i < pairs.length - 1; i+=2) {
			this.put((String) pairs[i], pairs[i + 1]);
		}
	}

}
