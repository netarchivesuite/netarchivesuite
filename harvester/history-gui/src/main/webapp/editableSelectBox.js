//EDITABLE HTML SELECT BOX SCRIPT
//Author: Teo Cerovski
//CommentsTo: teoDOTcerovskiATgmailDOTcom
/*
Include this script to your web page.
example: <SCRIPT language="javascript" type="text/javascript" src="editableSelectBox.js"></SCRIPT>

Put event attributes onClick="beginEditing(this);" and onBlur="finishEditing();" to your select box.
example: <SELECT name="foo" id="foo" onClick="beginEditing(this);" onBlur="finishEditing();">...</SELECT>

When editing is finished option values are overwriten with text values, so only text of the selected option will be posted.
*/


var o = null;
var isNN = (navigator.appName.indexOf("Netscape")!=-1);
var selectedIndex = 0;
var pointer = "|";
var blinkDelay = null;
var pos = 0;

function beginEditing(menu) {
	finishEditing();
	if(menu.selectedIndex > -1 && menu[menu.selectedIndex].value != "read-only") {
		o = new Object();
		o.editOption = menu[menu.selectedIndex];
		o.editOption.old = o.editOption.text;
		o.editOption.text += pointer;
		selectedIndex = menu.selectedIndex;
		if(navigator.userAgent.toLowerCase().indexOf("msie") != -1) //user is using IE
			document.onkeydown = keyPressHandler;
		else
			document.onkeypress = keyPressHandler;
	pos = o.editOption.text.indexOf(pointer);
	blinkDelay = setTimeout("blinkPointer()", 300);
	}

	function keyPressHandler(e){
		stopBlinking();
		menu.selectedIndex = selectedIndex;
		var option = o.editOption;	
		var keyCode = (window.event) ? event.keyCode : e.keyCode;
		var specialKey = true;
		if(keyCode == 0){
			keyCode = (isNN) ? e.which : event.keyCode;
			specialKey = false;
		}		
		
		if(keyCode == 16)
			return false;
		else if(keyCode == 116 && specialKey){
			finishEditing();
			window.location.reload(true);
		}
		else if(keyCode == 8)
			option.text = option.text.substring(0,option.text.indexOf(pointer)-1) + pointer + option.text.substring(option.text.indexOf(pointer)+1,option.text.length);
		else if(keyCode == 46  && option.text.indexOf(pointer) < option.text.length)
			option.text = option.text.substring(0,option.text.indexOf(pointer)) + pointer + option.text.substring(option.text.indexOf(pointer)+2,option.text.length);
		else if (keyCode == 13) 
			finishEditing();
		else if(keyCode == 37 && option.text.indexOf(pointer) > 0 && specialKey)
			option.text = option.text.substring(0,option.text.indexOf(pointer)-1) + pointer + option.text.substring(option.text.indexOf(pointer)-1,option.text.indexOf(pointer)) + option.text.substring(option.text.indexOf(pointer)+1,option.text.length);
		else if(keyCode == 39 && option.text.indexOf(pointer) < option.text.length && specialKey)
			option.text = option.text.substring(0,option.text.indexOf(pointer)) + option.text.substring(option.text.indexOf(pointer)+1,option.text.indexOf(pointer)+2) + pointer + option.text.substring(option.text.indexOf(pointer)+2,option.text.length);
		else if(((keyCode == 37 && option.text.indexOf(pointer) <= 0) || (keyCode == 39 && option.text.indexOf(pointer) >= option.text.length) || keyCode == 40 || keyCode == 38 || keyCode == 20 || keyCode == 33 || keyCode == 34) && specialKey){
			//do nothing
		}else if(keyCode == 36 && specialKey)
			option.text = pointer + option.text.substring(0,option.text.indexOf(pointer)) + option.text.substring(option.text.indexOf(pointer)+1,option.text.length);
		else if(keyCode == 35 && specialKey)
			option.text = option.text.substring(0,option.text.indexOf(pointer)) + option.text.substring(option.text.indexOf(pointer)+1,option.text.length) + pointer;
		else
			option.text = option.text.substring(0,option.text.indexOf(pointer)) + String.fromCharCode(keyCode) + pointer + option.text.substring(option.text.indexOf(pointer)+1,option.text.length);
		
		pos = option.text.indexOf(pointer);
		blinkDelay = setTimeout("blinkPointer()", 300);
		
		if(!((keyCode >= 48 && keyCode <= 90) || (keyCode >= 96 && keyCode <= 122)))
			return false;
			
	}
	
}

function blinkPointer(){
	if(o == null)
		return;
	pos = o.editOption.text.indexOf(pointer);
	o.editOption.text = o.editOption.text = o.editOption.text.substring(0,o.editOption.text.indexOf(pointer)) + "." + o.editOption.text.substring(o.editOption.text.indexOf(pointer)+1,o.editOption.text.length)
	blinkDelay = setTimeout("blinkPointer2()", 300);	
}
	
function blinkPointer2(){
	o.editOption.text = o.editOption.text = o.editOption.text.substring(0,pos) + pointer + o.editOption.text.substring(pos+1,o.editOption.text.length)
	blinkDelay = setTimeout("blinkPointer()", 300);
}

function stopBlinking(){
	clearTimeout(blinkDelay);
	if(o.editOption.text.charAt(pos) != pointer)
		o.editOption.text = o.editOption.text = o.editOption.text.substring(0,pos) + pointer + o.editOption.text.substring(pos+1,o.editOption.text.length)
}

function finishEditing() {
	if(o != null) { 		
		stopBlinking();
		option = o.editOption;
		option.text = option.text.substring(0,option.text.indexOf(pointer)) + option.text.substring(option.text.indexOf(pointer)+1,option.text.length);
			
		option.value = option.text;	
		document.onkeypress = null;
		document.onkeydown = null;
		o = null;
	}
}