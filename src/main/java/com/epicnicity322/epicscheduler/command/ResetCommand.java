package com.epicnicity322.epicscheduler.command;

import com.epicnicity322.epicpluginlib.bukkit.command.Command;
import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicscheduler.EpicScheduler;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;

public class ResetCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "reset";
    }

    @Override
    public @NotNull String getPermission() {
        return "epicscheduler.reset";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return (label, sender, args) -> EpicScheduler.getLanguage().send(sender, EpicScheduler.getLanguage().get("General.No Permission"));
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = EpicScheduler.getLanguage();

        boolean success = EpicScheduler.resetSchedules();

        // Console already gets log messages from resetSchedules method.
        if (sender instanceof ConsoleCommandSender) return;

        if (success) {
            lang.send(sender, lang.get("Reset.Success"));
        } else {
            lang.send(sender, lang.get("Reset.Error"));
        }
    }
}
