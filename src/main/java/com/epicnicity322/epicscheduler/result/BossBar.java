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
