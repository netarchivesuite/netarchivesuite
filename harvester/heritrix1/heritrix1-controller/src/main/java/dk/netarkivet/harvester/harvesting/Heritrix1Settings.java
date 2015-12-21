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

import dk.netarkivet.common.utils.Settings;

/**
 * Settings specific to the heritrix1 harvester module of NetarchiveSuite.
 */
public class Heritrix1Settings {



    /** The default place in classpath where the settings file can be found. */
    private static final String DEFAULT_SETTINGS_CLASSPATH = "dk/netarkivet/harvester/heritrix1/settings.xml";

    /*
     * The static initialiser is called when the class is loaded. It will add default values for all settings defined in
     * this class, by loading them from a settings.xml file in classpath.
     */
    static {
        Settings.addDefaultClasspathSettings(DEFAULT_SETTINGS_CLASSPATH);
    }

    // NOTE: The constants defining setting names below are left non-final on
    // purpose! Otherwise, the static initialiser that loads default values
    // will not run.

    
  /**
   * <b>settings.harvester.harvesting.heritrixControllerClass</b>:<br/>
   * The implementation of the HeritrixController interface to be used.
   */
  public static String HERITRIX_CONTROLLER_CLASS = "settings.harvester.harvesting.heritrixController.class";

  /**
   * <b>settings.harvester.harvesting.heritrixLauncherClass</b>:<br/>
   * The implementation of the HeritrixLauncher abstract class to be used.
   */
  public static String HERITRIX_LAUNCHER_CLASS = "settings.harvester.harvesting.heritrixLauncher.class";


  
}
