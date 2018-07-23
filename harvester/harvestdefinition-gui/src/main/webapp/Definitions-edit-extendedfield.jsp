<%--
File:       $Id$
Revision:   $Revision$
Author:     $Author$
Date:       $Date$

The Netarchive Suite - Software to harvest and preserve websites
Copyright 2004-2018 The Royal Danish Library,
the National Library of France and the Austrian
National Library.

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

--%><%@page import="
			dk.netarkivet.harvester.datamodel.extendedfield.ExtendedField,
			dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDAO,
			dk.netarkivet.harvester.datamodel.extendedfield.ExtendedFieldDataTypes,
			dk.netarkivet.harvester.webinterface.ExtendedFieldDefinition,
			dk.netarkivet.harvester.webinterface.ExtendedFieldConstants,
			dk.netarkivet.common.exceptions.ForwardedToErrorPage,
			dk.netarkivet.common.webinterface.HTMLUtils,
			dk.netarkivet.archive.Constants,			
			dk.netarkivet.common.utils.I18n" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<fmt:setLocale value="<%=HTMLUtils.getLocale(request)%>" scope="page"/>
<fmt:setBundle scope="page" basename="<%=dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE%>"/>
<%!
    private static final I18n I18N
            = new I18n(dk.netarkivet.harvester.Constants.TRANSLATIONS_BUNDLE);
%>
<%
	HTMLUtils.setUTF8(request);
    String action = request.getParameter(ExtendedFieldConstants.EXTF_ACTION);

    ExtendedFieldDAO extdao = ExtendedFieldDAO.getInstance();

    // extf_type_id must be set if Action read is used
    
	String extf_type_id = request.getParameter(ExtendedFieldConstants.EXTF_TYPE_ID);
	String extf_id = request.getParameter(ExtendedFieldConstants.EXTF_ID);
	
	ExtendedField extField = null;
	
	if (ExtendedFieldConstants.EXTF_ACTION_READ.equals(action)) {
		if (extf_id != null) {
	        extField = ExtendedFieldDefinition.readExtendedField(extf_id);
		}
		else {
            extField = new ExtendedField(extf_type_id);
		}
	}
	else {
        if (ExtendedFieldConstants.EXTF_ACTION_DELETE.equals(action)) {
        	extdao.delete(Integer.parseInt(extf_id));
            %>    
            <jsp:forward page="Definitions-list-extendedfields.jsp"/>
            <%
        }
        else if (ExtendedFieldConstants.EXTF_ACTION_CREATE.equals(action)) {
            extField = new ExtendedField(extf_type_id);
        }       
        else if (ExtendedFieldConstants.EXTF_ACTION_SUBMIT.equals(action)) {
	        try {
	        	extField = ExtendedFieldDefinition.processRequest(pageContext, I18N);
	        } catch (ForwardedToErrorPage e) {
	            HTMLUtils.forwardWithErrorMessage(pageContext, I18N,
	                    e.getMessage(), e);
	        }
	        %>    
	        <jsp:forward page="Definitions-list-extendedfields.jsp"/>
            <%
		}
	}

	// if changed following block must also be implemented in extfield.js  
	
    String toggle_textarea_style = "display:none;";
    String toggle_checkbox_style = "display:none;";
    String toggle_textfield_style = "display:block;";
    String toggle_maxlen_style = "display:block;";
    String toggle_options_style = "display:none;";
    String toggle_format_style = "display:none;";
    String toggle_mandatory_style = "display:block";
    String toggle_jscalendar_style = "display:none";
    
    if (extField.getDatatype() == ExtendedFieldDataTypes.NUMBER || extField.getDatatype() == ExtendedFieldDataTypes.TIMESTAMP) {
        toggle_format_style = "display:block;";        
    }

    if (extField.getDatatype() == ExtendedFieldDataTypes.JSCALENDAR) {
    	toggle_jscalendar_style = "display:block;";
    	toggle_maxlen_style = "display:none;";
    }
    
    if (extField.getDatatype() == ExtendedFieldDataTypes.SELECT) {
    	toggle_options_style = "display:block;";    	
        toggle_maxlen_style = "display:none;";        
    }

    if (extField.getDatatype() == ExtendedFieldDataTypes.NOTE) {
        toggle_textarea_style = "display:block;";
        toggle_checkbox_style = "display:none;";
        toggle_textfield_style = "display:none;";
    }
    
    if (extField.getDatatype() == ExtendedFieldDataTypes.BOOLEAN) {
        toggle_maxlen_style = "display:none;";        
        toggle_textarea_style = "display:none;";
        toggle_checkbox_style = "display:block;";
        toggle_textfield_style = "display:none;";
        toggle_mandatory_style = "display:none;";
    }
    

    String sel = " selected=\"selected\" ";
    String checked = " checked=\"checked\" ";
    
%>

	<%
	HTMLUtils.generateHeader(pageContext, "js/extfields.js");
	%>    
	
    <h3><fmt:message key="pagetitle;edit.extendedfield"/></h3>
    
    <form method="post" action="Definitions-edit-extendedfield.jsp">
    <input name="<%= ExtendedFieldConstants.EXTF_ACTION %>" value="<%= ExtendedFieldConstants.EXTF_ACTION_SUBMIT %>" type="hidden"/>
    <%
    if (extf_id != null) {
        %>    
        <input name="<%= ExtendedFieldConstants.EXTF_ID %>" value="<%= extf_id %>" type="hidden"/>
        <%
    }
    %>    
    <%
    if (extf_type_id != null) {
        %>    
        <input name="<%= ExtendedFieldConstants.EXTF_TYPE_ID %>" value="<%= extf_type_id %>" type="hidden"/>
        <%
    }
    %>    
    
	<table>
	    <tr>
	        <td>*<fmt:message key="extendedfields.name"/>:</td>
	        <td><span id="focusElement">
	        <input name="<%= ExtendedFieldConstants.EXTF_NAME %>" size="50" type="text" value="<%=HTMLUtils.escapeHtmlValues(extField.getName())%>"/>
	        </span></td>
	    </tr>
    </table>
    <table>
		<tr>
		    <td><fmt:message key="extendedfields.datatype"/>:</td>
		    <td><select name="<%=ExtendedFieldConstants.EXTF_DATATYPE%>" id="datatype_select" size="1" onchange="javascript:tooglefields(this.value);">
		            <option value="<%= ExtendedFieldDataTypes.STRING %>" <%= (extField.getDatatype() == ExtendedFieldDataTypes.STRING) ? sel : "" %>>
		            <fmt:message key="extendedfields.datatype.string"/>
		            </option>
		            <option value="<%= ExtendedFieldDataTypes.BOOLEAN %>" <%= (extField.getDatatype() == ExtendedFieldDataTypes.BOOLEAN) ? sel : "" %>>
		            <fmt:message key="extendedfields.datatype.boolean"/>
		            </option>
		            <option value="<%= ExtendedFieldDataTypes.NUMBER %>" <%= (extField.getDatatype() == ExtendedFieldDataTypes.NUMBER) ? sel : "" %>>
		            <fmt:message key="extendedfields.datatype.number"/>
		            </option>
		            <option value="<%= ExtendedFieldDataTypes.TIMESTAMP %>" <%= (extField.getDatatype() == ExtendedFieldDataTypes.TIMESTAMP) ? sel : "" %>>
		            <fmt:message key="extendedfields.datatype.timestamp"/>
		            </option>
                    <option value="<%= ExtendedFieldDataTypes.JSCALENDAR %>" <%= (extField.getDatatype() == ExtendedFieldDataTypes.JSCALENDAR) ? sel : "" %>>
                    <fmt:message key="extendedfields.datatype.jscalendar"/>
                    </option>
                    <option value="<%= ExtendedFieldDataTypes.NOTE %>" <%= (extField.getDatatype() == ExtendedFieldDataTypes.NOTE) ? sel : "" %>>
                    <fmt:message key="extendedfields.datatype.note"/>
                    </option>
                    <option value="<%= ExtendedFieldDataTypes.SELECT %>" <%= (extField.getDatatype() == ExtendedFieldDataTypes.SELECT) ? sel : "" %>>
                    <fmt:message key="extendedfields.datatype.select"/>
                    </option>
		        </select>
		    </td>
		</tr>
	</table>
    <div id="toggle_maxlen" style="<%= toggle_maxlen_style %>">
        <table>
            <tr>
                <td><fmt:message key="extendedfields.maxlen"/>:</td>
                <td><span id="focusElement">
                
                <input name="<%=ExtendedFieldConstants.EXTF_MAXLEN %>" size="10" type="text" maxlength="5" value="<%= HTMLUtils.escapeHtmlValues(String.valueOf(extField.getMaxlen()))%>" />
                </span></td>
            </tr>
        </table>
    </div>
    <div id="toggle_options" style="<%= toggle_options_style %>">
        <table>
	        <tr>
	            <td><fmt:message key="extendedfields.options"/>:</td>
	            <td><span id="focusElement">
                <textarea name="<%=ExtendedFieldConstants.EXTF_OPTIONS %>" rows="5" cols="50" maxlength="<%=ExtendedFieldConstants.MAXLEN_EXTF_OPTIONS %>"><%=HTMLUtils.escapeHtmlValues(extField.getOptions())%></textarea>
	            </span></td>
	        </tr>
	        <tr><td></td><td><small>(<fmt:message key="extendedfields.options.help"/>)</small></td></tr>
        </table>
    </div>
    <div id="toggle_textarea" style="<%= toggle_textarea_style %>">
        <table>
            <tr>
                <td><fmt:message key="extendedfields.defaultvalue"/>:</td>
                <td><span id="focusElement">
                <textarea name="<%=ExtendedFieldConstants.EXTF_DEFAULTVALUE_TEXTAREA %>" rows="5" cols="50" maxlength="<%=ExtendedFieldConstants.MAXLEN_EXTF_DEFAULTVALUE %>"><%=HTMLUtils.escapeHtmlValues(extField.getDefaultValue())%></textarea>
                </span></td>
            </tr>
            <tr><td></td><td></td></tr>
        </table>
    </div>
    <div id="toggle_textfield" style="<%= toggle_textfield_style %>">
	    <table>
	     <tr>
	         <td><fmt:message key="extendedfields.defaultvalue"/>:</td>
	         <td><span id="focusElement">
	         <input name="<%=ExtendedFieldConstants.EXTF_DEFAULTVALUE_TEXTFIELD%>" size="50" type="text" maxlength="<%=ExtendedFieldConstants.MAXLEN_EXTF_DEFAULTVALUE %>" value="<%= HTMLUtils.escapeHtmlValues(extField.getDefaultValue()) %>"/>
	         </span></td>
	     </tr>
	    </table>
    </div>
    <div id="toggle_checkbox" style="<%= toggle_checkbox_style %>">
        <table>
         <tr>
             <td><fmt:message key="extendedfields.defaultvalue"/>:</td>
             <td><span id="focusElement">
             <input name="<%=ExtendedFieldConstants.EXTF_DEFAULTVALUE_CHECKBOX%>" type="checkbox" value="<%= HTMLUtils.escapeHtmlValues(extField.getDefaultValue()) %>" <%= ExtendedFieldConstants.TRUE.equals(extField.getDefaultValue()) ? checked : "" %>/>
             </span></td>
         </tr>
        </table>
    </div>

    
    <div id="toggle_format" style="<%= toggle_format_style %>">
        <table>
            <tr>
                <td><fmt:message key="extendedfields.formattingpattern"/>:</td>
                <td><span id="focusElement">
                <input name="<%=ExtendedFieldConstants.EXTF_FORMAT %>" size="50" type="text" maxlength="<%=ExtendedFieldConstants.MAXLEN_EXTF_FORMAT %>" value="<%=HTMLUtils.escapeHtmlValues(extField.getFormattingPattern())%>"/>
                </span></td>
            </tr>
        </table>
    </div>
    
    <div id="toggle_jscalendar" style="<%= toggle_jscalendar_style %>">
        <table>
            <tr>
                <td><fmt:message key="extendedfields.formattingpattern"/>:</td>
	            <td><select name="<%=ExtendedFieldConstants.EXTF_FORMAT_JSCALENDAR %>" id="jscalendar_format" size="1">
	                    <!-- 
                        TODO Possible Pattern need to go to Settings.xml - modify also in extendedfields_element.jspf
	                     -->
                        <option value="dd/MM/yyyy" <%= "dd/MM/yyyy".equals(extField.getFormattingPattern()) ? sel : "" %>>
                        dd/MM/yyyy
                        </option>
                        <option value="dd/MM/yyyy HH:mm" <%= "dd/MM/yyyy HH:mm".equals(extField.getFormattingPattern()) ? sel : "" %>>
                        dd/MM/yyyy HH:mm
                        </option>
	                </select>
	            </td>
            </tr>
        </table>
    </div>
    
    
    <div id="toggle_mandatory" style="<%= toggle_mandatory_style %>">
	    <table>
	        <tr>
	            <td><fmt:message key="extendedfields.mandatory"/>:</td>
	            <td><span id="focusElement">
	            <input name="<%=ExtendedFieldConstants.EXTF_MANDATORY%>" type="checkbox" <%= (extField.isMandatory()) ? checked : "" %>/>
	            </span></td>
	        </tr>
	    </table>
    </div>
    
    <table>
        <tr>
            <td><fmt:message key="extendedfields.sequencenr"/>:</td>
            <td><span id="focusElement">
            <input name="<%=ExtendedFieldConstants.EXTF_SEQUENCENR%>" size="10" type="text" maxlength="5" value="<%= extField.getSequencenr() %>"/>
            </span></td>
        </tr>
	</table>
<br/>
<input type="submit" value="<fmt:message key="save"/>"/>
</form>

<%
    HTMLUtils.generateFooter(out);
%>