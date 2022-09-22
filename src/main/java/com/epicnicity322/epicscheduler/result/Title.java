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
import com.epicnicity322.yamlhandler.ConfigurationSection;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

public interface Title extends TargetableResult {
    @Override
    @NotNull
    default String resultName() {
        return "Title";
    }

    @Override
    default void perform(@NotNull Player player) {
        String title = TargetableResult.format(player, title());
        String subtitle = TargetableResult.format(player, subtitle());
        player.sendTitle(title, subtitle, fadeIn(), stay(), fadeOut());
    }

    @Override
    default void set(@NotNull ConfigurationSection section) {
        section.set("Title", title().replace(ChatColor.COLOR_CHAR, '&'));
        section.set("Subtitle", title().replace(ChatColor.COLOR_CHAR, '&'));
        section.set("Fade In", fadeIn());
        section.set("Stay", stay());
        section.set("Fade Out", fadeOut());
    }

    @NotNull String title();

    @NotNull String subtitle();

    int fadeIn();

    int stay();

    int fadeOut();

    record Record(@NotNull String title, @NotNull String subtitle, int fadeIn, int stay,
                  int fadeOut) implements Title, Serializable {
    }
}
