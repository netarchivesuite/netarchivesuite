/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
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

package dk.netarkivet.common.utils;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Utilities for sending an email.
 *
 */
public class EMailUtils {
    private static final String MAIL_FROM_PROPERTY_KEY = "mail.from";
    private static final String MAIL_HOST_PROPERTY_KEY = "mail.host";
    private static final String MIMETYPE = "text/plain";
    private static Log log = LogFactory.getLog(EMailUtils.class);

    /**
     * Send an email, throwing exceptions on errors.
     *
     * @param to The recipient of the email. Separate multiple recipients with
     *           commas.
     * @param from The sender of the email.
     * @param subject The subject of the email.
     * @param body The body of the email.
     *
     * @throws ArgumentNotValid If either parameter is null, if to, from or
     *                          subject is the empty string, if to or from
     *                          does not contain valid email adresses.
     * @throws IOFailure If the message cannot be sent for some reason.
     */
    public static void sendEmail(String to, String from, String subject,
                                 String body) {
        sendEmail(to, from, subject, body, false);
    }

    /**
     * Send an email, possibly forgiving errors.
     *
     * @param to The recipient of the email. Separate multiple recipients with
     *           commas. Supports only adresses of the type 'john@doe.dk', not
     *           'John Doe <john@doe.dk>'
     * @param from The sender of the email.
     * @param subject The subject of the email.
     * @param body The body of the email.
     * @param forgive On true, will send the email even on invalid email
     *        addresses, if at least one recipient can be set, on false, will
     *        throw exceptions on any invalid email address.
     *
     *
     * @throws ArgumentNotValid If either parameter is null, if to, from or
     *                          subject is the empty string, or no recipient
     *                          can be set. If "forgive" is false, also on
     *                          any invalid to or from address.
     * @throws IOFailure If the message cannot be sent for some reason.
     */
    public static void sendEmail(String to, String from, String subject,
                                 String body, boolean forgive) {
        ArgumentNotValid.checkNotNullOrEmpty(to, "String to");
        ArgumentNotValid.checkNotNullOrEmpty(from, "String from");
        ArgumentNotValid.checkNotNullOrEmpty(subject, "String subject");
        ArgumentNotValid.checkNotNull(body, "String body");

        Properties props = new Properties();
        props.put(MAIL_FROM_PROPERTY_KEY, from);
        props.put(MAIL_HOST_PROPERTY_KEY,
                  Settings.get(CommonSettings.MAIL_SERVER));

        Session session = Session.getDefaultInstance(props);
        Message msg = new MimeMessage(session);

        // to might contain more than one e-mail address
        for (String toAddressS : to.split(",")) {
            try {
                InternetAddress toAddress
                        = new InternetAddress(toAddressS.trim());
                msg.addRecipient(Message.RecipientType.TO, toAddress);
            } catch (AddressException e) {
                if (forgive) {
                    log.warn("To address '" + toAddressS
                                               + "' is not a valid email "
                                               + "address", e);
                } else {
                    throw new ArgumentNotValid("To address '" + toAddressS
                                               + "' is not a valid email "
                                               + "address", e);
                }
            } catch (MessagingException e) {
                if (forgive) {
                    log.warn("To address '" + toAddressS
                                           + "' could not be set in email", e);
                } else {
                    throw new ArgumentNotValid("To address '" + toAddressS
                                           + "' could not be set in email", e);
                }
            }
        }
        try {
            if (msg.getAllRecipients().length == 0) {
                throw new ArgumentNotValid("No valid recipients in '" + to
                                           + "'");
            }
        } catch (MessagingException e) {
            throw new ArgumentNotValid("Message invalid after setting"
                                       + " recipients", e);
        }

        try {
            InternetAddress fromAddress = null;
            fromAddress = new InternetAddress(from);
            msg.setFrom(fromAddress);
        } catch (AddressException e) {
            throw new ArgumentNotValid("From address '" + from
                                       + "' is not a valid email "
                                       + "address", e);
        } catch (MessagingException e) {
            if (forgive) {
                log.warn("From address '" + from
                         + "' could not be set in email", e);
            } else {
                throw new ArgumentNotValid("From address '" + from
                                           + "' could not be set in email", e);
            }
        }

        try {
            msg.setSubject(subject);
            msg.setContent(body, MIMETYPE);
            msg.setSentDate(new Date());
            Transport.send(msg);
        } catch (MessagingException e) {
            throw new IOFailure("Could not send email with subject '" + subject
                                +  "' from '" + from + "' to '" + to
                                + "'. Body:\n" + body, e);
        }
    }
}
