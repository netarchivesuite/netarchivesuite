/**
 * Displays the previous page of results (if available). This method works by
 * setting window.location, not by submitting a form, but it is dependent on
 * the parameter values set in the "filtersForm" form element on the calling
 * page - specifically the start-page index.
 * @param param1 a  parameter to be passed on to from the calling page.
 * Same for following parameter.
 * @param value1 the value of the parameter to be passed on from the calling page
 */
function previousPage(param1,value1,param2,value2,param3,value3) {
    document.filtersForm.START_PAGE_INDEX.value =
        parseInt(document.filtersForm.START_PAGE_INDEX.value) - 1;
    link=document.filtersForm.action+"?"+"START_PAGE_INDEX="
        + document.filtersForm.START_PAGE_INDEX.value;
    if(value1!="") {
        link=link+"&"+param1+"="+value1;
    }
    if(value2!="") {
        link=link+"&"+param2+"="+value2;
    }
    if(value3!="") {
        link=link+"&"+param3+"="+value3;
    }
    window.location = link;
}

/**
 * Displays the next page of results (if available). This method works by
 * setting window.location, not by submitting a form, but it is dependent on
 * the parameter values set in the "filtersForm" form element on the calling
 * page - specifically the start-page index.
 * @param param1 a parameter to be passed on to from the calling page.
 * Same for following parameter.
 * @param value1 the value of the parameter to be passed on from the calling page
 */
function nextPage(param1,value1,param2,value2,param3,value3) {
    document.filtersForm.START_PAGE_INDEX.value =
        parseInt(document.filtersForm.START_PAGE_INDEX.value) + 1;
    link=document.filtersForm.action+"?"
        + "START_PAGE_INDEX="+document.filtersForm.START_PAGE_INDEX.value;

    if(value1!="") {
        link=link+"&"+param1+"="+value1;
    }
    if(value2!="") {
        link=link+"&"+param2+"="+value2;
    }
    if(value3!="") {
        link=link+"&"+param3+"="+value3;
    }
    window.location = link;
}

//reset to the first page
function resetPagination() {
    document.filtersForm.START_PAGE_INDEX.value = "1";
}