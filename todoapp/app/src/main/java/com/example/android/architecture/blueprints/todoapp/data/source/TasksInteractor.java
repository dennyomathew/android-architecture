package com.example.android.architecture.blueprints.todoapp.data.source;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;

import com.example.android.architecture.blueprints.todoapp.BuildConfig;
import com.example.android.architecture.blueprints.todoapp.data.Task;
import com.example.android.architecture.blueprints.todoapp.data.source.local.TasksPersistenceContract;
import com.example.android.architecture.blueprints.todoapp.tasks.TasksFilterType;

public class TasksInteractor {

    private static TasksInteractor INSTANCE;

    public final static int TASKS_LOADER = 1;
    private final static int TASK_LOADER = 2;

    public final static String KEY_TASK_FILTER = BuildConfig.APPLICATION_ID + "TASK_FILTER";
    public final static String KEY_TASK_ID = BuildConfig.APPLICATION_ID + "TASK_ID";

    private final LoaderProvider mLoaderProvider;
    private final LoaderManager mLoaderManager;
    private final ContentResolver mContentResolver;

    public static TasksInteractor getInstance(LoaderProvider mLoaderProvider, LoaderManager mLoaderManager, ContentResolver mContentResolver) {
        if (INSTANCE == null) {
            INSTANCE = new TasksInteractor(mLoaderProvider, mLoaderManager, mContentResolver);
        }
        return INSTANCE;
    }

    private TasksInteractor(LoaderProvider mLoaderProvider, LoaderManager mLoaderManager, ContentResolver contentResolver) {
        this.mLoaderProvider = mLoaderProvider;
        this.mLoaderManager = mLoaderManager;
        this.mContentResolver = contentResolver;
    }

    public void getTasks(final Bundle extras, LoaderManager.LoaderCallbacks<Cursor> callback) {
        mLoaderManager.initLoader(TASKS_LOADER, extras, callback);
    }

    public void getTask(String taskId, GetTasksCallback callback) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(TasksInteractor.KEY_TASK_ID, taskId);

        if (mLoaderManager.getLoader(TASK_LOADER) == null) {
            mLoaderManager.initLoader(TASK_LOADER, bundle, new TasksCursorLoaderCallback(callback));
        } else {
            mLoaderManager.restartLoader(TASK_LOADER, bundle, new TasksCursorLoaderCallback(callback));
        }
    }

    public void completeTask(Task task) {
        try {
            ContentValues values = TaskValues.from(task);
            values.put(TasksPersistenceContract.TaskEntry.COLUMN_NAME_COMPLETED, 1);

            String selection = TasksPersistenceContract.TaskEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?";
            String[] selectionArgs = {task.getId()};

            mContentResolver.update(TasksPersistenceContract.TaskEntry.buildTasksUri(), values, selection, selectionArgs);
        } catch (IllegalStateException e) {
            // Send to analytics, log etc
        }
    }

    public void activateTask(Task activeTask) {
        try {
            ContentValues values = TaskValues.from(activeTask);
            values.put(TasksPersistenceContract.TaskEntry.COLUMN_NAME_COMPLETED, false);

            String selection = TasksPersistenceContract.TaskEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?";
            String[] selectionArgs = {activeTask.getId()};

            mContentResolver.update(TasksPersistenceContract.TaskEntry.buildTasksUri(), values, selection, selectionArgs);
        } catch (IllegalStateException e) {
            // Send to analytics, log etc
        }
    }

    public void clearCompletedTasks() {
        try {
            String selection = TasksPersistenceContract.TaskEntry.COLUMN_NAME_COMPLETED + " LIKE ?";
            String[] selectionArgs = {"1"};

            mContentResolver.delete(TasksPersistenceContract.TaskEntry.buildTasksUri(), selection, selectionArgs);
        } catch (IllegalStateException e) {
            // Send to analytics, log etc
        }
    }

    public void deleteAllTasks() {
        try {
            mContentResolver.delete(TasksPersistenceContract.TaskEntry.buildTasksUri(), null, null);
        } catch (IllegalStateException e) {
            // Send to analytics, log etc
        }
    }

    public void saveTask(Task newTask) {
        ContentValues values = TaskValues.from(newTask);
        mContentResolver.insert(TasksPersistenceContract.TaskEntry.buildTasksUri(), values);
    }

    public void deleteTask(Task deletedTask) {
        String selection = TasksPersistenceContract.TaskEntry.COLUMN_NAME_ENTRY_ID + " LIKE ?";
        String[] selectionArgs = {deletedTask.getId()};

        mContentResolver.delete(TasksPersistenceContract.TaskEntry.buildTasksUri(), selection, selectionArgs);
    }

    public interface GetTasksCallback {
        void onDataLoaded(Cursor data);

        void onDataEmpty();

        void onDataNotAvailable();

        void onDataReset();
    }

    private class TasksCursorLoaderCallback implements LoaderManager.LoaderCallbacks<Cursor> {
        private final GetTasksCallback callback;

        public TasksCursorLoaderCallback(GetTasksCallback callback) {
            this.callback = callback;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            switch (id) {
                case TASKS_LOADER:
                    TasksFilterType tasksFilterType = (TasksFilterType) args.getSerializable(KEY_TASK_FILTER);
                    return mLoaderProvider.createFilteredTasksLoader(tasksFilterType);
                case TASK_LOADER:
                    String taskId = args.getString(KEY_TASK_ID);
                    return mLoaderProvider.createTaskLoader(taskId);
                default:
                    throw new IllegalArgumentException("Loader Id not recognised");
            }
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
            if (data != null) {
                if (data.getCount() > 0) {
                    callback.onDataLoaded(data);
                } else {
                    callback.onDataEmpty();
                }
            } else {
                callback.onDataNotAvailable();
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            callback.onDataReset();
        }

    }

}
