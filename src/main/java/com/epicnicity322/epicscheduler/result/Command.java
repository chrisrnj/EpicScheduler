package com.epicnicity322.epicscheduler.result;

import com.epicnicity322.epicscheduler.result.type.Result;
import com.epicnicity322.epicscheduler.result.type.TargetableResult;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

public interface Command extends Result {
    @Override
    @NotNull
    default String resultName() {
        return "Command";
    }

    @NotNull List<CommandValue> values();

    @Override
    default void perform() {
        for (CommandValue command : values()) {
            String target = command.target();
            String commandInput = command.command();
            CommandValue.CommandValueExecutor executor = command.executor();

            if (target == null) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandInput);
            } else {
                for (Player player : TargetableResult.findTarget(target)) {
                    String formattedCommand = TargetableResult.format(player, commandInput);
                    CommandSender targetExecutor;

                    if (executor == CommandValue.CommandValueExecutor.PLAYER) {
                        targetExecutor = player;
                    } else {
                        targetExecutor = Bukkit.getConsoleSender();
                    }

                    Bukkit.dispatchCommand(targetExecutor, formattedCommand);
                }
            }
        }
    }

    interface CommandValue {
        /**
         * Who this command will be executed to. The options are:
         * <ul>
         *     <li>"!EVERYONE" - Everyone online in the server at the moment.</li>
         *     <li>{@literal <world>} - A world's name.</li>
         *     <li>{@literal <player>} - A player's {@link java.util.UUID}.</li>
         * </ul>
         *
         * @return "!EVERYONE", a world name, or an {@link java.util.UUID} of a player.
         */
        @Nullable String target();

        /**
         * Who should execute this command.
         *
         * @return The person who will dispatch the command.
         */
        @NotNull CommandValueExecutor executor();

        /**
         * The command of this {@link CommandValue}
         *
         * @return The command that will be dispatched by the executor.
         */
        @NotNull String command();

        enum CommandValueExecutor {
            PLAYER,
            CONSOLE
        }

        record Record(@Nullable String target, @NotNull CommandValueExecutor executor,
                      @NotNull String command) implements CommandValue, Serializable {
            public static @NotNull Record parseCommandValue(@NotNull String value) {
                int firstSeparator = value.indexOf(';');
                int spaceIndex = value.indexOf(' ');

                if (firstSeparator == -1 || (spaceIndex != -1 && firstSeparator > spaceIndex)) {
                    return new Record(null, CommandValueExecutor.CONSOLE, value);
                }

                String target = value.substring(0, firstSeparator);
                if (target.equals("EVERYONE")) target = "!EVERYONE";
                value = value.substring(firstSeparator + 1); // Removing target from value.
                int secondSeparator = value.indexOf(';');
                if (secondSeparator == -1) return new Record(target, CommandValueExecutor.CONSOLE, value);

                CommandValueExecutor executor;
                try {
                    executor = CommandValueExecutor.valueOf(value.substring(0, secondSeparator));
                } catch (IllegalArgumentException ignored) {
                    executor = CommandValueExecutor.CONSOLE;
                }

                value = value.substring(secondSeparator + 1); // Removing executor from value, leaving only the command.
                return new Record(target, executor, value);
            }
        }
    }

    record Record(@NotNull List<CommandValue> values) implements Command, Serializable {
    }
}
