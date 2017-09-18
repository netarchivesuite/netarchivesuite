package dk.netarkivet.heritrix3.monitor.resources;

import com.antiaction.common.templateengine.TemplateBuilderPlaceHolder;
import com.antiaction.common.templateengine.TemplatePlaceHolder;

public class ConfigTemplateBuilder extends MasterTemplateBuilder {

    @TemplateBuilderPlaceHolder("enabledhosts")
    public TemplatePlaceHolder enabledhostsPlace;

}
