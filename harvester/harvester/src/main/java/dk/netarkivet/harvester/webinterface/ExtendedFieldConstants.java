package dk.netarkivet.harvester.webinterface;

/** 
 * Constants primarily used by class ExtendedFieldDefinition and 
 * the jsp pages with extendedField functionality.
 * That is:
 * HarvestDefinition/Definitions-edit-extendedfield.jsp
 * HarvestDefinition/Definitions-list-extendedfields.jsp
 * HarvestDefinition/Definitions-edit-domain.jsp 
 * 
 */
public class ExtendedFieldConstants {

    public static final String EXTF_ACTION = "extf_action";
    public static final String EXTF_ACTION_CREATE = "create";
    public static final String EXTF_ACTION_READ = "read";
    public static final String EXTF_ACTION_DELETE = "delete";
    public static final String EXTF_ACTION_SUBMIT = "submit";
    
    public static final String EXTF_ID = "extf_id";
    public static final String EXTF_TYPE_ID = "exf_type_id";
    public static final String EXTF_NAME = "extf_name";
    public static final String EXTF_FORMAT = "extf_format";
    public static final String EXTF_FORMAT_JSCALENDAR = "extf_format_jscalendar";
    public static final String EXTF_DATATYPE = "extf_datatype";
    public static final String EXTF_MANDATORY = "extf_mandatory";
    public static final String EXTF_SEQUENCENR = "extf_sequencenr";
    public static final String EXTF_DEFAULTVALUE_TEXTFIELD = "extf_defaultvalue_textfield";
    public static final String EXTF_DEFAULTVALUE_TEXTAREA = "extf_defaultvalue_textarea";
    public static final String EXTF_DEFAULTVALUE_CHECKBOX = "extf_defaultvalue_checkbox";
    public static final String EXTF_OPTIONS = "extf_options";
    public static final String EXTF_MAXLEN = "extf_maxlen";
    
    public static final String EXTF_PREFIX = "extf_";
    public static final String EXTF_ALLFIELDIDS = "extf_allfieldids";
    
    public static final String TRUE = "1";
    public static final String FALSE = "0";
 
    public static final int MAXLEN_EXTF_BOOLEAN = 1;
    public static final int MAXLEN_EXTF_NAME = 50;
    public static final int MAXLEN_EXTF_FORMAT = 50;
    public static final int MAXLEN_EXTF_DEFAULTVALUE = 50;
    public static final int MAXLEN_EXTF_OPTIONS = 1000;
    
    public static final int MAXLEN_EXTF_CONTENT = 30000;
    
    
}
