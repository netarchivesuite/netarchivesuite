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
package dk.netarkivet.common.tools;

import dk.netarkivet.common.distribute.JMSConnectionFactory;

/**
 * Used to check if firewall ports are open and if the JMS broker is up and responding.
 */
public class JMSBroker {

    /**
     * Initializes a JMSConnection. If everything is fine it prints "success" to the console.
     *
     * @param args Takes no arguments
     */
    public static void main(final String[] args) {
        JMSConnectionFactory.getInstance().cleanup();
        System.out.println("success");
        Runtime.getRuntime().exit(0);
    }
}
