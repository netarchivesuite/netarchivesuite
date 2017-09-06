// Any time an element with the class of "tagline" is clicked
$(".tagline").click(function() {
    alert("jQuery works!");
});

$(document).ready(function() {
    $('#tabs').tabs({
      load: function( event, ui ) {
          var active = $('#tabs').tabs('option', 'active');
          if(event.currentTarget != null && event.currentTarget.firstChild.textContent === "Status") {
              $('#status_table').DataTable( {
                  ajax: {
                      url: 'http://localhost:8074/rest/rest/status/all',
                      dataSrc: ''
                  },
                  columns: [
                      { data: 'physicalLocation' },
                      { data: 'applicationName' },
                      { data: 'logMessage'}
                  ]
              } );
          }
          //$("#tabid").html().eq(active).attr("href"));
      }
    });


} );