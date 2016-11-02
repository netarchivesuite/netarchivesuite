package dk.netarkivet.harvester.webinterface.servlet;

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
