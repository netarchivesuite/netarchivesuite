<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
<meta content="text/html; charset=UTF-8" http-equiv= "content-type" /><meta http-equiv="Expires" content="0"/>
<meta http-equiv="Cache-Control" content="no-cache"/>
<meta http-equiv="Pragma" content="no-cache"/>
<meta http-equiv="refresh" content="60"/>
<title><placeholder id ="title" /></title>
<link type="text/css" rel="stylesheet" href="/History/css/bootstrap.css" />
<link type="text/css" rel="stylesheet" href="/History/css/bootstrap-responsive.css" />
<script type="text/javascript">
<!--
function giveFocus() {
    var e = document.getElementById('focusElement');
    if (e != null) {
        var elms = e.getElementsByTagName('*');
        if (elms != null && elms.length != null && elms.item != null && elms.length > 0) {
            var e2 = elms.item(0);
            if (e2 != null && e2.focus != null) {
                e2.focus();
            }
        }
    }
}

function submitForm(formId) {
    document.forms["myform"].submit();
    document.myform.submit();
    var oForm = document.getElementById(formId);
    if (oForm) {
        oForm.submit();
    }
    else {
        alert("DEBUG - could not find element " + formId);
    }
}
-->
</script>

<link rel="stylesheet" href="/History/netarkivet.css" type="text/css" />
<link rel="stylesheet" href="/History/jscalendar/calendar-win2k-cold-1.css" title="/History/jscalendar/win2k-cold-1" type="text/css" media="all" />
</head>
<body onload="giveFocus()">
<table id ="main_table">
<tr>
<td valign="top" id="menu">
<table id="menu_table">
<tr><td><a class="sidebarHeader" href="index.jsp"><img src="/History/transparent_menu_logo.png" alt="Menu"/> Menu</a></td></tr>
<tr><td><a href="/HarvestDefinition/Definitions-selective-harvests.jsp">Definitions</a></td></tr>
<tr><td><a href="/History/Harveststatus-alljobs.jsp">Harvest status</a></td></tr>
<tr><td>&nbsp; &nbsp; <a href="/History/Harveststatus-alljobs.jsp"> All Jobs</a></td></tr>
<tr><td>&nbsp; &nbsp; <a href="/History/Harveststatus-deprecatedperdomain.jsp"> All Jobs per domain</a></td></tr>
<tr><td>&nbsp; &nbsp; <a href="/History/Harveststatus-running.jsp"> Running Jobs</a></td></tr>
<tr><td>&nbsp; &nbsp; <a href="/History/history/"> H3 remote access</a></td></tr>
<placeholder id="menu" />
<tr><td><a href="/HarvestChannel/HarvestChannel-edit-harvest-mappings.jsp">Harvest Channels</a></td></tr>
<tr><td><a href="/BitPreservation/Bitpreservation-filestatus.jsp">Bitpreservation</a></td></tr>
<tr><td><a href="/QA/QA-status.jsp">Quality Assurance</a></td></tr>
<tr><td><a href="/Status/Monitor-JMXsummary.jsp">Systemstate</a></td></tr>
</table>
</td>
<td valign = "top" >
<div class="languagelinks"><a href="lang.jsp?locale=da&amp;name=Dansk">Dansk</a>&nbsp;<a href="lang.jsp?locale=en&amp;name=English">English</a>&nbsp;<a href="lang.jsp?locale=de&amp;name=Deutsch">Deutsch</a>&nbsp;<a href="lang.jsp?locale=it&amp;name=Italiano">Italiano</a>&nbsp;<a href="lang.jsp?locale=fr&amp;name=Fran%C3%A7ais">Français</a>&nbsp;</div>

<h3 class="page_heading"><placeholder id="heading" /></h3>

<placeholder id="content" />

</td>
</tr>
</table>
<div class='systeminfo'>NetarchiveSuite <placeholder id="version" />, <placeholder id="environment" /></div>

  <script src="/History/js/bootstrap.js"></script>
<!--
  <script src="js/bootstrap-transition.js"></script>
  <script src="js/bootstrap-alert.js"></script>
  <script src="js/bootstrap-modal.js"></script>
  <script src="js/bootstrap-dropdown.js"></script>
  <script src="js/bootstrap-scrollspy.js"></script>
  <script src="js/bootstrap-tab.js"></script>
  <script src="js/bootstrap-tooltip.js"></script>
  <script src="js/bootstrap-popover.js"></script>
  <script src="js/bootstrap-button.js"></script>
  <script src="js/bootstrap-collapse.js"></script>
  <script src="js/bootstrap-carousel.js"></script>
  <script src="js/bootstrap-typeahead.js"></script>
-->
</body>
</html>
