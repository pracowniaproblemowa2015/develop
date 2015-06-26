package com.scheduler.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.scheduler.service.impl.SchedulerServiceImpl.ShiftType;

public class ScheduleRequest {
	private Date startDate;

	private Map<Date, Map<ShiftType, List<Nurse>>> lastWeek;

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Map<Date, Map<ShiftType, List<Nurse>>> getLastWeek() {
		return lastWeek;
	}

	public void setLastWeek(Map<Date, Map<ShiftType, List<Nurse>>> lastWeek) {
		this.lastWeek = lastWeek;
	}
}
