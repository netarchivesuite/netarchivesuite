http://20040514.emu.dk/robots.txt 195.231.241.70 20040524210502 text/html 4196
HTTP/1.1 404 Object Not Found
Server: Microsoft-IIS/5.0
Date: Mon, 24 May 2004 21:05:31 GMT
Connection: close
Content-Length: 4040
Content-Type: text/html

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 3.2 Final//EN">
<html dir=ltr>

<head>
<style>
a:link			{font:8pt/11pt verdana; color:FF0000}
a:visited		{font:8pt/11pt verdana; color:#4e4e4e}
</style>

<META NAME="ROBOTS" CONTENT="NOINDEX">

<title>The page cannot be found</title>

<META HTTP-EQUIV="Content-Type" Content="text-html; charset=Windows-1252">
</head>

<script> 
function Homepage(){
<!--
// in real bits, urls get returned to our script like this:
// res://shdocvw.dll/http_404.htm#http://www.DocURL.com/bar.htm 

	//For testing use DocURL = "res://shdocvw.dll/http_404.htm#https://www.microsoft.com/bar.htm"
	DocURL = document.URL;
		
	//this is where the http or https will be, as found by searching for :// but skipping the res://
	protocolIndex=DocURL.indexOf("://",4);
	
	//this finds the ending slash for the domain server 
	serverIndex=DocURL.indexOf("/",protocolIndex + 3);

		//for the href, we need a valid URL to the domain. We search for the # symbol to find the begining 
	//of the true URL, and add 1 to skip it - this is the BeginURL value. We use serverIndex as the end marker.
	//urlresult=DocURL.substring(protocolIndex - 4,serverIndex);
	BeginURL=DocURL.indexOf("#",1) + 1;
	
	urlresult=DocURL.substring(BeginURL,serverIndex);
				
	//for display, we need to skip after http://, and go to the next slash
	displayresult=DocURL.substring(protocolIndex + 3 ,serverIndex);

	InsertElementAnchor(urlresult, displayresult);
}

function HtmlEncode(text)
{
    return text.replace(/&/g, '&amp').replace(/'/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function TagAttrib(name, value)
{
    return ' '+name+'="'+HtmlEncode(value)+'"';
}

function PrintTag(tagName, needCloseTag, attrib, inner){
    document.write( '<' + tagName + attrib + '>' + HtmlEncode(inner) );
    if (needCloseTag) document.write( '</' + tagName +'>' );
}

function URI(href)
{
    IEVer = window.navigator.appVersion;
    IEVer = IEVer.substr( IEVer.indexOf('MSIE') + 5, 3 );

    return (IEVer.charAt(1)=='.' && IEVer >= '5.5') ?
        encodeURI(href) :
        escape(href).replace(/%3A/g, ':').replace(/%3B/g, ';');
}

function InsertElementAnchor(href, text)
{
    PrintTag('A', true, TagAttrib('HREF', URI(href)), text);
}

//-->
</script>

<body bgcolor="FFFFFF">

<table width="410" cellpadding="3" cellspacing="5">

  <tr>    
    <td align="left" valign="middle" width="360">
	<h1 style="COLOR:000000; FONT: 13pt/15pt verdana"><!--Problem-->The page cannot be found</h1>
    </td>
  </tr>
  
  <tr>
    <td width="400" colspan="2">
	<font style="COLOR:000000; FONT: 8pt/11pt verdana">The page you are looking for might have been removed, had its name changed, or is temporarily unavailable.</font></td>
  </tr>
  
  <tr>
    <td width="400" colspan="2">
	<font style="COLOR:000000; FONT: 8pt/11pt verdana">

	<hr color="#C0C0C0" noshade>
	
    <p>Please try the following:</p>

	<ul>
      <li>If you typed the page address in the Address bar, make sure that it is spelled correctly.<br>
      </li>
	  
      <li>Open the 
	  
	  <script>
	  <!--
	  if (!((window.navigator.userAgent.indexOf("MSIE") > 0) && (window.navigator.appVersion.charAt(0) == "2")))
	  { 
	  	Homepage();
	  }
	  //-->
	   </script>

	   home page, and then look for links to the information you want.</li>
	   
      <li>Click the <a href="javascript:history.back(1)">Back</a> button to try another link.</li>
    </ul>
    
    <h2 style="font:8pt/11pt verdana; color:000000">HTTP 404 - File not found<br>
    Internet Information Services<BR></h2>
	 
	<hr color="#C0C0C0" noshade>
	
	<p>Technical Information (for support personnel)</p>
	
<ul>
<li>More information:<br>
<a href="http://www.microsoft.com/ContentRedirect.asp?prd=iis&sbp=&pver=5.0&pid=&ID=404&cat=web&os=&over=&hrd=&Opt1=&Opt2=&Opt3=" target="_blank">Microsoft Support</a>
</li>
</ul> 

    </font></td>
  </tr>
  
</table>
</body>
</html>

http://20040514.emu.dk/ 195.231.241.70 20040524210502 text/html 2725
HTTP/1.1 200 OK
Server: Microsoft-IIS/5.0
Date: Mon, 24 May 2004 21:05:31 GMT
X-Powered-By: ASP.NET
Pragma: no-cache
Expires: -1
Content-Length: 2411
Content-Type: text/html
Expires: Mon, 24 May 2004 21:04:31 GMT
Set-Cookie: ASPSESSIONIDQCSSDSCS=AKOPAJGDJENAJKBKNFEHKECF; path=/
Cache-control: no-store, no-cache

  <SCRIPT TYPE="text/javascript" SRC="includes/Dispatcher.js"></SCRIPT>
  <SCRIPT LANGUAGE="VBScript" SRC="includes/Dispatcher.vbs"></SCRIPT>
     <SCRIPT LANGUAGE="JavaScript">
   <!--
  	if (canPlayFlash("5.0", false) == 0) {
		document.location.href = "noflash.asp";
	}
	//-->
	</SCRIPT>
<HTML>
<HEAD>
<TITLE>Bryllup</TITLE>
<meta name="description" content="Send en hilsen til Frederik og Mary!">
<meta name="keywords" content="børn, bryllup, frederik, mary, kongeligt">
</HEAD>
<BODY bgcolor="#FFFFFF" scroll="no">
<table width="100%" height="100%"><tr><td width="100%" height="100%" align="center" valign="middle">
<OBJECT classid="clsid:D27CDB6E-AE6D-11cf-96B8-444553540000"
 codebase="http://download.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=6,0,0,0"
 WIDTH="100%" HEIGHT="100%" id="main" ALIGN="">
 <PARAM NAME=movie VALUE="main.swf?defaultPage="> <PARAM NAME=menu VALUE=false> <PARAM NAME=quality VALUE=high> <PARAM NAME=bgcolor VALUE=#FFFFFF> <EMBED src="main.swf?defaultPage=" menu=false quality=high bgcolor=#FFFFFF  WIDTH="100%" HEIGHT="100%" NAME="main" ALIGN=""
 TYPE="application/x-shockwave-flash" PLUGINSPAGE="http://www.macromedia.com/go/getflashplayer"></EMBED>
</OBJECT>
</td></tr></table>
<!-- START RedMeasure V4 - Java v1.1  Revision: 1.8 -->
<!-- COPYRIGHT 2000 Red Sheriff Limited -->
<script language="JavaScript"><!--
var pCid="dk_unic-dk_0";
var w0=1;
var refR=escape(document.referrer);
if (refR.length>=252) refR=refR.substring(0,252)+"...";
//--></script>
<script language="JavaScript1.1"><!--
var w0=0;
//--></script>

<script language="JavaScript1.1" src="http://server-dk.imrworldwide.com/a1.js">
</script>
<script language="JavaScript"><!--
if(w0){
var imgN='<img src="http://server-dk.imrworldwide.com/cgi-bin/count?ref='+
refR+'&cid='+pCid+'" width=1 height=1>';
if(navigator.userAgent.indexOf('Mac')!=-1){document.write(imgN);
}else{
document.write('<applet code="Measure.class" '+
'codebase="http://server-dk.imrworldwide.com/"'+'width=1 height=2>'+
'<param name="ref" value="'+refR+'">'+'<param name="cid" value="'+pCid+
'"><textflow>'+imgN+'</textflow></applet>');
}
}
document.write("<COMMENT>");
//-->
</script>
<noscript>
<img src="http://server-dk.imrworldwide.com/cgi-bin/count?cid=dk_unic-dk_0" width=1 height=1 />
</noscript>
</COMMENT>
<!-- END RedMeasure V4 -->
</BODY>
</HTML>


