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

import com.scheduler.service.SchedulerService;

public class SchedulerService2Impl implements SchedulerService {

	private static final Logger logger = LoggerFactory.getLogger(SchedulerService2Impl.class);

	private static final int WEEKS = 5;

	public enum ShiftType {
		EARLY, DAY, LATE, NIGHT
	}

	public static void main(String[] args) {
		SchedulerService2Impl service = new SchedulerService2Impl();

		List<Nurse> nurses = new ArrayList<Nurse>();
		for (int i = 0; i < 12; i++) {
			nurses.add(new Nurse(36 * WEEKS + 4, 36));
		}
		nurses.add(new Nurse(32 * WEEKS + 4, 32));
		for (int i = 0; i < 3; i++) {
			nurses.add(new Nurse(20 * WEEKS + 4, 20));
		}
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

		Map<Date, Map<ShiftType, List<Nurse>>> schedule = service.getSchedule(nurses, cal.getTime());

		logger.info("=================final solution===================");
		for (Date day : schedule.keySet()) {
			logger.info("{}", day.toString());
			logger.info("Earlt: {}", schedule.get(day).get(ShiftType.EARLY));
			logger.info("Day  : {}", schedule.get(day).get(ShiftType.DAY));
			logger.info("Late : {}", schedule.get(day).get(ShiftType.LATE));
			logger.info("Night: {}", schedule.get(day).get(ShiftType.NIGHT));
		}
		int hoursLeft = 0;
		for (Nurse nurse : nurses) {
			hoursLeft += nurse.getHoursLeft();
		}
		logger.info("Hours left: {}", hoursLeft);

	}

	public Map<Date, Map<ShiftType, List<Nurse>>> getSchedule(List<Nurse> nurses, Date start) {
		Map<Date, Map<ShiftType, List<Nurse>>> schedule = null;
		int cnt = 0;
		do {
			cnt++;
			do {
				schedule = emptyShedule(start);
				for (Nurse nurse : nurses) {
					nurse.resetWeekHours();
					nurse.setNightShifts(0);
					nurse.setSchedule(schedule);
					nurse.setSingleOverWorked(false);
				}
				setWeekends(schedule, nurses);

			} while (!twoFreeWeekends(schedule, nurses));
			setNights(schedule, nurses);
			setWeeksSets(schedule, nurses);
			// fillEmptyShifts(schedule, nurses);
		} while (hardConstrainViolation(schedule, nurses));

		nursesWorkingTimeIsOK(schedule, nurses);
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
				logger.info("Hard constrain violation: Nurse {} have more than 3 nights in row after {}", nurse.id, day);
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
				logger.info("Hard constrain violation: Nurse {} does not have 14 rest after {}", nurse.id, day);
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
					logger.info("Hard constrain violation: Nurse {} does not have 42 rest after {}", nurse.id, day);
					return false;
				}

			}
		}
		return true;
	}

	private boolean max3NigtShifts(Map<Date, Map<ShiftType, List<Nurse>>> schedule, List<Nurse> nurses) {
		for (Nurse nurse : nurses) {
			int nights = 0;
			for (Date day : schedule.keySet()) {
				if (schedule.get(day).get(ShiftType.NIGHT).contains(nurse)) {
					nights++;
				}
			}
			if (nights > 3) {
				logger.info("Hard constrain violation: Nurse {} have {} night shifts", nurse.id, nights);
				return false;
			}
		}
		return true;
	}

	private boolean nursesWorkingTimeIsOK(Map<Date, Map<ShiftType, List<Nurse>>> schedule, List<Nurse> nurses) {
		List<Date> days = new ArrayList<Date>(schedule.keySet());
		int score = 0;
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
					score++;
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
				// logger.info("Hard constrain violation: nurse {} have {} free weekend",
				// nurse.id, freeWeekends);
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

	private void fillEmptyShifts(Map<Date, Map<ShiftType, List<Nurse>>> schedule, List<Nurse> nurses) {
		int cnt = 0;
		while (!allShiftsAreSet(schedule) && cnt < 100) {
			cnt++;
			Map<Date, Set<Nurse>> mayWorkIn = new HashMap<Date, Set<Nurse>>();
			for (Nurse nurse : nurses) {
				for (Date day : schedule.keySet()) {
					Map<ShiftType, List<Nurse>> shifts = schedule.get(day);
					for (ShiftType type : ShiftType.values()) {
						if (shifts.get(type).contains(null) && mayWorkIn.get(day) == null) {
							mayWorkIn.put(day, new HashSet<Nurse>());
						}
						List<Nurse> singletonNurse = new ArrayList<Nurse>();
						singletonNurse.add(nurse);
						removeNursesAfterNight(singletonNurse, schedule, day);
						if (shifts.get(type).contains(null) && !shifts.get(type).contains(nurse)
								&& !singletonNurse.isEmpty()) {

							mayWorkIn.get(day).add(nurse);

						}
					}

				}
			}
			if (mayWorkIn.isEmpty()) {
				break;
			}
			int min = Integer.MAX_VALUE;
			Date minDay = null;
			for (Date day : mayWorkIn.keySet()) {
				if (!mayWorkIn.get(day).isEmpty() && mayWorkIn.get(day).size() < min) {
					min = mayWorkIn.get(day).size();
					minDay = day;
				}

			}
			if (minDay == null) {
				break;
			}
			Map<ShiftType, List<Nurse>> shifts = schedule.get(minDay);
			List<Nurse> availableNurses = new ArrayList<Nurse>(nurses);
			for (ShiftType type : ShiftType.values()) {
				for (int i = 0; i < shifts.get(type).size(); i++) {
					Nurse shiftNurse = shifts.get(type).get(i);
					if (shiftNurse == null) {
						if (ShiftType.NIGHT.equals(type)) {
							shifts.get(type).set(i, getNurseToWorkAtNightAndSubtract(availableNurses, 1, minDay));
						} else {
							shifts.get(type).set(i, getNurseToWorkAndSubtract(availableNurses, 1, minDay));
						}
					}
				}
			}
		}

		// logger.info(mayWorkIn.toString());
	}

	private void print(Map<Date, Map<ShiftType, List<Nurse>>> schedule) {
		for (Date day : schedule.keySet()) {
			System.out.println(day.toString());
			System.out.println("Earlt: " + schedule.get(day).get(ShiftType.EARLY));
			System.out.println("Day  : " + schedule.get(day).get(ShiftType.DAY));
			System.out.println("Late : " + schedule.get(day).get(ShiftType.LATE));
			System.out.println("Night: " + schedule.get(day).get(ShiftType.NIGHT));
		}
	}

	private void setWeeksSets(Map<Date, Map<ShiftType, List<Nurse>>> schedule, List<Nurse> nurses) {
		// at this point only Tuesday, Wednesday, and Thursday are empty;
		for (ShiftType type : ShiftType.values()) {
			if (type.equals(ShiftType.NIGHT)) {
				continue;
			}
			Calendar cal = Calendar.getInstance();
			Map<Date, List<Date>> emptyShiftSets = getEmptyShiftSets(schedule, type, 3);
			for (Date weekday : emptyShiftSets.keySet()) {
				List<Date> nextEmptyShiftSet = emptyShiftSets.get(weekday);
				List<Nurse> availableNurses = new ArrayList<Nurse>(nurses);
				for (Date day : nextEmptyShiftSet) {
					for (ShiftType shift : ShiftType.values()) {
						availableNurses.removeAll(schedule.get(day).get(shift));
					}
				}

				removeNursesAfterNight(availableNurses, schedule, weekday);

				if (filterNursesWithAvailableShifts(availableNurses, 3, weekday).size() >= 3) {

					Nurse nurse1 = getNurseToWorkAndSubtract(availableNurses, nextEmptyShiftSet.size(), weekday);
					Nurse nurse2 = getNurseToWorkAndSubtract(availableNurses, nextEmptyShiftSet.size(), weekday);
					Nurse nurse3 = getNurseToWorkAndSubtract(availableNurses, nextEmptyShiftSet.size(), weekday);
					logger.debug(Arrays.asList(nurse1, nurse2, nurse3).toString());

					for (Date day : nextEmptyShiftSet) {
						schedule.get(day).get(type).set(0, nurse1);
						schedule.get(day).get(type).set(1, nurse2);
						schedule.get(day).get(type).set(2, nurse3);
					}
				}
			}

			emptyShiftSets = getEmptyShiftSets(schedule, type, 2);
			for (Date weekday : emptyShiftSets.keySet()) {
				List<Date> nextEmptyShiftSet = emptyShiftSets.get(weekday);
				List<Nurse> availableNurses = new ArrayList<Nurse>(nurses);
				for (Date day : nextEmptyShiftSet) {
					for (ShiftType shift : ShiftType.values()) {
						availableNurses.removeAll(schedule.get(day).get(shift));
					}
				}

				removeNursesAfterNight(availableNurses, schedule, weekday);

				if (filterNursesWithAvailableShifts(availableNurses, 2, weekday).size() >= 3) {

					Nurse nurse1 = getNurseToWorkAndSubtract(availableNurses, nextEmptyShiftSet.size(), weekday);
					Nurse nurse2 = getNurseToWorkAndSubtract(availableNurses, nextEmptyShiftSet.size(), weekday);
					Nurse nurse3 = getNurseToWorkAndSubtract(availableNurses, nextEmptyShiftSet.size(), weekday);
					logger.debug(Arrays.asList(nurse1, nurse2, nurse3).toString());

					for (Date day : nextEmptyShiftSet) {
						schedule.get(day).get(type).set(0, nurse1);
						schedule.get(day).get(type).set(1, nurse2);
						schedule.get(day).get(type).set(2, nurse3);
					}
				}
			}

			emptyShiftSets = getEmptyShiftSets(schedule, type, 1);
			for (Date weekday : emptyShiftSets.keySet()) {
				List<Date> nextEmptyShiftSet = emptyShiftSets.get(weekday);
				List<Nurse> availableNurses = new ArrayList<Nurse>(nurses);
				for (Date day : nextEmptyShiftSet) {
					for (ShiftType shift : ShiftType.values()) {
						availableNurses.removeAll(schedule.get(day).get(shift));
					}
				}

				removeNursesAfterNight(availableNurses, schedule, weekday);

				if (filterNursesWithAvailableShifts(availableNurses, 1, weekday).size() >= 3) {

					Nurse nurse1 = getNurseToWorkAndSubtract(availableNurses, nextEmptyShiftSet.size(), weekday);
					Nurse nurse2 = getNurseToWorkAndSubtract(availableNurses, nextEmptyShiftSet.size(), weekday);
					Nurse nurse3 = getNurseToWorkAndSubtract(availableNurses, nextEmptyShiftSet.size(), weekday);
					logger.debug(Arrays.asList(nurse1, nurse2, nurse3).toString());

					for (Date day : nextEmptyShiftSet) {
						schedule.get(day).get(type).set(0, nurse1);
						schedule.get(day).get(type).set(1, nurse2);
						schedule.get(day).get(type).set(2, nurse3);
					}
				}
			}
		}
	}

	private void setNights(Map<Date, Map<ShiftType, List<Nurse>>> schedule, List<Nurse> nurses) {
		// at this point only Tuesday, Wednesday, and Thursday are empty;
		ShiftType type = ShiftType.NIGHT;

		Map<Date, List<Date>> emptyShiftSets = getEmptyShiftSets(schedule, type, 3);
		for (Date weekday : emptyShiftSets.keySet()) {
			List<Date> nextEmptyShiftSet = emptyShiftSets.get(weekday);
			List<Nurse> availableNurses = new ArrayList<Nurse>(nurses);
			for (Date day : nextEmptyShiftSet) {
				for (ShiftType shift : ShiftType.values()) {
					availableNurses.removeAll(schedule.get(day).get(shift));
				}
			}

			if (filterNursesWithAvailableNightShifts(filterNursesWithAvailableShifts(availableNurses, 3, weekday), 3)
					.size() >= 1) {

				Nurse nurse1 = getNurseToWorkAtNightAndSubtract(availableNurses, nextEmptyShiftSet.size(), weekday);
				logger.debug(Arrays.asList(nurse1).toString());

				for (Date day : nextEmptyShiftSet) {
					schedule.get(day).get(type).set(0, nurse1);
				}
			}
		}

		emptyShiftSets = getEmptyShiftSets(schedule, type, 2);
		for (Date weekday : emptyShiftSets.keySet()) {
			List<Date> nextEmptyShiftSet = emptyShiftSets.get(weekday);
			List<Nurse> availableNurses = new ArrayList<Nurse>(nurses);
			for (Date day : nextEmptyShiftSet) {
				for (ShiftType shift : ShiftType.values()) {
					availableNurses.removeAll(schedule.get(day).get(shift));
				}
			}

			if (filterNursesWithAvailableNightShifts(filterNursesWithAvailableShifts(availableNurses, 2, weekday), 2)
					.size() >= 1) {

				Nurse nurse1 = getNurseToWorkAtNightAndSubtract(availableNurses, nextEmptyShiftSet.size(), weekday);
				logger.debug(Arrays.asList(nurse1).toString());

				for (Date day : nextEmptyShiftSet) {
					schedule.get(day).get(type).set(0, nurse1);
				}
			}
		}

		emptyShiftSets = getEmptyShiftSets(schedule, type, 1);
		for (Date weekday : emptyShiftSets.keySet()) {
			List<Date> nextEmptyShiftSet = emptyShiftSets.get(weekday);
			List<Nurse> availableNurses = new ArrayList<Nurse>(nurses);
			for (Date day : nextEmptyShiftSet) {
				for (ShiftType shift : ShiftType.values()) {
					availableNurses.removeAll(schedule.get(day).get(shift));
				}
			}

			if (filterNursesWithAvailableNightShifts(filterNursesWithAvailableShifts(availableNurses, 1, weekday), 1)
					.size() >= 1) {

				Nurse nurse1 = getNurseToWorkAtNightAndSubtract(availableNurses, nextEmptyShiftSet.size(), weekday);
				logger.debug(Arrays.asList(nurse1).toString());

				for (Date day : nextEmptyShiftSet) {
					schedule.get(day).get(type).set(0, nurse1);
				}
			}
		}
	}

	private Map<Date, List<Date>> getEmptyShiftSets(Map<Date, Map<ShiftType, List<Nurse>>> schedule, ShiftType type,
			int cnt) {

		Calendar cal = Calendar.getInstance();
		Map<Date, List<Date>> shiftSets = new LinkedHashMap<Date, List<Date>>();
		List<Date> days = new ArrayList<Date>(schedule.keySet());
		Collections.sort(days);
		if (cnt == 3) {
			for (Date firstDay : days) {
				cal.setTime(firstDay);
				cal.add(Calendar.DATE, 1);
				Date secondDay = cal.getTime();
				cal.add(Calendar.DATE, 1);
				Date thirdDay = cal.getTime();

				if (schedule.get(firstDay) != null && schedule.get(secondDay) != null && schedule.get(thirdDay) != null) {
					boolean firstEmpty = true;
					for (Nurse nurse : schedule.get(firstDay).get(type)) {
						if (nurse != null) {
							firstEmpty = false;
						}
					}
					boolean secondEmpty = true;
					for (Nurse nurse : schedule.get(secondDay).get(type)) {
						if (nurse != null) {
							secondEmpty = false;
						}
					}
					boolean thirdEmpty = true;
					for (Nurse nurse : schedule.get(thirdDay).get(type)) {
						if (nurse != null) {
							thirdEmpty = false;
						}
					}
					if (firstEmpty && secondEmpty && thirdEmpty) {

						shiftSets.put(firstDay, Arrays.asList(firstDay, secondDay, thirdDay));
					}
				}
			}
		}
		if (cnt == 2) {
			for (Date firstDay : schedule.keySet()) {
				cal.setTime(firstDay);
				cal.add(Calendar.DATE, 1);
				Date secondDay = cal.getTime();

				if (schedule.get(firstDay) != null && schedule.get(secondDay) != null) {
					boolean firstEmpty = true;
					for (Nurse nurse : schedule.get(firstDay).get(type)) {
						if (nurse != null) {
							firstEmpty = false;
						}
					}
					boolean secondEmpty = true;
					for (Nurse nurse : schedule.get(secondDay).get(type)) {
						if (nurse != null) {
							secondEmpty = false;
						}
					}
					if (firstEmpty && secondEmpty) {
						shiftSets.put(firstDay, Arrays.asList(firstDay, secondDay));
					}
				}
			}
		}
		if (cnt == 1) {
			for (Date firstDay : schedule.keySet()) {
				cal.setTime(firstDay);

				if (schedule.get(firstDay) != null) {
					boolean firstEmpty = true;
					for (Nurse nurse : schedule.get(firstDay).get(type)) {
						if (nurse != null) {
							firstEmpty = false;
						}
					}
					if (firstEmpty) {
						shiftSets.put(firstDay, Arrays.asList(firstDay));
					}
				}
			}
		}
		return shiftSets;
	}

	private Map<Date, Map<ShiftType, List<Nurse>>> emptyShedule(Date start) {
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
		return schedule;
	}

	private void setWeekends(Map<Date, Map<ShiftType, List<Nurse>>> schedule, List<Nurse> nurses) {

		Calendar cal = Calendar.getInstance();
		List<Date> dates = new ArrayList<Date>(schedule.keySet());
		Collections.sort(dates);

		for (int i = 0; i < dates.size(); i++) {
			Date day = dates.get(i);
			cal.setTime(day);
			if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY && i != dates.size() - 1) {
				continue;
			}
			List<Nurse> availableNurses = new ArrayList<Nurse>(nurses);

			if (i == dates.size() - 1) {
				cal.add(Calendar.DAY_OF_MONTH, 1);
			}

			Map<ShiftType, List<Nurse>> mondayShifts = schedule.get(cal.getTime());
			cal.add(Calendar.DAY_OF_MONTH, -1);
			Map<ShiftType, List<Nurse>> sundayShift = schedule.get(cal.getTime());
			cal.add(Calendar.DAY_OF_MONTH, -1);
			Map<ShiftType, List<Nurse>> saturdayShifts = schedule.get(cal.getTime());
			cal.add(Calendar.DAY_OF_MONTH, -1);
			Map<ShiftType, List<Nurse>> fridayShift = schedule.get(cal.getTime());

			if (fridayShift != null && saturdayShifts != null && sundayShift != null && mondayShifts != null) {

				Nurse nightNurse1 = getNurseToWorkAtNightAndSubtract(availableNurses, 3, day);

				Nurse earlyNurse1 = getNurseToWorkAndSubtract(availableNurses, 4, day);
				Nurse earlyNurse2 = getNurseToWorkAndSubtract(availableNurses, 4, day);

				Nurse dayNurse1 = getNurseToWorkAndSubtract(availableNurses, 4, day);
				Nurse dayNurse2 = getNurseToWorkAndSubtract(availableNurses, 4, day);

				Nurse lateNurse1 = getNurseToWorkAndSubtract(availableNurses, 4, day);
				Nurse lateNurse2 = getNurseToWorkAndSubtract(availableNurses, 4, day);

				// Nurse fridayNight =
				// getNurseToWorkAtNightAndSubtract(availableNurses, 1);
				Nurse fridayErly = getNurseToWorkAndSubtract(availableNurses, 1, day);
				Nurse fridayDay = getNurseToWorkAndSubtract(availableNurses, 1, day);
				Nurse fridayLate = getNurseToWorkAndSubtract(availableNurses, 1, day);

				Nurse mondayErly = getNurseToWorkAndSubtract(availableNurses, 1, day);
				Nurse mondayDay = getNurseToWorkAndSubtract(availableNurses, 1, day);
				Nurse mondayLate = getNurseToWorkAndSubtract(availableNurses, 1, day);

				// fridayShift.put(ShiftType.NIGHT, Arrays.asList(fridayNight));
				fridayShift.put(ShiftType.EARLY, Arrays.asList(earlyNurse1, earlyNurse2, fridayErly));
				fridayShift.put(ShiftType.DAY, Arrays.asList(dayNurse1, dayNurse2, fridayDay));
				fridayShift.put(ShiftType.LATE, Arrays.asList(lateNurse1, lateNurse2, fridayLate));

				saturdayShifts.put(ShiftType.EARLY, Arrays.asList(earlyNurse1, earlyNurse2));
				saturdayShifts.put(ShiftType.DAY, Arrays.asList(dayNurse1, dayNurse2));
				saturdayShifts.put(ShiftType.LATE, Arrays.asList(lateNurse1, lateNurse2));
				saturdayShifts.put(ShiftType.NIGHT, Arrays.asList(nightNurse1));

				sundayShift.put(ShiftType.EARLY, Arrays.asList(earlyNurse1, earlyNurse2));
				sundayShift.put(ShiftType.DAY, Arrays.asList(dayNurse1, dayNurse2));
				sundayShift.put(ShiftType.LATE, Arrays.asList(lateNurse1, lateNurse2));
				sundayShift.put(ShiftType.NIGHT, Arrays.asList(nightNurse1));

				mondayShifts.put(ShiftType.EARLY, Arrays.asList(earlyNurse1, earlyNurse2, mondayErly));
				mondayShifts.put(ShiftType.DAY, Arrays.asList(dayNurse1, dayNurse2, mondayDay));
				mondayShifts.put(ShiftType.LATE, Arrays.asList(lateNurse1, lateNurse2, mondayLate));
				mondayShifts.put(ShiftType.NIGHT, Arrays.asList(nightNurse1));

			} else if (i == 0) {
				Nurse nightNurse1 = getNurseToWorkAtNightAndSubtract(availableNurses, 1, day);

				Nurse earlyNurse1 = getNurseToWorkAndSubtract(availableNurses, 1, day);
				Nurse earlyNurse2 = getNurseToWorkAndSubtract(availableNurses, 1, day);
				Nurse earlyNurse3 = getNurseToWorkAndSubtract(availableNurses, 1, day);

				Nurse dayNurse1 = getNurseToWorkAndSubtract(availableNurses, 1, day);
				Nurse dayNurse2 = getNurseToWorkAndSubtract(availableNurses, 1, day);
				Nurse dayNurse3 = getNurseToWorkAndSubtract(availableNurses, 1, day);

				Nurse lateNurse1 = getNurseToWorkAndSubtract(availableNurses, 1, day);
				Nurse lateNurse2 = getNurseToWorkAndSubtract(availableNurses, 1, day);
				Nurse lateNurse3 = getNurseToWorkAndSubtract(availableNurses, 1, day);

				mondayShifts.put(ShiftType.EARLY, Arrays.asList(earlyNurse1, earlyNurse2, earlyNurse3));
				mondayShifts.put(ShiftType.DAY, Arrays.asList(dayNurse1, dayNurse2, dayNurse3));
				mondayShifts.put(ShiftType.LATE, Arrays.asList(lateNurse1, lateNurse2, lateNurse3));
				mondayShifts.put(ShiftType.NIGHT, Arrays.asList(nightNurse1));

			} else if (i == dates.size() - 1) {

				Nurse nightNurse1 = getNurseToWorkAtNightAndSubtract(availableNurses, 2, day);

				Nurse earlyNurse1 = getNurseToWorkAndSubtract(availableNurses, 3, day);
				Nurse earlyNurse2 = getNurseToWorkAndSubtract(availableNurses, 3, day);

				Nurse dayNurse1 = getNurseToWorkAndSubtract(availableNurses, 3, day);
				Nurse dayNurse2 = getNurseToWorkAndSubtract(availableNurses, 3, day);

				Nurse lateNurse1 = getNurseToWorkAndSubtract(availableNurses, 3, day);
				Nurse lateNurse2 = getNurseToWorkAndSubtract(availableNurses, 3, day);

				Nurse fridayNight = getNurseToWorkAtNightAndSubtract(availableNurses, 1, day);
				Nurse fridayErly = getNurseToWorkAndSubtract(availableNurses, 1, day);
				Nurse fridayDay = getNurseToWorkAndSubtract(availableNurses, 1, day);
				Nurse fridayLate = getNurseToWorkAndSubtract(availableNurses, 1, day);

				fridayShift.put(ShiftType.NIGHT, Arrays.asList(fridayNight));
				fridayShift.put(ShiftType.EARLY, Arrays.asList(earlyNurse1, earlyNurse2, fridayErly));
				fridayShift.put(ShiftType.DAY, Arrays.asList(dayNurse1, dayNurse2, fridayDay));
				fridayShift.put(ShiftType.LATE, Arrays.asList(lateNurse1, lateNurse2, fridayLate));

				saturdayShifts.put(ShiftType.EARLY, Arrays.asList(earlyNurse1, earlyNurse2));
				saturdayShifts.put(ShiftType.DAY, Arrays.asList(dayNurse1, dayNurse2));
				saturdayShifts.put(ShiftType.LATE, Arrays.asList(lateNurse1, lateNurse2));
				saturdayShifts.put(ShiftType.NIGHT, Arrays.asList(nightNurse1));

				sundayShift.put(ShiftType.EARLY, Arrays.asList(earlyNurse1, earlyNurse2));
				sundayShift.put(ShiftType.DAY, Arrays.asList(dayNurse1, dayNurse2));
				sundayShift.put(ShiftType.LATE, Arrays.asList(lateNurse1, lateNurse2));
				sundayShift.put(ShiftType.NIGHT, Arrays.asList(nightNurse1));
			}
		}
	}

	private Nurse getNurseToWorkAndSubtract(List<Nurse> availableNurses, int shifts, Date day) {
		Nurse nurse = getNurseToWork(filterNursesWithAvailableShifts(availableNurses, shifts, day), day);
		if (nurse != null) {
			availableNurses.remove(nurse);
			nurse.subtractHours(8 * shifts);
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
			nurse.subtractHours(8 * shifts);
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

	private void removeNursesAfterNight(List<Nurse> availableNurses, Map<Date, Map<ShiftType, List<Nurse>>> schedule,
			Date day) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(day);
		cal.add(Calendar.DATE, -1);
		cal.add(Calendar.DATE, -1);
		Date day2Before = cal.getTime();
		cal.add(Calendar.DATE, -1);
		Date day3Before = cal.getTime();
		if (schedule.get(day3Before) != null
				&& schedule.get(day3Before).get(ShiftType.NIGHT).equals(schedule.get(day2Before).get(ShiftType.NIGHT))) {
			availableNurses.remove(schedule.get(day2Before).get(ShiftType.NIGHT).get(0));
		}
	}

	private List<Nurse> filterNursesWithAvailableShifts(List<Nurse> nurses, int shifts, Date day) {
		List<Nurse> nursesWithAvailableShifts = new ArrayList<Nurse>(nurses);
		Iterator<Nurse> iterator = nursesWithAvailableShifts.iterator();
		while (iterator.hasNext()) {
			Nurse nurse = iterator.next();
			if (nurse.isWorking(day) || nurse.getHoursLeft() < 8 * shifts || nurse.availableWeekHours(day) < 8 * shifts) {
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

	public static class Nurse {
		private static int lastId = 0;
		private int id;
		private int hours = 0;
		private int hoursLeft = 0;
		private int nightShifts = 0;
		private int weekHours = 0;
		private Map<Date, Map<ShiftType, List<Nurse>>> schedule;

		private boolean singleOverWorked = false;

		public Nurse(int allhours, int weekHours) {
			lastId++;
			this.id = lastId;
			this.hours = allhours;
			this.hoursLeft = allhours;
			this.weekHours = weekHours;
		}

		public int getHoursLeft() {
			return hoursLeft;
		}

		public void setHoursLeft(int hoursLeft) {
			this.hoursLeft = hoursLeft;
		}

		public void subtractHours(int hours) {
			this.hoursLeft = this.hoursLeft - hours;
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
		}

		public boolean isSingleOverWorked() {
			return singleOverWorked;
		}

		public void setSingleOverWorked(boolean singleOverWorked) {
			this.singleOverWorked = singleOverWorked;
		}

		public boolean isWorking(Date day) {
			for (ShiftType type : ShiftType.values()) {
				if (schedule.get(day).get(type).contains(this)) {
					return true;
				}
			}
			return false;
		}

		public int availableWeekHours(Date weekDay) {
			Calendar cal = Calendar.getInstance();
			cal.setFirstDayOfWeek(Calendar.MONDAY);
			cal.setTime(weekDay);
			int week = cal.get(Calendar.WEEK_OF_YEAR);
			int availableHours = 0;
			for (Date day : schedule.keySet()) {
				cal.setTime(day);
				if (cal.get(Calendar.WEEK_OF_YEAR) != week) {
					continue;
				}
				for (ShiftType shiftType : ShiftType.values()) {
					if (schedule.get(day).get(shiftType).contains(this)) {
						availableHours += 8;
					}
				}
			}
			return 48 - availableHours;

		}

		@Override
		public String toString() {
			return "Nurse [id=" + id + ", hours=" + hours + ", hoursLeft=" + hoursLeft + "]";
		}

	}

}
