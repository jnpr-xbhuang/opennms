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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.commons.io.FileUtils;
import org.opennms.reporting.core.svclayer.ReportWrapperService;
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
 * This servlet receives an HTTP POST with a list of events and export or export
 * all action for the selected events. and then it displays the event reports on
 * current URL page
 * 
 */

public class EventExportController extends AbstractController implements
		InitializingBean {

	/** Constant <code>EXPORT_ACTION="1"</code> */
	public final static String EXPORT_ACTION = "1";

	/** Constant <code>EXPORTALL_ACTION="2"</code> */
	public final static String EXPORTALL_ACTION = "2";

	/**
	 * OpenNMS event repository
	 */
	private WebEventRepository m_webEventRepository;

	/**
	 * OpenNMS report wrapper service
	 */
	private ReportWrapperService m_reportWrapperService;

	/**
	 * Logging
	 */
	private Logger logger = LoggerFactory.getLogger("OpenNMS.WEB."
			+ EventExportController.class.getName());

	/** To hold report file name <code>FILE_NAME="EMPTY"</code> */
	public static String FILE_NAME = "EMPTY";

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

	/** To hold default redirectView page */
	private String m_redirectView;

	/**
	 * <p>
	 * setReportWrapperService
	 * </p>
	 * 
	 * @param reportWrapperService
	 *            a
	 *            {@link org.opennms.reporting.core.svclayer.ReportWrapperService}
	 *            object.
	 */
	public void setReportWrapperService(
			ReportWrapperService reportWrapperService) {
		m_reportWrapperService = reportWrapperService;
		EventRestResource.setReportWrapperService(m_reportWrapperService);
	}

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
		Assert.notNull(m_webEventRepository, "webEventRepository must be set");
		Assert.notNull(m_reportWrapperService,
				"reportWrapperService must be set");
		Assert.notNull(m_redirectView, "redirectView must be set");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Export or export all action of the selected events specified in the POST
	 * and then display the client to an appropriate URL.
	 */
	protected ModelAndView handleRequestInternal(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		logger.info("Enter into the EventExportController action");

		// Handle the event and actionCode parameter
		EventBean eventBean = new EventBean();
		String[] eventIdStrings = request.getParameterValues("event");
		eventBean.setEventids(eventIdStrings);
		String action = request.getParameter("actionCode");
		eventBean.setAction(action);
		// Handle the report format and reportId parameter
		String reportId = request.getParameter("reportId");
		eventBean.setReportId(reportId);
		String requestFormat = request.getParameter("format");
		eventBean.setRequestFormat(requestFormat);
		String[] filterStrings = request.getParameterValues("filter");
		eventBean.setFilterStrings(filterStrings);
		String ackTypeString = request.getParameter("acktype");
		eventBean.setAcktype(ackTypeString);

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
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("Unable to marshall your event bean class because of  "
					+ ex.getMessage());
		} finally {
			if (out != null) {
				out.close();
			}
		}

		// Get the address for junos server
		String addressForServer = System.getProperty("junos.server.address");

		// Handle the rest client for junos rest api
		ClientResponse clientResponse = null;
		try {
			if (addressForServer != null && addressForServer != " ") {
				Client client = Client.create();
				client.setReadTimeout(10000);
				WebResource service = client
						.resource("http://" + addressForServer
								+ ":8080/fmpm/restful/events/export");
				clientResponse = service.type(MediaType.APPLICATION_XML).post(
						ClientResponse.class, bean);
				logger.info("Client Response is " + clientResponse.getStatus());
			} else {
				logger.error("Unable to call junose rest api because junos server address ["
						+ addressForServer + "] is invalid");
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			logger.error("Unable to call junose rest api from alarm report controller because of "
					+ ex.getMessage());
		}

		// Get the response status from junos rest api
		int taskId = -1;
		try {
			Task restTask = clientResponse.getEntity(Task.class);
			if (restTask != null) {
				taskId = restTask.getId();
			}
		} catch (Exception ex) {
			logger.error("Could not get the task response from junose rest api");
		}

		// Handle the redirect parameters
		String redirectParms = request.getParameter("redirectParms");
		String redirectPage = request.getParameter("redirectPage");
		String viewName = m_redirectView;

		if (redirectParms != null && redirectParms != ""
				&& redirectParms != " ") {
			viewName = m_redirectView + "?" + redirectParms;
		} else if (redirectPage != null) {
			viewName = redirectPage;
		}

		RedirectView view = new RedirectView(viewName, true);
		request.getSession().setAttribute("actionStatus", "E" + "," + taskId);

		logger.info("Terminated from the EventExportController action");
		return new ModelAndView(view);
	}

	/**
	 * {@local Method}
	 * 
	 * Compressed the event report folder
	 */
	public void compressedReportFolder(String baseDir, String folderName) {

		byte[] buffer = new byte[1024];

		try {

			ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(
					baseDir + "/" + folderName + ".zip"));
			File folder = new File(baseDir + "/" + folderName + "/");

			for (File file : folder.listFiles()) {
				if (file.isFile()) {
					zos.putNextEntry(new ZipEntry(file.getName()));
					FileInputStream in = new FileInputStream(baseDir + "/"
							+ folderName + "/" + file.getName());

					int len;
					while ((len = in.read(buffer)) > 0) {
						zos.write(buffer, 0, len);
					}
					in.close();
					zos.closeEntry();
				}
			}
			// remember close it
			zos.close();
		} catch (IOException ex) {
			ex.printStackTrace();
			logger.error("Unable to compressed the event report folder");
		}
	}

	/**
	 * {@local Method}
	 * 
	 * Delete the event report folder
	 */
	public void deleteReportFolderAfterCompressed(String baseDir,
			String folderName) {

		File tempFolderDir = new File(baseDir + "/" + folderName);

		if (new File(tempFolderDir + ".zip").exists()) {
			if (tempFolderDir.exists()) {
				try {
					FileUtils.deleteDirectory(tempFolderDir);
				} catch (IOException e) {
					logger.error(
							"Unable to delete the folder name ='{}' from the event report location",
							folderName);
					e.printStackTrace();
				}
			} else {
				logger.error(
						"The folder name ='{}' is not availabe in the event report location",
						folderName);
			}
		} else {
			logger.error(
					"Unable to compress the folder name ='{}' from the event report location",
					folderName);
		}

	}
}
