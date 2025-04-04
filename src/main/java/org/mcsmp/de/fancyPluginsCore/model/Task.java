package org.mcsmp.de.fancyPluginsCore.model;

/**
 * Represents a task being executed by the scheduler.
 * Supports Folia and proxy.
 */
public interface Task {

	/**
	 * Returns the taskId for the task.
	 *
	 * @return Task id number
	 */
	int getTaskId();

	/**
	 * Returns the Plugin that owns this task.
	 *
	 * @return The Plugin that owns the task
	 */
	Object getOwner();

	/**
	 * Returns true if the Task is a sync task.
	 *
	 * @return true if the task is run by main thread
	 */
	boolean isSync();

	/**
	 * Returns true if this task has been cancelled.
	 *
	 * @return true if the task has been cancelled
	 */
	boolean isCancelled();

	/**
	 * Will attempt to cancel this task.
	 */
	void cancel();
}
