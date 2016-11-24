package dk.netarkivet.harvester.webinterface.servlet;

import com.antiaction.common.templateengine.TemplateBuilderBase;
import com.antiaction.common.templateengine.TemplateBuilderPlaceHolder;
import com.antiaction.common.templateengine.TemplatePlaceHolder;

public class MasterTemplateBuilder extends TemplateBuilderBase {

    @TemplateBuilderPlaceHolder("title")
    public TemplatePlaceHolder titlePlace;

    @TemplateBuilderPlaceHolder("menu")
    public TemplatePlaceHolder menuPlace;

    @TemplateBuilderPlaceHolder("languages")
    public TemplatePlaceHolder languagesPlace;

    @TemplateBuilderPlaceHolder("heading")
    public TemplatePlaceHolder headingPlace;

    @TemplateBuilderPlaceHolder("content")
    public TemplatePlaceHolder contentPlace;

    @TemplateBuilderPlaceHolder("version")
    public TemplatePlaceHolder versionPlace;

    @TemplateBuilderPlaceHolder("environment")
    public TemplatePlaceHolder environmentPlace;

}
