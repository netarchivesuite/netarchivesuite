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

package dk.netarkivet.common.distribute.arcrepository;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.SettingsFactory;

/**
 * A factory for ArcRepositoryClients.
 * <p>
 * Implementation note: This implementation assumes that only one actual implementation class exists, pointed out by the
 * setting settings.common.arcrepositoryClient.class, and merely gives three different view on that class.
 */
public class ArcRepositoryClientFactory extends SettingsFactory<ArcRepositoryClient> {

    /**
     * Returns a new ArcRepositoryClient suitable for use by a harvester.
     *
     * @return An ArcRepositoryClient that implements the methods defined by HarvesterArcRepositoryClient. At end of
     * use, close() should be called on this to release any resources claimed.
     */
    public static HarvesterArcRepositoryClient getHarvesterInstance() {
        return SettingsFactory.getInstance(CommonSettings.ARC_REPOSITORY_CLIENT);
    }

    /**
     * Returns a new ArcRepositoryClient suitable for use by a viewer.
     *
     * @return An ArcRepositoryClient that implements the methods defined by ViewerArcRepositoryClient. At end of use,
     * close() should be called on this to release any resources claimed.
     */
    public static ViewerArcRepositoryClient getViewerInstance() {
        return SettingsFactory.getInstance(CommonSettings.ARC_REPOSITORY_CLIENT);
    }

    /**
     * Returns a new ArcRepositoryClient suitable for use in bit preservation.
     *
     * @return An ArcRepositoryClient that implements the methods defined by PreservationArcRepositoryClient. At end of
     * use, close() should be called on this to release any resources claimed.
     */
    public static PreservationArcRepositoryClient getPreservationInstance() {
        return SettingsFactory.getInstance(CommonSettings.ARC_REPOSITORY_CLIENT);
    }

}
