/* File:    $Id$
 * Version: $Revision$
 * Date:    $Date$
 * Author:  $Author$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301 
 *  USA
 */

package dk.netarkivet.common.webinterface;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.servlet.ServletRequest;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.arcrepository.Replica;
import dk.netarkivet.common.distribute.arcrepository.ReplicaType;
import dk.netarkivet.common.exceptions.ArgumentNotValid;
import dk.netarkivet.common.exceptions.ForwardedToErrorPage;
import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.exceptions.IllegalState;
import dk.netarkivet.common.exceptions.NetarkivetException;
import dk.netarkivet.common.exceptions.UnknownID;
import dk.netarkivet.common.utils.FileUtils;
import dk.netarkivet.common.utils.I18n;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.ByteJarLoader;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.common.utils.batch.LoadableJarBatchJob;

/**
 * Utility class for creating the web page content for the batchjob pages.
 */
public final class BatchGUI {
    /** The log.*/
    private static Log log = LogFactory.getLog(BatchGUI.class);

    /** The language translator.*/
    private static final I18n I18N = new I18n(
            dk.netarkivet.common.Constants.TRANSLATIONS_BUNDLE);
    
    /**
     * Private Constructor to prevent instantiation of this utility class.
     */
    private BatchGUI() {}
    
    /**
     * Method for creating the batchjob overview page.
     * Creates both the heading and the table for the batchjobs defined in 
     * settings.
     * 
     * @param context The context of the page. Contains the locale for the 
     * language package.
     * @throws ArgumentNotValid If the PageContext is null.
     * @throws IOException If it is not possible to write to the JspWriter.
     */
    public static void getBatchOverviewPage(PageContext context) 
            throws ArgumentNotValid, IOException {
        ArgumentNotValid.checkNotNull(context, "PageContext context");
        JspWriter out = context.getOut();
        
        // retrive the jobs etc.
        String[] jobs = Settings.getAll(CommonSettings.BATCHJOBS_CLASS);
        Locale locale = context.getRequest().getLocale();
        
        if(jobs.length == 0) {
            out.print("<h3>" + I18N.getString(locale, 
                    "batchpage;No.batchjobs.defined.in.settings", 
                    new Object[]{}) + "</h3>");
            return;
        }
        
        // add header for batchjob selection table
        out.print("<table class=\"selection_table\" cols=\"4\">\n");
        out.print("  <tr>\n");
        out.print("    <th>" + I18N.getString(locale, "batchpage;Batchjob", 
                new Object[]{}) + "</th>\n");
        out.print("    <th>" + I18N.getString(locale, "batchpage;Last.run", 
                new Object[]{}) + "</th>\n");
        out.print("    <th>" + I18N.getString(locale, "batchpage;Output.file",
                new Object[]{}) + "</th>\n");
        out.print("    <th>" + I18N.getString(locale, "batchpage;Error.file", 
                new Object[]{}) + "</th>\n");
        out.print("  </tr>\n");
        
        for(int i = 0; i < jobs.length; i++) {
            out.print("  <tr Class=\"" + HTMLUtils.getRowClass(i) + "\">\n");
            out.print(getOverviewTableEntry(jobs[i], locale));
            out.print("  </tr>\n");
        }
        
        out.print("</table>\n");
    }
    
    /**
     * Method for creating the page for a batchjob.
     * It contains the following informations:
     * 
     * <br/>
     * - Creates a line with the name of the batchjob.<br/>
     * - Write the description if the batchjob has a metadata resource 
     * annotation description of the batchjob class.<br/>
     * - The last run information, date and size of the error and output files.
     * <br/>
     * - The arguments of the batchjob, with information if they have been 
     * defined in the resource annotations of the class.<br/>
     * - Radio buttons for choosing the replica.<br/>
     * - Input box for regular expression for filenames to match.<br/>
     * - Execution button.<br/>
     * 
     * @param context The context of the page. Must contains a class name of 
     * the batchjob.
     * @throws UnknownID If the class cannot be found.
     * @throws ArgumentNotValid If the context is null.
     * @throws IllegalState If the class is not an instance of FileBatchJob.
     * @throws ForwardedToErrorPage If the context does not contain the 
     * required information.
     * @throws IOFailure If there is problems with the JspWriter.
     */
    @SuppressWarnings("rawtypes")
    public static void getPageForClass(PageContext context) throws UnknownID,
            ArgumentNotValid, IllegalState, ForwardedToErrorPage, IOFailure {
        ArgumentNotValid.checkNotNull(context, "PageContext context");

        HTMLUtils.forwardOnEmptyParameter(context, 
                Constants.BATCHJOB_PARAMETER);
        
        try {
            // Retrieve variables
            ServletRequest request = context.getRequest();
            Locale locale = request.getLocale();
            String className = request.getParameter(
                    Constants.BATCHJOB_PARAMETER);
            JspWriter out = context.getOut();

            // retrieve the batch class and the constructor.
            Class c = getBatchClass(className);

            out.print(I18N.getString(locale, "batchpage;Name.of.batchjob", 
                    new Object[]{}) + ": <b>" + c.getName() + "</b><br/>\n");
            out.print(getClassDescription(c, locale));
            out.print(getPreviousRuns(c.getName(), locale));
            
            // begin form
            out.println("<form method=\"post\" action=\""
                    + Constants.QA_BATCHJOB_EXECUTE + "?" 
                    + Constants.BATCHJOB_PARAMETER + "=" + className + "\">");
            
            out.print(getHTMLarguments(c, locale));
            out.print(getReplicaRadioButtons(locale));
            out.print(getRegularExpressionInputBox(locale));
            out.print(getSubmitButton(locale));
            
            // end form
            out.print("</form>");
        } catch (IOException e) {
            String errMsg = "Could not create page with batchjobs.";
            log.warn(errMsg, e);
            throw new IOFailure(errMsg, e);
        }
    }
    
    /**
     * Method for executing a batchjob.
     * 
     * @param context The page context containing the needed information for 
     * executing the batchjob.
     */
    @SuppressWarnings("rawtypes")
    public static void execute(PageContext context) {
        try {
            ServletRequest request = context.getRequest();

            // get parameters
            String filetype = request.getParameter(
                    Constants.FILETYPE_PARAMETER);
            String jobId = request.getParameter(Constants.JOB_ID_PARAMETER);
            String jobName = request.getParameter(Constants.BATCHJOB_PARAMETER);
            String repName = request.getParameter(Constants.REPLICA_PARAMETER);
            
            FileBatchJob batchjob;
            
            // Retrieve the list of arguments.
            List<String> args = new ArrayList<String>();
            String arg;
            Integer i = 1;
            // retrieve the constructor to find out how many arguments
            Class c = getBatchClass(jobName);
            Constructor con = findStringConstructor(c);
            
            // retrieve the arguments and put them into the list.
            while(i <= con.getParameterTypes().length) {
                arg = request.getParameter("arg" + i.toString());
                if(arg != null) {
                    args.add(arg);
                } else {
                    log.warn("Should contain argument number " + i + ", but "
                            + "found a null instead, indicating missing "
                            + "argument. Use empty string instead.");
                    args.add("");
                }
                i++;
            }
            
            File jarfile = getJarFile(jobName);
            if(jarfile == null) {
                // get the constructor and instantiate it.
                Constructor construct = findStringConstructor(
                        getBatchClass(jobName));
                batchjob = (FileBatchJob) construct.newInstance(args);
            } else {
                batchjob = new LoadableJarBatchJob(jobName, args, jarfile);
            }

            // get the regular expression.
            String regex = jobId + "-";
            if(filetype.equals(BatchFileType.Metadata.toString())) {
                regex += Constants.REGEX_METADATA;
            } else if(filetype.equals(BatchFileType.Content.toString())) {
                // TODO fix this 'content' regex.
                regex += Constants.REGEX_CONTENT;
            } else {
                regex += Constants.REGEX_ALL;
            }

            // validate the regular expression (throws exception if wrong).
            Pattern.compile(regex);
            
            Replica rep = Replica.getReplicaFromName(repName);
            
            new BatchExecuter(batchjob, regex, rep).start();
            
            JspWriter out = context.getOut();
            out.write("Executing batchjob with the following parameters. "
                    + "<br/>\n");
            out.write("BatchJob name: " + jobName + "<br/>\n");
            out.write("Replica: " + rep.getName() + "<br/>\n");
            out.write("Regular expression: " + regex + "<br/>\n");
        } catch (Exception e) {
            throw new IOFailure("Could not instantiate the batchjob.", e);
        }
    }
    
    /**
     * Extracts and validates the class from the class name.
     * 
     * @param className The name of the class to extract.
     * @return The class from the class name.
     * @throws UnknownID If the className does not refer to a known class.
     * @throws IllegalState If the class is not an instance of FileBatchJob.
     */
    @SuppressWarnings("rawtypes")
    private static Class getBatchClass(String className) 
            throws UnknownID, IllegalState {
        Class res;
        // validate whether a class with the classname can be found
        try {
            File arcfile = getJarFile(className);

            // handle whether internal or loadable batchjob.
            if(arcfile != null) {
                ByteJarLoader bjl = new ByteJarLoader(arcfile);
                res = bjl.findClass(className);
            } else {
                res = Class.forName(className);
            }
        } catch (ClassNotFoundException e) {
            String errMsg = "Cannot find the class '" + className 
            + "' in the classpath. Perhaps bad path or missing library file.";
            log.warn(errMsg);
            throw new UnknownID(errMsg, e);
        }

        // Test whether the class is a sub class to FileBatchJob
        if(!(FileBatchJob.class.isAssignableFrom(res))) {
            String errMsg = "The class '" + className + "' is not an instance " 
                    + "of '" + FileBatchJob.class.getName() + "' as required.";
            log.warn(errMsg);
            throw new IllegalState(errMsg);
        }
        
        return res;
    }
    
    /**
     * Finds a constructor which either does not take any arguments, or does 
     * only takes string arguments.
     * Any constructor which does has other arguments than string is ignored.
     * 
     * @param c The class to retrieve the constructor from.
     * @return The string argument based constructor of the class.
     * @throws UnknownID If no valid constructor can be found.
     */
    @SuppressWarnings("rawtypes")
    private static Constructor findStringConstructor(Class c) throws UnknownID {
        for(Constructor con : c.getConstructors()) {
            boolean valid = true;
            
            // validate the parameter classes. Ignore if not string.
            for(Class cl : con.getParameterTypes()) {
                if(!cl.equals(java.lang.String.class)) {
                    valid = false;
                    break;
                }
            }
            if(valid) {
                return con;
            }
        }
        
        // throw an exception if no valid constructor can be found.
        throw new UnknownID("No valid constructor can be found for class '"
                + c.getName() + "'.");
    }
    
    /**
     * Retrieves the HTML code for the description of the class.
     * The description of the class is given in the resource annotation which 
     * has the type of the given class.
     * 
     * <br/>
     * E.g. 
     * <br/>
     * @Resource(description="Batchjob for finding URLs which matches a given" 
     * + " regular expression and has a mimetype which matches another"
     * + " regular expression.", 
     * type=dk.netarkivet.common.utils.batch.UrlSearch.class)}
     * 
     * <br/><br/>
     * Which gives the UrlSearch batchjob the following description:
     * <br/><br/>
     * Description: Batchjob for finding URLs which matches a given regular 
     * expression and has a mimetype which matches another regular expression.
     * &lt;br/&gt;&lt;br/&gt;
     * 
     * @param c The class to be described.
     * @param locale The locale language package.
     * @return The HTML code describing the class.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static String getClassDescription(Class c, Locale locale) {
        // retrieve the resources.
        Resources r = (Resources) c.getAnnotation(Resources.class);
        if(r == null) {
            return "<br/>\n";
        }
        
        // Find and return the description of this class (if any).
        for(Resource resource : r.value()) {
            if(resource.type().getName().equals(c.getName())) {
                return I18N.getString(locale, "batchpage;Description", 
                        new Object[]{}) + ": "+ resource.description() 
                        + "<br/><br/>\n";
            }
        }
        
        // no description found, then return empty string.
        return "<br/>\n";
    }
    
    /**
     * Creates the HTML code for the arguments of the constructor.
     * It reads the resources for the batchjob, where the metadata for the 
     * constructor is defined in the 'resources' annotation for the class.
     * 
     * <br/>
     * E.g. The UrlSearch batchjob. Which has the following resources:<br/>
     * @Resource(name="regex", description="The regular expression for the " 
     *         + "urls.", type=java.lang.String.class)<br/>
     * @Resource(name="mimetype", type=java.lang.String.class)<br/>
     * Though the batchjob takes three arguments (thus one undefined).
     * <br/><br/>
     * 
     * Arguments:&lt;br/&gt;<br/>
     * regex (The regular expression for the urls.)&lt;br/&gt;<br/>
     * &lt;input name="arg1" size="50" value=""&gt;&lt;br/&gt;<br/>
     * mimetype&lt;br/&gt;<br/>
     * &lt;input name="arg2" size="50" value=""&gt;&lt;br/&gt;<br/>
     * Argument 3 (missing argument metadata)&lt;br/&gt;<br/>
     * &lt;input name="arg3" size="50" value=""&gt;&lt;br/&gt;<br/>
     * 
     * <br/>
     * Which will look like:
     * <br/><br/>
     * 
     * Arguments:<br/>
     * regex (The regular expression for the urls.)<br/>
     * <input name="arg1" size="50" value="" /><br/>
     * mimetype<br/>
     * <input name="arg2" size="50" value="" /><br/>
     * Argument 3 (missing argument metadata)<br/>
     * <input name="arg3" size="50" value="" /><br/>
     * 
     * TODO this does not work until batchjobs can be used with arguments.
     * 
     * @param c The class whose constructor should be used.
     * @param locale The language package.
     * @return The HTML code for the arguments for executing the batchjob.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static String getHTMLarguments(Class c, Locale locale) {
        Constructor con = findStringConstructor(c);
        Type[] params = con.getParameterTypes();
        
        // If no parameters, then return no content (new line).
        if(params.length < 1) {
            return "<br/>\n";
        }
        
        // Retrieve the resources (metadata for the arguments).
        Resources r = (Resources) c.getAnnotation(Resources.class);
        if(r == null) {
            return "<br/>\n";
        }
        Resource[] resource = r.value();
        
        StringBuilder res = new StringBuilder();
        
        res.append(I18N.getString(locale, "batchpage;Arguments", 
                new Object[]{}) + ":<br/>\n");
        
        if(resource.length < params.length) {
            // warn about no metadata.
            res.append(I18N.getString(locale, 
                    "batchpage;Bad.argument.metadata.for.the.constructor", 
                    con.toString()) + ".<br/>\n");
            // make default 'arguments'.
            for(int i = 1; i <= params.length; i++) {
                res.append(I18N.getString(locale, "batchpage;Argument.i", i)
                        + "<br/>\n");
                res.append("<input name=\"arg" + i + "\" size=\"" 
                        + Constants.HTML_INPUT_SIZE + "\" value=\"\"><br/>\n");
            }
        } else {
            // handle the case, when there is arguments.
            int parmIndex = 0;
            // retrieve the arguments from the resources.
            for(int i = 0; i < resource.length && parmIndex < params.length; 
                    i++) {
                if(resource[i].type() == params[parmIndex]) {
                    // Use the resource to describe the argument.
                    parmIndex++;
                    res.append(resource[i].name());
                    if(resource[i].description() != null 
                            && !resource[i].description().isEmpty()) {
                        res.append(" (" + resource[i].description() + ")");
                    }
                    res.append("<br/>\n");
                    res.append("<input name=\"arg" + parmIndex + "\" size=\"" 
                            + Constants.HTML_INPUT_SIZE 
                            + "\" value=\"\"><br/>\n");
                }
            }
            // If some arguments did not have a resource description, then
            // use a default 'unknown argument' input box.
            if(parmIndex < params.length) {
                for(int i = parmIndex + 1; i <= params.length; i++) {
                    res.append(I18N.getString(locale, 
                            "batchpage;Argument.i.missing.argument.metadata", 
                            i) + "<br/>\n");
                    res.append("<input name=\"arg" + i + "\" size=\"" 
                            + Constants.HTML_INPUT_SIZE 
                            + "\" value=\"\"><br/>\n");
                }
            }
        }
        
        res.append("<br/>\n");
        
        return res.toString();
    }
    
    /**
     * Creates the HTML code for describing the previous executions of a given 
     * batchjob. If any previous results are found, then a table will be 
     * created. Each result (output and/or error file) will have an entry in
     * the table. A row containing the following: <br/>
     * - The start date (extractable from the result file name). <br/>
     * - The end date (last modified date for either result file). <br/>
     * - The size of the output file.<br/>
     * - The number of lines in the output file. <br/>
     * - A link to download the output file.<br/>
     * - The size of the error file.<br/>
     * - The number of lines in the error file. <br/>
     * - A link to download the error file.<br/>
     * 
     * @param jobPath The name of the batch job. 
     * @param locale The locale language package.
     * @return The HTML code for describing the previous executions of the 
     * batchjob.
     */
    private static String getPreviousRuns(String jobPath, Locale locale) {
        // initialize the resulting string.
        StringBuilder res = new StringBuilder();
        
        // extract the final name of the batch job (used for the files).
        String batchName = getJobName(jobPath);
        
        // extract the batch directory, where the old batchjobs files lies.
        File batchDir = getBatchDir();
        
        // extract the files for the batchjob. 
        String[] filenames = batchDir.list();
        // use a hash-set to avoid counting both '.err' and '.out' files.
        Set<String> prefices = new HashSet<String>();
        
        for(String filename : filenames) {
            // match and put into set.
            if(filename.startsWith(batchName) 
                    && (filename.endsWith(Constants.ERROR_FILE_EXTENSION) 
                    || filename.endsWith(Constants.OUTPUT_FILE_EXTENSION))) {
                String prefix = filename.split("[.]")[0];
                // the prefix is not added twice, since it is a hash-set.
                prefices.add(prefix);
            }
        }
        
        // No files => No previous runs.
        if(prefices.isEmpty()) {
            res.append(I18N.getString(locale, 
                    "batchpage;Batchjob.has.never.been.run", new Object[]{})
                    + "<br/><br/>\n");
            return res.toString();
        }
        
        // make header of output 
        res.append(I18N.getString(locale, "batchpage;Number.of.runs.0", 
                prefices.size()) + "<br/>\n");
        res.append("<table class=\"selection_table\" cols=\"3\">\n");
        res.append("  <tr>\n");
        res.append("    <th colspan=\"1\">" +I18N.getString(locale, 
                "batchpage;Started.date", new Object[]{}) + "</th>\n");
        res.append("    <th colspan=\"1\">" + I18N.getString(locale, 
                "batchpage;Ended.date", new Object[]{}) + "</th>\n");
        res.append("    <th colspan=\"3\">" + I18N.getString(locale, 
                "batchpage;Output.file", new Object[]{}) + "</th>\n");
        res.append("    <th colspan=\"3\">" + I18N.getString(locale, 
                "batchpage;Error.file", new Object[]{}) + "</th>\n");
        res.append("  </tr>\n");
        
        int i = 0;
        for(String prefix : prefices) {
            res.append("  <tr class=" + HTMLUtils.getRowClass(i++) + ">\n");
            
            File outputFile = new File(batchDir, prefix + ".out");
            File errorFile = new File(batchDir, prefix + ".err");
            
            // Retrieve the timestamp from the file-name.
            String[] split = prefix.split("[-]");
            // default, if no timestamp is found.
            String timestamp = "";
            if(split.length >= 2) {
                try {
                    timestamp = new Date(Long.parseLong(split[1])).toString();
                } catch(NumberFormatException e) {
                    log.warn("Could not parse batchjob result file name: " 
                            + prefix, e);
                }
            }
            
            // initialise if uninitialised.
            if(timestamp == null || timestamp.isEmpty()) {
                timestamp = I18N.getString(locale, 
                        "batchpage;No.valid.timestamp", new Object[]{});
            }
            
            // insert start-time
            res.append("    <td>" + timestamp + "</td>\n");
            
            // retrieve the last-modified date for the files
            Long lastModified = 0L;
            if(outputFile.exists() && outputFile.lastModified() 
                    > lastModified) {
                lastModified = outputFile.lastModified();
            }
            if(errorFile.exists() && errorFile.lastModified() 
                    > lastModified) {
                lastModified = errorFile.lastModified();
            }
            
            // insert ended-time 
            res.append("    <td>" + new Date(lastModified).toString() 
                    + "</td>\n");
            
            // insert information about the output file.
            if(!outputFile.exists()) {
                res.append("    <td>" + I18N.getString(locale, 
                        "batchpage;No.outputfile", new Object[]{}) 
                        + "</td>\n");
                res.append("    <td>" + I18N.getString(locale, 
                        "batchpage;No.outputfile", new Object[]{}) 
                        + "</td>\n");
                res.append("    <td>" + I18N.getString(locale, 
                        "batchpage;No.outputfile", new Object[]{}) 
                        + "</td>\n");
            } else {
                res.append("    <td>" + outputFile.length() + " "
                        + I18N.getString(locale, "batchpage;bytes", 
                                new Object[]{}) + "</td>\n");
                res.append("    <td>" + FileUtils.countLines(outputFile) + " "
                        + I18N.getString(locale, "batchpage;lines", 
                                new Object[]{}) + "</td>\n");
                res.append("    <td><a href=" 
                        + Constants.QA_RETRIEVE_RESULT_FILES 
                        + "?filename=" + outputFile.getName() + ">"
                        + I18N.getString(locale, 
                                "batchpage;Download.outputfile", 
                                new Object[]{}) + "</a></td>\n");
            }
            
            // insert information about error file
            if(!errorFile.exists()) {
                res.append("    <td>" + I18N.getString(locale, 
                        "batchpage;No.errorfile", new Object[]{}) 
                        + "</td>\n");
                res.append("    <td>" + I18N.getString(locale, 
                        "batchpage;No.errorfile", new Object[]{}) 
                        + "</td>\n");
                res.append("    <td>" + I18N.getString(locale, 
                        "batchpage;No.errorfile", new Object[]{}) 
                        + "</td>\n");
            } else {
                res.append("    <td>" + errorFile.length() + " "
                        + I18N.getString(locale, "batchpage;bytes", 
                                new Object[]{}) + "</td>\n");
                res.append("    <td>" + FileUtils.countLines(errorFile) + " "
                        + I18N.getString(locale, "batchpage;lines", 
                                new Object[]{}) + "</td>\n");
                res.append("    <td><a href=" 
                        + Constants.QA_RETRIEVE_RESULT_FILES 
                        + "?filename=" + errorFile.getName() + ">"
                        + I18N.getString(locale, 
                                "batchpage;Download.errorfile", 
                                new Object[]{}) + "</a></td>\n");
            }
            
            // end row 
            res.append("  </tr>\n");
        }
        res.append("</table>\n");
        
        return res.toString();
    }
    
    /**
     * Creates the HTML code for making the radio buttons for choosing which 
     * replica the batchjob will be run upon.
     * <br/>E.g. the default replica settings (with two bitarchive replicas and
     * one checksum replica) will give:<br/><br/>
     * 
     * Choose replica: &lt;br/&gt;<br/>
     * &lt;input type="radio" name="replica" value="CsOne" disabled&gtCsOne 
     * CHECKSUM&lt;/input&gt&lt;br/&gt;<br/>
     * &lt;input type="radio" name="replica" value="BarOne" checked&gtBarOne 
     * BITARCHIVE&lt;/input&gt&lt;br/&gt;<br/>
     * &lt;input type="radio" name="replica" value="BarTwo"&gtBarTwo 
     * BITARCHIVE&lt;/input&gt;&lt;br/&gt;<br/>
     * 
     * <br/> which gives: <br/>
     * 
     * Choose replica: <br/>
     * <input type="radio" name="replica" value="CsOne" disabled>CsOne CHECKSUM
     * </input><br/>
     * <input type="radio" name="replica" value="BarOne" checked>BarOne 
     * BITARCHIVE</input><br/>
     * <input type="radio" name="replica" value="BarTwo">BarTwo BITARCHIVE
     * </input><br/>
     * <br/>
     * 
     * @param locale The locale language package.
     * @return The HTML code for the radio buttons for choosing which replica 
     * to run a batchjob upon.
     */
    private static String getReplicaRadioButtons(Locale locale) {
        StringBuilder res = new StringBuilder();
        
        res.append(I18N.getString(locale, "batchpage;Choose.replica", 
                new Object[]{}) + ": <br/>\n");
        
        // Make radio buttons and categorize them as replica.
        for(Replica rep : Replica.getKnown()) {
            res.append("<input type=\"radio\" name=\"replica\" value=\"" 
                    + rep.getName() + "\"");
            // Disable for checksum replica.
            if(rep.getType().equals(ReplicaType.CHECKSUM)) {
                res.append(" disabled");
            } else if(rep.getId().equals(Settings.get(
                    CommonSettings.USE_REPLICA_ID))) {
                res.append(" checked");
            }
            
            res.append(">" + rep.getName() + " " + rep.getType() + "</input>");
            res.append("<br/>\n");
        }
        res.append("<br/>\n");
        return res.toString();
    }
    
    /**
     * Creates the HTML code for choosing the regular expression for limiting
     * the amount the files to be run upon.
     * <br/>E.g. a default class with no specific argument for the limit will 
     * give:<br/><br/>
     * 
     * Which files: &lt;br/&gt;<br/>
     * Job ID: &nbsp; &nbsp; &nbsp; &lt;input name="JobId" size="25" value="1" 
     * /&gt;&lt;br/&gt;\n<br/>
     * &lt;input type="radio" name="filetype" value="Metadata" checked 
     * /&gt;Metadata&lt;br/&gt;\n<br/>
     * &lt;input type="radio" name="filetype" value="Content" checked 
     * /&gt;Content&lt;br/&gt;\n<br/>
     * &lt;input type="radio" name="filetype" value="Both" checked 
     * /&gt;Both&lt;br/&gt;\n<br/>
     * 
     * <br/>Which gives:<br/><br/>
     * 
     * Which files: <br/>
     * Job ID: &nbsp; &nbsp; &nbsp; <input name="JobId" size="25" value="1" />
     * <br/>
     * <input type="radio" name="filetype" value="Metadata" checked />Metadata
     * <br/>
     * <input type="radio" name="filetype" value="Content" checked />Content
     * <br/>
     * <input type="radio" name="filetype" value="Both" checked />Both<br/>
     * 
     * @param locale The locale language package.
     * @return The HTML code for creating the regular expression input box.
     */
    private static String getRegularExpressionInputBox(Locale locale) {
        StringBuilder res = new StringBuilder();
        
        // Make header ('Which files')
        res.append(I18N.getString(locale, "batchpage;Which.files", 
                new Object[]{}));
        res.append(":<br/>\n");
        
        // Make job id input:
        res.append(I18N.getString(locale, "batchpage;Job.ID", new Object[]{})
                + " &nbsp; &nbsp; &nbsp; <input name=\"" 
                + Constants.JOB_ID_PARAMETER + "\" size=\"25\" value=\"1\" "
                + "/><br/>\n");
        
        // Add metadata option (checked radiobutton)
        res.append("<input type=\"radio\" name=\"filetype\" value=\"" 
                + BatchFileType.Metadata + "\" checked />" 
                + I18N.getString(locale, "batchpage;Metadata", new Object[]{}) 
                + "<br/>\n");
        // Add content option
        res.append("<input type=\"radio\" name=\"filetype\" value=\""
                + BatchFileType.Content  + "\" />" 
                + I18N.getString(locale, "batchpage;Content", new Object[]{}) 
                + "<br/>\n");
        // Add both option
        res.append("<input type=\"radio\" name=\"filetype\" value=\""
                + BatchFileType.Both + "\" />" 
                + I18N.getString(locale, "batchpage;Both", new Object[]{}) 
                + "<br/>\n");
        
        return res.toString();
    }
    
    /**
     * Creates the HTML code for the submit button. 
     * 
     * <br/>E.g. a default class with no specific argument for the limit will 
     * give:<br/><br/>
     * 
     * Regular expression for file names (".*" = all files):&lt;br/&gt;<br/>
     * &lt;input name="regex" size="50" value=".*"&gt; 
     * &lt;/input&gt;&lt;br/&gt;&lt;br/&gt;<br/>
     * 
     * <br/>Which gives:<br/><br/>
     * 
     * Regular expression for file names (".*" = all files):<br/>
     * <input name="regex" size="50" value=".*"> </input><br/><br/>
     * 
     * @param locale The locale language package.
     * @return The HTML code for the submit button.
     */
    private static String getSubmitButton(Locale locale) {
        StringBuilder res = new StringBuilder();
        res.append("<br/>\n");
        res.append("<input type=\"submit\" name=\"execute\" value=\""
                + I18N.getString(locale, "batchpage;Execute.batchjob", 
                        new Object[]{}) + "\"/>");
        res.append("<br/><br/>\n");
        return res.toString();
    }
    
    /**
     * Creates an entry for the overview table for the batchjobs.
     * If the batchjob cannot be instatiated, then an error is written before
     * the table entry, and only the name of the batchjob is written, though 
     * the whole name.
     * 
     * <br/>
     * E.g.:
     * <br/>
     * &lt;tr&gt;<br/>
     *   &lt;th&gt;ChecksumJob&lt;/th&gt;<br/>
     *   &lt;th&gt;Tue Mar 23 13:56:45 CET 2010&lt;/th&gt;<br/>
     *   &lt;th&gt;&lt;input type="submit" name="ChecksumJob_output" 
     *   value="view" /&gt;<br/> 
     *   &lt;input type="submit" name="ChecksumJob_output" 
     *   value="download" /&gt;  <br/>
     *   5 bytes&lt;/th&gt;<br/>
     *   &lt;th&gt;&lt;input type="submit" name="ChecksumJob_error" 
     *   value="view" /&gt;<br/> 
     *   &lt;input type="submit" name="ChecksumJob_error" 
     *   value="download" /&gt;<br/>
     *   5 bytes&lt;/th&gt;<br/>
     * &lt;/tr&gt;
     * 
     * <br/>
     * Which looks something like this:
     * <br/><br/>
     * 
     * <tr>
     *   <th>ChecksumJob</th>
     *   <th>Tue Mar 23 13:56:45 CET 2010</th>
     *   <th><input type="submit" name="ChecksumJob_output" value="view" /> 
     *   <input type="submit" name="ChecksumJob_output" value="download" />  
     *   5 bytes</th>
     *   <th><input type="submit" name="ChecksumJob_error" value="view" /> 
     *   <input type="submit" name="ChecksumJob_error" value="download" />  
     *   5 bytes</th>
     * </tr>
     * 
     * @param batchClassPath The name of the batch job.
     * @param locale The language package.
     * @return The HTML code for the entry in the table.
     */
    private static String getOverviewTableEntry(String batchClassPath, 
            Locale locale) {
        StringBuilder res = new StringBuilder();
        try {
            // Check whether it is retrievable. (Throws UnknownID if not).
            getBatchClass(batchClassPath);

            final String batchName = getJobName(batchClassPath);
            File batchDir = getBatchDir();
            
            // retrieve the latest batchjob results.
            String timestamp = getLatestTimestamp(batchName);
            File outputFile = new File(batchDir, batchName + timestamp
                    + Constants.OUTPUT_FILE_EXTENSION);
            File errorFile = new File(batchDir, batchName + timestamp
                    + Constants.ERROR_FILE_EXTENSION);
            
            // write the HTML
            res.append("    <td><a href=\"" + Constants.QA_BATCHJOB_URL + "?" 
                    + Constants.BATCHJOB_PARAMETER + "=" + batchClassPath 
                    + "\">" + batchName + "</a></td>\n");
            // add time of last run
            String lastRun = "";
            if(timestamp.isEmpty()) {
                lastRun = I18N.getString(locale, 
                        "batchpage;Batchjob.has.never.been.run", 
                        new Object[]{});
            } else {
                try {
                    lastRun = new Date(Long.parseLong(
                            timestamp.substring(1))).toString();
                } catch (NumberFormatException e) {
                    log.warn("Could not parse the timestamp '" + timestamp 
                            + "'" , e);
                    lastRun = e.getMessage();
                }
            }
            res.append("    <td>" +  lastRun + "</td>\n");
            
            // add output file references (retrieval and size)
            if(outputFile.exists() && outputFile.isFile() 
                    && outputFile.canRead()) {
                res.append("    <td><a href=" 
                        + Constants.QA_RETRIEVE_RESULT_FILES 
                        + "?filename=" + outputFile.getName() + ">"
                        + I18N.getString(locale, 
                                "batchpage;Download.outputfile", 
                                new Object[]{}) + "</a> " + outputFile.length() 
                                + " bytes, " + FileUtils.countLines(outputFile) 
                                + " lines</td>\n");
            } else {
                res.append("    <td>" + I18N.getString(locale, 
                        "batchpage;No.output.file", new Object[]{}) 
                        + "</td>\n");
            }
            // add error file references (retrieval and size)
            if(errorFile.exists() && errorFile.isFile() 
                    && errorFile.canRead()) {
                res.append("    <td><a href=" 
                        + Constants.QA_RETRIEVE_RESULT_FILES 
                        + "?filename=" + errorFile.getName() + ">"
                        + I18N.getString(locale, 
                                "batchpage;Download.errorfile", 
                                new Object[]{}) + "</a> " + errorFile.length() 
                                + " bytes, " + FileUtils.countLines(errorFile) 
                                + " lines</td>\n");
            } else {
                res.append("    <td>" + I18N.getString(locale, 
                        "batchpage;No.error.file", new Object[]{}) 
                        + "</td>\n");
            }
        } catch (NetarkivetException e) {
            // Handle unretrievable batchjob.
            String errMsg = "Unable to instatiate '" + batchClassPath 
                    + "' as a batchjob.";
            log.warn(errMsg, e);
            
            // clear the string builder.
            res = new StringBuilder();
            res.append(I18N.getString(locale, "batchpage;Warning.0", 
                    errMsg) + "\n");
            res.append("    <td>" + batchClassPath + "</td>\n");
            res.append("    <td>" + "--" + "</td>\n");
            res.append("    <td>" + "--" + "</td>\n");
            res.append("    <td>" + "--" + "</td>\n");
        }
        
        return res.toString();
    }
    
    /**
     * Method for aquiring the name of the files with the latest timestamp.
     * Creates a list with all the names of the result-files for the given 
     * batchjob. The list is sorted and the last (and thus latest) is returned.
     *  
     * @param batchjobName The name of the batchjob in question. Is has to be
     * the name without the path (e.g. 
     * dk.netarkivet.archive.arcrepository.bitpreservation.ChecksumJob should
     * just be ChecksumJob).
     * @return The name of the files for the given batchjob. The empty string 
     * is returned if no result files have been found, indicating that the
     * batchjob has never been run.
     * @throws ArgumentNotValid If the name of the batchjob is either null
     * or the empty string.
     */
    private static String getLatestTimestamp(String batchjobName) 
            throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(batchjobName, 
                "String batchjobName");
        
        File dir = getBatchDir();
        File[] list = dir.listFiles();
        List<String> jobTimestamps = new ArrayList<String>();
        for(File f : list) {
            if(f.getName().startsWith(batchjobName)) {
                int dash = f.getName().indexOf("-");
                int dot = f.getName().lastIndexOf(".");
                // check whether valid positions.
                if(dash > 0 && dot > 0 && dot > dash) {
                    jobTimestamps.add(f.getName().substring(dash, dot));
                }
            }
        }
        
        // send empty string back, no valid files exists.
        if(jobTimestamps.isEmpty()) {
            return "";
        }
        
        // extract the latest.
        Collections.sort(jobTimestamps);
        return jobTimestamps.get(jobTimestamps.size() - 1);
    }
    
    /**
     * Method for extracting the name of the batchjob from the batchjob path.
     * E.g. the batchjob: 
     * dk.netarkivet.archive.arcrepository.bitpreservation.ChecksumJob would 
     * become ChecksumJob.
     * 
     * @param classPath The complete path for class (retrieve by 
     * class.getName()).
     * @return The batchjob name of the class.
     * @throws ArgumentNotValid If the classPath is either null or empty.
     */
    public static String getJobName(String classPath) throws ArgumentNotValid {
        ArgumentNotValid.checkNotNullOrEmpty(classPath, "String className");
        
        String[] jobSplit = classPath.split("[.]");
        return jobSplit[jobSplit.length - 1];
    }
    
    /**
     * Retrieves the directory for the batchDir (defined in settings).
     * 
     * @return The directory containing all the batchjob results.
     */
    public static File getBatchDir() {
        // extract the batch directory, where the old batchjobs files lies.
        File batchDir = new File(Settings.get(
                CommonSettings.BATCHJOBS_BASEDIR));

        // Create the directory, if it does not exist.
        if(!batchDir.exists()) {
            FileUtils.createDir(batchDir);
        }

        return batchDir;
    } 
    
    /**
     * Method for retrieving the path to the arcfile corresponding to the 
     * classpath.
     * 
     * @param classpath The classpath to a batchjob.
     * @return The path to the arc file for the batchjob.
     * @throws UnknownID If the classpath is not within the settings.
     */
    private static String getArcFileForBatchjob(String classpath) 
            throws UnknownID {
        String[] jobs = Settings.getAll(CommonSettings.BATCHJOBS_CLASS);
        String[] arcfiles = Settings.getAll(CommonSettings.BATCHJOBS_JARFILE);

        // go through the lists to find the arc-file.
        for(int i = 0; i < jobs.length; i++) {
            if(jobs[i].equals(classpath)) {
                return arcfiles[i];
            }
        }
        
        throw new UnknownID("Unknown or undefined classpath for batchjob: '" 
                + classpath + "'.");
    }
    
    /**
     * Method for retrieving and validating the arc-file for a given DOOM!
     * 
     * @param classPath The path to the file.
     * @return The arc-file at the given path, or if the path is null or the 
     * empty string, then a null is returned.
     * @throws ArgumentNotValid If the classPath argument is null or the empty 
     * string. 
     * @throws IOFailure If the file does not exist, or it is not a valid file.
     */
    public static File getJarFile(String classPath) throws ArgumentNotValid, 
            IOFailure {
        ArgumentNotValid.checkNotNullOrEmpty(classPath, "String classPath");
        
        // retrieve the path to the arc-file.
        String path = getArcFileForBatchjob(classPath);
        
        // If no file, then return null.
        if(path == null || path.isEmpty()) {
            return null;
        }
        
        // retrieve file, and ensure that it exists and is a valid file.
        File res = new File(path);
        if(!res.isFile()) {
            throw new IOFailure("The file '" + path + "' does not exist, or "
                    + "is maybe not a file but a directory.");
        }
        
        return res;
    }
}
