package dk.netarkivet;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.firefox.ExtensionConnection;
import org.openqa.selenium.firefox.FirefoxBinary;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxProfile;

public class BaoFirefoxDriver extends FirefoxDriver { 
    private static final String Localhost = "localhost"; 
    private static final String LocalhostIp = "127.0.0.1"; 
    public BaoFirefoxDriver() { 
        super(); 
    } 
    public BaoFirefoxDriver(Capabilities capabilities) { 
        super(capabilities); 
    } 
    public BaoFirefoxDriver(FirefoxBinary binary, FirefoxProfile profile) { 
        super(binary, profile); 
    } 
    public BaoFirefoxDriver(FirefoxProfile profile) { 
        super(profile); 
    } 
    @Override 
    protected ExtensionConnection connectTo(FirefoxBinary binary, 
            FirefoxProfile profile, String host) { 
        String hostToUse = host; 
        if (Localhost.equalsIgnoreCase(host)) { 
            hostToUse = LocalhostIp; 
        } 
        return super.connectTo(binary, profile, hostToUse); 
    } 
}