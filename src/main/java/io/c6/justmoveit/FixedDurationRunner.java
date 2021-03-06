package io.c6.justmoveit;

import static io.c6.justmoveit.Utils.MILLIS_PER_SECOND;
import static io.c6.justmoveit.Utils.ONE_SECOND;
import static java.time.Duration.ZERO;
import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Implementation for `IntervalRunner`. An executor will run the `FixedDurationTask`
 * given by the client for every second until the specified execution duration is completed.
 *
 * @author Chandrasekhar Thotakura
 */
final class FixedDurationRunner implements IntervalRunner {

  private static final int EXECUTOR_POOL_SIZE = 1;
  private static final long EXECUTOR_DELAY_MILLIS = 0L;

  private final FixedDurationTask task;
  private final ScheduledExecutorService executor = newScheduledThreadPool(EXECUTOR_POOL_SIZE);

  private Duration remainingDuration;
  private Duration elapsedDuration;

  FixedDurationRunner(final Duration executionDuration, final FixedDurationTask task) {
    remainingDuration = executionDuration;
    this.task = task;
    executor.scheduleAtFixedRate(
        this::run, EXECUTOR_DELAY_MILLIS, MILLIS_PER_SECOND, MILLISECONDS);
    elapsedDuration = ZERO;
  }

  @Override
  public void stop() {
    if (!isDone()) {
      executor.shutdownNow();
    }
  }

  @Override
  public boolean isDone() {
    return executor.isShutdown();
  }

  private void run() {
    task.execute(elapsedDuration, remainingDuration);
    remainingDuration = remainingDuration.minus(ONE_SECOND);
    elapsedDuration = elapsedDuration.plus(ONE_SECOND);
  }

  @FunctionalInterface
  interface FixedDurationTask {

    void execute(Duration elapsed, Duration remaining);
  }
}
