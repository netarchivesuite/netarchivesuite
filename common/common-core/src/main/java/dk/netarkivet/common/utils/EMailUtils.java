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

package dk.netarkivet.common.utils;

import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.IOFailure;

/**
 * Utilities for sending an email.
 */
public final class EMailUtils {

    /** The class logger. */
    private static final Logger log = LoggerFactory.getLogger(EMailUtils.class);

    /** name of property defining the sender of the mail. */
    private static final String MAIL_FROM_PROPERTY_KEY = "mail.from";
    /** name of property defining the host sending the mail. */
    private static final String MAIL_HOST_PROPERTY_KEY = "mail.host";
    /** The mimetype used in the mail. */
    private static final String MIMETYPE = "text/plain";

    /** private constructor to avoid initialization. */
    private EMailUtils() {
    }

    /**
     * Send an email, throwing exceptions on errors.
     *
     * @param to The recipient of the email. Separate multiple recipients with commas.
     * @param from The sender of the email.
     * @param subject The subject of the email.
     * @param body The body of the email.
     * @throws ArgumentNotValid If either parameter is null, if to, from or subject is the empty string, if to or from
     * does not contain valid email adresses.
     * @throws IOFailure If the message cannot be sent for some reason.
     */
    public static void sendEmail(String to, String from, String subject, String body) {
        sendEmail(to, from, subject, body, false);
    }

    /**
     * Send an email, possibly forgiving errors.
     *
     * @param to The recipient of the email. Separate multiple recipients with commas. Supports only adresses of the
     * type 'john@doe.dk', not 'John Doe <john@doe.dk>'
     * @param from The sender of the email.
     * @param subject The subject of the email.
     * @param body The body of the email.
     * @param forgive On true, will send the email even on invalid email addresses, if at least one recipient can be
     * set, on false, will throw exceptions on any invalid email address.
     * @throws ArgumentNotValid If either parameter is null, if to, from or subject is the empty string, or no recipient
     * can be set. If "forgive" is false, also on any invalid to or from address.
     * @throws IOFailure If the message cannot be sent for some reason.
     */
    public static void sendEmail(String to, String from, String subject, String body, boolean forgive) {
        ArgumentNotValid.checkNotNullOrEmpty(to, "String to");
        ArgumentNotValid.checkNotNullOrEmpty(from, "String from");
        ArgumentNotValid.checkNotNullOrEmpty(subject, "String subject");
        ArgumentNotValid.checkNotNull(body, "String body");

        Properties props = new Properties();
        props.put(MAIL_FROM_PROPERTY_KEY, from);
        props.put(MAIL_HOST_PROPERTY_KEY, Settings.get(CommonSettings.MAIL_SERVER));

        Session session = Session.getDefaultInstance(props);
        Message msg = new MimeMessage(session);

        // to might contain more than one e-mail address
        for (String toAddressS : to.split(",")) {
            try {
                InternetAddress toAddress = new InternetAddress(toAddressS.trim());
                msg.addRecipient(Message.RecipientType.TO, toAddress);
            } catch (AddressException e) {
                if (forgive) {
                    log.warn("To address '{}' is not a valid email address", toAddressS, e);
                } else {
                    throw new ArgumentNotValid("To address '" + toAddressS + "' is not a valid email address", e);
                }
            } catch (MessagingException e) {
                if (forgive) {
                    log.warn("To address '{}' could not be set in email", toAddressS, e);
                } else {
                    throw new ArgumentNotValid("To address '" + toAddressS + "' could not be set in email", e);
                }
            }
        }
        try {
            if (msg.getAllRecipients().length == 0) {
                throw new ArgumentNotValid("No valid recipients in '" + to + "'");
            }
        } catch (MessagingException e) {
            throw new ArgumentNotValid("Message invalid after setting recipients", e);
        }

        try {
            InternetAddress fromAddress = null;
            fromAddress = new InternetAddress(from);
            msg.setFrom(fromAddress);
        } catch (AddressException e) {
            throw new ArgumentNotValid("From address '" + from + "' is not a valid email address", e);
        } catch (MessagingException e) {
            if (forgive) {
                log.warn("From address '{}' could not be set in email", from, e);
            } else {
                throw new ArgumentNotValid("From address '" + from + "' could not be set in email", e);
            }
        }

        try {
            msg.setSubject(subject);
            msg.setContent(body, MIMETYPE);
            msg.setSentDate(new Date());
            Transport.send(msg);
        } catch (MessagingException e) {
            throw new IOFailure("Could not send email with subject '" + subject + "' from '" + from + "' to '" + to
                    + "'. Body:\n" + body, e);
        }
    }

}
