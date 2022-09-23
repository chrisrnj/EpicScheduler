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

package com.epicnicity322.epicscheduler.command.subcommand;

import com.epicnicity322.epicpluginlib.bukkit.command.Command;
import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.epicpluginlib.bukkit.command.TabCompleteRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicscheduler.EpicScheduler;
import com.epicnicity322.epicscheduler.Schedule;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class InfoSubCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "info";
    }

    @Override
    public @Nullable String getPermission() {
        return "epicscheduler.info";
    }

    @Override
    protected @Nullable CommandRunnable getNoPermissionRunnable() {
        return (label, sender, args) -> EpicScheduler.getLanguage().send(sender, EpicScheduler.getLanguage().get("General.No Permission"));
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = EpicScheduler.getLanguage();

        if (args.length > 1) {
            if (args.length < 3) {
                lang.send(sender, lang.get("Info.Specific.Error.Invalid Syntax").replace("<label>", label));
                return;
            }
        } else {
            // List running schedules.
            Set<Schedule> runningSchedules = EpicScheduler.getSchedules();
            if (runningSchedules.isEmpty()) {
                lang.send(sender, lang.get("Info.List.None"));
                return;
            }
            StringBuilder entries = new StringBuilder();
            String entryColor = lang.get("Info.List.Entry Color");
            String separator = lang.get("Info.List.Separator");

            for (Schedule s : runningSchedules) {
                entries.append(separator).append(entryColor).append(s.dueDate().format(EpicScheduler.TIME_FORMATTER));
            }

            lang.send(sender, lang.get("Info.List.Header").replace("<amount>", Integer.toString(runningSchedules.size())));
            lang.send(sender, false, entries.substring(separator.length()));
            lang.send(sender, lang.get("Info.List.Footer").replace("<label>", label));
            return;
        }

        // Specific schedule info.
        String date = args[1] + ' ' + args[2];
        Schedule schedule = null;
        for (Schedule s : EpicScheduler.getSchedules()) {
            if (date.equals(s.dueDate().format(EpicScheduler.TIME_FORMATTER))) {
                schedule = s;
                break;
            }
        }
        if (schedule == null) {
            lang.send(sender, lang.get("Info.Specific.Error.Unknown Schedule").replace("<date>", args[1] + " " + args[2]));
            return;
        }
        lang.send(sender, lang.get("Info.Specific.Header").replace("<date>", date));
        lang.send(sender, schedule.toString());
    }

    @Override
    protected @Nullable TabCompleteRunnable getTabCompleteRunnable() {
        return (completions, label, sender, args) -> {
            if (args.length == 2) {
                for (Schedule schedule : EpicScheduler.getSchedules()) {
                    String date = schedule.dueDate().format(EpicScheduler.TIME_FORMATTER);
                    if (date.startsWith(args[1])) {
                        completions.add(date);
                    }
                }
            }
        };
    }
}
