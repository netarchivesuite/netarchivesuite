/**
 * Displays the previous page of results (if available). This method works by
 * setting window.location, not by submitting a form, but it is dependent on
 * the parameter values set in the "filtersForm" form element on the calling
 * page - specifically the start-page index and the is_newest_first flag.
 * @param param a single parameter to be passed on to from the calling page
 * @param value the value of the parameter to be passed on from the calling page
 */
function previousPage(param,value) {
    document.filtersForm.START_PAGE_INDEX.value =
    	parseInt(document.filtersForm.START_PAGE_INDEX.value) - 1;
	link=document.filtersForm.action+"?"+"START_PAGE_INDEX="
		+ document.filtersForm.START_PAGE_INDEX.value;
    if (document.filtersForm.is_newest_first.value != null) {
        link+="&is_newest_first=" + document.filtersForm.is_newest_first.value;
    }
	if(value!="") {
		link=link+"&"+param	+"="+value;
	}
	window.location = link;
}

/**
 * Displays the previous page of results (if available). This method works by
 * setting window.location, not by submitting a form, but it is dependent on
 * the parameter values set in the "filtersForm" form element on the calling
 * page - specifically the start-page index and the is_newest_first flag.
 * @param param a single parameter to be passed on to from the calling page
 * @param value the value of the parameter to be passed on from the calling page
 */
function nextPage(param,value) {
    document.filtersForm.START_PAGE_INDEX.value =
    	parseInt(document.filtersForm.START_PAGE_INDEX.value) + 1;
 	link=document.filtersForm.action+"?"
                  + "START_PAGE_INDEX="+document.filtersForm.START_PAGE_INDEX.value;
    if (document.filtersForm.is_newest_first.value != null) {
        link+="&is_newest_first=" + document.filtersForm.is_newest_first.value;
    }
    if(value!="") {
		link=link+"&"+param	+"="+value;
	}
	window.location = link;

}

//reset to the first page
function resetPagination() {
    document.filtersForm.START_PAGE_INDEX.value = "1";
}