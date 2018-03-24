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
package dk.netarkivet.common.utils;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.PermissionDenied;
import dk.netarkivet.common.lifecycle.LifeCycleComponent;
import dk.netarkivet.common.management.MBeanConnectorCreator;

/**
 * This class provides functionality for starting applications.
 */
public abstract class ApplicationUtils {

    /** logger for this class. */
    private static final Logger log = LoggerFactory.getLogger(ApplicationUtils.class);

    /** System.exit() value for the case where wrong arguments are given. */
    public static final int WRONG_ARGUMENTS = 1;

    /**
     * System.exit() value for the case where the application does not have a factory method.
     */
    public static final int NO_FACTORY_METHOD = 2;

    /**
     * System.exit() value for the case where the application class could not be instantiated.
     */
    public static final int EXCEPTION_WHILE_INSTANTIATING = 3;

    /**
     * System.exit() value for the case where the Shutdown Hook for the application could not be added.
     */
    public static final int EXCEPTION_WHEN_ADDING_SHUTDOWN_HOOK = 4;

    /**
     * System.exit() value for the case where the management registration for the application could not be started.
     */
    public static final int EXCEPTION_WHEN_ADDING_MANAGEMENT = 6;

    /**
     * Helper class that prints a String to STDOUT, and logs it at INFO level at the same time.
     *
     * @param s the given string.
     */
    private static void logAndPrint(String s) {
        System.out.println(s);
        log.info(s);
    }

    /**
     * Helper class for logging an exception (at level fatal) and printing it to STDOUT at the same time. Also invokes
     * the error notifier.
     *
     * @param s a given String.
     * @param t a given Exception.
     */
    private static void logExceptionAndPrint(String s, Throwable t) {
        System.out.println(s);
        t.printStackTrace();
        log.error(s, t);
        NotificationsFactory.getInstance().notify(s, NotificationType.ERROR, t);
    }

    /**
     * Checks that the arguments for a class are empty. Exits the JVM with error code 1 if the arguments are not empty.
     *
     * @param args the argument array.
     */
    private static void checkArgs(String[] args) {
        if (showVersion(args)) {
            logAndPrint("NetarchiveSuite " + dk.netarkivet.common.Constants.getVersionString(false));
            System.exit(0);
        }
        if (args.length > 0) {
            logAndPrint("This application takes no arguments");
            System.exit(WRONG_ARGUMENTS);
        }
    }

    /**
     * Should we show the version of NetarchiveSuite.
     *
     * @param args commandline arguments to NetarchiveSuite
     * @return true, if we should show the version of NetarchiveSuite; otherwise false
     */
    private static boolean showVersion(String[] args) {
        if (args.length == 1 && (args[0].equals("-v") || args[0].equals("--version"))) {
            return true;
        }
        return false;
    }

    /**
     * Starts up an application. The applications class must: (i) Have a static getInstance() method which returns a an
     * instance of itself. (ii) Implement CleanupIF. If the class cannot be started and a shutdown hook added, the JVM
     * exits with a return code depending on the problem: 1 means wrong arguments 2 means no factory method exists for
     * class 3 means couldn't instantiate class 4 means couldn't add shutdown hook 5 means couldn't add liveness logger
     * 6 means couldn't add remote management
     *
     * @param c The class to be started.
     * @param args The arguments to the application (should be empty).
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void startApp(Class c, String[] args) {
        String appName = c.getName();
        Settings.set(CommonSettings.APPLICATION_NAME, appName);
        logAndPrint("Starting " + appName + "\n" + Constants.getVersionString(false));
        logAndPrint("Java VM: " + System.getProperty("java.version"));
        logAndPrint("java.home: " + System.getProperty("java.home"));
        logAndPrint("Working dir: " + System.getProperty("user.dir"));
        log.info("Using settings files '{}'", StringUtils.conjoin(File.pathSeparator, Settings.getSettingsFiles()));
        checkArgs(args);
        dirMustExist(FileUtils.getTempDir());
        Method factoryMethod = null;
        CleanupIF instance = null;
        // Start the remote management connector
        try {
            MBeanConnectorCreator.exposeJMXMBeanServer();
            log.trace("Added remote management for {}", appName);
        } catch (Throwable e) {
            logExceptionAndPrint("Could not add remote management for class " + appName, e);
            System.exit(EXCEPTION_WHEN_ADDING_MANAGEMENT);
        }
        // Get the factory method
        try {
            factoryMethod = c.getMethod("getInstance", (Class[]) null);
            int modifier = factoryMethod.getModifiers();
            if (!Modifier.isStatic(modifier)) {
                throw new Exception("getInstance is not static");
            }
            logAndPrint(appName + " Running");
        } catch (Throwable e) {
            logExceptionAndPrint("Class " + appName + " does not have required factory method", e);
            System.exit(NO_FACTORY_METHOD);
        }
        // Invoke the factory method
        try {
            log.trace("Invoking factory method.");
            instance = (CleanupIF) factoryMethod.invoke(null, (Object[]) null);
            log.trace("Factory method invoked.");
        } catch (Throwable e) {
            logExceptionAndPrint("Could not start class " + appName, e);
            System.exit(EXCEPTION_WHILE_INSTANTIATING);
        }
        // Add the shutdown hook
        try {
            log.trace("Adding shutdown hook for " + appName);
            Runtime.getRuntime().addShutdownHook((new CleanupHook(instance)));
            log.trace("Added shutdown hook for " + appName);
        } catch (Throwable e) {
            logExceptionAndPrint("Could not add shutdown hook for class " + appName, e);
            System.exit(EXCEPTION_WHEN_ADDING_SHUTDOWN_HOOK);
        }
    }

    /**
     * Starts up an LifeCycleComponent.
     *
     * @param component The component to start.
     */
    public static void startApp(LifeCycleComponent component) {
        ArgumentNotValid.checkNotNull(component, "LifeCycleComponent component");

        String appName = component.getClass().getName();
        Settings.set(CommonSettings.APPLICATION_NAME, appName);
        logAndPrint("Starting " + appName + "\n" + Constants.getVersionString(false));
        log.info("Using settings files '{}'", StringUtils.conjoin(File.pathSeparator, Settings.getSettingsFiles()));
        dirMustExist(FileUtils.getTempDir());
        // Start the remote management connector
        try {
            MBeanConnectorCreator.exposeJMXMBeanServer();
            log.trace("Added remote management for {}", appName);
        } catch (Throwable e) {
            logExceptionAndPrint("Could not add remote management for class " + appName, e);
            System.exit(EXCEPTION_WHEN_ADDING_MANAGEMENT);
        }

        component.start();
        logAndPrint(appName + " Running");

        // Add the shutdown hook
        try {
            log.trace("Adding shutdown hook for {}", appName);
            Runtime.getRuntime().addShutdownHook((new ShutdownHook(component)));
            log.trace("Added shutdown hook for {}", appName);
        } catch (Throwable e) {
            logExceptionAndPrint("Could not add shutdown hook for class " + appName, e);
            System.exit(EXCEPTION_WHEN_ADDING_SHUTDOWN_HOOK);
        }
    }

    /**
     * Ensure that a directory is available and writable. Will warn if the directory doesn't already exist (it ought to
     * be created by the install script) and throws a PermissionDenied exception if the directory cannot be created.
     *
     * @param dir A File object denoting a directory.
     * @throws PermissionDenied if the directory doesn't exist and cannot be created/written to, or if the File object
     * indicates an existing non-directory.
     */
    public static void dirMustExist(File dir) {
        ArgumentNotValid.checkNotNull(dir, "File dir");
        if (FileUtils.createDir(dir)) {
            log.warn("Non-existing directory created '{}'", dir);
        }
    }

}
