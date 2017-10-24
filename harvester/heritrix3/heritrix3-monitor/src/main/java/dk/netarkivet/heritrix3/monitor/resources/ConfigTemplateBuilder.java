package dk.netarkivet.heritrix3.monitor.resources;

import com.antiaction.common.templateengine.TemplateBuilderPlaceHolder;
import com.antiaction.common.templateengine.TemplatePlaceHolder;

public class ConfigTemplateBuilder extends MasterTemplateBuilder {

    @TemplateBuilderPlaceHolder("enabledhosts")
    public TemplatePlaceHolder enabledhostsPlace;

    public MasterTemplateBuilder insertContent(String title, String menu, String languages, String heading, String enableHosts, String content, String refresh) {
    	super.insertContent(title, menu, languages, heading, content, refresh);
        if (enabledhostsPlace != null) {
        	enabledhostsPlace.setText(enableHosts);
        }
        return this;
    }

}
