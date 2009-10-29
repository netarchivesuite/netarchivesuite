<%--
File:       $Id$
Revision:   $Revision$
Author:     $Author$
Date:       $Date$

The Netarchive Suite - Software to harvest and preserve websites
Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

--%><%--
    This class receives a request to either upload or replace an order xml
    template. Must be a form/multipart posting
    Parameters are:
    order_xml_to_upload - name of new order.xml-template
    order_xml_to_replace - name of order.xml-tempate to replace
    file - file posted to create or replace

--%><%@ page import="java.io.File,
                 java.util.List,
                 org.apache.commons.fileupload.DiskFileUpload,
                 org.apache.commons.fileupload.FileItem,
                 org.apache.commons.fileupload.FileUpload,
                 org.dom4j.Document,
                 dk.netarkivet.common.exceptions.ArgumentNotValid,
                 dk.netarkivet.common.exceptions.IOFailure,
                 dk.netarkivet.common.utils.FileUtils,
                 dk.netarkivet.common.utils.I18n,
                 dk.netarkivet.common.utils.XmlUtils,
                 dk.netarkivet.common.webinterface.HTMLUtils,
                 dk.netarkivet.harvester.Constants,
                 dk.netarkivet.harvester.datamodel.HeritrixTemplate, dk.netarkivet.harvester.datamodel.TemplateDAO"
         pageEncoding="UTF-8"
%><%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"
%><fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"
/><fmt:setBundle scope="page" basename="<%=Constants.TRANSLATIONS_BUNDLE%>"/><%!
    private static final I18n I18N = new I18n(Constants.TRANSLATIONS_BUNDLE);
%><%
    HTMLUtils.setUTF8(request);

    final String ORDER_XML_UPLOAD_FIELD_NAME = "order_xml_to_upload";
    final String ORDER_XML_REPLACE_FIELD_NAME = "order_xml_to_replace";
    File orderXmlFile
            = File.createTempFile("order", ".xml", FileUtils.getTempDir());
    String orderXmlToReplace = "";
    String orderXmlToUpload = "";
    boolean isMultiPart = FileUpload.isMultipartContent(request);
    if (!isMultiPart) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg;nothing.posted");
        return;
    }
    DiskFileUpload upload = new DiskFileUpload();
    String fileName = "";
    try {
        List items = upload.parseRequest(request);
        for (Object o : items) {
            FileItem item = (FileItem) o;
            if (!item.isFormField()) {
                item.write(orderXmlFile);
                fileName = item.getName();
            } else {
                String fieldName = item.getFieldName();
                if (fieldName.equals(ORDER_XML_REPLACE_FIELD_NAME)) {
                    orderXmlToReplace = item.getString();
                } else if (fieldName.equals(ORDER_XML_UPLOAD_FIELD_NAME)) {
                    orderXmlToUpload = item.getString();
                }
            }
        }
    } catch (Exception e) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg;template.upload.failed.with.exception.0", e);
        return;
    }

    if (orderXmlToReplace.isEmpty() && orderXmlToUpload.isEmpty()) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg;harvest.template.name.missing");
        return;
    }
    if (fileName.isEmpty()) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg;upload.file.missing");
        return;
    }
    if (!orderXmlFile.exists()) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                "errormsg;uploading.file.0.failed.it.does.not.exist", fileName);
        return;
    }
    TemplateDAO dao = TemplateDAO.getInstance();
    // We now have both the upload file itself and the name of a harvest
    // template to create or replace.

    boolean replaceOperation = !orderXmlToReplace.isEmpty();
    String message = "";
    try {
        // 1: Try to convert orderxml-file to HeritrixTemplate object
        // This throws ArgumentNotValid, if xml is invalid according to out requirements
        Document doc;
        try {
            doc = XmlUtils.getXmlDoc(orderXmlFile);
        } catch (ArgumentNotValid e) {
            HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                    "errormsg;invalid.order.file.0", orderXmlToReplace, e);
            return;
        }
        HeritrixTemplate ht = new HeritrixTemplate(doc);
        // 2: Update orderxml: 'orderXmlToReplace' if it exists, otherwise
        // create

        if (replaceOperation) {
            if (!dao.exists(orderXmlToReplace)) {
                HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                        "errormsg;harvest.template.0.does.not.exist",
                        orderXmlToReplace);
                return;
            }
            dao.update(orderXmlToReplace, ht);
            message = I18N.getString(response.getLocale(),
                    "harvest.template.0.has.been.updated", orderXmlToReplace);
        } else {
            if (dao.exists(orderXmlToUpload)) {
                HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                        "errormsg;harvest.template.0.already.exists",
                        orderXmlToUpload);
                return;
            }
            dao.create(orderXmlToUpload, ht);
            message = I18N.getString(response.getLocale(),
                    "harvest.template.0.has.been.created", orderXmlToUpload);
        }
    } catch (IOFailure e) {
        HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
                e, "errormsg;file.0.unreadable.or.illegal", fileName);
        ;
        return;
    }
    HTMLUtils.generateHeader(
            pageContext);%>
       
   <h4> <%=HTMLUtils.escapeHtmlValues(message) %> </h4><%
   HTMLUtils.generateFooter(out);
%>