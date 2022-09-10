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
