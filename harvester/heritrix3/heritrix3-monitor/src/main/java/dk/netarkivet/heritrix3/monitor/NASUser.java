/*
 * #%L
 * Netarchivesuite - heritrix 3 monitor
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

package dk.netarkivet.heritrix3.monitor;

import javax.servlet.http.HttpServletRequest;

import com.antiaction.common.templateengine.login.LoginTemplateUser;

public class NASUser implements LoginTemplateUser {

    public String id;

    public String username;

    public boolean active = false;

    public static NASUser getAdminByCredentials(String id, String password) {    
        return getDefaultUser();
    }

    @Override
    public String get_cookie_token(HttpServletRequest req) {
        return null; // Not needed to further implement at the moment
    }
    
    public static NASUser getDefaultUser() {
        NASUser u = new NASUser();
        u.active=true;
        u.id = "svc@kb.dk";
        u.username="admin";
        return u;
    }

}
