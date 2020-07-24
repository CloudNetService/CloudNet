package de.dytanic.cloudnet.common.concurrent;

/**
 * A listener for all tasks, that should handle the process
 *
 * @param <T> the type of the listener, which should accept if the operation from the ITask instance is complete
 * @see ITask
 */
public interface ITaskListener<T> {

    /**
     * An default implementation that prints an error into the System.err stream, if the
     * operation wasn't successful
     * @deprecated use {@link ITask#fireExceptionOnFailure()} instead
     */
    @Deprecated
    ITaskListener FIRE_EXCEPTION_ON_FAILURE = new ITaskListener() {
        @Override
        public void onFailure(ITask task, Throwable th) {
            th.printStackTrace();
        }
    };

    /**
     * Will fired if the operation was completed
     *
     * @param task
     * @param t
     */
    default void onComplete(ITask<T> task, T t) {
    }

    default void onCancelled(ITask<T> task) {
    }

    default void onFailure(ITask<T> task, Throwable th) {
    }
}