package com.epicnicity322.epicscheduler;

import com.epicnicity322.epicpluginlib.bukkit.command.CommandManager;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicpluginlib.bukkit.logger.Logger;
import com.epicnicity322.epicpluginlib.core.config.ConfigurationHolder;
import com.epicnicity322.epicpluginlib.core.config.ConfigurationLoader;
import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.epicscheduler.command.ResetCommand;
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
    private static final @NotNull HashMap<Schedule, BukkitTask> runningSchedules = new HashMap<>();
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
     * Sets a schedule to run once its time is due.
     * <p>
     * Schedules set here might be cancelled if all schedules are reset through {@link #resetSchedules()};
     *
     * @param schedule The schedule to run later.
     * @throws UnsupportedOperationException If EpicScheduler was not instantiated by bukkit yet.
     */
    public static void setSchedule(@NotNull Schedule schedule) {
        if (instance == null)
            throw new UnsupportedOperationException("Cannot run tasks while EpicScheduler is unloaded.");
        BukkitTask previous = runningSchedules.put(schedule, Bukkit.getScheduler().runTaskLater(instance, schedule, schedule.dueTime()));
        if (previous != null) previous.cancel();
    }

    /**
     * Cancels all running schedules, reloads configurations, and resets the schedules saved in config {@link Configurations#schedules}.
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
            logger.log(size + " already running schedules were cancelled.");
        }

        if (reloadConfigurations()) {
            logger.log("Because schedules config failed to load, all previous schedules were cancelled and there are no schedules running.", ConsoleLogger.Level.ERROR);
            return false;
        }

        // Read schedules from config and set them
        for (Schedule schedule : parseSchedules()) {
            setSchedule(schedule);
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
                dueDate = LocalDateTime.parse(sectionName, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (DateTimeParseException ignored) {
                logger.log("Schedule '" + sectionName + "' has an unknown date.", ConsoleLogger.Level.WARN);
                continue;
            }
            long dueTicks = ChronoUnit.SECONDS.between(LocalDateTime.now(), dueDate) * 20;
            List<ScheduleResult> scheduleResults = new ArrayList<>();

            for (Map.Entry<String, Object> resultNode : section.getNodes().entrySet()) {
                if (resultNode.getValue() instanceof ConfigurationSection resultSection) {
                    ScheduleResult result = parseScheduleResult(sectionName, resultNode.getKey(), resultSection);
                    if (result != null) scheduleResults.add(result);
                }
            }

            if (dueTicks <= 0) {
                dueTicks = 0;
                toRemove.add(sectionName);
            }
            schedules.add(new Schedule(dueTicks, Collections.unmodifiableList(scheduleResults)));
        }

        for (String key : toRemove) {
            schedulesConfig.set(key, null);
        }

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
        CommandManager.registerCommand(mainCommand, Collections.singleton(new ResetCommand()),
                // /epicscheduler Command.
                (label, sender, args) -> {
                    lang.send(sender, lang.get("Help.Header"));
                    lang.send(sender, lang.get("Help.Reset").replace("<label>", label));
                },
                // Unknown command
                (label, sender, args) -> {
                    if (!sender.hasPermission("epicscheduler.help")) {
                        lang.send(sender, lang.get("General.No Permission"));
                        return;
                    }
                    lang.send(sender, lang.get("General.Unknown Command").replace("<label>", label));
                });

        logger.log("Loading config...");
        if (reloadConfigurations()) {
            logger.log("Schedules config failed to load! No schedules were set.", ConsoleLogger.Level.ERROR);
            logger.log("Once you fix the configuration, use '/scheduler reset' to reset schedules.", ConsoleLogger.Level.ERROR);
        } else {
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
                        General:
                          No Permission: '&4You don''t have permission to do this.'
                          Prefix: '&8[&cEpicScheduler&8] '
                          Unknown Command: '&cUnknown command! Use &7&n/<label>&r&c to see the list of commands.'
                                                
                        Help:
                          Header: '&6List of commands:'
                          Reset: '<noprefix> &7&n/<label> reset&r&8 >> &e Resets all schedules from config.'
                                                
                        Reset:
                          Success: '&aAll running schedules were reset.'
                          Error: '&4Something went wrong while reading schedules configuration! All schedules were stopped.'""");

        static {
            loader.registerConfiguration(schedules);
            loader.registerConfiguration(lang);
        }
    }
}
