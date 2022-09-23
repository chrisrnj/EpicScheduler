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

import com.epicnicity322.epicpluginlib.bukkit.command.CommandManager;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicpluginlib.bukkit.logger.Logger;
import com.epicnicity322.epicpluginlib.core.config.ConfigurationHolder;
import com.epicnicity322.epicpluginlib.core.config.ConfigurationLoader;
import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.epicscheduler.command.ScheduleCommand;
import com.epicnicity322.epicscheduler.command.UnscheduleCommand;
import com.epicnicity322.epicscheduler.command.subcommand.InfoSubCommand;
import com.epicnicity322.epicscheduler.command.subcommand.ResetSubCommand;
import com.epicnicity322.epicscheduler.result.*;
import com.epicnicity322.epicscheduler.result.type.Result;
import com.epicnicity322.epicscheduler.result.type.ScheduleResult;
import com.epicnicity322.yamlhandler.Configuration;
import com.epicnicity322.yamlhandler.ConfigurationSection;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class EpicScheduler extends JavaPlugin {
    public static final @NotNull DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final @NotNull HashMap<Schedule, BukkitTask> runningSchedules = new HashMap<>();
    private static final @NotNull Set<Schedule> unmodifiableSchedules = Collections.unmodifiableSet(runningSchedules.keySet());
    private static final @NotNull Path folder = Paths.get("plugins", "EpicScheduler");
    private static final @NotNull Logger logger = new Logger("&8[&cEpicScheduler&8]&e ");
    private static final @NotNull MessageSender lang = new MessageSender(() -> "EN", Configurations.lang.getDefaultConfiguration());
    private static EpicScheduler instance;
    private static boolean papi = false;

    static {
        lang.addLanguage("EN", Configurations.lang);
    }

    public EpicScheduler() {
        instance = this;
        logger.setLogger(getLogger());
    }

    public static boolean hasPlaceholderAPI() {
        return papi;
    }

    public static @NotNull MessageSender getLanguage() {
        return lang;
    }

    /**
     * Reloads all configurations in {@link Configurations}.
     *
     * @return true if main config failed to load and the plugin was disabled.
     */
    private static boolean reloadConfigurations() {
        if (instance == null)
            throw new UnsupportedOperationException("Cannot reload configs while EpicScheduler is unloaded.");
        HashMap<ConfigurationHolder, Exception> exceptions = Configurations.loader.loadConfigurations();
        for (var exception : exceptions.entrySet()) {
            logger.log("'" + exception.getKey().getPath().getFileName() + "' could not be loaded due to an exception:", ConsoleLogger.Level.ERROR);
            exception.getValue().printStackTrace();
        }
        if (exceptions.containsKey(Configurations.schedules)) {
            // Removing all nodes from schedules config to avoid previous schedules from being started.
            Configuration config = Configurations.schedules.getConfiguration();
            config.getNodes().forEach((key, obj) -> config.set(key, null));
            return true;
        }
        return false;
    }

    /**
     * Sets a schedule to run once its time is due and saves it to config.
     *
     * @param schedule The schedule to run later.
     * @throws UnsupportedOperationException If EpicScheduler was not instantiated by bukkit yet.
     * @throws IOException                   If failed to save schedule to configuration.
     */
    public static void setSchedule(@NotNull Schedule schedule) throws IOException {
        if (instance == null)
            throw new UnsupportedOperationException("Cannot run tasks while EpicScheduler is unloaded.");

        List<ScheduleResult> scheduleResults = schedule.scheduleResults();
        if (scheduleResults.isEmpty()) return;

        ConfigurationHolder schedulesHolder = Configurations.schedules;
        // Reloading to prevent losses of any changes made to the config since last reload.
        // If schedules config fails to load, that's no issue, because it will be replaced by the last instance of
        //successfully loaded config. So it's safe to ignore the result.
        Configurations.loader.loadConfigurations();
        Configuration upToDateSchedules = schedulesHolder.getConfiguration();
        String dueDate = schedule.dueDate().format(TIME_FORMATTER);

        upToDateSchedules.set(dueDate, null); // Removing outdated schedule section.
        ConfigurationSection section = upToDateSchedules.createSection(dueDate);

        for (ScheduleResult scheduleResult : scheduleResults) {
            scheduleResult.set(section.createSection(scheduleResult.resultName()));
        }

        Files.deleteIfExists(schedulesHolder.getPath()); // Deleting and saving config with the schedule.
        upToDateSchedules.save(schedulesHolder.getPath());

        BukkitTask previous = runningSchedules.put(schedule, Bukkit.getScheduler().runTaskLater(instance, schedule,
                LocalDateTime.now().until(schedule.dueDate(), ChronoUnit.SECONDS) * 20));
        if (previous != null) previous.cancel();
    }

    public static @NotNull Set<Schedule> getSchedules() {
        LocalDateTime now = LocalDateTime.now();
        // Removing any already due schedules.
        runningSchedules.keySet().removeIf((schedule) -> now.until(schedule.dueDate(), ChronoUnit.SECONDS) <= 0);
        return unmodifiableSchedules;
    }

    public static void cancelSchedule(@NotNull Schedule schedule) throws IOException {
        // Do not call remove straight away, because config save might fail.
        BukkitTask task = runningSchedules.get(schedule);
        if (task == null) return;

        ConfigurationHolder schedulesHolder = Configurations.schedules;
        // Reloading to prevent losses of any changes made to the config since last reload.
        // If schedules config fails to load, that's no issue, because it will be replaced by the last instance of
        //successfully loaded config. So it's safe to ignore the result.
        Configurations.loader.loadConfigurations();
        Configuration upToDateSchedules = schedulesHolder.getConfiguration();

        upToDateSchedules.set(schedule.dueDate().format(TIME_FORMATTER), null);
        Files.deleteIfExists(schedulesHolder.getPath()); // Deleting and saving config without the schedule.
        upToDateSchedules.save(schedulesHolder.getPath());
        task.cancel();
        runningSchedules.remove(schedule);
        logger.log("Schedule with due date " + schedule.dueDate() + " was cancelled and removed from config.");
    }

    /**
     * Cancels all running schedules, reloads configurations, and resets the schedules saved in config {@link Configurations#schedules}.
     * <p>
     *
     * @return Whether schedules were set successfully.
     */
    public static boolean resetSchedules() {
        if (instance == null)
            throw new UnsupportedOperationException("Cannot run tasks while EpicScheduler is unloaded.");
        logger.log("Resetting saved schedules...");

        if (!runningSchedules.isEmpty()) {
            int size = runningSchedules.size();
            // Canceling all previous schedules.
            runningSchedules.entrySet().removeIf((entry) -> {
                entry.getValue().cancel();
                return true;
            });
            logger.log(size + " already running schedule" + (size == 1 ? " was" : "s were") + " cancelled.");
        }

        if (reloadConfigurations()) {
            logger.log("Because schedules config failed to load, all previous schedules were cancelled and there are no schedules running.", ConsoleLogger.Level.ERROR);
            return false;
        }

        List<Schedule> schedules = parseSchedules();
        LocalDateTime now = LocalDateTime.now();

        // Read schedules from config and set them
        for (Schedule schedule : schedules) {
            runningSchedules.put(schedule, Bukkit.getScheduler().runTaskLater(instance, schedule,
                    now.until(schedule.dueDate(), ChronoUnit.SECONDS) * 20));
        }
        if (runningSchedules.isEmpty()) {
            logger.log("No saved schedules were found.");
        } else {
            logger.log(runningSchedules.size() + (runningSchedules.size() == 1 ? " schedule was" : " schedules were") + " set from config.");
        }
        return true;
    }

    private static @NotNull List<Schedule> parseSchedules() {
        Configuration schedulesConfig = Configurations.schedules.getConfiguration();
        Set<Map.Entry<String, Object>> scheduleNodes = schedulesConfig.getNodes().entrySet();
        List<Schedule> schedules = new ArrayList<>(scheduleNodes.size());
        List<String> toRemove = new ArrayList<>();

        for (var scheduleNode : scheduleNodes) {
            if (!(scheduleNode.getValue() instanceof ConfigurationSection section)) continue;
            String sectionName = scheduleNode.getKey();
            LocalDateTime dueDate;
            try {
                dueDate = LocalDateTime.parse(sectionName, TIME_FORMATTER);
            } catch (DateTimeParseException ignored) {
                logger.log("Schedule '" + sectionName + "' has an unknown date.", ConsoleLogger.Level.WARN);
                continue;
            }
            List<ScheduleResult> scheduleResults = new ArrayList<>();

            for (Map.Entry<String, Object> resultNode : section.getNodes().entrySet()) {
                if (resultNode.getValue() instanceof ConfigurationSection resultSection) {
                    ScheduleResult result = parseScheduleResult(sectionName, resultNode.getKey(), resultSection);
                    if (result != null) scheduleResults.add(result);
                }
            }

            if (LocalDateTime.now().until(dueDate, ChronoUnit.SECONDS) <= 0) toRemove.add(sectionName);
            schedules.add(new Schedule(dueDate, Collections.unmodifiableList(scheduleResults)));
        }

        for (String key : toRemove) schedulesConfig.set(key, null);

        if (!toRemove.isEmpty()) {
            try {
                Files.deleteIfExists(Configurations.schedules.getPath());
                schedulesConfig.save(Configurations.schedules.getPath());
                if (toRemove.size() == 1) {
                    logger.log("Schedule " + toRemove + " was removed from config because its due time was already met.");
                } else {
                    logger.log("Schedules " + toRemove + " were removed from config because their due time was already met.");
                }
            } catch (IOException e) {
                logger.log("Unable to remove " + toRemove + " due schedules from config. Because of this, the plugin might run the results again the next time schedules are reset, or the next server start!", ConsoleLogger.Level.ERROR);
                e.printStackTrace();
            }
        }

        return schedules;
    }

    private static @Nullable ScheduleResult parseScheduleResult(@NotNull String scheduleName, @NotNull String name, @NotNull ConfigurationSection resultSection) {
        Set<Map.Entry<String, Object>> resultNodes = resultSection.getNodes().entrySet();
        var results = new ArrayList<Result>(resultNodes.size());
        boolean targetable = true;

        switch (name.toLowerCase(Locale.ROOT)) {
            case "action bars" -> {
                for (var node : resultNodes) {
                    if (!(node.getValue() instanceof ConfigurationSection section)) continue;
                    String text = section.getString("Text").orElse("");
                    if (text.isBlank()) continue;
                    results.add(new ActionBar.Record(color(text)));
                }
            }
            case "boss bars" -> {
                for (var node : resultNodes) {
                    if (!(node.getValue() instanceof ConfigurationSection section)) continue;
                    BarColor color = null;
                    BarStyle style = null;
                    try {
                        String title = section.getString("Title").orElse("");
                        if (title.isBlank()) continue;
                        color = BarColor.valueOf(section.getString("Color").orElse("PINK"));
                        style = BarStyle.valueOf(section.getString("Style").orElse("SOLID"));
                        double progress = section.getNumber("Progress").orElse(0.0).doubleValue();
                        if (progress > 1.0) progress = 1.0;
                        if (progress < 0.0) progress = 0.0;
                        results.add(new BossBar.Record(color(title), color, style, progress));
                    } catch (Exception ignored) {
                        if (color == null)
                            logger.log("Boss bar result " + node.getKey() + " of '" + scheduleName + "' has an unknown color: " + section.getString("Color").orElse(""));
                        else if (style == null)
                            logger.log("Boss bar result " + node.getKey() + " of '" + scheduleName + "' has an unknown style: " + section.getString("Style").orElse(""));
                    }
                }
            }
            case "chat messages" -> {
                for (var node : resultNodes) {
                    if (!(node.getValue() instanceof ConfigurationSection section)) continue;
                    String text = section.getString("Text").orElse("");
                    if (text.isBlank()) continue;
                    results.add(new ChatMessage.Record(color(text)));
                }
            }
            case "commands" -> {
                targetable = false;
                for (var node : resultNodes) {
                    if (!(node.getValue() instanceof ConfigurationSection section)) continue;
                    List<Command.CommandValue> commandValues = section.getCollection("Values",
                            (obj) -> Command.CommandValue.Record.parseCommandValue(obj.toString()));
                    if (commandValues.isEmpty()) continue;
                    results.add(new Command.Record(Collections.unmodifiableList(commandValues)));
                }
            }
            case "titles" -> {
                for (var node : resultNodes) {
                    if (!(node.getValue() instanceof ConfigurationSection section)) continue;
                    String title = section.getString("Title").orElse("");
                    String subtitle = section.getString("Subtitle").orElse("");
                    if (title.isBlank() || subtitle.isBlank()) continue;
                    int fadeIn = section.getNumber("Fade In").orElse(10).intValue();
                    int stay = section.getNumber("Stay").orElse(70).intValue();
                    int fadeOut = section.getNumber("Fade Out").orElse(20).intValue();
                    results.add(new Title.Record(color(title), color(subtitle), fadeIn, stay, fadeOut));
                }
            }
            default -> {
                logger.log('\'' + scheduleName + "' schedule has an unknown result: " + name);
                return null;
            }
        }

        boolean random = resultSection.getString("Pick").orElse("ALL").equals("RANDOM");
        String target = resultSection.getString("Target").orElse("");

        if (target.isBlank()) {
            target = null;
            if (targetable) {
                logger.log("Result " + name + " of '" + scheduleName + "' is targetable, yet it has no specified target.", ConsoleLogger.Level.WARN);
                return null;
            }
        } else if (target.equals("EVERYONE")) target = "!EVERYONE";

        return new ScheduleResult.Record(name, random, Collections.unmodifiableList(results), target);
    }

    private static @NotNull String color(@NotNull String string) {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    private static void loadCommands(@NotNull PluginCommand mainCommand, @Nullable PluginCommand scheduleCommand, @Nullable PluginCommand unscheduleCommand) {
        CommandManager.registerCommand(mainCommand, Set.of(new ResetSubCommand(), new InfoSubCommand()),
                // /epicscheduler Command.
                (label, sender, args) -> {
                    lang.send(sender, lang.get("Help.Header"));
                    if (sender.hasPermission("epicscheduler.info"))
                        lang.send(sender, lang.get("Help.Info").replace("<label>", label));
                    if (sender.hasPermission("epicscheduler.reset"))
                        lang.send(sender, lang.get("Help.Reset").replace("<label>", label));
                    if (sender.hasPermission("epicscheduler.schedule")) {
                        lang.send(sender, lang.get("Help.Schedule").replace("<label>", label));
                        lang.send(sender, lang.get("Help.Unschedule").replace("<label>", label));
                    }
                },
                // Unknown command
                (label, sender, args) -> {
                    if (!sender.hasPermission("epicscheduler.help")) {
                        lang.send(sender, lang.get("General.No Permission"));
                        return;
                    }
                    lang.send(sender, lang.get("General.Unknown Command").replace("<label>", label));
                });

        if (scheduleCommand == null) {
            logger.log("Could not get 'schedule' command.", ConsoleLogger.Level.WARN);
        } else {
            var schedule = new ScheduleCommand();
            scheduleCommand.setExecutor(schedule);
            scheduleCommand.setTabCompleter(schedule);
        }
        if (unscheduleCommand == null) {
            logger.log("Could not get 'unschedule' command.", ConsoleLogger.Level.WARN);
        } else {
            var unschedule = new UnscheduleCommand();
            unscheduleCommand.setExecutor(unschedule);
            unscheduleCommand.setTabCompleter(unschedule);
        }
    }

    @Override
    public void onEnable() {
        papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;
        if (papi) logger.log("PlaceholderAPI was found and hooked.");

        // Loading commands
        PluginCommand mainCommand = getCommand("epicscheduler");
        if (mainCommand == null) {
            logger.log("Could not find 'epicscheduler' main command. Disabling plugin.", ConsoleLogger.Level.ERROR);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        loadCommands(mainCommand, getCommand("schedule"), getCommand("unschedule"));

        logger.log("Loading config...");
        if (reloadConfigurations()) {
            logger.log("Schedules config failed to load! No schedules were set.", ConsoleLogger.Level.ERROR);
            logger.log("Once you fix the configuration, use '/scheduler reset' to reset schedules.", ConsoleLogger.Level.ERROR);
        } else {
            logger.log("Configuration loaded successfully. Schedules will be set when the server is done loading.");
            // Running in a task makes sure the calculations of remaining due ticks of schedules are correct.
            Bukkit.getScheduler().runTaskAsynchronously(this, EpicScheduler::resetSchedules);
        }
    }

    private static final class Configurations {
        private static final ConfigurationLoader loader = new ConfigurationLoader();
        // # If you want to repeat you can add the 'Repeat' setting.
        // # Format is <time> <unit>. Available units: days, hours, minutes, seconds.
        // Repeat: 30 days # Once the date is met, the results will be rescheduled to happen in 30 days or '2100-10-08 7:54:24'.
        private static final @NotNull ConfigurationHolder schedules = new ConfigurationHolder(folder.resolve("schedules.yml"),
                """
                        # Schedules results that will execute on the specified date.
                        # Each schedule have results.
                        # Dates have the following format: 'yyyy-MM-dd HH:mm:ss'
                        # The local timezone will be used.
                        # If the date is due when the server is offline, the results are ran the next time the server goes online.
                        # Schedules are deleted once their dates are due.
                        # Here's an example of how to set a schedule:
                        '2100-09-08 19:54:24':
                          Boss Bars: # Available results: Action Bars, Boss Bars, Chat Messages, Commands and Titles.
                            Target: EVERYONE # To who this result will happen. Available: EVERYONE, <worldName>, <playerUUID>, <world1,world2...>, and <player1,player2...>.
                            Pick: RANDOM # Tells that a RANDOM bar should be picked. Use ALL to send all results at once.
                            '1': # You must number each bar that you add to 'Boss Bars'.
                              Color: BLUE # Available: BLUE, GREEN, RED, PINK, PURPLE, WHITE, YELLOW.
                              Style: SEGMENTED_6 # Available: SOLID, SEGMENTED_6, SEGMENTED_10, SEGMENTED_12, SEGMENTED_20.
                              Progress: 1.0 # The progress of the bar. Must be 1.0 for full, and 0.0 for empty.
                              Title: '&6Hello %player_displayname%' # The title of the boss bar. Supports PlaceHolderAPI.
                            '2':
                              Color: GREEN
                              Style: SOLID
                              Progress: 0.5
                              Title: '&5How ya doing?'
                          Chat Messages:
                            # My UUID. Since names can change between schedules, only UUIDs are allowed in Target.
                            # This is useful when you want to handle the end of VIPs in your server.
                            Target: 1fc1bfdf-ead0-4ee9-9b40-a3f58db88552
                            Pick: ALL
                            '1':
                              Text: "&9What's up, Epicnicity322?"
                            '2':
                              Text: "Hope everything is fine."
                          Titles:
                            Target: world_the_end
                            #Pick: ALL # Pick is irrelevant here, since there's only one title, and I don't want to use random.
                            '1':
                              Title: '&1Hello'
                              Subtitle: '&7How''s the dragon fight?'
                              Fade In: 10 # Time in ticks for title to fade in.
                              Stay: 70 # Time in ticks for title to stay.
                              Fade Out: 20 # Time in ticks for title to fade out.
                          Commands:
                            # Commands are the only result which don't include Target option. By default, they are executed once in console, but
                            #you can see more info on how to change that behavior below.
                            Pick: RANDOM
                            '1':
                              Values:
                                # You can specify a target to run the command for every target. Available: EVERYONE, <worldName>, <playerUUID>, <world1,world2...>, and <player1,player2...>.
                                # The second argument is who will be executing the command. CONSOLE or the PLAYER.
                                # Third argument is the command itself.
                                # The format is: <target>;<executor>;<command>
                                - 'EVERYONE;CONSOLE;tell %player_name% This is console whispering to you, %player_displayname%'
                                - 'world_the_end;PLAYER;me I''m in the end fighting the dragon!'
                                # If you don't specify a target and executor, the command will run once in console.
                                - 'say hi everyone, from console'
                            '2':
                              Values:
                                - 'say what''s up everyone'
                          Action Bars:
                            Target: EVERYONE
                            '1':
                              Text: '&aHello there!'""");
        private static final @NotNull ConfigurationHolder lang = new ConfigurationHolder(folder.resolve("lang.yml"),
                """
                        # Global variables: <label>
                        # All messages can have <noprefix> property at the start to send the message without a prefix.
                        # All messages can have a cooldown with property <cooldown=TIME> where 'TIME' is the cooldown time in MILLIS.
                        # Properties will be removed before the message is sent.
                        # Example: '<cooldown=5000><noprefix> Testing' outputs just 'Testing'.
                                                
                        General:
                          No Permission: '&4You don''t have permission to do this.'
                          Prefix: '&8[&cEpicScheduler&8] '
                          Unknown Command: '&4Unknown command! Use &7&n/<label>&r&4 to see the list of commands.'
                                                
                        Help:
                          Header: '&6List of commands:'
                          Info: '<noprefix> &7&n/<label> info <yyyy-MM-dd> <HH:mm:ss>&r&8 >> &eShow info about a schedule.'
                          Reset: '<noprefix> &7&n/<label> reset&r&8 >> &eResets all schedules from config.'
                          Schedule: '<noprefix> &7&n/schedule <date> <result> [target] <resultValue>&r&8 >> &eSet a schedule.'
                          Unschedule: '<noprefix> &7&n/unschedule <yyyy-MM-dd> <HH:mm:ss>&r&8 >> &eCancel a schedule.'
                                                
                        Info:
                          Error:
                            Invalid Syntax: '&4Invalid arguments! Use &7&n/<label> info <yyyy-MM-dd> <HH:mm:ss>&r&4'
                            # Variables: <date>
                            Unknown Schedule: '&4Schedule with date ''&7<date>&4'' was not found running.'
                          # Variables: <date>
                          Header: '&7Results to happen in <date>:'
                                                
                        Reset:
                          Success: '&aAll running schedules were reset.'
                          Error: '&4Something went wrong while reading schedules configuration! All schedules were stopped.
                           &cCheck console to see if there are any issues with the &oYAML Syntax&c. Once you fix the issue, type &7/<label> reset&c again to resume schedules.'
                                                
                        Schedule:
                          Error:
                            Invalid Syntax: '&4Invalid arguments! Use &7&n/<label> <date> <result> [target] <resultValue>&r&4.'
                            # Variables: <value>
                            Not A Date: '&4The value "&7<value>&r&4" is not a valid date! Use ''&ayyyy-MM-dd HH:mm:ss&4'' format!'
                            # Variables: <value>, <resultTypes>
                            Not A Result: '&4Result with name "&7<value>&4" was not found. Available results: &a<resultTypes>&c.'
                            # Variables: <date>, <target>
                            Title Syntax: "&4Titles and subtitles must be enclosed in &7\\"&4quotes&7\\"&4! For example:\\n&a/<label> <date> title <target> \\"This is title\\" \\"This is subtitle\\"&4."
                            Default: '&4An unknown error occurred while setting this schedule.'
                          # Variables for notice keys: <date>, <target>, <title>
                          Notice:
                            Boss Bar Syntax: "&7You can add the bar color, style, and progress to the last three arguments. For example:\\n&a/<label> <date> bossbar <target> <title> PINK SOLID 1.0&7."
                            # Variables: <subtitle>
                            Title Syntax: "&7You can add the title's fade in, stay, and fade out to the last three arguments. For example:\\n&a/<label> <date> title <target> <title> <subtitle> 10 70 20&7."
                          # Variables for success keys: <date>, <target>
                          Success:
                            # Variables: <title>, <color>, <style>, <progress>
                            Boss Bar: '&2Boss Bar set to perform on &a<date>&2 with title &7"<title>&r&7"&2, color &a<color>&2, style &a<style>&2, and progress &a<progress>&2 to target &a<target>&2.'
                            # Variables: <title>, <subtitle>, <fadeIn>, <stay>, <fadeOut>
                            Title: '&2Title set to perform on &a<date>&2 with title &7"<title>&r&7"&2, subtitle &7"<subtitle>&r&7"&2, fade in &a<fadeIn>&2, stay &a<stay>&2, and fade out &a<fadeOut>&2 to target &a<target>&2.'
                            # Variables: <result>, <value>
                            Default: '&2<result> set to perform on &a<date>&2 with value &7"<value>&r&7"&2 to target &a<target>&2.'
                                                
                        Unschedule:
                          Error:
                            # Variables: <date>
                            Default: '&4An IO error occurred while unscheduling &7<date>&4 schedule.'
                            Invalid Syntax: '&4Invalid arguments! Use &7&n/<label> <yyyy-MM-dd> <HH:mm:ss>&r&4.'
                            # Variables: <date>
                            Unknown Schedule: '&4Schedule with date ''&7<date>&4'' was not found running.'
                          # Variables: <date>, <results>
                          Success: '&2Schedule with due date &7<date>&2 and results &7<results>&2 was cancelled and removed from schedules.yml successfully.'""");

        static {
            loader.registerConfiguration(schedules);
            loader.registerConfiguration(lang);
        }
    }
}
