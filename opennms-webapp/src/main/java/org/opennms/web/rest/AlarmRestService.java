/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2008-2012 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2012 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.web.rest;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.core.utils.WebSecurityUtils;
import org.opennms.netmgt.dao.api.AcknowledgmentDao;
import org.opennms.netmgt.dao.api.AlarmDao;
import org.opennms.netmgt.model.AckAction;
import org.opennms.netmgt.model.OnmsAcknowledgment;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.netmgt.model.OnmsAlarmCollection;
import org.opennms.web.alarm.AlarmUtil;
import org.opennms.web.controller.alarm.AlarmPurgeController;
import org.opennms.web.controller.alarm.AlarmReportController;
import org.opennms.web.filter.Filter;
import org.opennms.web.api.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sun.jersey.spi.resource.PerRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
@PerRequest
@Scope("prototype")
@Path("alarms")
public class AlarmRestService extends AlarmRestServiceBase {
    private static final Logger LOG = LoggerFactory.getLogger(AlarmRestService.class);
    @Autowired
    private AlarmDao m_alarmDao;

    @Autowired
    private AcknowledgmentDao m_ackDao;

    @Context
    UriInfo m_uriInfo;

    @Context
    SecurityContext m_securityContext;
    
    @Context
    ServletContext m_servletContext;
    
    /**
     * <p>
     * getAlarm
     * </p>
     * 
     * @param alarmId
     *            a {@link java.lang.String} object.
     * @return a {@link org.opennms.netmgt.model.OnmsAlarm} object.
     */
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Path("{alarmId}")
    @Transactional
    public OnmsAlarm getAlarm(@PathParam("alarmId")
    final String alarmId) {
        readLock();
        try {
            return m_alarmDao.get(new Integer(alarmId));
        } finally {
            readUnlock();
        }
    }

    /**
     * <p>
     * getCount
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Path("count")
    @Transactional
    public String getCount() {
        readLock();
        try {
            return Integer.toString(m_alarmDao.countAll());
        } finally {
            readUnlock();
        }
    }

    /**
     * <p>
     * getAlarms
     * </p>
     * 
     * @return a {@link org.opennms.netmgt.model.OnmsAlarmCollection} object.
     */
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Transactional
    public OnmsAlarmCollection getAlarms() {
        readLock();

        try {
            final CriteriaBuilder builder = getCriteriaBuilder(m_uriInfo.getQueryParameters(), false);
            builder.distinct();
            final OnmsAlarmCollection coll = new OnmsAlarmCollection(m_alarmDao.findMatching(builder.toCriteria()));

            // For getting totalCount
            coll.setTotalCount(m_alarmDao.countMatching(builder.clearOrder().limit(0).offset(0).toCriteria()));

            return coll;
        } finally {
            readUnlock();
        }
    }

    /**
     * <p>
     * Purge Alarm
     * </p>
     * 
     * @param alarmBeanString a {@link java.lang.String} object.
     */
    @POST
    @Path("purge")
    @Consumes(MediaType.APPLICATION_XML)
    public Response purgeAlarm(final String alarmBeanString) {
    	
    	LOG.info("Entering into the AlarmRestService to do AlarmPurgeController action");
    	writeLock();
        ResponseBuilder builder = Response.ok();
        
        try{
        	
        	// Get the alarm rest resource
        	AlarmRestResource alarmRestResource= new AlarmRestResource();
        	
        	// Convert the alarm bean string to object
        	final AlarmBean alarmBean = (AlarmBean) alarmRestResource.convertToJaxb(alarmBeanString);
        	
		 	// Handle the alarmid strings parameter
        	final String[] alarmIdStrings = alarmBean.getAlarmids();
	    	
	    	// Handle the actionCode parameter
        	final String action = alarmBean.getAction();
	        
	        // Handle the acknowledge type parameter
        	final String ackTypeString = alarmBean.getAcktype();
	        
	        // Handle the sortStyle type parameter
        	final String sortStyleString = alarmBean.getSortStyle();
		 	
		 	// Handle the filter parameter
        	final String[] filterStrings = alarmBean.getFilterStrings();
        	
	        // Get the alarm list for alarm purge selected option
	        List<OnmsAlarm> alarmList = new ArrayList<OnmsAlarm>();
	        
	        if (alarmIdStrings != null && action.equals(AlarmPurgeController.PURGE_ACTION)) {
	        	
	        	// Convert the alarm id strings to int's
	            int[] alarmIds = new int[alarmIdStrings.length];
	            for (int i = 0; i < alarmIds.length; i++) {
	            	try{
	            		alarmIds[i] = WebSecurityUtils.safeParseInt(alarmIdStrings[i]);
	            	} catch (Exception e) {
	            		LOG.error("Could not parse alarm ID ["+alarmIdStrings[i]+"] to integer.");
	    			}
	            }
	            
	            // Get alarm by it's id
	    		for (int alarmId : alarmIds) {
	    			try {
	        			alarmList.add(m_alarmDao.get(alarmId));
	    			} catch (Exception e) {
	    				LOG.error("Could not retrieve alarm from webAlarmRepository for ID=["+alarmId+"]");
	    			}
	    		}
	        }
	        
	        // Get the filters for purge all option
	        final List<Filter> filterList = new ArrayList<Filter>();
	        if (action.equals(AlarmPurgeController.PURGEALL_ACTION)) {
	        	if(filterStrings != null){
		            for (int i = 0; i < filterStrings.length; i++) {
		                Filter filter = AlarmUtil.getFilter(filterStrings[i], m_servletContext);
		                if (filter != null) {
		                    filterList.add(filter);
		                }
		            }
	        	}
	        }
	        
	        // Get the alarm purge limit from the opennms.properties file
	        String purgeLimitString = System.getProperty("opennms.alarm.purge.limit");
	        int purgeLimit = AlarmPurgeController.PURGE_LIMIT;
	        if(purgeLimitString!=null && purgeLimitString!="" && purgeLimitString!=" "){
	        	try{
	        		purgeLimit = Integer.parseInt(purgeLimitString);
	        	} catch(Exception ex){
	        		ex.printStackTrace();
	        		LOG.error("Could not parse alarm purge limit value ["+purgeLimitString+"] to int.");
	        	}
	        }
	        
	        // Handle the purge and purge all action
	        Filter[] alarmFilters = filterList.toArray(new Filter[0]);
	        
	        if(action.equals(AlarmPurgeController.PURGE_ACTION)){
	        	try{
	        		int purgeStatus = alarmRestResource.splitEventListByLimit(alarmList,purgeLimit);
	        		if(purgeStatus == 1){
	        			builder.entity("1");
	        			LOG.info("The Purge action is successfully completed for the alarm list "+alarmList);
	        		} else {
	        			builder.entity("0");
	        			LOG.error("Unable to do purge action for this alarm list "+alarmList);
	        		}
	        	} catch(Exception ex){
	        		builder.entity("0");
	        		ex.printStackTrace();
	        	    LOG.error("Unable to do purge action for this alarm list "+alarmList);
	        	}
	        } else if(action.equals(AlarmPurgeController.PURGEALL_ACTION)){
	        	try{
	        		int purgeStatus = alarmRestResource.splitAlarmListByLimit(alarmFilters, sortStyleString, ackTypeString,purgeLimit);
	        		if(purgeStatus == 1){
	        			builder.entity("1");
	        			LOG.error("The purge all action is successfully completed.");
	        		} else {
	        			builder.entity("0");
		        	    LOG.error("Unable to do purge all action.");
	        		}
	        	} catch(Exception ex){
	        		builder.entity("0");
	        		ex.printStackTrace();
	        	    LOG.error("Unable to do purge all action.");
	        	}
	        } else {
	        	LOG.error("Unknown action name");
	        	builder.entity("0");
	        }
	        
        } catch(Exception ex) {
        	builder.entity("0");
        	throw getException(null, "Can't get the alarm details because, " + ex.getMessage());
        } finally {
        	writeUnlock();
        }
        LOG.info("Terminated from the AlarmRestService after completed the AlarmPurgeController action");
        
        return (Response) builder.build();
    }
    
    /**
     * <p>
     * Export Alarm
     * </p>
     * 
     * @param alarmBeanString a {@link java.lang.String} object.
     */
    @POST
    @Path("export")
    @Consumes(MediaType.APPLICATION_XML)
    public Response exportAlarm(final String alarmBeanString) {
    	
    	LOG.info("Entering into the AlarmRestService to do AlarmReportController action");
    	writeLock();
    	ResponseBuilder builder = Response.ok();
    	
        try{
        	
        	// Get the alarm rest resource
        	AlarmRestResource alarmRestResource= new AlarmRestResource();
        	
        	// Convert the alarm bean string to object
        	final AlarmBean alarmBean = (AlarmBean) alarmRestResource.convertToJaxb(alarmBeanString);
        	
		 	// Handle the alarm id strings parameter
        	final String[] alarmIdStrings = alarmBean.getAlarmids();
	    	
	    	// Handle the actionCode parameter
        	final String action = alarmBean.getAction();
	        
	        // Handle the acknowledge type parameter
        	final String ackTypeString = alarmBean.getAcktype();
	        
	        // Handle the sortStyle type parameter
        	final String sortStyleString = alarmBean.getSortStyle();
		 	
		 	// Handle the filter parameter
        	final String[] filterStrings = alarmBean.getFilterStrings();
        	
            // Handle the reportId parameter
            final String reportId = alarmBean.getReportId();
            
            // Handle the report format parameter
            final String requestFormat = alarmBean.getRequestFormat();
            
            // Handle the alarm report folder name parameter
            final String folderName = alarmBean.getFolderName();
        	
	        // Get the alarm list for alarm purge selected option
            final List<OnmsAlarm> alarmList = new ArrayList<OnmsAlarm>();
            if (alarmIdStrings != null && action.equals(AlarmReportController.EXPORT_ACTION)) {
            	
            	// Convert the alarm id strings to int's
                int[] alarmIds = new int[alarmIdStrings.length];
                for (int i = 0; i < alarmIds.length; i++) {
                	try{
                		alarmIds[i] = WebSecurityUtils.safeParseInt(alarmIdStrings[i]);
                	} catch (Exception e) {
        				LOG.error("Could not parse alarm ID "+alarmIdStrings[i]+" to integer.");
        			}
                }
                
                // Get alarm by it's id
        		for (int alarmId : alarmIds) {
        			try {
        				alarmList.add(m_alarmDao.get(alarmId));
        			} catch (Exception e) {
        				LOG.error("Could not retrieve alarm from webAlarmRepository for ID="+alarmId);
        			}
        		}
            }
            
            // Handle the filter parameter
            final List<Filter> filterList = new ArrayList<Filter>();
            
            if (action.equals(AlarmReportController.EXPORTALL_ACTION)) {
            	if(filterStrings != null){
    	            for (int i = 0; i < filterStrings.length; i++) {
    	                Filter filter = AlarmUtil.getFilter(filterStrings[i], m_servletContext);
    	                if (filter != null) {
    	                    filterList.add(filter);
    	                }
    	            }
            	}
            }
            
            // Handle the alarm export data limit
            String reportLimitString = System.getProperty("opennms.alarm.export.limit");
            int exportLimit = AlarmReportController.EXPORT_LIMIT;
            
            if(reportLimitString!=null && reportLimitString!="" && reportLimitString!=" "){
            	try{
            		exportLimit = Integer.parseInt(reportLimitString);
            	} catch(Exception ex){
            		ex.printStackTrace();
            		LOG.error("Could not parse alarm export limit value "+reportLimitString+" to int.");
            	}
            }
	        
	        // Handle the purge and purge all action
	        Filter[] alarmFilters = filterList.toArray(new Filter[0]);
	        
	        if(action.equals(AlarmReportController.EXPORT_ACTION)){
	        	try{
	        		
	        		int exportStatus = alarmRestResource.splitEventListByLimit(alarmList, reportId, requestFormat, folderName , exportLimit);
	        		if(exportStatus==1){
	        			// Handle the folder delete action
		        		alarmRestResource.deleteReportFolderAfterCompressed(folderName);
		        		
		        		LOG.info("The Export action is successfully completed for the alarm list "+alarmList);
		        		builder.entity("1"+","+folderName);
	        		} else {
	        			builder.entity("0");
	        			LOG.error("Unable to do export action for this alarm list "+alarmList);
	        		}
	        		
	        	} catch(Exception ex){
	        		builder.entity("0");
	        		ex.printStackTrace();
	        	    LOG.error("Unable to do export action for this alarm list "+alarmList);
	        	}
	        } else if(action.equals(AlarmReportController.EXPORTALL_ACTION)){
	        	try{
	        		int exportStatus = alarmRestResource.splitAlarmListByLimit(alarmFilters, sortStyleString, ackTypeString, reportId, requestFormat, folderName ,exportLimit);
	        		if(exportStatus==1){
	        			// Handle the folder delete action
	        			alarmRestResource.deleteReportFolderAfterCompressed(folderName);
            		
	        			builder.entity("1"+","+folderName);
	        			LOG.info("The Export all action is successfully completed.");
	        		} else {
	        			builder.entity("0");
		        	    LOG.error("Unable to do export all action.");
	        		}
	        	} catch(Exception ex){
	        		builder.entity("0");
	        		ex.printStackTrace();
	        	    LOG.error("Unable to do export all action.");
	        	}
	        } else {
	        	LOG.error("Unknown action name");
	        	builder.entity("0");
	        }
	         		
        } catch(Exception ex) {
        	builder.entity("0");
        	throw getException(null, "Can't get the alarm details because, " + ex.getMessage());
        } finally {
        	writeUnlock();
        }
        
        LOG.info("Terminated from the AlarmRestService after completed the AlarmReportController action");
        
        return (Response) builder.build();
    }
    
    /**
     * <p>
    * updateAlarm
     * </p>
     * 
     * @param alarmId
     *            a {@link java.lang.String} object.
     * @param ack
     *            a {@link java.lang.Boolean} object.
     */
    @PUT
    @Path("{alarmId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response updateAlarm(@PathParam("alarmId") final Integer alarmId, final MultivaluedMapImpl formProperties) {
        writeLock();

        try {
            if (alarmId == null) {
                throw new IllegalArgumentException("Unable to determine alarm ID to update based on query path.");
            }

            final String ackValue = formProperties.getFirst("ack");
            formProperties.remove("ack");
            final String escalateValue = formProperties.getFirst("escalate");
            formProperties.remove("escalate");
            final String clearValue = formProperties.getFirst("clear");
            formProperties.remove("clear");
            final String ackUserValue = formProperties.getFirst("ackUser");
            formProperties.remove("ackUser");

            final OnmsAlarm alarm = m_alarmDao.get(alarmId);
            if (alarm == null) {
                throw new IllegalArgumentException("Unable to locate alarm with ID '" + alarmId + "'");
            }

            final String ackUser = ackUserValue == null ? m_securityContext.getUserPrincipal().getName() : ackUserValue;
            assertUserCredentials(ackUser);

            final OnmsAcknowledgment acknowledgement = new OnmsAcknowledgment(alarm, ackUser);
            acknowledgement.setAckAction(AckAction.UNSPECIFIED);
            if (ackValue != null) {
                if (Boolean.parseBoolean(ackValue)) {
                    acknowledgement.setAckAction(AckAction.ACKNOWLEDGE);
                } else {
                    acknowledgement.setAckAction(AckAction.UNACKNOWLEDGE);
                }
            } else if (escalateValue != null) {
                if (Boolean.parseBoolean(escalateValue)) {
                    acknowledgement.setAckAction(AckAction.ESCALATE);
                }
            } else if (clearValue != null) {
                if (Boolean.parseBoolean(clearValue)) {
                    acknowledgement.setAckAction(AckAction.CLEAR);
                }
            } else {
                throw new IllegalArgumentException("Must supply one of the 'ack', 'escalate', or 'clear' parameters, set to either 'true' or 'false'.");
            }
            m_ackDao.processAck(acknowledgement);
            return Response.seeOther(getRedirectUri(m_uriInfo)).build();
        } finally {
            writeUnlock();
        }
    }

    /**
     * <p>
     * updateAlarms
     * </p>
     * 
     * @param formProperties
     *            a {@link org.opennms.web.rest.MultivaluedMapImpl} object.
     */
    @PUT
    @Transactional
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateAlarms(final MultivaluedMapImpl formProperties) {
        writeLock();

        try {
            final String ackValue = formProperties.getFirst("ack");
            formProperties.remove("ack");
            final String escalateValue = formProperties.getFirst("escalate");
            formProperties.remove("escalate");
            final String clearValue = formProperties.getFirst("clear");
            formProperties.remove("clear");

            final CriteriaBuilder builder = getCriteriaBuilder(formProperties, false);
            builder.distinct();
            builder.limit(0);
            builder.offset(0);

            final String ackUser = formProperties.containsKey("ackUser") ? formProperties.getFirst("ackUser") : m_securityContext.getUserPrincipal().getName();
            formProperties.remove("ackUser");
            assertUserCredentials(ackUser);

            final List<OnmsAlarm> alarms = m_alarmDao.findMatching(builder.toCriteria());
            for (final OnmsAlarm alarm : alarms) {
                final OnmsAcknowledgment acknowledgement = new OnmsAcknowledgment(alarm, ackUser);
                acknowledgement.setAckAction(AckAction.UNSPECIFIED);
                if (ackValue != null) {
                    if (Boolean.parseBoolean(ackValue)) {
                        acknowledgement.setAckAction(AckAction.ACKNOWLEDGE);
                    } else {
                        acknowledgement.setAckAction(AckAction.UNACKNOWLEDGE);
                    }
                } else if (escalateValue != null) {
                    if (Boolean.parseBoolean(escalateValue)) {
                        acknowledgement.setAckAction(AckAction.ESCALATE);
                    }
                } else if (clearValue != null) {
                    if (Boolean.parseBoolean(clearValue)) {
                        acknowledgement.setAckAction(AckAction.CLEAR);
                    }
                } else {
                    throw new IllegalArgumentException("Must supply one of the 'ack', 'escalate', or 'clear' parameters, set to either 'true' or 'false'.");
                }
                m_ackDao.processAck(acknowledgement);
            }
            
            if (alarms.size() == 1) {
                return Response.seeOther(getRedirectUri(m_uriInfo, alarms.get(0).getId())).build();
            } else {
                return Response.seeOther(getRedirectUri(m_uriInfo)).build();
            }
        } finally {
            writeUnlock();
        }
    }

    private void assertUserCredentials(final String ackUser) {
        final String currentUser = m_securityContext.getUserPrincipal().getName();
        if (!(m_securityContext.isUserInRole(Authentication.ROLE_ADMIN)) && !(ackUser.equals(currentUser))) {
            throw new IllegalArgumentException("You are logged in as non-admin user '" + currentUser + "', but you are trying to update an alarm as another user ('" + ackUser + "')!");
        }
    }

}

