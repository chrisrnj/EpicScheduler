package com.epicnicity322.epicscheduler.result;

import com.epicnicity322.epicscheduler.result.type.TargetableResult;
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

    @NotNull String title();

    @NotNull String subtitle();

    int fadeIn();

    int stay();

    int fadeOut();

    record Record(@NotNull String title, @NotNull String subtitle, int fadeIn, int stay,
                  int fadeOut) implements Title, Serializable {
    }
}
