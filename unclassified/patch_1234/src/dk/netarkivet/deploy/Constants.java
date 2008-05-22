/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.deploy;

/**
 * Shared constants for deploy package.
 *
 */
public class Constants {
    /** The name of the file that JMX reads passwords from */
    static final String JMX_PASSWORD_FILENAME = "jmxremote.password";
    /** The ports used for our JMX connections.  Each <em>application</em>
     * should have one of these
     */
    static final String JMXPORT_PROPERTY = "jmxport";
    /** Property used to specify a Java class. */
    static final String JMS_CLASS_PROPERTY = "class";
    /** Property used to specify the user name used in FTP, if RemoteFile uses
     * FTP */
    static final String FTP_USER_PROPERTY = "user";
    /** Property used to specify the password used in FTP, if RemoteFile uses
     * FTP */
    static final String FTP_PASSWORD_PROPERTY = "pw";
    /** Property used to specify the mail address for mails on errors */
    static final String MAIL_RECEIVER_PROPERTY = "receiver";
    /** Property used to specify the mail address used as the sender on
     * error mails. */
    static final String MAIL_SENDER_PROPERTY = "sender";
    /** Property used to specify the directory used to store files in the
     * bitarchive */
    static final String BITARCHIVE_FILEDIR_PROPERTY = "filedir";
    /** Property used to specify the port used for HTTP file transfer, if
     * RemoteFile uses HTTP. */
    static final String HTTPFILETRANSFERPORT_PROPERTY = "remotefilePort";
    /** Property used to specify the port where the Heritrix gui for a
     * harvester is accessible */
    static final String HERITRIX_GUI_PORT_PROPERTY = "heritrixGuiPort";
    /** Property used to specify the port where the Heritrix JMX for a
     * harvester is accessible */
    static final String HERITRIX_JMX_PORT_PROPERTY = "heritrixJmxPort";
    /** Name of the default security policy file */
    static final String SECURITY_POLICY_FILE_NAME = "security.policy";
}
