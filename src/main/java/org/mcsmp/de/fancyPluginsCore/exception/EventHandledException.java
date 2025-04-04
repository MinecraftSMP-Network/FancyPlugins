package org.mcsmp.de.fancyPluginsCore.exception;

import lombok.Getter;
import org.mcsmp.de.fancyPluginsCore.model.FancyComponent;

/**
 * Represents a silent exception thrown then handling events,
 * this will only send the event player a message.
 */
public final class EventHandledException extends CommandException {

  private static final long serialVersionUID = 1L;

  /**
   * Should we cancel this event?
   */
  @Getter
  private final boolean cancelled;

  public EventHandledException() {
    this(true);
  }

  /**
   * Create a new command exception with messages for the command sender
   *
   * @param cancelled
   */
  public EventHandledException(final boolean cancelled) {
    super();

    this.cancelled = cancelled;
  }

  /**
   * Create a new command exception with messages for the command sender
   *
   * @param cancelled
   * @param message
   */
  public EventHandledException(final boolean cancelled, final FancyComponent message) {
    super(message);

    this.cancelled = cancelled;
  }
}
