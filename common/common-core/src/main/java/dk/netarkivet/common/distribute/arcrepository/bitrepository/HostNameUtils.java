/*
 * #%L
 * Netarchivesuite - common
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
package dk.netarkivet.common.distribute.arcrepository.bitrepository;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provide the hostname of the machine on which the program is running.
 */
public class HostNameUtils {
	/** Logging mechanism. */
	private static Logger logger = LoggerFactory.getLogger(HostNameUtils.class.getName());

	/**
	 * disallow construction by making this private.
	 */
	private HostNameUtils() {
	}

	/**
	 * @return the hostname of the machine as a {@link String}
	 */
	public static String getHostName() {
		try {
			//Trying to get hostname through InetAddress
			final InetAddress iAddress = InetAddress.getLocalHost();
			String hostName = iAddress.getHostName();

			//Trying to do better and get Canonical hostname
			final String canonicalHostName = iAddress.getCanonicalHostName();

			if (StringUtils.isNotEmpty(canonicalHostName)) {
				logger.info("Local hostname (provided  by getCanonicalHostName): " + canonicalHostName);
				return canonicalHostName;
			} else if (StringUtils.isNotEmpty(hostName)) {
				logger.info("Local hostname (provided  by iAddress): " + hostName);
				return hostName;
			}

		} catch (UnknownHostException  e) {
			logger.info("Failed finding hostname the standard Java way, returning: localhost", e);
		}
		return "localhost";
	}
}
