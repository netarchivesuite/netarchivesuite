/* File:        $Id: WaybackIndexerApplication.java 1407 2010-05-25 08:32:42Z csrster $
 * Revision:    $Revision: 1407 $
 * Author:      $Author: csrster $
 * Date:        $Date: 2010-05-25 10:32:42 +0200 (Tue, 25 May 2010) $
 *
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package dk.netarkivet.wayback.aggregator;

import dk.netarkivet.common.utils.ApplicationUtils;

/**
 * This wrapper class is used to start the {@link AggregationWorker} inside Jetty.
 */
public class AggregatorApplication {

    /**
     * Runs the <code>IndexAggregator</code>. Settings are read from config files so the
     * arguments array should be empty.
     * @param args an empty array.
     */
    public static void main(String[] args) {
        ApplicationUtils.startApp(AggregationWorker.class, args);
    }

}