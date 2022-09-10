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
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

public class ScheduleCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        MessageSender lang = EpicScheduler.getLanguage();

        if (args.length < 4) {
            lang.send(sender, lang.get("General.Invalid Syntax").replace("<label>", label).replace("<args>", "<date> command <command>"));
            return true;
        }
        if (!args[2].equals("command")) {
            lang.send(sender, lang.get("Schedule.Error.Not A Result").replace("<value>", args[2]));
            return true;
        }
        String date = args[0] + " " + args[1];
        try {
            LocalDateTime.parse(date, EpicScheduler.TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
            lang.send(sender, lang.get("Schedule.Error.Not A Date").replace("<value>", date));
            return true;
        }

        return true;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("epicscheduler.schedule")) return null;
        if (args.length == 1 || args.length == 2) {
            String currentInput = args[0] + (args.length == 2 ? " " + args[1] : "");
            String today = LocalDateTime.now().format(EpicScheduler.TIME_FORMATTER);
            if (today.startsWith(currentInput)) return Collections.singletonList(today);
        } else if (args.length == 3) {
            //TODO: Add more results
            List<String> results = new ArrayList<>(1);
            results.add("command");
            results.removeIf(result -> !result.toLowerCase(Locale.ROOT).startsWith(args[2].toLowerCase(Locale.ROOT)));
            return results;
        } else if (args.length == 4) {
            Collection<? extends Player> online = Bukkit.getOnlinePlayers();
            List<World> worlds = Bukkit.getWorlds();
            List<String> targets = new ArrayList<>(online.size() + worlds.size() + 1);
            targets.add("EVERYONE;");
            for (var world : worlds) targets.add(world.getName() + ";");
            for (var player : online) targets.add(player.getUniqueId() + ";");

            if (args[3].isBlank()) return targets;

            int firstSeparator = args[3].indexOf(';');

            if (firstSeparator == -1) {
                targets.removeIf(target -> !target.toLowerCase(Locale.ROOT).startsWith(args[3].toLowerCase(Locale.ROOT)));
                return targets;
            }
            int secondSeparator = args[3].substring(firstSeparator + 1).indexOf(';');
            if (secondSeparator == -1) {
                String value = args[3].substring(0, firstSeparator + 1);
                if (!targets.contains(value)) return null;
                List<String> executors = new ArrayList<>(2);
                executors.add(value + "CONSOLE;");
                executors.add(value + "PLAYER;");
                if (args[3].isBlank()) return executors;
                executors.removeIf(executor -> !executor.toLowerCase(Locale.ROOT).startsWith(args[3].toLowerCase(Locale.ROOT)));
                return executors;
            }
        }
        return null;
    }
}
