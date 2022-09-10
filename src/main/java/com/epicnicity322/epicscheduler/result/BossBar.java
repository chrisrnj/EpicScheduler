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
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.UUID;

public interface BossBar extends TargetableResult {
    @Override
    @NotNull
    default String resultName() {
        return "Boss Bar";
    }

    @Override
    default void perform(@NotNull Player player) {
        // Each player must have their own boss bar, for variables and stuff.
        var bossBar = Bukkit.createBossBar(TargetableResult.format(player, title()), color(), style());
        var previousBar = BossBarUtil.previousBossBar.put(player.getUniqueId(), bossBar);
        if (previousBar != null) previousBar.removePlayer(player);
        bossBar.setProgress(progress());
        bossBar.addPlayer(player);
    }

    @NotNull BarColor color();

    @NotNull BarStyle style();

    double progress();

    @NotNull String title();

    record Record(@NotNull String title, @NotNull BarColor color, @NotNull BarStyle style,
                  double progress) implements BossBar, Serializable {
    }

    final class BossBarUtil {
        private static final @NotNull HashMap<UUID, org.bukkit.boss.BossBar> previousBossBar = new HashMap<>();
    }
}
