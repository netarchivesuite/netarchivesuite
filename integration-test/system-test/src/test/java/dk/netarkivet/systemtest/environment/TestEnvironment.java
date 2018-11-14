/*
 * #%L
 * NetarchiveSuite System test
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
package dk.netarkivet.systemtest.environment;

public interface TestEnvironment {
    /**
     * Possibly in a better world these would be values retrieved
     * by method calls rather than constants.
     */
    public static String DEPLOYMENT_SERVER = "kb-prod-udv-001.kb.dk";
    public static String DEPLOYMENT_USER = "devel";
    public static String DEPLOYMENT_HOME = "/home/" + DEPLOYMENT_USER;
    public static String JOB_ADMIN_SERVER = "kb-test-adm-001.kb.dk";
    public static String ARCHIVE_ADMIN_SERVER = "kb-test-adm-001.kb.dk";
    public static String CHECKSUM_SERVER = "kb-test-acs-001.kb.dk";

    /**
     * The NAS environment (settings.common.environmentName) in which
     * the test is to be run.
     */
    public String getTESTX();

    /**
     * The port on the host where the GUI is to be deployed.
     */
    public String getGuiHost();

    /**
     * The port where the GUI is to be deployed.
     */
    public int getGuiPort();

    /**
     * The timestamp of the NAS software version to be run in the test. This does not
     * have to be an actual timestamp. The software in unpacked from the file
     * Netarchivesuite-<timestamp>.zip in the directory release_software_dist/releases
     * on the deployment server.
     */
    public String getTimestamp();

    /**
     * A comma-separated list of addresses to receive mail from failed tests.
     */
    public String getMailreceivers();

    /**
     * The deployment configuration to use.
     */
    public String getDeployConfig();

    /**
     * The full path to the heritrix3 bundler file
     * @return
     */
    public String getH3Zip();

}
