/* File:                $Id$
 * Revision:            $Revision$
 * Author:              $Author$
 * Date:                $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.viewerproxy;

import java.net.URI;
import java.util.Observable;
import java.util.Observer;

/**
 * Super class for all URIObservers - calls the URIObserver notify method on
 * all notifications of a URI and its response code.
 *
 */
public abstract class URIObserver implements Observer {
    /**
     * This notify method is called on every notification of URI and response
     * code.
     *
     * @param uri The uri notified about
     * @param responseCode The response code of this uri.
     */
    public abstract void notify(URI uri, int responseCode);

    /** Helper class to be able to notify about a pair of <uri,responsecode> */
    static final class URIResponseCodePair {
        /** The uri */
        private final URI uri;
        /** The response code */
        private final int responseCode;

        /** initialise values
         *
         * @param uri The URI
         * @param code The code
         */
        public URIResponseCodePair(URI uri, int code) {
            this.uri = uri;
            this.responseCode = code;
        }
    }

    /** Will call the abstract notify method if arg is an URIResponseCodePair
     * value.
     *
     * @param o The observable which called this method. Ignored.
     * @param arg The argument. If Response instance, notify is called.
     * Otherwise ignored.
     */
    public final void update(Observable o, Object arg) {
        if (arg != null && arg instanceof URIResponseCodePair) {
            URIResponseCodePair URIResponseCodePair = (URIResponseCodePair) arg;
            notify(URIResponseCodePair.uri, URIResponseCodePair.responseCode);
        }
    }
}