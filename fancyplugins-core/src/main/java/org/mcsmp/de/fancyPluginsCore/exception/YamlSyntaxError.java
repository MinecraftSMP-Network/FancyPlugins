package org.mcsmp.de.fancyPluginsCore.exception;

import java.io.File;

/**
 * Thrown when we load data from yaml and the syntax is invalid.
 */
public final class YamlSyntaxError extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/**
	 * The file that has the syntax error, if any.
	 */
	private final File file;

	public YamlSyntaxError(final Throwable parent, final File file) {
		super(parent);

		this.file = file;
	}

	@Override
	public String getMessage() {
		return "Failed to read yaml file" + (this.file != null ? " " + this.file : "") + " due to bad syntax! Copy and paste its content to yaml-online-parser.appspot.com and fix it. Please note emojis in itemstacks might be unsupported on some systems. Got: " + super.getMessage();
	}
}
