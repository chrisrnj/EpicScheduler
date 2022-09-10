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
