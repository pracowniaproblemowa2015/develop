package com.scheduler.model;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.scheduler.service.impl.SchedulerServiceImpl.ShiftType;

public class Nurse {

	private int id;

	@JsonIgnore
	private int hours = 0;

	@JsonIgnore
	private int hoursLeft = 0;

	@JsonIgnore
	private int nightShifts = 0;

	@JsonIgnore
	private int weekHours = 0;

	@JsonIgnore
	private Map<Date, Map<ShiftType, List<Nurse>>> schedule;

	@JsonIgnore
	private Set<Date> workingDays;

	@JsonIgnore
	private Map<Integer, Integer> availableHours;

	@JsonIgnore
	private boolean singleOverWorked = false;

	public Nurse(int id, int allhours, int weekHours) {
		this.id = id;
		this.hours = allhours;
		this.hoursLeft = allhours;
		this.weekHours = weekHours;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getHoursLeft() {
		return hoursLeft;
	}

	public void setHoursLeft(int hoursLeft) {
		this.hoursLeft = hoursLeft;
	}

	public void subtractHours(int shifts, Date day) {
		this.hoursLeft = this.hoursLeft - (shifts * 8);
		Calendar cal = Calendar.getInstance();
		cal.setFirstDayOfWeek(Calendar.MONDAY);
		for (int i = 0; i < shifts; i++) {
			cal.setTime(day);
			cal.add(Calendar.DATE, i);
			if (availableHours.get(cal.get(Calendar.WEEK_OF_YEAR)) == null) {
				continue;
			}
			availableHours.put(cal.get(Calendar.WEEK_OF_YEAR), availableHours.get(cal.get(Calendar.WEEK_OF_YEAR)) - 8);
			workingDays.add(cal.getTime());
		}

	}

	public int getNightShifts() {
		return nightShifts;
	}

	public void setNightShifts(int nightShifts) {
		this.nightShifts = nightShifts;
	}

	public void resetWeekHours() {
		this.hoursLeft = this.hours;
	}

	public int getWeekHours() {
		return weekHours;
	}

	public void setWeekHours(int weekendHours) {
		this.weekHours = weekendHours;
	}

	public Map<Date, Map<ShiftType, List<Nurse>>> getSchedule() {
		return schedule;
	}

	public void setSchedule(Map<Date, Map<ShiftType, List<Nurse>>> schedule) {
		this.schedule = schedule;
		Calendar cal = Calendar.getInstance();
		cal.setFirstDayOfWeek(Calendar.MONDAY);
		availableHours = new HashMap<Integer, Integer>();
		for (Date day : schedule.keySet()) {
			cal.setTime(day);
			availableHours.put(cal.get(Calendar.WEEK_OF_YEAR), 48);
		}
		workingDays = new HashSet<Date>();

	}

	public boolean isSingleOverWorked() {
		return singleOverWorked;
	}

	public void setSingleOverWorked(boolean singleOverWorked) {
		this.singleOverWorked = singleOverWorked;
	}

	public boolean isWorking(Date day, int shifts) {
		Calendar cal = Calendar.getInstance();
		for (int i = 0; i < shifts; i++) {
			cal.setTime(day);
			cal.add(Calendar.DATE, i);
			if (workingDays.contains(cal.getTime())) {
				return true;
			}
			/*
			 * for (ShiftType type : ShiftType.values()) { if
			 * (schedule.get(cal.getTime()).get(type).contains(this)) { return
			 * true; } }
			 */
		}
		return false;
	}

	public int availableWeekHours(Date weekDay) {
		Calendar cal = Calendar.getInstance();
		cal.setFirstDayOfWeek(Calendar.MONDAY);
		cal.setTime(weekDay);
		int week = cal.get(Calendar.WEEK_OF_YEAR);
		return availableHours.get(week);

		/*
		 * int availableHours = 0; for (Date day : schedule.keySet()) {
		 * cal.setTime(day); if (cal.get(Calendar.WEEK_OF_YEAR) != week) {
		 * continue; } for (ShiftType shiftType : ShiftType.values()) { if
		 * (schedule.get(day).get(shiftType).contains(this)) { availableHours +=
		 * 8; } } } return 48 - availableHours;
		 */

	}

	public int freeWeekends() {
		Nurse nurse = this;
		Map<Date, ShiftType> nurseShedule = new HashMap<Date, ShiftType>();
		for (Date day : schedule.keySet()) {
			Map<ShiftType, List<Nurse>> shifts = schedule.get(day);
			for (ShiftType shiftType : shifts.keySet()) {
				List<Nurse> nursesOnShift = shifts.get(shiftType);
				if (nursesOnShift.contains(nurse)) {
					nurseShedule.put(day, shiftType);
				}
			}
		}
		Calendar cal = Calendar.getInstance();
		int freeWeekends = 0;
		for (Date day : schedule.keySet()) {
			cal.setTime(day);
			if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
				continue;
			}
			ShiftType saturdayShift = nurseShedule.get(day);
			cal.add(Calendar.DAY_OF_MONTH, -1);
			ShiftType fridayShift = nurseShedule.get(cal.getTime());
			cal.add(Calendar.DAY_OF_MONTH, 2);
			ShiftType sundayShift = nurseShedule.get(cal.getTime());
			cal.add(Calendar.DAY_OF_MONTH, 1);
			ShiftType mondayShift = nurseShedule.get(cal.getTime());

			if (freeWeekend(fridayShift, saturdayShift, sundayShift, mondayShift)) {
				freeWeekends++;
			}
		}
		return freeWeekends;
	}

	public boolean has42hoursRest(Date day) {
		Calendar cal = Calendar.getInstance();
		Nurse nurse = this;

		cal.setTime(day);
		cal.add(Calendar.DATE, -1);
		Date dayBefore = cal.getTime();
		cal.add(Calendar.DATE, -1);
		Date day2Before = cal.getTime();
		cal.add(Calendar.DATE, -1);
		Date day3Before = cal.getTime();

		if (schedule.get(day2Before) != null && schedule.get(dayBefore) != null
				&& schedule.get(day2Before).get(ShiftType.NIGHT).contains(nurse)
				&& schedule.get(dayBefore).get(ShiftType.NIGHT).contains(nurse)) {
			return false;
		}

		if (schedule.get(day3Before) != null && schedule.get(day2Before) != null
				&& schedule.get(day3Before).get(ShiftType.NIGHT).contains(nurse)
				&& schedule.get(day2Before).get(ShiftType.NIGHT).contains(nurse)) {
			return false;
		}

		return true;
	}

	@Override
	public String toString() {
		return "Nurse [id=" + id + ", hours=" + hours + ", hoursLeft=" + hoursLeft + "]";
	}

	private boolean freeWeekend(ShiftType fridayShift, ShiftType saturdayShift, ShiftType sundayShift,
			ShiftType mondayShift) {
		// free between Sat 00:00 to Mon 04:00.
		boolean freeFromSatToMon = saturdayShift == null && sundayShift == null
				&& (fridayShift == null || !fridayShift.equals(ShiftType.NIGHT));

		if (!freeFromSatToMon) {
			return false;
		}

		if (fridayShift != null && fridayShift.equals(ShiftType.LATE) && mondayShift != null
				&& (mondayShift.equals(ShiftType.EARLY) || mondayShift.equals(ShiftType.DAY))) {
			return false;
		}

		return true;
	}

}