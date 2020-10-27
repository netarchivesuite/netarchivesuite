/*
 * #%L
 * Netarchivesuite - deploy
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
package dk.netarkivet.deploy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestScripts {

    /**
     * This test method is not a real test, is more a helper for debugging the DeployApplication. 
     * All used resources from src/test/resources are dummy parameters to make it possible to debug 
     */
    @Test
    public void test_template() {
        String[] args = {
        		"-Csrc/test/resources/deploy_config.xml",
        		"-Zsrc/test/resources/nas.zip",
        		"-O.",
        		"-Bsrc/test/resources/h3.zip",
        		"-Ssrc/test/resources/s.policy",
        		"-Lsrc/test/resources/SLF4J.xml",
        		"-lsrc/test/resources/logo.png",
        		"-msrc/test/resources/menulogo.png"
        };
        
        
        DeployApplication.main(args);
    }
}
