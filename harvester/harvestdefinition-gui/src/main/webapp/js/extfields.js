        function tooglefields(selection) {
            document.getElementById("toggle_textarea").style.display = 'none';
            document.getElementById("toggle_checkbox").style.display = 'none';
            document.getElementById("toggle_textfield").style.display = 'block';
            document.getElementById("toggle_maxlen").style.display = 'block';
            document.getElementById("toggle_options").style.display = 'none';
            document.getElementById("toggle_format").style.display = 'none';
            document.getElementById("toggle_mandatory").style.display = 'block';
            document.getElementById("toggle_jscalendar").style.display = 'none';
            
            if (selection == 3 ||	// ExtendedFieldDataTypes.NUMBER
                selection == 4) {		// ExtendedFieldDataTypes.TIMESTAMP
                document.getElementById("toggle_format").style.display = 'block';
            }

            if (selection == 7) {	// ExtendedFieldDataTypes.JSCALENDAR
                document.getElementById("toggle_jscalendar").style.display = 'block';
                document.getElementById("toggle_maxlen").style.display = 'none';
            }
            
            if (selection == 6) {	// ExtendedFieldDataTypes.SELECT
                document.getElementById("toggle_options").style.display = 'block';
                document.getElementById("toggle_maxlen").style.display = 'none';
                }
            
            if (selection == 5) {	// ExtendedFieldDataTypes.NOTE
                document.getElementById("toggle_textarea").style.display = 'block';
                document.getElementById("toggle_checkbox").style.display = 'none';
                document.getElementById("toggle_textfield").style.display = 'none';
            }                            

            if (selection == 2) {	// ExtendedFieldDataTypes.BOOLEAN
                document.getElementById("toggle_maxlen").style.display = 'none';
                document.getElementById("toggle_textarea").style.display = 'none';
                document.getElementById("toggle_checkbox").style.display = 'block';
                document.getElementById("toggle_textfield").style.display = 'none';
                document.getElementById("toggle_mandatory").style.display = 'none';
            }
            
        }
