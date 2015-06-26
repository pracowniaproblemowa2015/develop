package com.scheduler.controller.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.scheduler.model.Nurse;
import com.scheduler.model.ScheduleRequest;
import com.scheduler.model.User;
import com.scheduler.repository.UserRepository;
import com.scheduler.service.SchedulerService;
import com.scheduler.service.impl.SchedulerServiceImpl.ShiftType;

@Transactional
@Controller
@RequestMapping("/api/schedule")
public class ScheduleController {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private SchedulerService scheduleService;

	@RequestMapping(method = RequestMethod.POST, headers = "Accept=application/json", produces = "application/json")
	@ResponseBody
	public Map<Date, Map<ShiftType, List<Nurse>>> generate(@RequestBody ScheduleRequest request) {
		List<User> users = userRepository.findAll();
		List<Nurse> nurses = new ArrayList<Nurse>();
		for (User user : users) {
			if (user.getRole().equals("ROLE_ADMIN")) {
				continue;
			}
			nurses.add(new Nurse(user.getId().intValue(), user.getWeekHours() * 5 + 4, user.getWeekHours()));
		}

		Map<Date, Map<ShiftType, List<Nurse>>> schedule = scheduleService.getSchedule(nurses, request.getStartDate(),
				request.getLastWeek());
		int nurseWithoutLateShift = nurseWithoutLateShift(schedule, nurses);
		int userWithoutLateShift = 0;
		for (User user : users) {
			if (!user.getLateShift()) {
				userWithoutLateShift = user.getId().intValue();
			}
		}

		if (userWithoutLateShift == nurseWithoutLateShift) {
			return schedule;
		}

		Iterator<Nurse> iterator = nurses.iterator();
		while (iterator.hasNext()) {
			Nurse nurse = iterator.next();
			if (nurse.getId() == nurseWithoutLateShift) {
				nurse.setId(userWithoutLateShift);
			} else if (nurse.getId() == userWithoutLateShift) {
				nurse.setId(nurseWithoutLateShift);
			}
		}
		return schedule;
	}

	private int nurseWithoutLateShift(Map<Date, Map<ShiftType, List<Nurse>>> schedule, List<Nurse> nurses) {
		List<Nurse> nurseCopy = new ArrayList<Nurse>(nurses);
		for (Nurse nurse : nurses) {
			if (nurse.getWeekHours() != 36) {
				nurseCopy.remove(nurse);
				continue;
			}
			for (Date day : schedule.keySet()) {
				if (schedule.get(day).get(ShiftType.LATE).contains(nurse)) {
					nurseCopy.remove(nurse);
					break;
				}
			}
		}
		if (nurseCopy.isEmpty()) {
			return nurses.get(0).getId();
		}
		return nurseCopy.get(0).getId();
	}
}
