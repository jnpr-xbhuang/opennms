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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with OpenNMS(R). If not, see:
* http://www.gnu.org/licenses/
*
* For more information contact:
* OpenNMS(R) Licensing <license@opennms.org>
* http://www.opennms.org/
* http://www.opennms.com/
*******************************************************************************/

package org.opennms.web.rest;

import java.text.ParseException;
import java.util.Date;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriInfo;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.opennms.core.criteria.CriteriaBuilder;
import org.opennms.netmgt.dao.api.EventDao;
import org.opennms.netmgt.model.OnmsEvent;
import org.opennms.netmgt.model.OnmsEventCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sun.jersey.spi.resource.PerRequest;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.servlet.ServletContext;
import org.opennms.web.controller.event.EventExportController;
import org.opennms.web.controller.event.EventPurgeController;
import org.opennms.core.utils.WebSecurityUtils;
import org.springframework.orm.hibernate3.HibernateObjectRetrievalFailureException;
import org.hibernate.ObjectNotFoundException;
import org.opennms.netmgt.model.OnmsAlarm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Component
@PerRequest
@Scope("prototype")
@Path("events")
public class EventRestService extends OnmsRestService {
	private static final Logger LOG = LoggerFactory.getLogger(EventRestService.class);
    private static final DateTimeFormatter ISO8601_FORMATTER_MILLIS = ISODateTimeFormat.dateTime();
    private static final DateTimeFormatter ISO8601_FORMATTER = ISODateTimeFormat.dateTimeNoMillis();

    @Autowired
    private EventDao m_eventDao;

    @Context
    UriInfo m_uriInfo;

    @Context
    HttpHeaders m_headers;

    @Context
    SecurityContext m_securityContext;

	@Context
	ServletContext m_servletContext;

    /**
* <p>
* getEvent
* </p>
*
* @param eventId
* a {@link java.lang.String} object.
* @return a {@link org.opennms.netmgt.model.OnmsEvent} object.
*/
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Path("{eventId}")
    @Transactional
    public OnmsEvent getEvent(@PathParam("eventId") final String eventId) {
        readLock();
        try {
            return m_eventDao.get(new Integer(eventId));
        } finally {
            readUnlock();
        }
    }

    /**
* returns a plaintext string being the number of events
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
            return Integer.toString(m_eventDao.countAll());
        } finally {
            readUnlock();
        }
    }

    /**
* Returns all the events which match the filter/query in the query
* parameters
*
* @return Collection of OnmsEvents (ready to be XML-ified)
* @throws java.text.ParseException
* if any.
*/
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Transactional
    public OnmsEventCollection getEvents() throws ParseException {
        readLock();

        try {
            final CriteriaBuilder builder = new CriteriaBuilder(OnmsEvent.class);
            applyQueryFilters(m_uriInfo.getQueryParameters(), builder);
            builder.orderBy("eventTime").asc();

            final OnmsEventCollection coll = new OnmsEventCollection(m_eventDao.findMatching(builder.toCriteria()));
            coll.setTotalCount(m_eventDao.countMatching(builder.clearOrder().toCriteria()));

            return coll;
        } finally {
            readUnlock();
        }
    }

    /**
* Returns all the events which match the filter/query in the query
* parameters
*
* @return Collection of OnmsEvents (ready to be XML-ified)
* @throws java.text.ParseException
* if any.
*/
    @GET
    @Produces({MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_ATOM_XML})
    @Path("between")
    @Transactional
    public OnmsEventCollection getEventsBetween() throws ParseException {
        readLock();

        try {
            final CriteriaBuilder builder = new CriteriaBuilder(OnmsEvent.class);
            final MultivaluedMap<String, String> params = m_uriInfo.getQueryParameters();

            final String column;
            if (params.containsKey("column")) {
                column = params.getFirst("column");
                params.remove("column");
            } else {
                column = "eventTime";
            }
            Date begin;
            if (params.containsKey("begin")) {
                try {
                    begin = ISO8601_FORMATTER.parseLocalDateTime(params.getFirst("begin")).toDate();
                } catch (final Throwable t) {
                    begin = ISO8601_FORMATTER_MILLIS.parseDateTime(params.getFirst("begin")).toDate();
                }
                params.remove("begin");
            } else {
                begin = new Date(0);
            }
            Date end;
            if (params.containsKey("end")) {
                try {
                    end = ISO8601_FORMATTER.parseLocalDateTime(params.getFirst("end")).toDate();
                } catch (final Throwable t) {
                    end = ISO8601_FORMATTER_MILLIS.parseLocalDateTime(params.getFirst("end")).toDate();
                }
                params.remove("end");
            } else {
                end = new Date();
            }

            applyQueryFilters(params, builder);
            builder.match("all");
            try {
                builder.between(column, begin, end);
            } catch (final Throwable t) {
                throw new IllegalArgumentException("Unable to parse " + begin + " and " + end + " as dates!");
            }

            final OnmsEventCollection coll = new OnmsEventCollection(m_eventDao.findMatching(builder.toCriteria()));
            coll.setTotalCount(m_eventDao.countMatching(builder.clearOrder().toCriteria()));

            return coll;
        } finally {
            readUnlock();
        }
    }

    /**
* Updates the event with id "eventid" If the "ack" parameter is "true",
* then acks the events as the current logged in user, otherwise unacks
* the events
*
* @param eventId
* a {@link java.lang.String} object.
* @param ack
* a {@link java.lang.Boolean} object.
*/
    @PUT
    @Path("{eventId}")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response updateEvent(@PathParam("eventId") final String eventId, @FormParam("ack") final Boolean ack) {
        writeLock();

        try {
            final OnmsEvent event = m_eventDao.get(new Integer(eventId));
            if (ack == null) {
                throw new IllegalArgumentException("Must supply the 'ack' parameter, set to either 'true' or 'false'");
            }
            processEventAck(event, ack);
            return Response.seeOther(getRedirectUri(m_uriInfo)).build();
        } finally {
            writeUnlock();
        }
    }

    /**
* Updates all the events that match any filter/query supplied in the
* form. If the "ack" parameter is "true", then acks the events as the
* current logged in user, otherwise unacks the events
*
* @param formProperties
* Map of the parameters passed in by form encoding
*/
    @PUT
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Transactional
    public Response updateEvents(final MultivaluedMapImpl formProperties) {
        writeLock();

        try {
            Boolean ack = false;
            if (formProperties.containsKey("ack")) {
                ack = "true".equals(formProperties.getFirst("ack"));
                formProperties.remove("ack");
            }

            final CriteriaBuilder builder = new CriteriaBuilder(OnmsEvent.class);
            applyQueryFilters(formProperties, builder);
            builder.orderBy("eventTime").desc();

            for (final OnmsEvent event : m_eventDao.findMatching(builder.toCriteria())) {
                processEventAck(event, ack);
            }
            return Response.seeOther(getRedirectUri(m_uriInfo)).build();
        } finally {
            writeUnlock();
        }
    }

    private void processEventAck(final OnmsEvent event, final Boolean ack) {
        if (ack) {
            event.setEventAckTime(new Date());
            event.setEventAckUser(m_securityContext.getUserPrincipal().getName());
        } else {
            event.setEventAckTime(null);
            event.setEventAckUser(null);
        }
        m_eventDao.save(event);
    }

	/**
	 * <p>
	 * Purge Event
	 * </p>
	 * 
	 * @param eventBeanString
	 *            a {@link java.lang.String} object.
	 */
	@POST
	@Path("purge")
	@Consumes(MediaType.APPLICATION_XML)
	public Response purgeEvent(final String eventBeanString) {
		writeLock();
		LOG.debug("In purge event rest service for eventBean " + eventBeanString);
		ResponseBuilder builder = Response.ok();
		try {
			// Get the event rest resource
			EventRestResource eventRestResource = new EventRestResource();

			// Convert the event bean string to object
			final EventBean eventBean = (EventBean) eventRestResource
					.convertToJaxb(eventBeanString);

			// Handle the eventid strings parameter
			final String[] eventIdStrings = eventBean.getEventids();

			// Handle the actionCode parameter
			final String action = eventBean.getAction();

			// Handle the acknowledge type parameter
			final String ackTypeString = eventBean.getAcktype();

			// Handle the sortStyle type parameter
			final String sortStyleString = eventBean.getSortStyle();

			// Handle the filter parameter
			final String[] filterStrings = eventBean.getFilterStrings();

			
			// convert the event id strings to int's
			
			List<Integer> eventIdList = new ArrayList<Integer>();
			if (action.equals(EventPurgeController.PURGE_ACTION)) {
				int[] eventIds = new int[eventIdStrings.length];
				try {
					for (int i = 0; i < eventIdStrings.length; i++) {
						try {
							eventIds[i] = WebSecurityUtils
									.safeParseInt(eventIdStrings[i]);
							OnmsEvent event = m_eventDao.get(eventIds[i]);
							OnmsAlarm alarm = event.getAlarm();
							if (alarm == null || alarm.getId() != 0)
								eventIdList.add(event.getId());
							else {
								LOG.debug(
										"Active alarm is present for event with id "
												+ event.getId());
							}

						} catch (HibernateObjectRetrievalFailureException e) {
							LOG.error(
									"HibernateObjectRetrievalFailureException : No active alarm is present for event with id "
											+ eventIds[i]);
							eventIdList.add(eventIds[i]);
							continue;
						} catch (ObjectNotFoundException oe) {
							LOG.error(
									"ObjectNotFoundException : No active alarm is present for event with id "
											+ eventIds[i]);
							eventIdList.add(eventIds[i]);
							continue;
						} catch (Exception e) {
							LOG.error(
									"Could not retrieve event ID "
											+ eventIdStrings[i]);
							continue;
						}
					}

					int status =1;
					if(eventIdList.size() > 0)
						status = eventRestResource.purgeEvents(eventIdList);
					builder.entity(String.valueOf(status));
					LOG.info(
							"The Purge action is successfully completed for the event list "
									+ eventIdList);
				} catch (Exception e) {
					builder.entity("0");
					e.printStackTrace();
					LOG.error(
							"Unable to do purge action for this event list "
									+ eventIdList);
				}
			} else if (action.equals(EventPurgeController.PURGEALL_ACTION)) {
				try {
					int status = eventRestResource.splitAndPurgeEvents(filterStrings,
							sortStyleString, ackTypeString);
					builder.entity(String.valueOf(status));
				} catch (Exception e) {
					builder.entity("0");
					LOG.error("Unable to do purge all action ");
				}
			}
			LOG.debug("purgeEvent method completed for event bean " + eventBeanString);
			return (Response) builder.build();
		} catch (Exception ex) {
			throw getException(null, "Can't get the event details because, "
					+ ex.getMessage());
		} finally {
			writeUnlock();
		}
	}

	@POST
	@Path("export")
	@Consumes(MediaType.APPLICATION_XML)
	public Response exportEvent(final String eventBeanString) {

		writeLock();
		LOG.debug("In export event rest service for eventBean " + eventBeanString);
		try {
			ResponseBuilder builder = Response.ok();

			// Convert the event bean string to object
			// Get the event rest resource
			EventRestResource eventRestResource = new EventRestResource();
			final EventBean eventBean = (EventBean) eventRestResource
					.convertToJaxb(eventBeanString);

			final String[] eventIdStrings = eventBean.getEventids();
			final String action = eventBean.getAction();

			// Handle the report format and reportId parameter
			final String reportId = eventBean.getReportId();
			final String requestFormat = eventBean.getRequestFormat();
			final String[] filterStrings = eventBean.getFilterStrings();
			final String ackTypeString = eventBean.getAcktype();
			final String baseDir = System
					.getProperty("opennms.event.report.dir");

			if (eventIdStrings != null
					&& action.equals(EventExportController.EXPORT_ACTION)) {
				try {
				String fileName = "event_report"
						+ new SimpleDateFormat("_MMddyyyy_HHmmss")
								.format(new Date()) + "."
						+ requestFormat.toLowerCase();
				LOG.debug("File to be created " + fileName);
				int status = eventRestResource.exportEvents(eventIdStrings, fileName,
						reportId, requestFormat);
				builder.entity(status + "," + fileName);
				}catch(Exception ex){
	        		builder.entity("0");
	        		ex.printStackTrace();
	        	    LOG.error("Unable to do export action for this event list " + Arrays.toString(eventIdStrings));
	        	}
			}

			else if (action.equals(EventExportController.EXPORTALL_ACTION)) {
				try{
				String dirStr = "event_report"
						+ new SimpleDateFormat("_MMddyyyy_HHmmss")
								.format(new Date());
				String zipFile = dirStr + ".zip";
				int status = eventRestResource.splitAndExportEvents(filterStrings,
						ackTypeString, dirStr, reportId, requestFormat);
				
				builder.entity(status + "," + zipFile);
				}catch(Exception ex){
	        		builder.entity("0");
	        		ex.printStackTrace();
	        	    LOG.error("Unable to do export action for this event list " + Arrays.toString(eventIdStrings));
	        	}
			} else {
				LOG.error("Unknown event action: " + action);
			}
			LOG.debug("exportEvent method completed for event bean " + eventBeanString);
			return (Response) builder.build();
		} catch (Exception ex) {
			throw getException(null, "Can't get the event details because, "
					+ ex.getMessage());
		} finally {
			writeUnlock();
		}
	}
}

