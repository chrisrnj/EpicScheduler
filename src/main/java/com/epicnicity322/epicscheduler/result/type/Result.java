package com.epicnicity322.epicscheduler.result.type;

import org.jetbrains.annotations.NotNull;

/**
 * The result to happen when the time of a command is due.
 */
public interface Result {
    @NotNull String resultName();

    void perform();
}
