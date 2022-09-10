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

package com.epicnicity322.epicscheduler.result.type;

import com.epicnicity322.epicscheduler.EpicScheduler;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * A result that will be executed for every target.
 */
public interface TargetableResult extends Result {
    static @NotNull String format(@NotNull Player player, @NotNull String text) {
        if (EpicScheduler.hasPlaceholderAPI()) {
            return PlaceholderAPI.setPlaceholders(player, text);
        } else {
            return text;
        }
    }

    static @NotNull Collection<? extends Player> findTarget(@Nullable String target) {
        if (target == null || target.isBlank()) return Collections.emptyList();
        if (target.equals("!EVERYONE")) {
            return Bukkit.getOnlinePlayers();
        } else {
            for (World w : Bukkit.getWorlds()) {
                // Looking for matching world name.
                if (target.equals(w.getName())) {
                    return w.getPlayers();
                }
            }
            // World name not found, so it could be a UUID of a player.
            try {
                Player p = Bukkit.getPlayer(UUID.fromString(target));
                return p != null ? Collections.singleton(p) : Collections.emptyList();
            } catch (IllegalArgumentException e) {
                // Unknown target, returning empty collection.
                return Collections.emptyList();
            }
        }
    }

    /**
     * Will do nothing as {@link TargetableResult} requires a target to perform.
     */
    @Override
    default void perform() {
    }

    void perform(@NotNull Player player);
}
