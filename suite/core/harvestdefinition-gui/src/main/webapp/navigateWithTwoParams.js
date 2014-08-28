//Displays the previous page of results (if available).
function previousPage(param,value, param1, value1) {
    document.filtersForm.START_PAGE_INDEX.value = 
    parseInt(document.filtersForm.START_PAGE_INDEX.value) - 1;
	link=document.filtersForm.action+"?"
	+"START_PAGE_INDEX="
	+document.filtersForm.START_PAGE_INDEX.value;
	if(value!=""){
		link=link+"&"+param	+"="+value;	
	}
	if(value1!=""){
		link=link+"&"+param1 +"="+value1;
        }

	window.location = link;

}

//Displays the next page of results (if available).
function nextPage(param,value, param1, value1) {
    document.filtersForm.START_PAGE_INDEX.value = 
    parseInt(document.filtersForm.START_PAGE_INDEX.value) + 1;
 	link=document.filtersForm.action+"?"
	+"START_PAGE_INDEX="
	+document.filtersForm.START_PAGE_INDEX.value;
	if(value!=""){
		link=link+"&"+param +"="+value;
	}
	if (value1!=""){
		link=link+"&"+param1 +"="+value1;
	}

	window.location = link;
}


//reset to the first page
function resetPagination() {
    document.filtersForm.START_PAGE_INDEX.value = "1";
}
