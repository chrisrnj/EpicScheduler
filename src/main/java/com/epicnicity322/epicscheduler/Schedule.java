package com.epicnicity322.epicscheduler;

import com.epicnicity322.epicscheduler.result.type.ScheduleResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @param dueTime         The time in ticks to wait before executing the results.
 * @param scheduleResults The results to be executed.
 */
public record Schedule(long dueTime, @NotNull List<ScheduleResult> scheduleResults) implements Runnable {
    @Override
    public void run() {
        for (ScheduleResult result : scheduleResults) {
            result.perform();
        }
    }
}
