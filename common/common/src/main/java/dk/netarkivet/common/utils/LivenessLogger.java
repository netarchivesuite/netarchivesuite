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
package dk.netarkivet.common.utils;

import java.text.SimpleDateFormat;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.exceptions.ArgumentNotValid;

/**
 * This class logs lifesigns at level FINE every 2 minutes.
 * This is implemented by using the Timer class.
 * @see java.util.Timer#schedule(java.util.TimerTask, long, long)
 * This class is instantiated by ApplicationUtils.startApp().
 */
public class LivenessLogger extends TimerTask {
   /** The predefined frequency in milliseconds for this LivenessLogger. */
    public static final long LIFESIGNS_FREQUENCY = 600000L; // = 10 minutes
    /** The class for which this LivenessLogger acts. */
    private Class theClass;
    /** The logger for this instance of the LivenessLogger. */
    private Log log = LogFactory.getLog(getClass());
    /** Dateformat for the liveness-logging. */
    private static final SimpleDateFormat LIFESIGNS_TIMEFORMAT =
        new SimpleDateFormat("HH:mm:ss z (dd.MM.yyyy)");

    /** The constructor for the LivenessLogger class.
     * @param theClass the class to log for.
     */
    public LivenessLogger(Class theClass) {
        ArgumentNotValid.checkNotNull(theClass, "Class theClass");
        this.theClass = theClass;
        Timer theTimer = new Timer();
        theTimer.schedule(this, 0L, LIFESIGNS_FREQUENCY);
    }

    /**
     * This method writes a lifesign-logmessage to the log.
     * It is performed the first time immediately after instantiation,
     * and then every 2 minutes.
     */
    public void run() {
        log.debug(theClass.getName()
                  + " is alive at " + LIFESIGNS_TIMEFORMAT.format(
                        System.currentTimeMillis()));
    }

}
