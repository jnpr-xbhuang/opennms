package org.opennms.reporting.jasperreports.svclayer;

import java.util.List;


public class AlarmReportBean {
	
	private List<AlarmReportStructure> m_alarmReportStructure;

	public List<AlarmReportStructure> getAlarmReportStructure() {
		return m_alarmReportStructure;
	}

	public void setAlarmReportStructure(List<AlarmReportStructure> m_alarmReportStructure) {
		this.m_alarmReportStructure = m_alarmReportStructure;
	}
	
}
