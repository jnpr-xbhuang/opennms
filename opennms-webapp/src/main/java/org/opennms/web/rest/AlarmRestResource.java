package org.opennms.web.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;
import javax.ws.rs.core.Context;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.opennms.api.reporting.ReportFormat;
import org.opennms.netmgt.dao.api.AlarmRepository;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.reporting.core.svclayer.ReportWrapperService;
import org.opennms.web.alarm.AcknowledgeType;
import org.opennms.web.alarm.AlarmUtil;
import org.opennms.web.alarm.SortStyle;
import org.opennms.web.alarm.filter.AlarmCriteria;
import org.opennms.web.event.Event;
import org.opennms.web.event.EventUtil;
import org.opennms.web.event.WebEventRepository;
import org.opennms.web.event.filter.EventCriteria;
import org.opennms.web.filter.Filter;
import org.xml.sax.InputSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlarmRestResource {
	  private static final Logger LOG = LoggerFactory.getLogger(AlarmRestResource.class);
	
	@Context
    ServletContext m_servletContext;
	
    /**
   	 * OpenNMS alarm repository
   	 */
	private static AlarmRepository m_webAlarmRepository;
	
	/**
	 * OpenNMS event repository
	 */
	private static WebEventRepository m_webEventRepository;
	
	/**
     * OpenNMS report wrapper service
     */
    private static ReportWrapperService m_reportWrapperService;
	
	/**
	 * OpenNMS alarm default acknowledge type
	 */
	private org.opennms.web.alarm.AcknowledgeType m_defaultAlarmAcknowledgeType = org.opennms.web.alarm.AcknowledgeType.UNACKNOWLEDGED;
	
	/**
	 * OpenNMS event default acknowledge type
	 */
	private org.opennms.web.event.AcknowledgeType m_defaultEventAcknowledgeType = org.opennms.web.event.AcknowledgeType.UNACKNOWLEDGED;
	
	/**
	 * OpenNMS alarm default sort style
	 */
	private org.opennms.web.alarm.SortStyle m_defaultAlarmSortStyle = org.opennms.web.alarm.SortStyle.ID;
	
	/**
	 * OpenNMS alarm default sort style
	 */
	private org.opennms.web.event.SortStyle m_defaultEventSortStyle = org.opennms.web.event.SortStyle.ID;
	
    /**
     * Location of alarm report
     */
    private String baseDir = System.getProperty("opennms.alarm.report.dir");
	
    /**
   	 * Convert alarm string XMl to object
   	 * 
   	 * @param alarmBeanString a {@link java.lang.String} object.
   	 * @return a {@link java.lang.Object} object.
   	 */
    public Object convertToJaxb(String alarmBeanString) {
		AlarmBean result = null;
		try {
			JAXBContext jc = JAXBContext.newInstance(AlarmBean.class);
			Unmarshaller u = jc.createUnmarshaller();
			InputSource inputSource = new InputSource(new StringReader(alarmBeanString));
			result = (AlarmBean) u.unmarshal(inputSource);
		} catch (Exception ex) {
			ex.printStackTrace();
			LOG.error("Unable to unmarshal of alarm bean xml string because of " + ex);
		}
		return result;
	}
    
    /**
     * Compressed the alarm report folder
     * 
     * @param folderName a {@link java.lang.String} object.
     * @throws IOException 
     */
    public void compressedReportFolder(String folderName) throws IOException{
    	
    	byte[] buffer = new byte[1024];
    	ZipOutputStream zipOutputStream = null;
    	FileInputStream fileInputStream = null;
    	try{
    		
    		zipOutputStream = new ZipOutputStream(new FileOutputStream(baseDir+"/"+folderName+".zip"));
    		File folder = new File(baseDir+"/"+folderName+"/");
    		
			for (File file : folder.listFiles()) {
				if (file.isFile()) {
					zipOutputStream.putNextEntry(new ZipEntry(file.getName()));
					fileInputStream = new FileInputStream(baseDir+"/"+folderName+"/"+file.getName());
					 
		    		int len;
		    		while ((len = fileInputStream.read(buffer)) > 0) {
		    			zipOutputStream.write(buffer, 0, len);
		    		}
		    		fileInputStream.close();
				}
			}
    		//remember close it
			zipOutputStream.closeEntry();
			zipOutputStream.close();
    	} catch(IOException ex) {
    	   ex.printStackTrace();
    	   LOG.error("Unable to compressed the alarm report folder");
    	} finally {
    		if(fileInputStream != null){
    			fileInputStream.close();
    		}
    		if(zipOutputStream != null){
    			zipOutputStream.close();
    		}
    	}
    }
    
    /**
     * Delete the alarm report folder from local server
     * 
     * @param folderName a {@link java.lang.String} object.
     */
    public void deleteReportFolderAfterCompressed(String folderName){
    	
    	// Handle the compressed files
    	try {
			compressedReportFolder(folderName);
		} catch (IOException ex) {
			ex.printStackTrace();
			LOG.error("Unable to compressed the alarm report folder");
		}
    	
    	File tempFolderDir = new File(baseDir+"/"+folderName);
    	
    	if(new File(tempFolderDir+".zip").exists()){
			if(tempFolderDir.exists()){
				try {
					FileUtils.deleteDirectory(tempFolderDir);
				} catch (IOException e) {
					LOG.error("Unable to delete the folder name ="+folderName+" from the alarm report location");
					e.printStackTrace();
				}
			} else {
				LOG.error("The folder name ="+folderName+" is not availabe in the alarm report location");
			}
		} else {
			LOG.error("Unable to compress the folder name ="+folderName+" from the alarm report location");
		}
    	
    }
    
    /**
   	 * Split the alarm list by it's limit
   	 * 
   	 * @param alarmFilters an array of {@link org.opennms.web.filter.Filter} object.
   	 * @param sortStyleString a {@link java.lang.String} object.
   	 * @param ackTypeString a {@link java.lang.String} object.
   	 * @param purgeLimit an int type.
   	 * @return an int type
   	 */
    public int splitAlarmListByLimit(Filter[] alarmFilters, String sortStyleString, String ackTypeString, int purgeLimit){
    	
    	// Handle the acknowledge type parameter
        org.opennms.web.alarm.AcknowledgeType alarmAckType = m_defaultAlarmAcknowledgeType;
        
        if (ackTypeString != null) {
        	try{
		        alarmAckType = AcknowledgeType.getAcknowledgeType(ackTypeString);
	        } catch (Exception e) {
				LOG.error("Could not retrieve acknowledge type for this "+ackTypeString);
			}
        }
        
        // Handle the sortStyle type parameter
        org.opennms.web.alarm.SortStyle alarmSortStyle = m_defaultAlarmSortStyle;
        
        if (sortStyleString != null) {
        	try{
        		alarmSortStyle = SortStyle.getSortStyle(sortStyleString);
	        } catch (Exception e) {
				LOG.error("Could not retrieve sortStyle type for this "+sortStyleString);
			}
        }
        
    	boolean isAlarmAvailable = true;
        int alarmOffset = 0;
        List<OnmsAlarm> alarmList = new ArrayList<OnmsAlarm>();
        
    	try{
        	while(isAlarmAvailable){

        		AlarmCriteria alarmQueryCriteria = new AlarmCriteria(alarmFilters, alarmSortStyle, alarmAckType, purgeLimit, purgeLimit * alarmOffset);
	        	OnmsAlarm[] alarms = m_webAlarmRepository.getMatchingAlarms(AlarmUtil.getOnmsCriteria(alarmQueryCriteria));
		        
	        	if(alarms.length>0){
	        		for(OnmsAlarm alarm : alarms){
	        			alarmList.add(alarm);
	        		}
	        		
	        		int eventStatus = splitEventListByLimit(alarmList,purgeLimit);
	        		if(eventStatus == 1){
	        			LOG.info("The purge all action is successfully completed for the alarm list "+alarmList);
	        		} else {
	        			LOG.error("Unable to do purge all action for this alarm list "+alarmList);
	        			return 0;
	        		}
	        		
	        	} else {
	        		isAlarmAvailable = false;
	        	}
	        	alarmOffset++;
        	}
		} catch(final Exception ex){
			ex.printStackTrace();
    	    LOG.error("Unable to do purge all action for this alarm list "+alarmList);
    	    return 0;
		}
    	return 1;
    }
    
    /**
   	 * Split the event list by it's limit
   	 * 
   	 * @param alarmList a list of {@link org.opennms.netmgt.model.OnmsAlarm} object.
   	 * @param purgeLimit an int type.
   	 * @return an int type
   	 */
    public int splitEventListByLimit(List<OnmsAlarm> alarmList, int purgeLimit){
    	
    	for(OnmsAlarm alarm : alarmList){
         	
    		// Get the default event filters
    		List<Filter> filterList = new ArrayList<Filter>(); 
         	for (String filterString : m_webAlarmRepository.getFilterStringsForEvent(alarm)) {
         		try{
         			Filter filter= EventUtil.getFilter(filterString, m_servletContext);
         			if(filter != null){
         				filterList.add(filter);
         			}
         		} catch(Exception e){
         			LOG.error("Could not retrieve filter name for filterString= "+filterString);
         		}
             }
         	
         	// Delete an alarm by Id
         	int alarmPurgeStatus = 0;
    		try{
    			alarmPurgeStatus = m_webAlarmRepository.purgeAlarm(alarm.getId());
    		} catch(final Exception e){
    			e.printStackTrace();
	        	LOG.error("Unable to do purge alarm action for this alarm Id ["+alarm.getId()+"].");
	        	return 0;
	        }
    		
    		if(alarmPurgeStatus > 0){
	 	    	Filter[] filters = filterList.toArray(new Filter[0]);
	 	        boolean isEventAvailable = true;
	 	        int eventOffset = 0;
	        	
	 	        while(isEventAvailable){
	
	 	        	// Get the events by it's an event criteria
	 	        	Event[] events = null;
	 	        	EventCriteria eventCriteria = new EventCriteria(filters, m_defaultEventSortStyle, m_defaultEventAcknowledgeType, purgeLimit, purgeLimit * eventOffset);
	 	        	try{
	 	        		events = m_webEventRepository.getMatchingEvents(eventCriteria);
	 	        	} catch(Exception e){
	 	        		LOG.error("Could not retrieve events for this EventCriteria = "+eventCriteria);
	 	        	}
	 		        
	 	        	if(events.length>0){
	 	        		for(Event event : events){
	 	        			//Delete an event by Id
		 	           		try{
		 	           			if(alarm.getId().equals(event.getAlarmId())){
		 	           				m_webAlarmRepository.purgeEvent(event.getId());
		 	           			}
		 	           		} catch(final Exception e){
		 	           			e.printStackTrace();
		 	       	        	LOG.error("Unable to do purge event action for this event Id ["+event.getId()+"].");
		 	       	        }
	 	        		}
	 	        	} else {
	 	        		isEventAvailable = false;
	 	        	}
	 	        	eventOffset++;
	 	        }
	 	        
	 	        //Delete an acknowledgment by refId
	    		try{
	    			m_webAlarmRepository.purgeAcknowledge(alarm.getId());
	    		} catch(final Exception e){
	    			e.printStackTrace();
		        	LOG.error("Unable to do purge acknowledgment action for this acknowledgment Id ["+alarm.getId()+"].");
		        }
    		}
 		}
    	return 1;
    }
    
    /**
   	 * Split the alarm list by it's limit
   	 * 
   	 * @param alarmFilters an array of {@link org.opennms.web.filter.Filter} object.
   	 * @param sortStyleString a {@link java.lang.String} object.
   	 * @param ackTypeString a {@link java.lang.String} object.
   	 * @param reportId a {@link java.lang.String} object.
   	 * @param requestFormat a {@link java.lang.String} object.
   	 * @param folderName a {@link java.lang.String} object.
   	 * @param exportLimit an int type.
   	 * @return an int type
   	 */
    public int splitAlarmListByLimit(Filter[] alarmFilters, String sortStyleString, String ackTypeString, 
    		String reportId, String requestFormat, String folderName, int exportLimit){

    	// Handle the acknowledge type parameter
    	org.opennms.web.alarm.AcknowledgeType alarmAckType = m_defaultAlarmAcknowledgeType;
        if (ackTypeString != null) {
        	try{
		        alarmAckType = org.opennms.web.alarm.AcknowledgeType.getAcknowledgeType(ackTypeString);
	        } catch (Exception e) {
				LOG.error("Could not retrieve acknowledge type for this "+ackTypeString);
			}
        }
        
        // Handle the sortStyle type parameter
        org.opennms.web.alarm.SortStyle alarmSortStyle = m_defaultAlarmSortStyle;
        if (sortStyleString != null) {
        	try{
        		alarmSortStyle = SortStyle.getSortStyle(sortStyleString);
	        } catch (Exception e) {
				LOG.error("Could not retrieve sortStyle type for this "+sortStyleString);
			}
        }
        
    	List<OnmsAlarm> alarmList = new ArrayList<OnmsAlarm>();
    	boolean isAlarmAvailable = true;
    	int alarmOffset = 0;
    	try{
	    	while(isAlarmAvailable){
	    		
	        	AlarmCriteria alarmQueryCriteria = new AlarmCriteria(alarmFilters, alarmSortStyle, alarmAckType, exportLimit, exportLimit * alarmOffset);
	        	OnmsAlarm[] alarms = m_webAlarmRepository.getMatchingAlarms(AlarmUtil.getOnmsCriteria(alarmQueryCriteria));
		        
	        	if(alarms.length>0){
	        		for(OnmsAlarm alarm : alarms){
	        			alarmList.add(alarm);
	        		}
	        		
	        		int eventStatus = splitEventListByLimit(alarmList, reportId, requestFormat,folderName, exportLimit);
	        		if(eventStatus == 1){
	        			LOG.info("The Export all action is successfully completed for the alarm list "+alarmList);
	        		} else {
	        			LOG.error("Unable to do export all action for this alarm list "+alarmList);
	        			return 0;
	        		}
	        	} else {
	        		isAlarmAvailable = false;
	        	}
	        	alarmOffset++;
	    	}
	    	
		} catch(final Exception ex){
			ex.printStackTrace();
    	    LOG.error("Unable to do export all action for this alarm list "+alarmList);
    	    return 0;
		}
    	return 1;
    }
    
    /**
   	 * Split the event list by it's limit
   	 * 
   	 * @param alarmList a list of {@link org.opennms.netmgt.model.OnmsAlarm} object .
   	 * @param reportId a {@link java.lang.String} object.
   	 * @param requestFormat a {@link java.lang.String} object.
   	 * @param folderName a {@link java.lang.String} object.
   	 * @param exportLimit an int type.
   	 * @return an int type
   	 */
    public int splitEventListByLimit(List<OnmsAlarm> alarmList, String reportId, String requestFormat, 
    		String folderName, int exportLimit){
    	 
    	 HashMap<Integer, List<Integer>> eventIdsForAlarms = null;
	     List<Integer> alarmIds = null;
	     
	     int eventCounter = 0;
	     int internalExportLimit = exportLimit;
	     int alarmCounter = alarmList.size();
	     boolean isEventAvailabeForReport = true;
	     
    	 for(OnmsAlarm alarm : alarmList){
    		 
    		 alarmCounter--;
    		 
 	        // Get the default event filters
    		 List<Filter> filterList = new ArrayList<Filter>();
         	for (String filterString : m_webAlarmRepository.getFilterStringsForEvent(alarm)) {
         		try{
         			Filter filter= EventUtil.getFilter(filterString, m_servletContext);
         			if(filter != null){
         				filterList.add(filter);
         			}
         		} catch(Exception e){
         			LOG.error("Could not retrieve filter name for filterString="+filterString);
         		}
             }
         	
 	    	Filter[] filters = filterList.toArray(new Filter[0]);
 	        boolean isEventAvailable = true;
 	        int eventOffset = 0;
 	        
 	        // Split the event report by it's limit
 	        while(isEventAvailable){
 	        	
 	        	// Get the events by it's an event criteria
 	        	Event[] events = null;
 	        	EventCriteria eventCriteria = new EventCriteria(filters, m_defaultEventSortStyle, m_defaultEventAcknowledgeType, internalExportLimit, eventOffset);
 	        	try{
 	        		events = m_webEventRepository.getMatchingEvents(eventCriteria);
 	        	} catch(Exception e){
 	        		LOG.error("Could not retrieve events for this EventCriteria ="+eventCriteria);
 	        	}
 		        
 	        	if(events.length>0){
 	        		
 	        		if(isEventAvailabeForReport){
	 	        		eventIdsForAlarms = new HashMap<Integer, List<Integer>>();
	 	 	            alarmIds = new ArrayList<Integer>();
 	        		}
 	        		
 		        	// Get the event Id's
 	        		List<Integer> eventIdsList = new ArrayList<Integer>();;
 	        		for(Event event : events){
 	        			eventCounter++;
 	        			eventIdsList.add(event.getId());
 	        		}
 		        	alarmIds.add(alarm.getId());
 			        eventIdsForAlarms.put(alarm.getId(), eventIdsList);
 			        
 			        if(eventCounter < exportLimit && alarmCounter!=0){
 			        	isEventAvailabeForReport = false;
 			        	internalExportLimit = exportLimit - eventCounter;
 			        	break;
 			        } 
 			        
 			        String fileName = null;
 	 	        	if(requestFormat != null){
 	 	        		fileName = "alarm_"+new SimpleDateFormat("MMddyyyy_HHmmss").format(new Date())+"."+requestFormat.toLowerCase();
 	 	        	} else {
 	 	        		LOG.error("Could not create the alarm report file name using with this file format ["+requestFormat+"]");
 	 	        	}
 	 	        	
 			        // Handle the alarm export action
 		        	try{
 		        		m_reportWrapperService.getAlarmReport(alarmIds, eventIdsForAlarms, reportId,
 		        				ReportFormat.valueOf(requestFormat), fileName, folderName);
 		        		isEventAvailabeForReport = true;
 		        		eventCounter = 0;
 		        		eventOffset = eventOffset + internalExportLimit;
 		 	        	internalExportLimit = exportLimit;
 		        	} catch(final Exception e){
 		        		e.printStackTrace();
 		        	    LOG.error("Unable to do export action for this alarm Id's "+alarmIds);
 		        	    return 0;
 		        	}
 	        	} else {
 	        		isEventAvailable = false;
 	        	}
 	        }
 		}
    	 return 1;
    }
    
    /**
     * <p>setWebAlarmRepository</p>
     *
     * @param webAlarmRepository a {@link org.opennms.netmgt.dao.AlarmRepository} object.
     */
    public static void setAlarmRepository(AlarmRepository webAlarmRepository) {
        m_webAlarmRepository = webAlarmRepository;
    }

    /**
     * <p>setWebEventRepository</p>
     *
     * @param webEventRepository a {@link org.opennms.web.event.WebEventRepository} object.
     */
    public static void setWebEventRepository(WebEventRepository webEventRepository) {
        m_webEventRepository = webEventRepository;
    }
    
    /**
     * <p>setReportWrapperService</p>
     *
     * @param reportWrapperService a {@link org.opennms.reporting.core.svclayer.ReportWrapperService} object.
     */
    public static void setReportWrapperService(ReportWrapperService reportWrapperService) {
        m_reportWrapperService = reportWrapperService;
    }
    
}

