/** Toggles the status of all checkboxes with a given class */
function toggleCheckboxes(command) {
    var toggler = document.getElementById("toggle" + command);
    if (toggler.checked) {
        var setOn = true;
    } else {
        var setOn = false;
    }
    var elements = document.getElementsByName(command);
    var maxToggle = document.getElementById("toggleAmount" + command).value;
    if (maxToggle <= 0) {
        maxToggle = elements.length;
    }
    for (var i = 0; i < elements.length && i < maxToggle; i++) {
        elements[i].checked = setOn;
    }
}
