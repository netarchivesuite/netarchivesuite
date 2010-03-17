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

package dk.netarkivet.common.distribute.arcrepository;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.SettingsFactory;

/**
 * A factory for ArcRepositoryClients.
 *
 * Implementation note: This implementation assumes that only one actual
 * implementation class exists, pointed out by the setting
 * settings.common.arcrepositoryClient.class, and merely gives three different
 * view on that class.
 *
 */

public class ArcRepositoryClientFactory
        extends SettingsFactory<ArcRepositoryClient> {
    /** Returns a new ArcRepositoryClient suitable for use by a harvester.
     *
     * @return An ArcRepositoryClient that implements the methods defined by
     * HarvesterArcRepositoryClient.  At end of use, close() should be called
     * on this to release any resources claimed.
     */
    public static HarvesterArcRepositoryClient getHarvesterInstance() {
        return SettingsFactory.getInstance(CommonSettings.ARC_REPOSITORY_CLIENT);
    }

    /** Returns a new ArcRepositoryClient suitable for use by a viewer.
     *
     * @return An ArcRepositoryClient that implements the methods defined by
     * ViewerArcRepositoryClient.  At end of use, close() should be called
     * on this to release any resources claimed.
     */
    public static ViewerArcRepositoryClient getViewerInstance() {
        return SettingsFactory.getInstance(CommonSettings.ARC_REPOSITORY_CLIENT);
    }

    /** Returns a new ArcRepositoryClient suitable for use in bit preservation.
     *
     * @return An ArcRepositoryClient that implements the methods defined by
     * PreservationArcRepositoryClient. At end of use, close() should be
     * called on this to release any resources claimed.
     */
    public static PreservationArcRepositoryClient getPreservationInstance() {
        return SettingsFactory.getInstance(CommonSettings.ARC_REPOSITORY_CLIENT);
    }
}
