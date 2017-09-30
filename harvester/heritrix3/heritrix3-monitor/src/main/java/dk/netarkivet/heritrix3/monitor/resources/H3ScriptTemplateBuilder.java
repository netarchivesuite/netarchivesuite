package dk.netarkivet.heritrix3.monitor.resources;

import com.antiaction.common.templateengine.TemplateBuilderPlaceHolder;
import com.antiaction.common.templateengine.TemplatePlaceHolder;

public class H3ScriptTemplateBuilder extends MasterTemplateBuilder {

    @TemplateBuilderPlaceHolder("script")
    public TemplatePlaceHolder scriptPlace;

    public MasterTemplateBuilder insertContent(String title, String menu, String languages, String heading, String script, String content, String refresh) {
    	super.insertContent(title, menu, languages, heading, content, refresh);
        if (scriptPlace != null) {
            scriptPlace.setText(script);
        }
        return this;
    }

}
