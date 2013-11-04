/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2009-2012 The OpenNMS Group, Inc.
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

package org.opennms.web.controller.event;

import java.io.ByteArrayOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.opennms.netmgt.dao.api.AlarmRepository;
import org.opennms.web.event.WebEventRepository;
import org.opennms.web.rest.EventBean;
import org.opennms.web.rest.EventRestResource;
import org.opennms.web.rest.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.AbstractController;
import org.springframework.web.servlet.view.RedirectView;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

/**
 * This servlet receives an HTTP POST with a list of events and acknowledgments
 * for the selected events to purge , and then it redirects the client to a URL
 * for display. The target URL is configurable in the servlet config (web.xml
 * file).
 * 
 */

public class EventPurgeController extends AbstractController implements
		InitializingBean {

	/** Constant <code>PURGE_ACTION="1"</code> */
	public final static String PURGE_ACTION = "1";

	/** Constant <code>PURGEALL_ACTION="2"</code> */
	public final static String PURGEALL_ACTION = "2";

	/** To hold default redirectView page */
	private String m_redirectView;

	/**
	 * OpenNMS event repository
	 */
	private WebEventRepository m_webEventRepository;

	/**
	 * OpenNMS alarm repository
	 */
	private AlarmRepository m_alarmRepository;

	/**
	 * Logging
	 */
	private Logger logger = LoggerFactory.getLogger("OpenNMS.WEB."
			+ EventPurgeController.class.getName());

	/**
	 * <p>
	 * setRedirectView
	 * </p>
	 * 
	 * @param redirectView
	 *            a {@link java.lang.String} object.
	 */
	public void setRedirectView(String redirectView) {
		m_redirectView = redirectView;
	}

	/**
	 * <p>
	 * afterPropertiesSet
	 * </p>
	 * 
	 * @throws java.lang.Exception
	 *             if any.
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(m_redirectView, "redirectView must be set");
		Assert.notNull(m_webEventRepository, "webEventRepository must be set");
		Assert.notNull(m_alarmRepository, "alarmRepository must be set");
	}

	

	/**
	 * {@inheritDoc}
	 * 
	 * Purge of the selected events specified in the POST and then redirect the
	 * client to an appropriate URL for display.
	 */
	protected ModelAndView handleRequestInternal(
			final HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		logger.info("Enter into the EventPurgeController action");

		// Handle the event bean class
        EventBean eventBean = new EventBean();
    	
		// handle the event and actionCode parameter
		final String[] eventIdStrings = request.getParameterValues("event");
		eventBean.setEventids(eventIdStrings);
		
		final String action = request.getParameter("actionCode");
		eventBean.setAction(action);
		// Handle the acknowledge type parameter
		final String ackTypeString = request.getParameter("acktype");
		eventBean.setAcktype(ackTypeString);
		// Handle the sortStyle type parameter
		final String sortbyString = request.getParameter("sortby");
		eventBean.setSortStyle(sortbyString);
		String[] filterStrings = request.getParameterValues("filter");
        eventBean.setFilterStrings(filterStrings);
        
        
    	// Handle the event bean marshal string 
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		String bean = null;
		try {
			JAXBContext m_context = JAXBContext.newInstance(EventBean.class);
			Marshaller m_marshaller = m_context.createMarshaller();
			m_marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			m_marshaller = m_context.createMarshaller();
			m_marshaller.marshal(eventBean, out);
			bean = out.toString();
			out.close();
		} catch(Exception ex) {
    		ex.printStackTrace();
    		logger.error("Unable to marshall your event bean class because of  "+ex.getMessage());
    	} finally {
    		if(out != null){
    			out.close();
    		}
    	}
    	
        // Get the address for junos server
        String addressForServer = System.getProperty("junos.server.address");
		
    	// Handle the rest client for junos rest api
    	ClientResponse clientResponse = null;
        try{	
			if(addressForServer!=null && addressForServer!=" "){
				Client client = Client.create();
				WebResource service=client.resource("http://"+addressForServer+":8080/fmpm/restful/events/purge");
				client.setReadTimeout(10000);
				clientResponse = service.type(MediaType.APPLICATION_XML).post(ClientResponse.class,bean);
				logger.info("Client Response is " + clientResponse.getStatus());
			} else {
    			logger.error("Unable to call junose rest api because junos server address ["+addressForServer+"] is invalid");
    		}
    	} catch(Exception ex) {
    		ex.printStackTrace();
    		logger.error("Unable to call junose rest api from event purge controller because of "+ex.getMessage());
    	}
            
        
        // Get the response status from junos rest api
        int taskId = -1;
        try {
	        Task restTask = clientResponse.getEntity(Task.class);
	        if(restTask!=null){
	        	taskId = restTask.getId();
	        }
        } catch(Exception ex) {
        	logger.error("Could not get the task response from junose rest api");
        }
            
    	// Handle the redirect parameters
        String redirectParms = request.getParameter("redirectParms");
        String viewName = m_redirectView;
        if(redirectParms!=null && redirectParms != "" && redirectParms != " "){
        	viewName = m_redirectView + "?" + redirectParms;
        }
        
        RedirectView view = new RedirectView(viewName, true);
        request.getSession().setAttribute("actionStatus","P"+","+taskId);
    	
        logger.info("Terminated from the EventPurgeController action");
        return new ModelAndView(view);
    }
        

	/**
	 * <p>
	 * setWebEventRepository
	 * </p>
	 * 
	 * @param webEventRepository
	 *            a {@link org.opennms.web.event.WebEventRepository} object.
	 */
	public void setWebEventRepository(WebEventRepository webEventRepository) {
		m_webEventRepository = webEventRepository;
		EventRestResource.setWebEventRepository(m_webEventRepository);
	}

	/**
	 * <p>
	 * setAlarmRepository
	 * </p>
	 * 
	 * @param webAlarmRepository
	 *            a {@link org.opennms.web.event.WebAlarmRepository} object.
	 */
	public void setAlarmRepository(AlarmRepository alarmRepository) {
		m_alarmRepository = alarmRepository;
		EventRestResource.setAlarmRepository(m_alarmRepository);
	}

}
