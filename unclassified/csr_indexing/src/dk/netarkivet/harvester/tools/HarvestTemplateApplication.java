/*$Id$
* $Revision$
* $Author$
* $Date$
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
* Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
*/
package dk.netarkivet.harvester.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.dom4j.Document;
import org.dom4j.io.XMLWriter;

import dk.netarkivet.common.exceptions.IOFailure;
import dk.netarkivet.common.utils.XmlUtils;
import dk.netarkivet.harvester.datamodel.HeritrixTemplate;
import dk.netarkivet.harvester.datamodel.TemplateDAO;

/**
 * Utility for maintaining harvest-templates from the commandline.
 * With this utility, you can
 *  - create new templates
 *  - update existing templates
 *  - download one or more templates
 *  - show all existing templates
 */
public class HarvestTemplateApplication {

    /**
     * The main method of the HarvestTemplateApplication.
     * @param args array of commandline arguments
     */
    public static void main(final String[] args) {
        if (args.length < 1) {
            printUsage();
        } else {
            String command = args[0];
            ArrayList<String> arguments = new ArrayList<String>();
            for (int i = 1; i < args.length; i++) {
                arguments.add(args[i]);
            }
            String[] parameters = new String[arguments.size()];
            parameters = arguments.toArray(parameters);
            if (command.equals("create")) {
                createTemplate(parameters);
            } else if (command.equals("update")) {
                updateTemplate(parameters);
            } else if (command.equals("download")) {
                downloadTemplates(parameters);
            } else if (command.equals("showall")) {
                showallTemplates();
            } else {
                System.err.println("The command '" + command
                        + "' is not one of the legal commands.");
                printUsage();
            }
        }
    }
    /**
     * Show all available templates.
     */
    private static void showallTemplates() {
        TemplateDAO dao = TemplateDAO.getInstance();
        Iterator<String> templateIterator = dao.getAll();
        if (!templateIterator.hasNext()) {
            System.err.println("No templates found in database!");
        } else {
            while (templateIterator.hasNext()) {
                System.out.println(templateIterator.next());
            }
        }
    }

    /**
     * Download one or more templates to current working directory.
     * if length of args is 0, all templates are downloaded.
     * if length of args > 0, the strings in args are considered
     * to be names of templates to be downloaded.
     * @param args String-array containing template-names
     */
    private static void downloadTemplates(final String[] args) {
        TemplateDAO dao = TemplateDAO.getInstance();
        String templateName = "";
        if (args.length < 1) { // download all templates to Current Working dir
            Iterator<String> templateIterator = dao.getAll();
            while (templateIterator.hasNext()) {
                templateName = templateIterator.next();
                download(templateName);
            }
        } else { // Download the templates mentioned as arguments
            for (String arg : args) {
                templateName = arg;
                if (!dao.exists(templateName)) {
                    System.err.println("Unable to download template '"
                            + templateName
                            + "'. It does not exist.");
                } else {
                    download(templateName);
                }
            }
        }
    }

    /**
     * Download the template with a given name.
     * The template is assumed to exist.
     * @param templateName The name of a given template
     */
    private static void download(final String templateName) {
        System.out.println("Downloading template '" + templateName
                + "'.");
        try {
            TemplateDAO dao = TemplateDAO.getInstance();
            HeritrixTemplate doc = dao.read(templateName);
            OutputStream os = new FileOutputStream(templateName + ".xml");
            XMLWriter writer = new XMLWriter(os);
            writer.write(doc.getTemplate());
        } catch (IOException e) {
            System.err.println("Error downloading template '" + templateName
                    + "': " + e);
            e.printStackTrace(System.err);
        }
    }

    /**
     * Update a given template.
     * @param args array of commandline-arguments
     *        args[0]: templateName
     *        args[1]: File that should replace an existing template
     */
    private static void updateTemplate(final String[] args) {
        TemplateDAO dao = TemplateDAO.getInstance();
        if (!(args.length == 2)) {
            System.err.println("Unable to update template: Wrong number("
                    + (args.length) + ") of arguments.");
            printUsage();
        } else {
            String templateName = args[0];
            File templateFile = new File(args[1]);
            if (!dao.exists(templateName)) {
                System.err.println("There is no template named '" + templateName
                        + "'. Use the create-command instead.");
            } else {
                try {
                    // Try to convert orderxml-file to Document object
                    Document doc = XmlUtils.getXmlDoc(templateFile);
                    HeritrixTemplate ht = new HeritrixTemplate(doc);
                    dao.update(templateName, ht);
                    System.out.println("The template '" + templateName
                            + "' has now been updated.");
                } catch (IOFailure e) {
                    System.err.println("The file '" + args[1]
                            + "' could not be read or is not valid xml.");
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Create a new template.
     * @param args
     *            array of commandline-arguments
     *            args[0]: templateName
     *            args[1]: file containing the new template.
     */
    private static void createTemplate(final String[] args) {
        TemplateDAO dao = TemplateDAO.getInstance();
        if (!(args.length == 2)) {
            System.err.println("Unable to create template: Wrong number("
                    + (args.length) + ") of arguments.");
            printUsage();
        } else {
            String templateName = args[0];
            File templateFile = new File(args[1]);
            if (dao.exists(templateName)) {
                System.err.println("There is already a template with name '"
                        + templateName + "'.");
            } else {
                try {
                    // Try to convert orderxml-file to Document object
                    Document doc = XmlUtils.getXmlDoc(templateFile);
                    HeritrixTemplate ht = new HeritrixTemplate(doc);
                    dao.create(templateName, ht);
                    System.out.println("The template '" + templateName
                            + "' has now been created.");
                } catch (IOFailure e) {
                    System.err.println("The File '" + args[1]
                            + "' is not readable or is not valid xml.");
                    e.printStackTrace(System.err);
                }
            }
        }
    }

    /**
     * Print usage information.
     *
     */
    private static void printUsage() {
        System.err.print("java " 
                + HarvestTemplateApplication.class.getName());
        System.err.println(" <command> <args>");
        
        System.err.println("create <template-name> "
                + "<xml-file for this template>");
        System.err.println("download [<template-name>] ");
        System.err.println("update <template-name> "
                + "<xml-file to replace this template>");
        System.err.println("showall");
    }

 }
