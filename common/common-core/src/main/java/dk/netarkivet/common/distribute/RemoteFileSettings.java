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
package dk.netarkivet.common.distribute;

import java.io.Serializable;

/**
 * Container for the RemoteFile settings used by one app, so they can be used by another app.
 */
@SuppressWarnings({"serial"})
public class RemoteFileSettings implements Serializable {

    /** server host name. */
    private String serverName;

    /** The server port. */
    private int serverPort;

    /** The username used to connect to the server. */
    private String userName;

    /** The password used to connect to the server. */
    private String userPassword;

    /**
     * Constructor.
     *
     * @param serverName The hostname of the server.
     * @param serverPort The port name of the server.
     * @param userName The username used for connecting.
     * @param userPassword The password used for connecting.
     */
    public RemoteFileSettings(String serverName, int serverPort, String userName, String userPassword) {
        this.serverName = serverName;
        this.serverPort = serverPort;
        this.userName = userName;
        this.userPassword = userPassword;
    }

    /**
     * @return servername
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * @return server port.
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * @return user name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @return user password
     */
    public String getUserPassword() {
        return userPassword;
    }

}
