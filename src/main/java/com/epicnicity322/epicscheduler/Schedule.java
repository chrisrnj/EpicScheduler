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

import com.epicnicity322.epicscheduler.result.type.ScheduleResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @param dueTime         The time in ticks to wait before executing the results.
 * @param scheduleResults The results to be executed.
 */
public record Schedule(long dueTime, @NotNull List<ScheduleResult> scheduleResults) implements Runnable {
    @Override
    public void run() {
        for (ScheduleResult result : scheduleResults) {
            result.perform();
        }
    }
}
