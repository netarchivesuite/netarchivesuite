// Any time an element with the class of "tagline" is clicked
$(".tagline").click(function() {
    alert("jQuery works!");
});

$(document).ready(function() {
    $('#tabs').tabs({
      load: function( event, ui ) {
          if(event.currentTarget != null && event.currentTarget.firstChild.textContent === "Status") {
              $('#status_table').DataTable( {
                  ajax: {
                      url: '../rest/rest/status/all',
                      dataSrc: ''
                  },
                  columns: [
                      { data: 'physicalLocation' },
                      { data: 'applicationName' },
                      { data: 'logMessage'}
                  ]
              } );
          }
      }
    });


} );