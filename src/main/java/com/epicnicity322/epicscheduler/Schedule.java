/*
 * EpicScheduler - Minecraft Spigot plugin that schedules results to happen in specified dates.
 * Copyright (C) 2022  Christiano Rangel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.epicnicity322.epicscheduler;

import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.epicscheduler.result.type.ScheduleResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @param dueDate         The date the schedule will have its results performed.
 * @param scheduleResults The results to be executed.
 * @param repeat          The repeat interval in seconds the schedule will wait before performing again. 0 if this schedule does not repeat.
 */
public record Schedule(@NotNull LocalDateTime dueDate,
                       @NotNull List<ScheduleResult> scheduleResults,
                       @Range(from = 0L, to = Long.MAX_VALUE) long repeat,
                       boolean skipMissedRepeats) implements Runnable, Serializable {

    public Schedule {
        if (repeat < 0) throw new IllegalArgumentException("Schedule can not have a negative repeat interval.");
    }

    @Override
    public void run() {
        for (ScheduleResult result : scheduleResults) {
            result.perform();
        }

        // Don't want to create threads all the time on schedules that repeat fast.
        if (repeat == 0 || repeat >= 600) {
            new Thread(this::cancelSchedule, "Saver of " + formatted() + " schedule").start();
        } else {
            cancelSchedule();
        }
    }

    private void cancelSchedule() {
        try {
            EpicScheduler.cancelSchedule(this);
        } catch (IOException e) {
            EpicScheduler.getConsoleLogger().log("Unable to remove schedule " + formatted() + " from config:", ConsoleLogger.Level.ERROR);
            e.printStackTrace();
            return;
        }

        if (repeat != 0) {
            LocalDateTime repeatDate = dueDate.plusSeconds(repeat);
            LocalDateTime now = LocalDateTime.now();

            // Since missed schedules perform immediately, repeat date should be updated accordingly.
            if (repeatDate.isBefore(now)) {
                repeatDate = repeatDate.plusSeconds(repeat);
            }

            if (skipMissedRepeats) {
                while (repeatDate.isBefore(now)) {
                    repeatDate = repeatDate.plusSeconds(repeat);
                }
            }

            try {
                EpicScheduler.setSchedule(new Schedule(repeatDate, scheduleResults, repeat, skipMissedRepeats));
            } catch (IOException e) {
                EpicScheduler.getConsoleLogger().log("Unable to save repeating schedule " + formatted() + " to config:", ConsoleLogger.Level.ERROR);
                e.printStackTrace();
            }
        }
    }

    private @NotNull String formatted() {
        return dueDate.format(EpicScheduler.TIME_FORMATTER);
    }
}
