package dk.netarkivet.heritrix3.monitor.resources;

import com.antiaction.common.templateengine.TemplateBuilderBase;
import com.antiaction.common.templateengine.TemplateBuilderPlaceHolder;
import com.antiaction.common.templateengine.TemplatePlaceHolder;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.utils.Settings;

public class MasterTemplateBuilder extends TemplateBuilderBase {

    protected final String version = Constants.getVersionString(true);

    protected final String environment = Settings.get(CommonSettings.ENVIRONMENT_NAME);

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
    
    @TemplateBuilderPlaceHolder("refresh")
    public TemplatePlaceHolder refreshPlace;

    public MasterTemplateBuilder insertContent(String title, String menu, String languages, String heading, String content, String refresh) {
        if (titlePlace != null) {
            titlePlace.setText(title);
        }
        if (menuPlace != null) {
            menuPlace.setText(menu);
        }
        if (languagesPlace != null) {
            languagesPlace.setText(languages);
        }
        if (headingPlace != null) {
            headingPlace.setText(heading);
        }
        if (contentPlace != null) {
            contentPlace.setText(content);
        }
        if (versionPlace != null) {
            versionPlace.setText(version);
        }
        if (environmentPlace != null) {
            environmentPlace.setText(environment);
        }
        if (refreshPlace != null) {
        	refreshPlace.setText(refresh);
        }
        return this;
    }

}
