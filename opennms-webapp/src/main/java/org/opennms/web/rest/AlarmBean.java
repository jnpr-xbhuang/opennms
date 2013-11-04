package org.opennms.web.rest;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Contains information on alarm purge controller parameters .
 */
@XmlRootElement(name="alarmbean")
@XmlAccessorType(XmlAccessType.FIELD)
public class AlarmBean implements Serializable {

	private static final long serialVersionUID = 1L;

	 @XmlElement(name="alarmids", required=true)
	 private String[] m_alarmids;
	 
	 @XmlElement(name = "action", required = true)
	 private String m_action;
		
	 @XmlElement(name = "acktype", required = true)
	 private String m_acktype;
		
	 @XmlElement(name = "sortstyle", required = true)
	 private String m_sortstyle;
	 
	 @XmlElement(name="filters", required=true)
	 private String[] m_filters;
	 
	 @XmlElement(name = "reportid", required = true)
	 private String m_reportid;
	 
	 @XmlElement(name = "requestformat", required = true)
	 private String m_requestformat;
	 
	 @XmlElement(name = "foldername", required = true)
	 private String m_foldername;
	 
	 public String getAction() {
			return m_action;
	 }

	 public void setAction(String action) {
		this.m_action = action;
	 }

	 public String getAcktype() {
		return m_acktype;
	 }

	 public void setAcktype(String acktype) {
		this.m_acktype = acktype;
	 }

	 public String getSortStyle() {
		return m_sortstyle;
	 }

	 public void setSortStyle(String sortstyle) {
		this.m_sortstyle = sortstyle;
	 }

	public String[] getAlarmids() {
		return m_alarmids;
	}

	public void setAlarmids(String[] alarmids) {
		this.m_alarmids = alarmids;
	}

	public String[] getFilterStrings() {
		return m_filters;
	}

	public void setFilterStrings(String[] filterStrings) {
		this.m_filters = filterStrings;
	}

	public String getReportId() {
		return m_reportid;
	}

	public void setReportId(String m_reportid) {
		this.m_reportid = m_reportid;
	}

	public String getRequestFormat() {
		return m_requestformat;
	}

	public void setRequestFormat(String m_requestformat) {
		this.m_requestformat = m_requestformat;
	}

	public String getFolderName() {
		return m_foldername;
	}

	public void setFolderName(String m_foldername) {
		this.m_foldername = m_foldername;
	}
	
	@Override
	public String toString() {
		return 	"alarmids"+this.getAlarmids()+
				"action"+this.getAction()+
				"acktype"+this.getAcktype()+ 
				"sortstyle"+this.getSortStyle()+
				"filters"+this.getFilterStrings()+
				"reportid"+this.getReportId()+
				"requestformat"+this.getRequestFormat()+
				"foldername"+this.getFolderName();
	}
}
