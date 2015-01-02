package org.hpccsystems.dashboard.manage.widget;

import org.apache.commons.lang.StringEscapeUtils;
import org.hpccsystems.dashboard.Constants;
import org.hpccsystems.dashboard.entity.widget.Attribute;
import org.hpccsystems.dashboard.entity.widget.ChartdataJSON;
import org.hpccsystems.dashboard.entity.widget.Field;
import org.hpccsystems.dashboard.entity.widget.Measure;
import org.hpccsystems.dashboard.entity.widget.charts.XYChart;
import org.hpccsystems.dashboard.service.WSSQLService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Button;
import org.zkoss.zul.Div;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.ListitemRenderer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@VariableResolver(org.zkoss.zkplus.spring.DelegatingVariableResolver.class)
public class XYChartController extends ConfigurationComposer<Component> {
    private static final String ON_LOADING = "onLoading";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(XYChartController.class);
    
    private XYChart xyChart;
    
    @Wire
    private Listbox chartMeasureListbox;
    private ListModelList<Measure> measures = new ListModelList<Measure>();
    
    @Wire
    private Listbox chartAttributeListbox;
    private ListModelList<Attribute> attributes = new ListModelList<Attribute>();
    
    @WireVariable
    private WSSQLService wssqlService;
    
    @Wire
    private Div chart;
    
    private ListitemRenderer<Measure> chartMeasureRenderer = (listitem, measure, index) -> {
        Listcell listItemCell=new Listcell();
        listItemCell.setLabel(measure.getColumn());
        listItemCell.setParent(listitem);
        Button closeButton=new Button();
        closeButton.setParent(listItemCell);
        closeButton.setIconSclass("z-icon-times");
        listitem.appendChild(listItemCell);
        closeButton.addEventListener("onClick", event -> {
            measures.remove(measure);    
            xyChart.removeMeasure(measure);
        });
    };
    private ListitemRenderer<Attribute> chartAttributeRenderer = (listitem, attribute, index) -> {
        Listcell listItemCell=new Listcell();
        listItemCell.setLabel(attribute.getColumn());
        listItemCell.setParent(listitem);
        Button closeButton=new Button();
        closeButton.setParent(listItemCell);
        closeButton.setIconSclass("z-icon-times");
        listitem.appendChild(listItemCell);
        closeButton.addEventListener("onClick", event -> {
            attributes.remove(attribute);    
            xyChart.setAttribute(null);
            chartAttributeListbox.setDroppable(Constants.TRUE);
        });
    };
    
    
    @Override
    public void doAfterCompose(final Component comp) throws Exception {
        super.doAfterCompose(comp);
        xyChart = (XYChart) widgetConfiguration.getWidget();
        hpccConnection = widgetConfiguration.getDashboard().getHpccConnection();
        comp.addEventListener(ON_LOADING, loadingListener);
        
        Clients.showBusy(comp, "Fetching fields");
        Events.echoEvent(ON_LOADING, comp, null);
        
        chartMeasureListbox.setModel(measures);
        chartMeasureListbox.setItemRenderer(chartMeasureRenderer);
        chartAttributeListbox.setModel(attributes);
        chartAttributeListbox.setItemRenderer(chartAttributeRenderer);
        if(xyChart.isConfigured()){
            measures.addAll(xyChart.getMeasures());
            attributes.add(xyChart.getAttribute());
            drawChart();
        }
    }    
    
    @Listen("onDrop = #chartMeasureListbox")
    public void onDropWeight(DropEvent event) {
        Listitem draggedItem = (Listitem) event.getDragged();
        Field field = draggedItem.getValue();
        Measure measure = new Measure(field);
        if(event.getDragged().getParent().equals(attributeListbox)){
            Clients.showNotification("Only measure objects can be dropped","warning",chartMeasureListbox,"end_center", 5000, true);
        }else{
            xyChart.addMeasure(measure);
            measures.add(measure);
        }
        if(xyChart.isConfigured()) {            
            try {
                drawChart();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    @Listen("onDrop = #chartAttributeListbox")
    public void onDropLabel(DropEvent event) {
        Listitem draggedItem = (Listitem) event.getDragged();
        Field field = draggedItem.getValue();
        Attribute attribute = new Attribute(field);
        xyChart.setAttribute(attribute);
        attributes.add(attribute);
        chartAttributeListbox.setDroppable(Constants.FALSE);
        if(xyChart.isConfigured()) {            
            try {
                drawChart();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    private void drawChart() throws Exception {
        ChartdataJSON chartData = wssqlService.getChartdata(xyChart, hpccConnection);
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Div id -{}\nJSON - {}", chart.getUuid(), new GsonBuilder().setPrettyPrinting().create().toJson(chartData));
        }
        Clients.evalJavaScript("createPreview('"+ chart.getUuid()+"','xyChart','"+ StringEscapeUtils.escapeJavaScript(new Gson().toJson(chartData))+"')");
    }

    
}

