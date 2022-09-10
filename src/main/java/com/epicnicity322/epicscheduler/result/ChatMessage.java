package com.epicnicity322.epicscheduler.result;

import com.epicnicity322.epicscheduler.result.type.TargetableResult;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public interface ChatMessage extends TargetableResult {
    @Override
    @NotNull
    default String resultName() {
        return "Chat Message";
    }

    @Override
    default void perform(@NotNull Player player) {
        player.sendMessage(TargetableResult.format(player, text()));
    }

    @NotNull String text();

    record Record(@NotNull String text) implements ChatMessage, Serializable {
    }
}
