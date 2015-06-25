package com.scheduler.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.scheduler.model.Nurse;
import com.scheduler.service.SchedulerService;

@Service
public class SchedulerServiceImpl implements SchedulerService {

	private static final Logger logger = LoggerFactory.getLogger(SchedulerServiceImpl.class);

	private static final int WEEKS = 5;

	public enum ShiftType {
		EARLY, DAY, LATE, NIGHT
	}

	public static void main(String[] args) {
		SchedulerServiceImpl service = new SchedulerServiceImpl();

		List<Nurse> nurses = new ArrayList<Nurse>();
		for (int i = 0; i < 12; i++) {
			nurses.add(new Nurse(i + 1, 36 * WEEKS + 4, 36));
		}
		nurses.add(new Nurse(13, 32 * WEEKS + 4, 32));
		for (int i = 0; i < 3; i++) {
			nurses.add(new Nurse(i + 14, 20 * WEEKS + 4, 20));
		}
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
		Date start = cal.getTime();

		Map<Date, Map<ShiftType, List<Nurse>>> schedule = service.getSchedule(nurses, start, null);

		logger.info("=================previous schedule===================");
		print(schedule);

		List<Date> days = new ArrayList<Date>(schedule.keySet());
		Collections.sort(days);
		Map<Date, Map<ShiftType, List<Nurse>>> lastWeek = new HashMap<Date, Map<ShiftType, List<Nurse>>>();
		int cnt = 0;
		for (int i = days.size() - 7; i < days.size(); i++) {
			cal.setTime(days.get(cnt));
			cal.add(Calendar.DATE, -7);
			lastWeek.put(cal.getTime(), schedule.get(days.get(i)));
			cnt++;
		}

		logger.info("=================last week===================");
		print(lastWeek);

		schedule = service.getSchedule(nurses, start, lastWeek);

		logger.info("=================final solution===================");
		print(schedule);

	}

	@Override
	public Map<Date, Map<ShiftType, List<Nurse>>> getSchedule(List<Nurse> nurses, Date start,
			Map<Date, Map<ShiftType, List<Nurse>>> lastWeek) {
		Map<Date, Map<ShiftType, List<Nurse>>> schedule = null;
		do {
			do {
				schedule = emptyShedule(start, lastWeek);
				for (Nurse nurse : nurses) {
					nurse.resetWeekHours();
					nurse.setNightShifts(0);
					nurse.setSchedule(schedule);
					nurse.setSingleOverWorked(false);
				}
				setWeekends(schedule, nurses);

			} while (!twoFreeWeekends(schedule, nurses));

			setNights2(schedule, nurses);
			setWeeksSets(schedule, nurses);
		} while (hardConstrainViolation(schedule, nurses));

		nursesWorkingTimeIsOK(schedule, nurses);

		logger.info("Soft constrain 1 points: {}", fromFridayToMondayNoOrMoreThanTwoShifts(schedule, nurses));
		logger.info("Soft constrain 3 points: {}", nightShiftsShoudBeTwoOrThree(schedule));
		return schedule;
	}

	private boolean hardConstrainViolation(Map<Date, Map<ShiftType, List<Nurse>>> schedule, List<Nurse> nurses) {
		if (!allShiftsAreSet(schedule)) {
			return true;
		}
		if (!twoFreeWeekends(schedule, nurses)) {
			return true;
		}
		if (!oneShiftPerDay(schedule)) {
			return true;
		}
		if (!max3NigtShifts(schedule, nurses)) {
			return true;
		}
		if (!free42hoursAftrTwoOrThreeNights(schedule, nurses)) {
			return true;
		}
		if (!restAfterNight(schedule)) {
			return true;
		}
		if (!max3NightsInRow(schedule)) {
			return true;
		}
		if (!max6ShiftsInRow(schedule, nurses)) {
			return true;
		}
		if (!oneNurseDoesNotTakeLateShift(schedule, nurses)) {
			return true;
		}

		return false;
	}

	private int nightShiftsShoudBeTwoOrThree(Map<Date, Map<ShiftType, List<Nurse>>> schedule) {
		List<Date> days = new ArrayList<Date>(schedule.keySet());
		Collections.sort(days);

		if (schedule.size() / 7 != WEEKS) {
			int cnt = 0;
			Iterator<Date> iterator = days.iterator();
			while (iterator.hasNext()) {
				iterator.next();
				iterator.remove();
				cnt++;
				if (cnt >= 7) {
					break;
				}
			}
		}
		int points = 0;
		Nurse nurse = null;
		int cnt = 0;
		for (Date day : days) {
			Nurse tmpNurse = schedule.get(day).get(ShiftType.NIGHT).get(0);
			if (nurse == null) {
				nurse = tmpNurse;
				cnt++;
			} else if (tmpNurse == nurse) {
				cnt++;
			} else {
				if (cnt < 2 || cnt > 3) {
					points += 1000;
				}
				nurse = tmpNurse;
				cnt = 1;
			}
		}

		return points;
	}

	private int fromFridayToMondayNoOrMoreThanTwoShifts(Map<Date, Map<ShiftType, List<Nurse>>> schedule,
			List<Nurse> nurses) {
		List<Date> days = new ArrayList<Date>(schedule.keySet());
		Collections.sort(days);

		if (schedule.size() / 7 != WEEKS) {
			int cnt = 0;
			Iterator<Date> iterator = days.iterator();
			while (iterator.hasNext()) {
				iterator.next();
				iterator.remove();
				cnt++;
				if (cnt >= 7) {
					break;
				}
			}
		}
		int points = 0;
		for (Nurse nurse : nurses) {
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

			for (Date day : days) {
				cal.setTime(day);
				if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
					continue;
				}
				ShiftType saturdayShift = nurseShedule.get(day);
				cal.add(Calendar.DAY_OF_MONTH, -1);
				ShiftType fridayShift = nurseShedule.get(cal.getTime());
				cal.add(Calendar.DAY_OF_MONTH, 2);
				ShiftType sundayShift = nurseShedule.get(cal.getTime());

				// no shift between Friday 22:00 to Monday 0:00 - OK
				if ((fridayShift == null || fridayShift.equals(ShiftType.EARLY) || fridayShift.equals(ShiftType.DAY))
						&& saturdayShift == null && sundayShift == null) {
					continue;
				}
				int shiftInThisWeekend = 0;

				// shift on Friday
				if (fridayShift != null && (fridayShift.equals(ShiftType.LATE) || fridayShift.equals(ShiftType.NIGHT))) {
					shiftInThisWeekend++;
				}

				if (saturdayShift != null) {
					shiftInThisWeekend++;
				}

				if (sundayShift != null) {
					shiftInThisWeekend++;
				}

				// two or more shift in this weekend - OK
				if (shiftInThisWeekend >= 2) {
					continue;
				}
				points += 1000;
			}
		}
		return points;
	}

	private boolean oneNurseDoesNotTakeLateShift(Map<Date, Map<ShiftType, List<Nurse>>> schedule, List<Nurse> nurses) {
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
			logger.info("Hard constrain violation: No nurse without LATE shift");
			return false;
		}
		logger.info("Nurses without LATE shift: {}", nurseCopy);
		return true;
	}

	private boolean max6ShiftsInRow(Map<Date, Map<ShiftType, List<Nurse>>> schedule, List<Nurse> nurses) {
		List<Date> days = new ArrayList<Date>(schedule.keySet());
		Collections.sort(days);
		int gcnt = 0;
		for (Nurse nurse : nurses) {
			int cnt = 0;
			for (Date day : days) {
				boolean workThisDay = false;
				for (ShiftType type : ShiftType.values()) {
					if (schedule.get(day).get(type).contains(nurse)) {
						workThisDay = true;
					}
				}
				if (workThisDay) {
					cnt++;
				} else {
					cnt = 0;
				}
			}
			if (cnt > 6) {
				gcnt++;
			}
		}
		if (gcnt > 0) {
			logger.info("Hard constrain violation: {} nurses have more than 6 working day in row after {}", gcnt);
			return false;
		}
		return true;
	}

	private boolean max3NightsInRow(Map<Date, Map<ShiftType, List<Nurse>>> schedule) {
		Calendar cal = Calendar.getInstance();

		for (Date day : schedule.keySet()) {
			cal.setTime(day);
			cal.add(Calendar.DATE, 1);
			Date dayAfter = cal.getTime();
			cal.add(Calendar.DATE, 1);
			Date day2After = cal.getTime();
			cal.add(Calendar.DATE, 1);
			Date day3After = cal.getTime();

			if (schedule.get(dayAfter) == null || schedule.get(day2After) == null) {
				continue;
			}
			Nurse nurse = schedule.get(day).get(ShiftType.NIGHT).get(0);
			if (schedule.get(dayAfter).get(ShiftType.NIGHT).contains(nurse)
					&& schedule.get(day2After).get(ShiftType.NIGHT).contains(nurse) && schedule.get(day3After) != null
					&& schedule.get(day3After).get(ShiftType.NIGHT).contains(nurse)) {
				logger.info("Hard constrain violation: Nurse {} have more than 3 nights in row after {}",
						nurse.getId(), day);
				return false;
			}
		}

		return true;
	}

	private boolean restAfterNight(Map<Date, Map<ShiftType, List<Nurse>>> schedule) {
		Calendar cal = Calendar.getInstance();
		for (Date day : schedule.keySet()) {
			cal.setTime(day);
			cal.add(Calendar.DATE, 1);
			Date nextDay = cal.getTime();
			Nurse nurse = schedule.get(day).get(ShiftType.NIGHT).get(0);
			if (schedule.get(nextDay) != null
					&& (schedule.get(nextDay).get(ShiftType.EARLY).contains(nurse)
							|| schedule.get(nextDay).get(ShiftType.DAY).contains(nurse) || schedule.get(nextDay)
							.get(ShiftType.LATE).contains(nurse))) {
				logger.info("Hard constrain violation: Nurse {} does not have 14 rest after {}", nurse.getId(), day);
				return false;
			}

		}
		return true;
	}

	private boolean free42hoursAftrTwoOrThreeNights(Map<Date, Map<ShiftType, List<Nurse>>> schedule, List<Nurse> nurses) {
		Calendar cal = Calendar.getInstance();
		for (Nurse nurse : nurses) {
			for (Date day : schedule.keySet()) {
				cal.setTime(day);
				cal.add(Calendar.DATE, -1);
				Date dayBefore = cal.getTime();
				cal.add(Calendar.DATE, 2);
				Date dayAfter = cal.getTime();
				cal.add(Calendar.DATE, 1);
				Date day2After = cal.getTime();
				// if not last day of nurse set of two or three night shifts
				// skip
				if (schedule.get(dayAfter) == null
						|| schedule.get(day2After) == null
						|| !(schedule.get(dayBefore) != null
								&& schedule.get(dayBefore).get(ShiftType.NIGHT).contains(nurse)
								&& schedule.get(day).get(ShiftType.NIGHT).contains(nurse) && !schedule.get(dayAfter)
								.get(ShiftType.NIGHT).contains(nurse))) {
					continue;
				}
				if (schedule.get(dayAfter).get(ShiftType.NIGHT).contains(nurse)
						|| schedule.get(dayAfter).get(ShiftType.EARLY).contains(nurse)
						|| schedule.get(dayAfter).get(ShiftType.DAY).contains(nurse)
						|| schedule.get(dayAfter).get(ShiftType.LATE).contains(nurse)
						|| schedule.get(day2After).get(ShiftType.NIGHT).contains(nurse)
						|| schedule.get(day2After).get(ShiftType.EARLY).contains(nurse)
						|| schedule.get(day2After).get(ShiftType.DAY).contains(nurse)
						|| schedule.get(day2After).get(ShiftType.LATE).contains(nurse)) {
					logger.info("Hard constrain violation: Nurse {} does not have 42 rest after {}", nurse.getId(), day);
					return false;
				}

			}
		}
		return true;
	}

	private boolean max3NigtShifts(Map<Date, Map<ShiftType, List<Nurse>>> schedule, List<Nurse> nurses) {
		List<Date> days = new ArrayList<Date>(schedule.keySet());
		Collections.sort(days);
		if (schedule.size() / 7 != WEEKS) {
			int cnt = 0;
			Iterator<Date> iterator = days.iterator();
			while (iterator.hasNext()) {
				iterator.next();
				iterator.remove();
				cnt++;
				if (cnt >= 7) {
					break;
				}
			}
		}
		for (Nurse nurse : nurses) {
			int nights = 0;
			for (Date day : days) {
				if (schedule.get(day).get(ShiftType.NIGHT).contains(nurse)) {
					nights++;
				}
			}
			if (nights > 3) {
				logger.info("Hard constrain violation: Nurse {} have {} night shifts", nurse.getId(), nights);
				return false;
			}
		}
		return true;
	}

	private boolean nursesWorkingTimeIsOK(Map<Date, Map<ShiftType, List<Nurse>>> schedule, List<Nurse> nurses) {
		List<Date> days = new ArrayList<Date>(schedule.keySet());
		for (Nurse nurse : nurses) {
			List<Date> nurseDays = new ArrayList<Date>();
			for (Date day : days) {
				Map<ShiftType, List<Nurse>> shifts = schedule.get(day);
				for (ShiftType shiftType : shifts.keySet()) {
					List<Nurse> nursesOnShift = shifts.get(shiftType);
					if (nursesOnShift.contains(nurse)) {
						// nurseShedule.put(day, shiftType);
						nurseDays.add(day);
					}
				}
			}

			Calendar cal = Calendar.getInstance();
			cal.setFirstDayOfWeek(Calendar.MONDAY);
			HashMap<Integer, Integer> nurseWeeks = new HashMap<Integer, Integer>();
			for (Date day : nurseDays) {
				cal.setTime(day);
				int weekNb = cal.get(Calendar.WEEK_OF_YEAR);
				if (nurseWeeks.get(weekNb) == null) {
					nurseWeeks.put(weekNb, 8);
				} else {
					nurseWeeks.put(weekNb, nurseWeeks.get(weekNb) + 8);
				}
			}

			for (Integer week : nurseWeeks.keySet()) {
				int workedHours = nurseWeeks.get(week);
				if (workedHours > nurse.getWeekHours()) {
					logger.info("{} {} " + workedHours, nurse, week);
				}
			}

		}
		// if (score > 16) {
		// return false;
		// }

		return true;
	}

	private boolean allShiftsAreSet(Map<Date, Map<ShiftType, List<Nurse>>> schedule) {
		int cnt = 0;
		for (Date day : schedule.keySet()) {
			Map<ShiftType, List<Nurse>> shifts = schedule.get(day);
			for (List<Nurse> nursesOnShift : shifts.values()) {
				for (Nurse nurse : nursesOnShift) {
					if (nurse == null) {
						cnt++;
						return false;
					}
				}
			}
		}
		if (cnt > 0) {
			// logger.info("Hard constrain violation: {} shifts not set", cnt);
			return false;
		}
		return true;
	}

	private boolean oneShiftPerDay(Map<Date, Map<ShiftType, List<Nurse>>> schedule) {
		for (Date day : schedule.keySet()) {
			Map<ShiftType, List<Nurse>> shifts = schedule.get(day);
			List<Nurse> nursesList = new ArrayList<Nurse>();
			Set<Nurse> nursesSet = new HashSet<Nurse>();
			for (List<Nurse> nursesOnShift : shifts.values()) {
				for (Nurse nurse : nursesOnShift) {
					if (nurse == null) {
						continue;
					}
					nursesList.add(nurse);
					nursesSet.add(nurse);
				}

			}
			if (nursesSet.size() != nursesList.size()) {
				logger.info("Hard constrain violation: some nurses appears more than once at {}", day);
				return false;
			}
		}
		return true;
	}

	private static boolean twoFreeWeekends(Map<Date, Map<ShiftType, List<Nurse>>> schedule, List<Nurse> nurses) {
		for (Nurse nurse : nurses) {
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
			if (freeWeekends < 2) {
				logger.info("Hard constrain violation: nurse {} have {} free weekend", nurse.getId(), freeWeekends);
				return false;
			}
		}
		return true;
	}

	private static boolean freeWeekend(ShiftType fridayShift, ShiftType saturdayShift, ShiftType sundayShift,
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

	private static void print(Map<Date, Map<ShiftType, List<Nurse>>> schedule) {
		List<Date> days = new ArrayList<Date>(schedule.keySet());
		Collections.sort(days);
		for (Date day : days) {
			System.out.println(day.toString());
			System.out.println("Earlt: " + schedule.get(day).get(ShiftType.EARLY));
			System.out.println("Day  : " + schedule.get(day).get(ShiftType.DAY));
			System.out.println("Late : " + schedule.get(day).get(ShiftType.LATE));
			System.out.println("Night: " + schedule.get(day).get(ShiftType.NIGHT));
		}
	}

	private void setWeeksSets(Map<Date, Map<ShiftType, List<Nurse>>> schedule, List<Nurse> nurses) {
		List<Date> days = new ArrayList<Date>(schedule.keySet());
		Collections.sort(days);

		for (ShiftType type : ShiftType.values()) {
			if (type.equals(ShiftType.NIGHT)) {
				continue;
			}
			List<Date> emptySet = new ArrayList<Date>();
			int maxSet = 5;
			int lastDay = 0;
			do {
				List<Nurse> availableNurses = new ArrayList<Nurse>(nurses);
				for (int i = lastDay; i < days.size(); i++) {
					Date day = days.get(i);
					lastDay = i;
					if (schedule.get(day).get(type).contains(null)) {
						emptySet.add(day);
						if (emptySet.size() >= maxSet) {
							break;
						}
					} else if (!emptySet.isEmpty()) {
						break;
					}
				}
				if (emptySet.isEmpty()) {
					break;
				}

				Calendar cal = Calendar.getInstance();
				cal.setTime(emptySet.get(0));
				cal.add(Calendar.DATE, -1);
				Date dayBefore = cal.getTime();
				cal.add(Calendar.DATE, -1);
				Date day2Before = cal.getTime();

				if (schedule.get(dayBefore) != null && schedule.get(dayBefore).get(ShiftType.NIGHT).get(0) != null) {
					availableNurses.remove(schedule.get(dayBefore).get(ShiftType.NIGHT).get(0));
				}
				if (schedule.get(day2Before) != null && schedule.get(day2Before).get(ShiftType.NIGHT).get(0) != null) {
					availableNurses.remove(schedule.get(day2Before).get(ShiftType.NIGHT).get(0));
				}

				List<Nurse> filteredNurses = filterNursesWithAvailableShifts(availableNurses, emptySet.size(),
						emptySet.get(0));
				if (filteredNurses.isEmpty()) {
					lastDay++;
					if (lastDay >= days.size() && maxSet <= 1) {
						break;
					}
					if (lastDay >= days.size()) {
						maxSet--;
						emptySet = new ArrayList<Date>();
						lastDay = 0;
					}
					continue;
				}

				//
				Nurse nurse = getNurseToWork(filteredNurses, emptySet.get(0));
				nurse.subtractHours(emptySet.size(), emptySet.get(0));

				for (Date day : emptySet) {
					int index = schedule.get(day).get(type).indexOf(null);
					schedule.get(day).get(type).set(index, nurse);
				}
				emptySet = new ArrayList<Date>();
				lastDay = 0;
			} while (true);
		}
	}

	private void setNights2(Map<Date, Map<ShiftType, List<Nurse>>> schedule, List<Nurse> nurses) {
		List<Date> days = new ArrayList<Date>(schedule.keySet());
		Collections.sort(days);

		for (ShiftType type : ShiftType.values()) {
			if (!type.equals(ShiftType.NIGHT)) {
				continue;
			}
			List<Date> emptySet = new ArrayList<Date>();
			int maxSet = 2;
			int lastDay = 0;
			do {
				List<Nurse> availableNurses = new ArrayList<Nurse>(nurses);
				for (int i = lastDay; i < days.size(); i++) {
					Date day = days.get(i);
					lastDay = i;
					if (schedule.get(day).get(type).contains(null)) {
						emptySet.add(day);
						if (emptySet.size() >= maxSet) {
							break;
						}
					} else if (!emptySet.isEmpty()) {
						break;
					}
				}
				if (emptySet.isEmpty()) {
					break;
				}
				List<Nurse> filteredNurses = filterNursesWithAvailableShifts(
						filterNursesWithAvailableNightShifts(availableNurses, emptySet.size()), emptySet.size(),
						emptySet.get(0));
				if (filteredNurses.isEmpty()) {
					lastDay++;
					if (lastDay >= days.size() && maxSet <= 1) {
						break;
					}
					if (lastDay >= days.size()) {
						maxSet--;
						emptySet = new ArrayList<Date>();
						lastDay = 0;
					}
					continue;
				}

				//
				Nurse nurse = getNurseToWork(filteredNurses, emptySet.get(0));
				nurse.subtractHours(emptySet.size(), emptySet.get(0));
				nurse.setNightShifts(nurse.getNightShifts() + emptySet.size());

				for (Date day : emptySet) {
					int index = schedule.get(day).get(type).indexOf(null);
					schedule.get(day).get(type).set(index, nurse);
				}
				emptySet = new ArrayList<Date>();
				lastDay = 0;
			} while (true);
		}
	}

	private void setWeekends(Map<Date, Map<ShiftType, List<Nurse>>> schedule, List<Nurse> nurses) {

		Calendar cal = Calendar.getInstance();
		List<Date> dates = new ArrayList<Date>(schedule.keySet());
		Collections.sort(dates);

		if (schedule.size() / 7 != WEEKS) {
			int cnt = 0;
			Iterator<Date> iterator = dates.iterator();
			while (iterator.hasNext()) {
				iterator.next();
				iterator.remove();
				cnt++;
				if (cnt >= 7) {
					break;
				}
			}
		}

		for (int i = 1; i < dates.size(); i++) {
			Date day = dates.get(i);
			cal.setTime(day);
			if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY && i != dates.size() - 1) {
				continue;
			}
			List<Nurse> availableNurses = new ArrayList<Nurse>(nurses);

			Iterator<Nurse> iterator = availableNurses.iterator();
			while (iterator.hasNext()) {
				Nurse nurse = iterator.next();
				if (nurse.freeWeekends() <= 2) {
					iterator.remove();
				}
			}

			if (i == dates.size() - 1) {
				cal.add(Calendar.DAY_OF_MONTH, 1);
			}

			Date monday = cal.getTime();
			Map<ShiftType, List<Nurse>> mondayShifts = schedule.get(monday);
			cal.add(Calendar.DAY_OF_MONTH, -1);
			Date sunday = cal.getTime();
			Map<ShiftType, List<Nurse>> sundayShift = schedule.get(sunday);
			cal.add(Calendar.DAY_OF_MONTH, -1);
			Date saturday = cal.getTime();
			Map<ShiftType, List<Nurse>> saturdayShifts = schedule.get(saturday);
			cal.add(Calendar.DAY_OF_MONTH, -1);
			Date friday = cal.getTime();
			Map<ShiftType, List<Nurse>> fridayShift = schedule.get(friday);

			if (i == dates.size() - 1) {
				Nurse nightNurse1 = getNurseToWorkAtNightAndSubtract(availableNurses, 3, friday);

				Nurse earlyNurse1 = getNurseToWorkAndSubtract(availableNurses, 3, friday);
				Nurse earlyNurse2 = getNurseToWorkAndSubtract(availableNurses, 3, friday);

				Nurse dayNurse1 = getNurseToWorkAndSubtract(availableNurses, 3, friday);
				Nurse dayNurse2 = getNurseToWorkAndSubtract(availableNurses, 3, friday);

				Nurse lateNurse1 = getNurseToWorkAndSubtract(availableNurses, 3, friday);
				Nurse lateNurse2 = getNurseToWorkAndSubtract(availableNurses, 3, friday);

				// fridayShift.put(ShiftType.EARLY, Arrays.asList(earlyNurse1,
				// earlyNurse2, null));
				fridayShift.put(ShiftType.DAY, Arrays.asList(dayNurse1, dayNurse2, earlyNurse1));
				fridayShift.put(ShiftType.LATE, Arrays.asList(lateNurse1, lateNurse2, earlyNurse2));
				fridayShift.put(ShiftType.NIGHT, Arrays.asList(nightNurse1));

				saturdayShifts.put(ShiftType.EARLY, Arrays.asList(earlyNurse1, earlyNurse2));
				saturdayShifts.put(ShiftType.DAY, Arrays.asList(dayNurse1, dayNurse2));
				saturdayShifts.put(ShiftType.LATE, Arrays.asList(lateNurse1, lateNurse2));
				saturdayShifts.put(ShiftType.NIGHT, Arrays.asList(nightNurse1));

				sundayShift.put(ShiftType.EARLY, Arrays.asList(earlyNurse1, earlyNurse2));
				sundayShift.put(ShiftType.DAY, Arrays.asList(dayNurse1, dayNurse2));
				sundayShift.put(ShiftType.LATE, Arrays.asList(lateNurse1, lateNurse2));
				sundayShift.put(ShiftType.NIGHT, Arrays.asList(nightNurse1));
			} else if (fridayShift != null && saturdayShifts != null && sundayShift != null && mondayShifts != null) {

				Nurse nightNurse1 = getNurseToWorkAtNightAndSubtract(availableNurses, 3, friday);

				Nurse earlyNurse1 = getNurseToWorkAndSubtract(availableNurses, 4, friday);
				Nurse earlyNurse2 = getNurseToWorkAndSubtract(availableNurses, 4, friday);

				Nurse dayNurse1 = getNurseToWorkAndSubtract(availableNurses, 4, friday);
				Nurse dayNurse2 = getNurseToWorkAndSubtract(availableNurses, 4, friday);

				Nurse lateNurse1 = getNurseToWorkAndSubtract(availableNurses, 4, friday);
				Nurse lateNurse2 = getNurseToWorkAndSubtract(availableNurses, 4, friday);

				// fridayShift.put(ShiftType.EARLY, Arrays.asList(earlyNurse1,
				// earlyNurse2, null));
				fridayShift.put(ShiftType.DAY, Arrays.asList(dayNurse1, dayNurse2, earlyNurse1));
				fridayShift.put(ShiftType.LATE, Arrays.asList(lateNurse1, lateNurse2, earlyNurse2));
				fridayShift.put(ShiftType.NIGHT, Arrays.asList(nightNurse1));

				saturdayShifts.put(ShiftType.EARLY, Arrays.asList(earlyNurse1, earlyNurse2));
				saturdayShifts.put(ShiftType.DAY, Arrays.asList(dayNurse1, dayNurse2));
				saturdayShifts.put(ShiftType.LATE, Arrays.asList(lateNurse1, lateNurse2));
				saturdayShifts.put(ShiftType.NIGHT, Arrays.asList(nightNurse1));

				sundayShift.put(ShiftType.EARLY, Arrays.asList(earlyNurse1, earlyNurse2));
				sundayShift.put(ShiftType.DAY, Arrays.asList(dayNurse1, dayNurse2));
				sundayShift.put(ShiftType.LATE, Arrays.asList(lateNurse1, lateNurse2));
				sundayShift.put(ShiftType.NIGHT, Arrays.asList(nightNurse1));

				mondayShifts.put(ShiftType.EARLY, Arrays.asList(earlyNurse1, earlyNurse2, null));
				mondayShifts.put(ShiftType.DAY, Arrays.asList(dayNurse1, dayNurse2, null));
				mondayShifts.put(ShiftType.LATE, Arrays.asList(lateNurse1, lateNurse2, null));

			}
		}
	}

	private Map<Date, Map<ShiftType, List<Nurse>>> emptyShedule(Date start,
			Map<Date, Map<ShiftType, List<Nurse>>> lastWeek) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(start);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		Map<Date, Map<ShiftType, List<Nurse>>> schedule = new LinkedHashMap<Date, Map<ShiftType, List<Nurse>>>();
		while (schedule.size() < WEEKS * 7) {
			boolean weekend = cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
					|| cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;

			Map<ShiftType, List<Nurse>> shifts = new HashMap<ShiftType, List<Nurse>>();
			shifts.put(ShiftType.EARLY, new ArrayList<Nurse>());
			shifts.get(ShiftType.EARLY).add(null);
			shifts.get(ShiftType.EARLY).add(null);
			if (!weekend) {
				shifts.get(ShiftType.EARLY).add(null);
			}

			shifts.put(ShiftType.DAY, new ArrayList<Nurse>());
			shifts.get(ShiftType.DAY).add(null);
			shifts.get(ShiftType.DAY).add(null);
			if (!weekend) {
				shifts.get(ShiftType.DAY).add(null);
			}

			shifts.put(ShiftType.LATE, new ArrayList<Nurse>());
			shifts.get(ShiftType.LATE).add(null);
			shifts.get(ShiftType.LATE).add(null);
			if (!weekend) {
				shifts.get(ShiftType.LATE).add(null);
			}

			shifts.put(ShiftType.NIGHT, new ArrayList<Nurse>());
			shifts.get(ShiftType.NIGHT).add(null);

			schedule.put(cal.getTime(), shifts);
			cal.add(Calendar.DATE, 1);
		}

		if (lastWeek != null) {
			for (Date day : lastWeek.keySet()) {
				schedule.put(day, lastWeek.get(day));
			}
		}

		return schedule;
	}

	private Nurse getNurseToWorkAndSubtract(List<Nurse> availableNurses, int shifts, Date day) {
		Nurse nurse = getNurseToWork(filterNursesWithAvailableShifts(availableNurses, shifts, day), day);
		if (nurse != null) {
			availableNurses.remove(nurse);
			nurse.subtractHours(shifts, day);
		}
		return nurse;
	}

	private Nurse getNurseToWorkAtNightAndSubtract(List<Nurse> availableNurses, int shifts, Date day) {
		Nurse nurse = getNurseToWork(
				filterNursesWithAvailableNightShifts(filterNursesWithAvailableShifts(availableNurses, shifts, day),
						shifts), day);
		if (nurse != null) {
			availableNurses.remove(nurse);
			nurse.setNightShifts(nurse.getNightShifts() + shifts);
			nurse.subtractHours(shifts, day);
		}
		return nurse;
	}

	// http://en.wikipedia.org/wiki/Exponential_distribution. Get by weight
	// (where weight for nurse is her available time)
	private Nurse getNurseToWork(List<Nurse> availableNurses, Date day) {
		if (availableNurses.isEmpty()) {
			return null;
		}
		int result = -1;
		double bestValue = Double.MAX_VALUE;
		Random random = new Random();
		for (int i = 0; i < availableNurses.size(); i++) {
			Nurse nurse = availableNurses.get(i);
			double value = -Math.log(random.nextDouble()) / nurse.getHoursLeft();

			if (value < bestValue) {
				bestValue = value;
				result = i;
			}
		}
		Nurse nurse = availableNurses.get(result);
		return nurse;
	}

	private List<Nurse> filterNursesWithAvailableShifts(List<Nurse> nurses, int shifts, Date day) {
		List<Nurse> nursesWithAvailableShifts = new ArrayList<Nurse>(nurses);
		Iterator<Nurse> iterator = nursesWithAvailableShifts.iterator();
		while (iterator.hasNext()) {
			Nurse nurse = iterator.next();
			if (nurse.isWorking(day, shifts) || nurse.getHoursLeft() < 8 * shifts
					|| nurse.availableWeekHours(day) < 8 * shifts) {
				iterator.remove();
				continue;
			}
		}
		return nursesWithAvailableShifts;
	}

	private List<Nurse> filterNursesWithAvailableNightShifts(List<Nurse> nurses, int shifts) {
		List<Nurse> nursesWithAvailableShifts = new ArrayList<Nurse>(nurses);
		Iterator<Nurse> iterator = nursesWithAvailableShifts.iterator();
		while (iterator.hasNext()) {
			Nurse nurse = iterator.next();
			if (nurse.getNightShifts() + shifts > 3) {
				iterator.remove();
			}
		}
		return nursesWithAvailableShifts;
	}

}
