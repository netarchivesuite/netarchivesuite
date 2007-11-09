/* $Id$
 * $Revision$
 * $Date$
 * $Author$
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
package dk.netarkivet.deploy;

import java.io.File;

/** The application that is run to generate install and start/stop scripts
 * for all other applications.
 */
public class DeployApplication {
    /** Run the application.
     *
     * @param args Command-line arguments:
     *
     * First arg is the it-conf.xml file to base the installation on.
     * Second arg is the settings.xml file to use as base.
     * Third arg is the name of the environment this will be used in,
     * e.g. test or prod.
     * Fourth (optional) arg is the directory to output files to.  If not given,
     * files will be placed in a subdirectory of the current directory named the
     * same as the envorinment arg.
     */
    public static void main(String[] args) {

        if (args.length < 3) {
            System.out
                    .println("Usage: DeployApplication it-config-xml "
                            + "settings-xml environment [outputdir]");
            System.out.println("output dir defaults to ./environmentName");
            System.out
                    .println("Example: DeployApplication "
                            + "./conf/it_conf_test.xml "
                            + "./conf/settings.xml SSC");
            System.exit(0);
        }
        String fname = args[0];
        String fnNameSettings = args[1];
        String environmentName = args[2];
        String outputdir = "./" + environmentName + "/";
        if (args.length == 4) {
            outputdir = args[3];
        }

        ItConfiguration itConfig = new ItConfiguration(new File(fname));
        itConfig.setEnvironment(environmentName);
        itConfig.calculateDefaultSettings(new File(fnNameSettings)
                .getParentFile());
        itConfig.loadDefaultSettings(new File(fnNameSettings), environmentName);
        File subdir = new File(outputdir);
        itConfig.writeSettings(subdir);
        for (String loc : itConfig.locations) {
            itConfig.writeInstallAllSSH(new File(subdir, "install_" + loc + ".sh"),
                                        new File(subdir, "startall_" + loc + ".sh"),
                                        new File(subdir, "killall_" + loc + ".sh"),
                                        loc);
        }
//        System.out.println(itConfig);

    }

}
