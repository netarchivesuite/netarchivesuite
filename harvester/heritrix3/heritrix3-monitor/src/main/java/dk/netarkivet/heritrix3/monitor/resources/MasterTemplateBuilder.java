package dk.netarkivet.heritrix3.monitor.resources;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import com.antiaction.common.templateengine.TemplateBuilderBase;
import com.antiaction.common.templateengine.TemplateBuilderPlaceHolder;
import com.antiaction.common.templateengine.TemplatePlaceHolder;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.Constants;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.webinterface.HTMLUtils;
import dk.netarkivet.heritrix3.monitor.Heritrix3JobMonitor;
import dk.netarkivet.heritrix3.monitor.NASEnvironment;

/**
 * Class to handle the generation of HTML using a template engine.
 * Extends a template engine class which uses some reflection magic.
 */
public class MasterTemplateBuilder extends TemplateBuilderBase {

	/** Get and cache the versions string from the jar manifest file. */
    protected final String version = Constants.getVersionString(true);

    /** Get and cache the environment name used for this installation. */
    protected final String environment = Settings.get(CommonSettings.ENVIRONMENT_NAME);

    /** Hook into the template to insert title content. */
	@TemplateBuilderPlaceHolder("title")
    public TemplatePlaceHolder titlePlace;

    /** Hook into the template to insert menu content. */
    @TemplateBuilderPlaceHolder("menu")
    public TemplatePlaceHolder menuPlace;

    /** Hook into the template to insert language selection content. */
    @TemplateBuilderPlaceHolder("languages")
    public TemplatePlaceHolder languagesPlace;

    /** Hook into the template to insert heading content. */
    @TemplateBuilderPlaceHolder("heading")
    public TemplatePlaceHolder headingPlace;

    /** Hook into the template to insert main content. */
    @TemplateBuilderPlaceHolder("content")
    public TemplatePlaceHolder contentPlace;

    /** Hook into the template to insert version content. */
    @TemplateBuilderPlaceHolder("version")
    public TemplatePlaceHolder versionPlace;

    /** Hook into the template to insert environment content. */
    @TemplateBuilderPlaceHolder("environment")
    public TemplatePlaceHolder environmentPlace;

    /** Hook into the template to insert refresh content. */
    @TemplateBuilderPlaceHolder("refresh")
    public TemplatePlaceHolder refreshPlace;

    /**
     * Construct menu HTML based on the HTTP request, requested locale and optional h3 job monitor.
     * 
     * @param menuSb <code>StringBuilder</code> used to construct the menu HTML
     * @param req HTTP request object
     * @param locale <code>Locale</code> of the requested response language to use
     * @param h3Job H3 job monitor to use if the menu need to show a job sub sub menu item
     * @return constructed menu HTML
     * @throws IOException if an I/O exception occurs during construction
     */
    public StringBuilder buildMenu(StringBuilder menuSb, HttpServletRequest req, Locale locale, Heritrix3JobMonitor h3Job) throws IOException {
    	String subMenu = null;
        if (h3Job != null) {
        	StringBuilder subMenuSb = new StringBuilder();
        	subMenuSb.append("<tr><td>&nbsp; &nbsp; &nbsp; <a href=\"");
        	subMenuSb.append(NASEnvironment.servicePath);
        	subMenuSb.append("job/");
            subMenuSb.append(h3Job.jobId);
            subMenuSb.append("/");
            subMenuSb.append("\"><b> Job ");
            subMenuSb.append(h3Job.jobId);
            subMenuSb.append("</b></a></td></tr>");
            subMenu = subMenuSb.toString();
        }
        HTMLUtils.generateNavigationTree(menuSb, req, req.getRequestURL().toString(), subMenu, locale);
        return menuSb;
    }

    /**
     * Insert content into the different placeholders defined as template engine attributes above.
     * 
     * @param title title text
     * @param menu menu HTML
     * @param languages language selection HTML
     * @param heading heading HTML
     * @param content main content text
     * @param refresh refresh meta header text
     * @return a reference to this object so more methods can be called on it immediately
     */
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
