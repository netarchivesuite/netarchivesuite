package dk.netarkivet.common.tools;

import java.io.File;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.EMailNotifications;
import dk.netarkivet.common.utils.NotificationType;
import dk.netarkivet.common.utils.NotificationsFactory;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.SystemUtils;

/**
 * Reads the mail-settings and tries to send a mail-notification. 
 */
public class MailValidator {

    public static final String SETTINGSFILEPATH = "dk.netarkivet.settings.file";
    public static final String EmailNotificationsClass = EMailNotifications.class.getName();

    /**
     * @param args The path to a settings.xml (optional). If no argument, uses the existing settings.(e.g. by explicit setting it
     *  -Ddk.netarkivet.settings.file=/fullOrrelative/path/to/settings.xml )
     */
    public static void main(String[] args) {
        if (args.length == 1) {
            System.out.println("Using settingsfile given as argument: " + args[0]); 
            System.setProperty(SETTINGSFILEPATH, args[0]);
            File settingsfile = new File(args[0]);
            if (!settingsfile.exists()) {
                System.err.println("Aborting program. Settingsfile '" + settingsfile.getAbsolutePath() + "' does not exist or is not a file");
                System.exit(1);
            }
        } else {
            String settingsfilename = System.getProperty(SETTINGSFILEPATH);
            if (settingsfilename == null) {
                System.out.println("Using default settings");
            } else {
                System.out.println("Using settingsfile '" + settingsfilename + "' defined by setting '" + SETTINGSFILEPATH + "'");
            }
        }
        String notificationsClass = Settings.get(CommonSettings.NOTIFICATIONS_CLASS);
        if (!notificationsClass.equals(EmailNotificationsClass)) {
            System.err.println("Aborting program. Wrong notificationClass defined in the settings. Should be '" + EmailNotificationsClass + "' but was '" + notificationsClass + "'");
            System.exit(1);
        }
        NotificationsFactory.getInstance().notify("Test-message sent from " + MailValidator.class.getName() + " from host '" + SystemUtils.getLocalHostName(), NotificationType.INFO);
        System.out.println("Test-Mail now sent successfully to address '" + Settings.get(EMailNotifications.MAIL_RECEIVER_SETTING) + "' using '" 
                + Settings.get(CommonSettings.MAIL_SERVER) + "' as mailserver, and '" + Settings.get(EMailNotifications.MAIL_SENDER_SETTING) + "' as sender.");
    }
}
