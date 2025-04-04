package org.mcsmp.de.fancyPluginsCore.exception;

import org.mcsmp.de.fancyPluginsCore.utility.ReflectionUtil;

import java.io.Serial;

/**
 * Represents an exception during reflection operation.
 *
 * @see ReflectionUtil
 */
public final class ReflectionException extends RuntimeException {

	@Serial
	private static final long serialVersionUID = 1L;

	public ReflectionException(final String message) {
		super(message);
	}

	public ReflectionException(final Throwable ex, final String message) {
		super(message, ex);
	}
}
