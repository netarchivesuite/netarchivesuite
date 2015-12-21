/*
 * #%L
 * Netarchivesuite - harvester
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
package dk.netarkivet.harvester.harvesting;

import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.utils.SettingsFactory;

/**
 * Factory class for instantiating a specific implementation of {@link HeritrixLauncher}. The implementation class is
 * defined by the setting <em>dk.netarkivet.harvester.harvesting.heritrixLauncher.class</em>.
 */
public class HeritrixLauncherFactory extends SettingsFactory<HeritrixLauncher> {

    /**
     * Returns an instance of the default {@link HeritrixLauncher} implementation defined by the setting
     * dk.netarkivet.harvester.harvesting.heritrixLauncher.class . This class must have a constructor or factory method
     * with a signature matching the array args.
     *
     * @param args the arguments to the constructor or factory method
     * @return the {@link HeritrixLauncher} instance.
     * @throws ArgumentNotValid if the instance cannot be constructed.
     */
    public static HeritrixLauncher getInstance(Object... args) throws ArgumentNotValid {
        return SettingsFactory.getInstance(Heritrix1Settings.HERITRIX_LAUNCHER_CLASS, args);
    }

}
