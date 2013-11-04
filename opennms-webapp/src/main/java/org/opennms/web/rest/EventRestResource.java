package org.opennms.web.rest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.hibernate.ObjectNotFoundException;

import org.opennms.api.reporting.ReportFormat;
import org.opennms.core.utils.WebSecurityUtils;

import org.opennms.netmgt.dao.api.AlarmRepository;
import org.opennms.netmgt.model.OnmsAlarm;
import org.opennms.netmgt.model.OnmsEvent;
import org.opennms.reporting.core.svclayer.ReportWrapperService;
import org.opennms.web.event.AcknowledgeType;

import org.opennms.web.event.EventUtil;
import org.opennms.web.event.SortStyle;
import org.opennms.web.event.Event;
import org.opennms.web.event.WebEventRepository;
import org.opennms.web.event.filter.EventCriteria;
import org.opennms.web.filter.Filter;
import org.springframework.orm.hibernate3.HibernateObjectRetrievalFailureException;
import org.xml.sax.InputSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventRestResource {
	  private static final Logger LOG = LoggerFactory.getLogger(EventRestResource.class);

	@Context
	ServletContext m_servletContext;

	/**
	 * OpenNMS event repository
	 */
	private static WebEventRepository m_webEventRepository;

	/**
	 * OpenNMS alarm repository
	 */
	private static AlarmRepository m_alarmRepository;
	/**
	 * OpenNMS report wrapper service
	 */
	private static ReportWrapperService m_reportWrapperService;

	
	/**
	 * Location of event report
	 */
	private String baseDir = System.getProperty("opennms.event.report.dir");

	/**
	 * Convert event string XMl to object
	 * 
	 * @param eventBeanString
	 *            a {@link java.lang.String} object.
	 * @return a {@link java.lang.Object} object.
	 */
	public Object convertToJaxb(String eventBeanString) {
		EventBean result = null;
		try {
			JAXBContext jc = JAXBContext.newInstance(EventBean.class);
			Unmarshaller u = jc.createUnmarshaller();
			InputSource inputSource = new InputSource(new StringReader(
					eventBeanString));
			result = (EventBean) u.unmarshal(inputSource);
		} catch (Exception ex) {
			ex.printStackTrace();
			LOG.error(
					"Unable to unmarshal of event bean xml string because of "
							+ ex);
		}
		return result;

	}

	/**
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
			LOG.error("Unable to compressed the event report folder");
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
					LOG.error(
							"Unable to delete the folder name =" + folderName
									+ " from the event report location");
					e.printStackTrace();
				}
			} else {

				LOG.error(
						"The folder name ="
								+ folderName
								+ " is not availabe in the event report location");
			}
		} else {

			LOG.error(
					"Unable to compress the folder name =" + folderName
							+ " from the event report location");
		}

	}

	/**
	 * <p>
	 * setWebEventRepository
	 * </p>
	 * 
	 * @param webEventRepository
	 *            a {@link org.opennms.web.event.WebEventRepository} object.
	 */
	public static void setWebEventRepository(
			WebEventRepository webEventRepository) {
		m_webEventRepository = webEventRepository;
	}

	/**
	 * <p>
	 * setAlarmRepository
	 * </p>
	 * 
	 * @param webAlarmRepository
	 *            
	 */
	public static void setAlarmRepository(
			AlarmRepository alarmRepository) {
		m_alarmRepository = alarmRepository;
	}
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
	public static void setReportWrapperService(
			ReportWrapperService reportWrapperService) {
		m_reportWrapperService = reportWrapperService;
	}

	public int purgeEvents(List<Integer> eventList) {
		int status = 1;
		try {
		this.m_webEventRepository.purgeEvents(eventList);
		}catch (Exception e) {
			status =0;
		}
		return status;
	}

	public int splitAndPurgeEvents(String[] filterStrings,
			String sortbyString, String ackTypeString) {
		LOG.debug("In splitAndPurgeEvents");
		int status = 1;
		try {
		List<Filter> filterList = new ArrayList<Filter>();
		if (filterStrings != null) {
			for (int i = 0; i < filterStrings.length; i++) {
				Filter filter = EventUtil.getFilter(filterStrings[i],
						m_servletContext);
				if (filter != null) {
					filterList.add(filter);
				}
			}
		}
		Filter[] eventFilters = filterList.toArray(new Filter[0]);
		int limit = 50000;
		int offset = 0;
		boolean isEventAvailable = true;

		try {
			limit = Integer.parseInt(System
					.getProperty("opennms.event.purge.limit"));
		} catch (Exception e) {
			LOG.error(
					"Error in getting event export limit. Setting to 3 lakhs");
			limit = 50000;
		}

		SortStyle sortStyle = null;
		// String sortbyString = request.getParameter("sortby") ;
		if (sortbyString != null)
			sortStyle = SortStyle.getSortStyle(sortbyString);

		// String ackTypeString = request.getParameter("acktype");
		AcknowledgeType ackType = null;
		if (ackTypeString != null) {
			ackType = AcknowledgeType.getAcknowledgeType(ackTypeString);
		}
		int count = 1;
		while (isEventAvailable) {
			List<Integer> eventIdList = new ArrayList<Integer>();
			EventCriteria eventQueryCriteria = new EventCriteria(eventFilters,
					sortStyle, ackType, limit, offset);
			Event[] events = m_webEventRepository
					.getMatchingEvents(eventQueryCriteria);

			if (events.length == 0) {
				isEventAvailable = false;
			} else {
				LOG.debug("Processing events.Loop count is " + count);
				count++;
				eventIdList = new ArrayList<Integer>();

				for (int i = 0; i < events.length; i++) {
					int eventId = 0;
					Event event = events[i];
					eventId = event.getId();
					int alarmid = event.getAlarmId();
					if(alarmid <1) {
						eventIdList.add(eventId);
					}
					else {
						OnmsAlarm alarm = null;
						try {
							alarm = m_alarmRepository.getAlarm(alarmid);
						} catch (HibernateObjectRetrievalFailureException e) {
                            LOG.error("HibernateObjectRetrievalFailureException : No active alarm is present for event with id "
                                    + event.getId());
                            eventIdList.add(eventId);
                            offset = offset+i+1;
                            continue;
						} catch (ObjectNotFoundException oe) {
							LOG.error("ObjectNotFoundException : No active alarm is present for event with id "
                                    + event.getId());
							eventIdList.add(eventId);
							offset = offset+i+1;
							continue;
						}

						if(alarm == null)		
							eventIdList.add(eventId);
						else {
							offset = offset+i+1;
							LOG.debug(
									"Active alarm is present for event with id "
											+ event.getId());
						}
					}
				}
				if(eventIdList.size() > 0)
					purgeEvents(eventIdList);
			}
		}
		}catch (Exception e) {
			e.printStackTrace();
			status = 0;
		}
		LOG.debug("splitAndPurgeEvents Completed and status is " + status);
		return status;
	}

	public int exportEvents(String[] eventIdStrings, String fileName,
			String reportId, String requestFormat) {
		LOG.debug("In exportEvents");
		int status = 1;
		int[] eventIdArray = new int[eventIdStrings.length];
		List<Integer> eventIds = new ArrayList<Integer>();
		for (int i = 0; i < eventIdArray.length; i++) {
			try {
				eventIdArray[i] = WebSecurityUtils
						.safeParseInt(eventIdStrings[i]);
				Event event = m_webEventRepository.getEvent(eventIdArray[i]);
				eventIds.add(event.getId());
			} catch (Exception e) {
				LOG.error(
						"Could not parse event ID '{}' to integer."
								+ eventIdStrings[i]);
			}
		}

		try {
			m_reportWrapperService.getEventReport(eventIds, reportId,
					ReportFormat.valueOf(requestFormat), fileName, null);
		} catch (final Exception e) {
			status = 0;
			e.printStackTrace();
			LOG.error(
					"Unable to do export action for this event Id's."
							+ eventIds);
		}
		LOG.debug("In exportEvents completed and status is " + status);
		return status;
	}

	public int splitAndExportEvents(String[] filterStrings,
			String ackTypeString, String dirStr, String reportId,
			String requestFormat) {
		LOG.debug("In splitAndExportEvents");
		int status = 1;
		try {
			List<Filter> filterList = new ArrayList<Filter>();

			if (filterStrings != null) {
				for (int i = 0; i < filterStrings.length; i++) {
					Filter filter = EventUtil.getFilter(filterStrings[i],
							m_servletContext);
					if (filter != null) {
						filterList.add(filter);
					}
				}
			}

			SortStyle sortStyle = SortStyle.ID;

			AcknowledgeType ackType = null;
			if (ackTypeString != null) {
				ackType = AcknowledgeType.getAcknowledgeType(ackTypeString);
			}
			int limit = 0;
			try {
				limit = Integer.parseInt(System
						.getProperty("opennms.event.export.limit"));

			} catch (Exception e) {
				LOG.error(
						"Error in getting event export limit. Setting to 1 lakh");

			}
			int offset = 0;
			// Get the events by event criteria
			Filter[] eventFilters = filterList.toArray(new Filter[0]);
			List<String> fileList = new ArrayList<String>();
			boolean isEventPresent = true;
			int count = 1;

			while (isEventPresent) {
				List<Integer> eventIds = new ArrayList<Integer>();
				EventCriteria eventQueryCriteria = new EventCriteria(
						eventFilters, sortStyle, ackType, limit, offset);
				Event[] events = m_webEventRepository
						.getMatchingEvents(eventQueryCriteria);
				LOG.debug("events lenth" + events.length);
				if (events.length != 0) {
					LOG.debug("File number " + count);
					for (int i = 0; i < events.length; i++) {
						eventIds.add(events[i].getId());
					}

					File dir = new File(baseDir + "/" + dirStr);
					dir.mkdir();
					String fileName = dirStr + "_" + count + "."
							+ requestFormat.toLowerCase();

					try {
						m_reportWrapperService.getEventReport(eventIds,
								reportId, ReportFormat.valueOf(requestFormat),
								fileName, dirStr);
					} catch (Exception e) {
						LOG.error(
								"Unable to do export action for this event Id's."
										+ eventIds);
					}

					fileList.add(fileName);
					count++;
					offset = offset + limit;
				} else {
					isEventPresent = false;
				}

			}

			compressedReportFolder(baseDir, dirStr);
			deleteReportFolderAfterCompressed(baseDir, dirStr);
		} catch (Exception e) {
			status =0;
			e.printStackTrace();
			LOG.error("Error while export all action");
		}
		LOG.debug("splitAndExportEvents completed and status is " + status);
		return status;
	}
	
	
}

