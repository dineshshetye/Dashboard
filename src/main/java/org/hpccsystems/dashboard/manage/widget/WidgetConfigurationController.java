package org.hpccsystems.dashboard.manage.widget;

import org.apache.commons.lang.StringEscapeUtils;
import org.hpccsystems.dashboard.Constants;
import org.hpccsystems.dashboard.manage.WidgetConfiguration;
import org.hpccsystems.dashboard.service.AuthenticationService;
import org.hpccsystems.dashboard.service.CompositionService;
import org.hpccsystems.dashboard.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Include;
import com.google.gson.JsonObject;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class WidgetConfigurationController extends SelectorComposer<Component> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(WidgetConfigurationController.class);

    
    @Wire
    private Include holder;    
    
    @WireVariable
    private CompositionService compositionService;
    @WireVariable
    private DashboardService dashboardService;
    @WireVariable
    private AuthenticationService authenticationService;
    
    private WidgetConfiguration configuration;
    @Override
    public void doAfterCompose(Component comp) throws Exception {
        super.doAfterCompose(comp);
        configuration = 
                (WidgetConfiguration) Executions.getCurrent().getArg().get(Constants.WIDGET_CONFIG);
        configuration.setHolder(holder);
        
        holder.setDynamicProperty(Constants.WIDGET_CONFIG, configuration);
        holder.setSrc("widget/chartList.zul");
        holder.addEventListener(WidgetConfiguration.ON_CHART_TYPE_SELECT, event -> {
                holder.setSrc("widget/fileBrowser.zul");
        });
        
        holder.addEventListener(WidgetConfiguration.ON_FILE_SELECT, event -> {
            holder.setSrc(configuration.getWidget().getChartConfiguration().getEditLayout());
        });
    }
    
    
    /**
     * once chart is rendered, Hipie composition and plugin is created/updated
     */
    @Listen("onClick = #configOkButton")
    public void onClickOk() {
        this.getSelf().detach();
        if(configuration.getDashboard().getCompositionName() == null){
            createComposition();
        }else{
            addOnCompositionCharts();
        }
       
        drawChart();
        
    }

    /**
     * Updates composition while adding new additional charts to
     * dashboard 
     */
    private void addOnCompositionCharts() {
        String user=authenticationService.getUserCredential().getId();
        compositionService.updateComposition(configuration.getDashboard(), configuration.getWidget(),user);
        compositionService.runComposition(configuration.getDashboard(),user);
        dashboardService.updateDashboard(configuration.getDashboard(),
                authenticationService.getUserCredential().getId());
    }


    /**
     * Creates new composition while adding first chart
     */
    private void createComposition() {
        String user=authenticationService.getUserCredential().getId();
        compositionService.createComposition(configuration.getDashboard(),
                configuration.getWidget(),user);
        compositionService.runComposition(configuration.getDashboard(),user);
        dashboardService.updateDashboard(configuration.getDashboard(),
                authenticationService.getUserCredential().getId());
    }


    /**
     * Renders chart in dashboard container
     */
    //TODO:Need to render the new chart alone instead of creating all the charts in dashboard
    private void drawChart() {
        try {
           
            String viaualizationURL = configuration.getDashboard().generateVisualizationURL();
            if(LOGGER.isDebugEnabled()){
                LOGGER.debug("viaualizationURL -->"+viaualizationURL);
            }
            JsonObject chartObj = new JsonObject();
            chartObj.addProperty(Constants.URL, viaualizationURL);
            chartObj.addProperty(Constants.TARGET, configuration.getChartDiv().getUuid());
            String data = StringEscapeUtils.escapeJavaScript(chartObj.toString());
            Clients.evalJavaScript("visualizeDDLChart('"+ data+"')");
        } catch (Exception e) {
            LOGGER.error(Constants.EXCEPTION,e);
            Clients.showNotification("Unable to recreate chart",Clients.NOTIFICATION_TYPE_ERROR,configuration.getChartDiv(),"middle_center", 5000, true);
        }
    }
    

}
