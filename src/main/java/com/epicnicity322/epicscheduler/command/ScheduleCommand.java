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
import com.epicnicity322.epicscheduler.result.*;
import com.epicnicity322.epicscheduler.result.type.Result;
import com.epicnicity322.epicscheduler.result.type.ScheduleResult;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.checkerframework.common.value.qual.ArrayLenRange;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

public class ScheduleCommand implements CommandExecutor, TabCompleter {
    /**
     * The supported result types of 'schedule' command.
     */
    public static final @NotNull List<String> RESULT_TYPES = List.of("actionbar", "bossbar", "chatmessage", "command", "title");
    private static final @NotNull List<String> barColors = Arrays.stream(BarColor.values()).map(Enum::name).toList();
    private static final @NotNull List<String> barStyles = Arrays.stream(BarStyle.values()).map(Enum::name).toList();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        MessageSender lang = EpicScheduler.getLanguage();

        if (args.length < 4) {
            lang.send(sender, lang.get("Schedule.Error.Invalid Syntax").replace("<label>", label));
            return true;
        }
        String resultType = args[2].toLowerCase(Locale.ROOT);
        if (!RESULT_TYPES.contains(resultType)) {
            lang.send(sender, lang.get("Schedule.Error.Not A Result").replace("<value>", args[2]).replace("<resultTypes>", RESULT_TYPES.toString()));
            return true;
        }
        LocalDateTime dueDate;
        try {
            dueDate = LocalDateTime.parse(args[0] + " " + args[1], EpicScheduler.TIME_FORMATTER);
        } catch (DateTimeParseException ignored) {
            lang.send(sender, lang.get("Schedule.Error.Not A Date").replace("<value>", args[0] + ' ' + args[1]));
            return true;
        }

        ScheduleResult scheduleResults = getScheduleResult(label, sender, resultType, args, args[0] + ' ' + args[1]);
        if (scheduleResults == null) return true;
        try {
            EpicScheduler.setSchedule(new Schedule(dueDate, Collections.singletonList(scheduleResults), 0, false));
        } catch (IOException ignored) {
            lang.send(sender, lang.get("Schedule.Error.Default"));
        }
        return true;
    }

    private @Nullable ScheduleResult getScheduleResult(@NotNull String label, @NotNull CommandSender sender, @NotNull String name, @ArrayLenRange(from = 4) @NotNull String[] args, @NotNull String date) {
        MessageSender lang = EpicScheduler.getLanguage();

        switch (name) {
            case "command":
                String commandValue = join(3, args.length, args);
                lang.send(sender, lang.get("Schedule.Success.Default").replace("<result>", "Command").replace("<date>", date)
                        .replace("<target>", "null").replace("<value>", commandValue));
                return singleResult("Commands", new Command.Record(
                        Collections.singletonList(Command.CommandValue.Record.parseCommandValue(commandValue))), null);
            default:
                if (args.length < 5) {
                    lang.send(sender, lang.get("Schedule.Error.Invalid Syntax").replace("<label>", label));
                    return null;
                }
            case "actionbar":
                String text = join(4, args.length, args);
                lang.send(sender, lang.get("Schedule.Success.Default").replace("<result>", "Action Bar").replace("<date>", date)
                        .replace("<target>", args[3]).replace("<value>", text));
                return singleResult("Action Bars", new ActionBar.Record(text), target(args));
            case "bossbar":
                int argLength = args.length;
                String title;
                BarColor color = BarColor.PINK;
                BarStyle style = BarStyle.SOLID;
                Double progress = null;

                try {
                    progress = Double.parseDouble(args[argLength - 1]);
                    if (progress > 1.0) progress = 1.0;
                    if (progress < 0.0) progress = 0.0;
                } catch (NumberFormatException ignored) {
                }

                // Last 3 arguments can specify color, style, and progress, respectively. Example command:
                // /schedule 2022-09-13 3:01:10 bossbar EVERYONE title  PINK   SOLID  1.0
                // Args:     arg[0]     arg[1]  arg[2]  arg[3]   arg[4] arg[5] arg[6] arg[7]
                if (args.length >= 8 && progress != null && barColors.contains(args[argLength - 3].toUpperCase(Locale.ROOT))
                    && barStyles.contains(args[argLength - 2].toUpperCase(Locale.ROOT))) {
                    color = BarColor.valueOf(args[argLength - 3].toUpperCase(Locale.ROOT));
                    style = BarStyle.valueOf(args[argLength - 2].toUpperCase(Locale.ROOT));
                    title = join(4, argLength - 3, args);
                } else {
                    title = join(4, argLength, args);
                    lang.send(sender, lang.get("Schedule.Notice.Boss Bar Syntax").replace("<label>", label)
                            .replace("<date>", date).replace("<target>", args[3]).replace("<title>", title));
                }
                if (progress == null) progress = 1.0;
                lang.send(sender, lang.get("Schedule.Success.Boss Bar").replace("<date>", date).replace("<target>", args[3])
                        .replace("<title>", title).replace("<progress>", progress.toString()).replace("<color>", color.toString())
                        .replace("<style>", style.toString()));
                return singleResult("Boss Bars", new BossBar.Record(title, color, style, progress), target(args));
            case "chatmessage":
                String message = join(4, args.length, args);
                lang.send(sender, lang.get("Schedule.Success.Default").replace("<result>", "Chat Message").replace("<date>", date)
                        .replace("<target>", args[3]).replace("<value>", message));
                return singleResult("Chat Messages", new ChatMessage.Record(message), target(args));
            case "title":
                String[] titleAndSubtitle = findTitleAndSubtitle(args);

                if (titleAndSubtitle == null) {
                    lang.send(sender, lang.get("Schedule.Error.Title Syntax").replace("<label>", label)
                            .replace("<date>", date).replace("<target>", args[3]));
                    return null;
                }

                Integer fadeIn = null, stay = null, fadeOut = null;

                // Last 3 arguments can specify fadeIn, stay, and fadeOut, respectively. Example command:
                // /schedule 2022-09-13 12:08:19 title  EVERYONE "Title" "Subtitle" 10     70     20
                // Args:     arg[0]     arg[1]   arg[2] arg[3]   arg[4]  arg[5]     arg[6] arg[7] arg[8]
                if (args.length >= 9) {
                    try {
                        fadeIn = Integer.parseInt(args[args.length - 3]);
                        stay = Integer.parseInt(args[args.length - 2]);
                        fadeOut = Integer.parseInt(args[args.length - 1]);
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (fadeIn == null || stay == null || fadeOut == null) {
                    lang.send(sender, lang.get("Schedule.Notice.Title Syntax").replace("<label>", label)
                            .replace("<date>", date).replace("<target>", args[3])
                            .replace("<title>", '"' + titleAndSubtitle[0] + '"').replace("<subtitle>", '"' + titleAndSubtitle[1] + '"'));
                    fadeIn = 10;
                    stay = 70;
                    fadeOut = 20;
                }

                lang.send(sender, lang.get("Schedule.Success.Title").replace("<date>", date).replace("<target>", args[3])
                        .replace("<title>", titleAndSubtitle[0]).replace("<subtitle>", titleAndSubtitle[1])
                        .replace("<fadeIn>", fadeIn.toString()).replace("<stay>", stay.toString()).replace("<fadeOut>", fadeOut.toString()));
                return singleResult("Titles", new Title.Record(titleAndSubtitle[0], titleAndSubtitle[1], fadeIn, stay, fadeOut), target(args));
        }
    }

    private @Nullable String[] findTitleAndSubtitle(@NotNull String[] args) {
        int start = 4;
        String[] titleAndSubtitle = new String[2];
        StringBuilder builder = new StringBuilder();

        if (!args[start].startsWith("\"")) return null;

        while (start < args.length) {
            String arg = args[start];
            builder.append(arg);
            ++start;
            if (arg.endsWith("\"")) break;
            builder.append(' ');
        }

        // Didn't specify a subtitle.
        if (start == args.length || !args[start].startsWith("\"")) return null;

        titleAndSubtitle[0] = ChatColor.translateAlternateColorCodes('&', builder.substring(1, builder.length() - 1));
        builder = new StringBuilder();

        while (start < args.length) {
            String arg = args[start];
            builder.append(arg);
            ++start;
            if (arg.endsWith("\"")) {
                titleAndSubtitle[1] = ChatColor.translateAlternateColorCodes('&', builder.substring(1, builder.length() - 1));
                return titleAndSubtitle;
            }
            builder.append(' ');
        }
        // Didn't end subtitle with quotation mark.
        return null;
    }

    @Contract("null -> null")
    private String target(@NotNull String @Nullable [] args) {
        if (args == null) return null;
        String arg = args[3];
        if (arg.equals("EVERYONE")) return "!EVERYONE";
        if (Bukkit.getWorld(arg) != null) return arg;
        // Convert player name to UUID if no world with the name was found.
        Player p = Bukkit.getPlayer(arg);
        if (p != null) return p.getUniqueId().toString();
        return arg;
    }

    private @NotNull ScheduleResult singleResult(@NotNull String name, @NotNull Result result, @Nullable String target) {
        return new ScheduleResult.Record(name, false, Collections.singletonList(result), target);
    }

    private @NotNull String join(int start, int end, @NotNull String[] args) {
        if (end > args.length) end = args.length;
        StringBuilder string = new StringBuilder();
        for (int i = start; i < end; ++i) {
            string.append(' ').append(args[i]);
        }
        if (string.isEmpty()) return "";
        return ChatColor.translateAlternateColorCodes('&', string.substring(1));
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("epicscheduler.schedule")) return null;

        switch (args.length) {
            // Day of schedule arg
            case 1 -> {
                String today = LocalDateTime.now().format(EpicScheduler.TIME_FORMATTER);
                if (today.startsWith(args[0])) return Collections.singletonList(today);
            }
            // Hour of schedule arg
            case 2 -> {
                String input = args[0] + " " + args[1];
                String today = LocalDateTime.now().format(EpicScheduler.TIME_FORMATTER);
                if (today.startsWith(input)) return Collections.singletonList(today.substring(today.indexOf(' ') + 1));
            }
            // Result type arg
            case 3 -> {
                String argument = args[2].toLowerCase(Locale.ROOT);
                ArrayList<String> resultTypes = new ArrayList<>(ScheduleCommand.RESULT_TYPES);
                resultTypes.removeIf(result -> !result.startsWith(argument));
                return resultTypes;
            }
            // Target or result value arg
            case 4 -> {
                String argument = args[3].toLowerCase(Locale.ROOT);
                ArrayList<String> targets = getPossibleTargets();

                // Command is the only supported result that doesn't have a target.
                // Command argument can be either TARGET;EXECUTOR;COMMAND or just COMMAND.
                if (args[2].toLowerCase(Locale.ROOT).equals("command")) {
                    ArrayList<String> fixedTargets = new ArrayList<>(targets.size());

                    for (String target : targets) {
                        fixedTargets.add(target + ";");
                    }

                    if (argument.isBlank()) return fixedTargets;

                    int firstSeparator = argument.indexOf(';');
                    if (firstSeparator == -1) {
                        fixedTargets.removeIf(target -> !target.toLowerCase(Locale.ROOT).startsWith(argument));
                        return fixedTargets;
                    }

                    String executor = argument.substring(firstSeparator + 1);
                    int secondSeparator = executor.indexOf(';');
                    if (secondSeparator == -1) {
                        String target = argument.substring(0, firstSeparator + 1);
                        if (fixedTargets.stream().noneMatch(target1 -> target1.toLowerCase(Locale.ROOT).equals(target)))
                            return null;
                        ArrayList<String> executors = new ArrayList<>(2);
                        String realCaseTarget = args[3].substring(0, firstSeparator + 1);
                        executors.add(realCaseTarget + "CONSOLE;");
                        executors.add(realCaseTarget + "PLAYER;");
                        if (executor.isBlank()) return executors;
                        executors.removeIf(executor1 -> !executor1.toLowerCase(Locale.ROOT).startsWith(argument));
                        return executors;
                    }
                    return null;
                } else {
                    targets.removeIf(target -> !target.toLowerCase(Locale.ROOT).startsWith(argument));
                    return targets;
                }
            }
        }
        return null;
    }

    private @NotNull ArrayList<String> getPossibleTargets() {
        Collection<? extends Player> online = Bukkit.getOnlinePlayers();
        List<World> worlds = Bukkit.getWorlds();
        ArrayList<String> targets = new ArrayList<>(online.size() + worlds.size() + 1);

        targets.add("EVERYONE");
        for (var world : worlds) targets.add(world.getName());
        for (var player : online) targets.add(player.getUniqueId().toString());
        return targets;
    }
}
