/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.common.utils;

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Handles serious errors by sending email.
 */

public class EMailNotifications extends Notifications {
    
    /** The default place in classpath where the settings file can be found. */
    private static String DEFAULT_SETTINGS_CLASSPATH
            = "dk/netarkivet/common/utils/EMailNotificationsSettings.xml";

    /*
     * The static initialiser is called when the class is loaded.
     * It will add default values for all settings defined in this class, by
     * loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(
                DEFAULT_SETTINGS_CLASSPATH
        );
    }

    /** 
     * <b>settings.common.notifications.receiver</b>: <br>
     * The setting for the receiver of email notifications. */
    public static String MAIL_RECEIVER_SETTING
            = "settings.common.notifications.receiver";

    /** 
     * <b>settings.common.notifications.sender</b>: <br>
     * The setting for the sender of email notifications (and receiver of 
     * bounces). */
    public static String MAIL_SENDER_SETTING
            = "settings.common.notifications.sender";
    
    /** The email receiver of the errors. */
    private static final String MAIL_RECEIVER = Settings.get(
            MAIL_RECEIVER_SETTING);
    /** The email sender of the errors. */
    private static final String MAIL_SENDER = Settings.get(
            MAIL_SENDER_SETTING);
    
    /** Subject prefix for notifications by mail. */
    private static String SUBJECT_PREFIX = "NetarchiveSuite ";
    
    /** The error logger we notify about error messages on. */
    private Log log = LogFactory.getLog(getClass());

    /** Sends a notification including an exception.
     *
     * @param message The message to notify about.
     * @param e The exception to notify about.
     */
    public void notify(String message, NotificationType eventType, Throwable e) {
        if (message == null) {
            message = "";
        }
        sendMailNotifications(message, eventType, e);
    }

    /**
     * Send mailNotications. Can be either error or a warning notification. 
     * @param message The message body itself
     * @param e An exception (can be null)
     * @param isError Is this an error or a warning notification
     */
    private void sendMailNotifications(String message, NotificationType eventType, Throwable e){
        String subjectPrefix = SUBJECT_PREFIX + eventType + ": ";
        
        // Subject is a specified string + first line of error message
        String subject =  subjectPrefix + message.split("\n")[0];

        // Body consists of four parts.
        StringBuffer body = new StringBuffer();

        // 1: The host of the message
        body.append("Host: " + SystemUtils.getLocalHostName() + "\n");
        body.append("Date: " + new Date().toString() + "\n");

        // 2: The origin of the message, found by inspecting stack trace
        for (StackTraceElement elm : Thread.currentThread().getStackTrace()) {
            if (!elm.toString().startsWith(getClass().getName())
                && !elm.toString().startsWith(Notifications.class.getName())
                && !elm.toString().startsWith(Thread.class.getName())) {
                body.append(elm.toString() + "\n");
                break;
            }
        }

        // 3: The given message
        body.append(message + "\n");

        // 4: Optionally the exception
        if (e != null) {
            body.append(ExceptionUtils.getStackTrace(e));
        }

        try {
            // Send the mail
            EMailUtils.sendEmail(MAIL_RECEIVER,
                                 MAIL_SENDER,
                                 subject,
                                 body.toString());

            //Log as error
            
            log.error("Mailing " + subjectPrefix + message, e);
        } catch (Exception e1) {
            // On trouble: Log and print it to system out, it's the best we can
            // do!
            
            String msg = "Could not send email on " 
            + eventType.toString().toLowerCase()+ " notification:\n"
                         + body.toString() + "\n";
            System.err.println(msg);
            e1.printStackTrace(System.err);
            log.error(msg, e1);
        }
    }
}
