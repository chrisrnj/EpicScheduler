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
