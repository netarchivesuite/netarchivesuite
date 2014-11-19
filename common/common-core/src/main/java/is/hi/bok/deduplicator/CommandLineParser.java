/* CommandLineParser
 * 
 * Created on 10.04.2006
 *
 * Copyright (C) 2006 National and University Library of Iceland
 * 
 * This file is part of the DeDuplicator (Heritrix add-on module).
 * 
 * DeDuplicator is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 * 
 * DeDuplicator is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with DeDuplicator; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package is.hi.bok.deduplicator;

import java.io.PrintWriter;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.cli.UnrecognizedOptionException;

/**
 * Print DigestIndexer command-line usage message.
 *
 * @author Kristinn Sigur&eth;sson
 */
@SuppressWarnings({"rawtypes", "unused"})
public class CommandLineParser {
    private static final String USAGE = "Usage: ";
    private static final String NAME = "DigestIndexer";
    private Options options = null;
    private CommandLine commandLine = null;
    private PrintWriter out = null;

    /**
     * Block default construction.
     */
    private CommandLineParser() {
        super();
    }

    /**
     * Constructor.
     *
     * @param args Command-line arguments to process.
     * @param out PrintStream to write on.
     * @throws ParseException Failed parse of command line.
     */
    public CommandLineParser(String[] args, PrintWriter out) throws ParseException {
        super();

        this.out = out;

        this.options = new Options();
        this.options.addOption(new Option("h", "help", false, "Prints this message and exits."));

        Option opt = new Option("o", "mode", true, "Index by URL, HASH or BOTH. Default: BOTH.");
        opt.setArgName("type");
        this.options.addOption(opt);

        this.options.addOption(new Option("s", "equivalent", false,
                "Include a stripped URL in the index for equivalent URL " + "matches."));

        this.options.addOption(new Option("t", "timestamp", false, "Include the time of fetch in the index."));

        this.options.addOption(new Option("e", "etag", false,
                "Include etags in the index (if available in the source)."));

        opt = new Option("m", "mime", true, "A filter on what mime types are added into the index "
                + "(blacklist). Default: ^text/.*");
        opt.setArgName("reg.expr.");
        this.options.addOption(opt);

        this.options.addOption(new Option("w", "whitelist", false,
                "Make the --mime filter a whitelist instead of blacklist."));

        opt = new Option("i", "iterator", true, "An iterator suitable for the source data (default iterator "
                + "works on Heritrix's crawl.log).");
        opt.setArgName("classname");
        this.options.addOption(opt);

        this.options.addOption(new Option("a", "add", false, "Add source data to existing index."));

        opt = new Option("r", "origin", true, "If set, the 'origin' of each URI will be added to the index."
                + " If no origin is provided by the source data then the " + "argument provided here will be used.");
        opt.setArgName("origin");
        this.options.addOption(opt);

        this.options.addOption(new Option("d", "skip-duplicates", false,
                "If set, URIs marked as duplicates will not be added to the " + "index."));

        PosixParser parser = new PosixParser();
        try {
            this.commandLine = parser.parse(this.options, args, false);
        } catch (UnrecognizedOptionException e) {
            usage(e.getMessage(), 1);
        }
    }

    /**
     * Print usage then exit.
     */
    public void usage() {
        usage(0);
    }

    /**
     * Print usage then exit.
     *
     * @param exitCode
     */
    public void usage(int exitCode) {
        usage(null, exitCode);
    }

    /**
     * Print message then usage then exit.
     * <p>
     * The JVM exits inside in this method.
     *
     * @param message Message to print before we do usage.
     * @param exitCode Exit code to use in call to System.exit.
     */
    public void usage(String message, int exitCode) {
        outputAndExit(message, true, exitCode);
    }

    /**
     * Print message and then exit.
     * <p>
     * The JVM exits inside in this method.
     *
     * @param message Message to print before we do usage.
     * @param exitCode Exit code to use in call to System.exit.
     */
    public void message(String message, int exitCode) {
        outputAndExit(message, false, exitCode);
    }

    /**
     * Print out optional message an optional usage and then exit.
     * <p>
     * Private utility method. JVM exits from inside in this method.
     *
     * @param message Message to print before we do usage.
     * @param doUsage True if we are to print out the usage message.
     * @param exitCode Exit code to use in call to System.exit.
     */
    private void outputAndExit(String message, boolean doUsage, int exitCode) {
        if (message != null) {
            this.out.println(message);
        }

        if (doUsage) {
            HelpFormatter formatter = new DigestHelpFormatter();
            formatter.printHelp(this.out, 80, NAME, "Options:", this.options, 1, 2, "Arguments:", false);
            this.out.println(" source                     Data to iterate " + "over (typically a crawl.log). If");
            this.out.println("                            using a non-standard " + "iterator, consult relevant.");
            this.out.println("                            documentation");
            this.out.println(" target                     Target directory " + "for index output. Directory need not");
            this.out.println("                            exist, but " + "unless --add should be empty.");
        }

        // Close printwriter so stream gets flushed.
        this.out.close();
        System.exit(exitCode);
    }

    /**
     * @return Options passed on the command line.
     */
    public Option[] getCommandLineOptions() {
        return this.commandLine.getOptions();
    }

    /**
     * @return Arguments passed on the command line.
     */
    public List getCommandLineArguments() {
        return this.commandLine.getArgList();
    }

    /**
     * @return Command line.
     */
    public CommandLine getCommandLine() {
        return this.commandLine;
    }

    /**
     * Override so can customize usage output.
     */
    public class DigestHelpFormatter extends HelpFormatter {
        public DigestHelpFormatter() {
            super();
        }

        public void printUsage(PrintWriter pw, int width, String cmdLineSyntax) {
            out.println(USAGE + NAME + " --help");
            out.println(USAGE + NAME + " [options] source target");
        }

        public void printUsage(PrintWriter pw, int width, String app, Options options) {
            this.printUsage(pw, width, app);
        }
    }
}
