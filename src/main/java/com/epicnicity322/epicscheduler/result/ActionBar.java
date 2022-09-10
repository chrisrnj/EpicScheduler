package com.epicnicity322.epicscheduler.result;

import com.epicnicity322.epicscheduler.result.type.TargetableResult;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public interface ActionBar extends TargetableResult {
    @Override
    @NotNull
    default String resultName() {
        return "Action Bar";
    }

    default void perform(@NotNull Player player) {
        String legacyText = TargetableResult.format(player, text());
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(legacyText));
    }

    @NotNull String text();

    record Record(@NotNull String text) implements ActionBar, Serializable {
    }
}
