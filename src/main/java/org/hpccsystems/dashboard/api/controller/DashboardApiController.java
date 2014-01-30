package org.hpccsystems.dashboard.api.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hpccsystems.dashboard.api.entity.ChartConfiguration;
import org.hpccsystems.dashboard.api.entity.Field;
import org.hpccsystems.dashboard.common.Constants;
import org.hpccsystems.dashboard.entity.Application;
import org.hpccsystems.dashboard.entity.ChartDetails;
import org.hpccsystems.dashboard.entity.Dashboard;
import org.hpccsystems.dashboard.entity.Portlet;
import org.hpccsystems.dashboard.entity.chart.XYChartData;
import org.hpccsystems.dashboard.entity.chart.utils.ChartRenderer;
import org.hpccsystems.dashboard.services.DashboardService;
import org.hpccsystems.dashboard.services.WidgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.zkoss.json.JSONArray;
import org.zkoss.json.JSONObject;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Controller to interact with Circuit application
 *
 */
@Controller
@RequestMapping("*.do")
public class DashboardApiController {	
	private static final  Log LOG = LogFactory.getLog(DashboardApiController.class); 
	DashboardService dashboardService;
	WidgetService widgetService;
	
	@Autowired
	public void setDashboardService(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}
	@Autowired
	public void setWidgetService(WidgetService widgetService) {
		this.widgetService = widgetService;
	}
	
/**
 * Method to process delete dashboard request from circuit
 * @param request
 * @param response
 */
@RequestMapping(value = Constants.CIRCUIT_DELETE_REQ , method = RequestMethod.GET)
public void deleteDashboard(HttpServletRequest request, HttpServletResponse response)throws Exception
{	
	JSONObject jsObj = new JSONObject();
	int rowsDeleted = 0;
	try { 
			String dashboardId = null;
			dashboardId = request.getParameter(Constants.DB_DASHBOARD_ID);	
			rowsDeleted = dashboardService.deleteDashboard(Integer.valueOf(dashboardId),null);	
			if(rowsDeleted > 0)	{
				jsObj.put(Constants.STATUS, Constants.STATUS_SUCCESS);
			}else{
				jsObj.put(Constants.STATUS, Constants.STATUS_FAIL);
				jsObj.put(Constants.STATUS_MESSAGE, Constants.DASHBOARD_NOT_EXIST);
			}
		}
	catch(Exception ex)
	{
		LOG.error("Exception while processing 'Delete Dashboard' request from Circuit",ex);
		jsObj.put(Constants.STATUS, Constants.STATUS_FAIL);
		jsObj.put(Constants.STATUS_MESSAGE, ex.getMessage());
	}
	response.setContentType(Constants.RES_TEXT_TYPE_JSON);
	response.setCharacterEncoding(Constants.CHAR_CODE);
	try
	{
	response.getWriter().write(jsObj.toJSONString());
	}catch(Exception ex)
	{
		LOG.error("Exception while writing JSON response to Circuit",ex);
		throw new Exception("Unable to process Delete Request");
	}
	
}

@RequestMapping(value = Constants.CIRCUIT_CHARTLIST_REQ , method = RequestMethod.GET)
public void getChartList(HttpServletRequest request, HttpServletResponse response)throws Exception
 {
		try {
			String paramValue = request.getParameter(Constants.CHARTLIST_FORMAT);			
			if (paramValue != null &&Constants.JSON .equals(paramValue)) {
				JsonObject json;
				JsonArray array = new JsonArray();
				Map<Integer, ChartDetails> chartdetailsMap = Constants.CHART_MAP;
				for (Map.Entry<Integer, ChartDetails> entry : chartdetailsMap.entrySet()) {
					json = new JsonObject();
					json.addProperty(Constants.VALUE, entry.getValue().getChartId());
					json.addProperty(Constants.LABEL, entry.getValue().getChartName());
					json.addProperty(Constants.DESCRIPTION, entry.getValue().getChartDesc());
					array.add(json);
				}
				JsonObject result = new JsonObject();
				result.add(Constants.CHART_LIST, array);
				response.setContentType(Constants.RES_TEXT_TYPE_JSON);
				response.getWriter().write(result.toString());
			} 
		} catch (Exception e) {
			LOG.error("Exception while processing ChartLsit Request from Circuit", e);
			throw new Exception("Unable to process ChartLsit Request");
		}

	}

@RequestMapping(value = Constants.CIRCUIT_SEARCH_REQ , method = RequestMethod.GET)
public void searchDashboard(HttpServletRequest request, HttpServletResponse response)throws Exception
 {
		try {
			final Application application = new Application();
			String sourceName = request.getParameter(Constants.SOURCE);
			String sourceId = request.getParameter(Constants.SOURCE_ID);
			application.setAppId(sourceId);			
			application.setAppName(sourceName);
			List<Dashboard> dashboardList = null;
			Dashboard dashBoard = null;

			JSONObject obj = null;
			JSONObject jsonResposeObj = new JSONObject();
			JSONArray jsonObjList = new JSONArray();
			try{
			dashboardList = new ArrayList<Dashboard>(dashboardService.retrieveDashboardMenuPages(application,null));
			}catch(Exception ex){
				LOG.error("Exception while fetching dahhboards from DB",ex);
				jsonResposeObj.put(Constants.STATUS_FAIL,ex.getMessage());
			}
			if (dashboardList != null) {
				for (final Iterator<Dashboard> iter = dashboardList.iterator(); iter.hasNext();) {
					dashBoard = (Dashboard) iter.next();
					obj = new JSONObject();
					obj.put(Constants.NAME_SMALL, dashBoard.getName());
					obj.put(Constants.DB_DASHBOARD_ID, dashBoard.getDashboardId());
					jsonObjList.add(obj);
				}
				if(jsonObjList.size() > 0){
					jsonResposeObj.put(Constants.DASHBOARDS, jsonObjList);
				}else{
					jsonResposeObj.put(Constants.DASHBOARDS,"No Dashboard Exists for the given Source" );
				}
			}			
			response.setContentType(Constants.RES_TEXT_TYPE_JSON);
			response.getWriter().write(jsonResposeObj.toString());
		} catch (Exception e) {
			LOG.error("Exception while processing Search dahhboard request from Circuit", e);
			throw new Exception("Unable to process Search Request");
		}
	}

/**
 * validateDashboard() is responsible for validate that the field list sent in contains all fields needed to render a dashboard. 
 * @param request
 * @param response
 * @throws IOException
 */
@RequestMapping(value = Constants.CIRCUIT_VALIDATE_REQ, method = RequestMethod.GET)
public void validateDashboard(HttpServletRequest request, HttpServletResponse response) throws Exception {

	 try {
			ChartRenderer chartRenderer = new ChartRenderer();
			XYChartData chartData = null;
			List<String> xColumnList = null;
			List<String> yColumnList = null;
			String filterColumn = null;
			Integer filterDataType = 0;
			ChartConfiguration chartConfiguration = null;
			JsonObject jsonObject = new JsonObject();
			List<String> failedValColumnList = new ArrayList<String>();
			String circuitFields = request.getParameter(Constants.CIRCUIT_CONFIG);
			String dashboard_id = request.getParameter(Constants.CIRCUIT_DASHBOARD_ID);
			chartConfiguration = new GsonBuilder().create().fromJson(circuitFields, ChartConfiguration.class);
			try {
				List<Portlet> portletList = widgetService.retriveWidgetDetails(Integer.valueOf(dashboard_id));
				for (Portlet portlet : portletList) {
						chartData = chartRenderer.parseXML(portlet.getChartDataXML());
						if (chartData != null) {
							xColumnList = chartData.getXColumnNames();
							yColumnList = chartData.getYColumnNames();
							// For XAxis & YAxis Validation
							failedValColumnList.addAll(xColumnValidation(failedValColumnList, xColumnList, chartConfiguration));
							failedValColumnList.addAll(yColumnValidation(failedValColumnList, yColumnList, chartConfiguration));
							//Filter Column Validation
							if (chartData.getIsFiltered()) {
								filterColumn = chartData.getFilter().getColumn();
								filterDataType = chartData.getFilter().getType();
								failedValColumnList.addAll(filterColumnValidation(failedValColumnList, filterColumn, filterDataType, chartConfiguration));
							}
						}
				}
			} catch (Exception ex) {
				LOG.error("Exception while processing 'Validate' request from Circuit",	ex);
				jsonObject.addProperty(Constants.STATUS, Constants.STATUS_FAIL);
				jsonObject.addProperty(Constants.STATUS_MESSAGE, ex.getMessage());
			}
			if (failedValColumnList.isEmpty()) {
				jsonObject.addProperty(Constants.STATUS, Constants.STATUS_SUCCESS);
			} else {
				jsonObject.addProperty(Constants.STATUS, Constants.STATUS_FAIL);
				for (String failedColumn : failedValColumnList) {
					jsonObject.addProperty(Constants.STATUS_MESSAGE, failedColumn + Constants.FIELD_NOT_EXIST);
				}
			}
			response.setContentType(Constants.RES_TEXT_TYPE_JSON);
			response.setCharacterEncoding(Constants.CHAR_CODE);
			response.getWriter().write(jsonObject.toString());
		} catch (Exception e) {
			LOG.error("Exception while processing Validate dashboard request from Circuit", e);
			throw new Exception("Unable to process Validate Request");
		}
	}

	/**
	 * xColumnValidation() is responsible for Validate the xColumn.
	 * @param failedColumnList
	 * @param xColumnList
	 * @param configuration
	 * @return
	 */
	private List<String> xColumnValidation(List<String> failedColumnList,
			List<String> xColumnList, ChartConfiguration configuration) {
		Boolean xAxisValStatus = false;
		for (String fieldValue : xColumnList) {
			for (Field entry : configuration.getFields()) {
				if (fieldValue.equals(entry.getColumnName().trim())) {
					xAxisValStatus = true;
					break;
				}
			}
			if (!xAxisValStatus && !failedColumnList.contains(fieldValue.trim())) {
				failedColumnList.add(fieldValue.trim());
			}
			xAxisValStatus = false;
		}
		return failedColumnList;
	}


	/**
	 * yColumnValidation() is responsible for Validate the yColumn.
	 * @param failedColumnList
	 * @param yColumnList
	 * @param configuration
	 * @return
	 */
	private List<String> yColumnValidation(List<String> failedColumnList, 
			List<String> yColumnList, ChartConfiguration configuration) {
		Boolean yAxisValStatus = false;
		for (String fieldValue : yColumnList) {
			for (Field entry : configuration.getFields()) {
				if (fieldValue.equals(entry.getColumnName().trim())
						&& Constants.NUMERIC_DATA == checkNumeric(entry.getDataType().trim())) {
					yAxisValStatus = true;
					break;
				}
			}
			if (!yAxisValStatus && !failedColumnList.contains(fieldValue.trim())) {
				failedColumnList.add(fieldValue.trim());
			}
			yAxisValStatus = false;
		}
		return failedColumnList;
	}

	/**
	 * filterColumnValidation() is responsible for Validate the filterColumn.
	 * @param failedColumnList
	 * @param filterColumn
	 * @param filterDataType
	 * @param configuration
	 * @return
	 */
	private List<String> filterColumnValidation(List<String> failedColumnList,
			String filterColumn, Integer filterDataType, ChartConfiguration configuration) {
		Boolean filterColumnValStatus = false;
		for (Field entry : configuration.getFields()) {
			if (filterColumn.equals(entry.getColumnName().trim())
					&& filterDataType == checkNumeric(entry.getDataType().trim())) {
				filterColumnValStatus = true;
				break;
			}
		}
		if (!filterColumnValStatus
				&& !failedColumnList.contains(filterColumn.trim())) {
			failedColumnList.add(filterColumn.trim());
		}
		return failedColumnList;
	}

	/**
	 * Checks whether a column is numeric.
	 * 
	 * @param column
	 * @param dataType
	 * @return
	 */
	private Integer checkNumeric(final String dataType) {
		Integer dataTypeValue = 0;
		if (dataType.contains("integer") || dataType.contains("real")
				|| dataType.contains("decimal")
				|| dataType.contains("unsigned")) {
			dataTypeValue = 1;
		}
		else if (dataType.contains("STRING") || dataType.contains("string")) {
			dataTypeValue = 2;
		}
		return dataTypeValue;
	}
}