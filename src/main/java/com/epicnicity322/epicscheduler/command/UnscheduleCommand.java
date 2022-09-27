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

package com.epicnicity322.epicscheduler.command;

import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicscheduler.EpicScheduler;
import com.epicnicity322.epicscheduler.Schedule;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class UnscheduleCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        MessageSender lang = EpicScheduler.getLanguage();
        if (args.length < 2) {
            lang.send(sender, lang.get("Unschedule.Error.Invalid Syntax").replace("<label>", label));
            return true;
        }
        LocalDateTime dueDate;
        try {
            dueDate = LocalDateTime.parse(args[0] + " " + args[1], EpicScheduler.TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
            lang.send(sender, lang.get("Schedule.Error.Not A Date").replace("<value>", args[0] + ' ' + args[1]));
            return true;
        }
        Schedule value = null;
        for (Schedule schedule : EpicScheduler.getSchedules()) {
            if (schedule.dueDate().equals(dueDate)) {
                value = schedule;
                break;
            }
        }
        if (value == null) {
            lang.send(sender, lang.get("Unschedule.Error.Unknown Schedule").replace("<date>", args[0] + " " + args[1]));
            return true;
        }
        try {
            EpicScheduler.cancelSchedule(value);
            lang.send(sender, lang.get("Unschedule.Success").replace("<date>", dueDate.toString()).replace("<results>", value.scheduleResults().toString()));
        } catch (IOException e) {
            lang.send(sender, lang.get("Unschedule.Default").replace("<date>", dueDate.toString()));
            e.printStackTrace();
        }
        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> scheduleNames = scheduleNames();
            scheduleNames.removeIf(schedule -> !schedule.startsWith(args[0]));
            return scheduleNames;
        }
        return null;
    }

    private @NotNull List<String> scheduleNames() {
        Set<Schedule> schedules = EpicScheduler.getSchedules();
        var scheduleNames = new ArrayList<String>(schedules.size());
        for (Schedule schedule : schedules) {
            scheduleNames.add(schedule.formatted());
        }
        return scheduleNames;
    }
}
