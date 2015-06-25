package com.scheduler.service;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.scheduler.model.Nurse;
import com.scheduler.service.impl.SchedulerServiceImpl.ShiftType;

public interface SchedulerService {

	Map<Date, Map<ShiftType, List<Nurse>>> getSchedule(List<Nurse> nurses, Date start,
			Map<Date, Map<ShiftType, List<Nurse>>> lastWeek);

}
