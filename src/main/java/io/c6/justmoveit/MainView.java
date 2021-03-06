package io.c6.justmoveit;

import static io.c6.justmoveit.Utils.UTILS;
import static java.time.Duration.ZERO;
import static java.util.Optional.ofNullable;
import static java.util.logging.Level.ALL;
import static java.util.logging.Level.SEVERE;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.WindowConstants.EXIT_ON_CLOSE;

import java.awt.AWTException;
import java.awt.CardLayout;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Main application window which contains the core logic
 * of triggering key press using `java.awt.Robot`
 *
 * @author Chandrasekhar Thotakura
 */
final class MainView {

  private static final Logger LOG;

  static {
    LOG = Logger.getLogger(Strings.LOGGER_NAME);
    LOG.setLevel(ALL);
    try {
      final FileHandler fileHandler = new FileHandler(Strings.LOG_FILE_NAME, true);
      fileHandler.setFormatter(new SimpleFormatter());
      LOG.addHandler(fileHandler);
    } catch (final IOException e) {
      Logger.getGlobal().log(SEVERE, e.getMessage(), e);
    }
  }

  private static final int WINDOW_WIDTH = 300;
  private static final int WINDOW_HEIGHT = 200;

  private final JFrame frame;
  private final CardLayout cardLayout;
  private final JPanel pane;
  private final InputView inputPanel;
  private final OutputView outputPanel;

  private IntervalRunner runner;
  private Robot robot;

  MainView() {
    frame = new JFrame(Strings.FRAME_TITLE);
    cardLayout = new CardLayout();
    pane = new JPanel(cardLayout);
    inputPanel = new InputView(this);
    outputPanel = new OutputView(this);
  }

  void open() {
    pane.add(inputPanel.getContainer());
    pane.add(outputPanel.getContainer());
    frame.setContentPane(pane);
    frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
    frame.addWindowListener(onBeforeClosing());
    frame.setResizable(false);
    frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
    initRobot();
  }

  private WindowAdapter onBeforeClosing() {
    return new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        super.windowClosing(e);
        LOG.fine(Strings.LOG_MSG_EXITING_APP);
        cleanup();
      }
    };
  }

  private void initRobot() {
    try {
      robot = new Robot();
    } catch (final AWTException ex) {
      LOG.log(SEVERE, Strings.LOG_ERR_ROBOT_INIT_ERROR, ex);
      LOG.fine(Strings.LOG_MSG_EXITING_APP);
      System.exit(1);
    }
  }

  private void tryPressingKey(final Duration elapsed) {
    if (UTILS.isDivisibleInSeconds(elapsed, inputPanel.getIntervalDuration())) {
      LOG.info(Strings.LOG_MSG_KEY_PRESSED);
      robot.keyRelease(KeyEvent.VK_F23);
    }
  }

  private void foreverConsumerTask(final Duration elapsed) {
    tryPressingKey(elapsed);
    outputPanel.updateLabels(elapsed, null);
  }

  private void fixedDurationConsumerTask(final Duration elapsed, final Duration remaining) {
    if (ZERO.equals(remaining)) {
      onExitHandler();
    } else {
      tryPressingKey(elapsed);
      outputPanel.updateLabels(elapsed, remaining);
    }
  }

  private void switchToInputView() {
    cardLayout.first(pane);
    cleanup();
  }

  private void switchToOutputView() {
    cardLayout.last(pane);
    outputPanel.updateIntervalDuration(inputPanel.getIntervalDuration());
  }

  private void startIntervalRunner() {
    // Lazy initialization of `IntervalRunner` based on `isFixedTimeEnabled`
    final Supplier<IntervalRunner> fixedDurationSupplier = () -> new FixedDurationRunner(
        inputPanel.getExecutionDuration(), this::fixedDurationConsumerTask);
    final Supplier<IntervalRunner> foreverSupplier = () -> new ForeverRunner(
        this::foreverConsumerTask);
    runner = inputPanel.isFixedTimeEnabled() ? fixedDurationSupplier.get() : foreverSupplier.get();
  }

  private void cleanup() {
    ofNullable(runner).ifPresent(IntervalRunner::stop);
  }

  void onStartHandler() {
    startIntervalRunner();
    switchToOutputView();
  }

  void onStopHandler() {
    switchToInputView();
  }

  void onExitHandler() {
    LOG.fine(Strings.LOG_MSG_EXITING_APP);
    cleanup();
    invokeLater(frame::dispose);
  }
}
