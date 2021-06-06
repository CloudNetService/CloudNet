package de.dytanic.cloudnet.common.concurrent;

public class CompletableTaskListener<T> implements ITaskListener<T> {

  private final CompletableTask<T> task;

  public CompletableTaskListener(CompletableTask<T> task) {
    this.task = task;
  }

  @Override
  public void onCancelled(ITask<T> task) {
    this.task.cancel(true);
  }

  @Override
  public void onComplete(ITask<T> task, T t) {
    this.task.complete(t);
  }

  @Override
  public void onFailure(ITask<T> task, Throwable th) {
    this.task.fail(th);
  }
}
