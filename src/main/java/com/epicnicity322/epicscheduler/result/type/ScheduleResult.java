package com.epicnicity322.epicscheduler.result.type;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface ScheduleResult extends Result {
    boolean pickRandom();

    @NotNull List<Result> results();

    /**
     * Who this result will be executed to. The options are:
     * <ul>
     *     <li>"!EVERYONE" - Everyone online in the server at the moment.</li>
     *     <li>{@literal <world>} - A world's name.</li>
     *     <li>{@literal <player>} - A player's {@link java.util.UUID}.</li>
     * </ul>
     *
     * @return "!EVERYONE", a world name, or an {@link java.util.UUID} of a player.
     */
    @Nullable String target();

    @Override
    default void perform() {
        Collection<? extends Player> targets = TargetableResult.findTarget(target());
        List<Result> results = results();
        if (pickRandom() && !results.isEmpty()) {
            results = new ArrayList<>(results);
            Collections.shuffle(results);
            results = Collections.singletonList(results.stream().findAny().orElseThrow());
        }
        for (Result result : results) {
            if (result instanceof TargetableResult targetable) {
                if (targets.isEmpty()) continue;
                for (Player player : targets) targetable.perform(player);
            } else {
                result.perform();
            }
        }
    }

    record Record(@NotNull String resultName, boolean pickRandom, @NotNull List<Result> results,
                  @Nullable String target) implements ScheduleResult, Serializable {
    }
}
