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
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicscheduler.EpicScheduler;
import com.epicnicity322.epicscheduler.Schedule;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InfoSubCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "info";
    }

    @Override
    public int getMinArgsAmount() {
        return 2;
    }

    @Override
    protected @Nullable CommandRunnable getNotEnoughArgsRunnable() {
        return (label, sender, args) -> EpicScheduler.getLanguage().send(sender, EpicScheduler.getLanguage().get("Info.Error.Invalid Syntax"));
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
        String date = args[0] + ' ' + args[1];
        Schedule schedule = null;
        for (Schedule s : EpicScheduler.getSchedules()) {
            if (date.equals(s.dueDate().format(EpicScheduler.TIME_FORMATTER))) {
                schedule = s;
                break;
            }
        }
        if (schedule == null) {
            lang.send(sender, lang.get("Info.Error.Unknown Schedule").replace("<date>", args[0] + " " + args[1]));
            return;
        }
        lang.send(sender, lang.get("Info.Header").replace("<date>", date));
        lang.send(sender, schedule.toString());
    }
}
