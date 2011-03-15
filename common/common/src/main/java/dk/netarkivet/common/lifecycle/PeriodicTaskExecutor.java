/* File:       $Id$
 * Revision:   $Revision$
 * Author:     $Author$
 * Date:       $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package dk.netarkivet.common.lifecycle;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class wraps a {@link ScheduledThreadPoolExecutor}, allowing to
 * periodically run one or several {@link Runnable} tasks
 * (fixed rate execution).
 * It actively monitors task execution in a separate "checker" thread, allowing
 * to catch and process any {@link RuntimeException} that would be thrown during
 * task execution, which cannot be done by simply overriding
 * {@link ScheduledThreadPoolExecutor#afterExecute}.
 *
 * TODO: Currently {@link RuntimeException} are only caught and logged, but the
 * executor stops scheduling future executions. We should implement a
 * configurable restart mechanism, possibly with exception filtering.
 */
public class PeriodicTaskExecutor {

    /**
     * Represents a periodic task
     */
    public static class PeriodicTask {

        /**
         * A string identifying the task. It should be unique for this executor,
         * though there is no such check made.
         */
        private final String taskId;

        /**
         * The actual task implementation.
         */
        private final Runnable task;

        /**
         * Delay in seconds between starting the executor and the initial task
         * execution.
         */
        private final long secondsBeforeFirstExec;

        /**
         * Delay in seconds between two successive task executions.
         */
        private final long secondsBetweenExec;

        /**
         * The wrapper object for future task executions.
         */
        private ScheduledFuture<?> future = null;

        /**
         * Builds a new task.
         * @param taskId the task id string (should be unique)
         * @param task the actual {@link Runnable} object.
         * @param secondsBeforeFirstExec the delay in seconds between starting
         * the executor and the initial task execution.
         * @param secondsBetweenExec the delay in seconds between two successive
         * task executions.
         */
        public PeriodicTask(
                String taskId,
                Runnable task,
                long secondsBeforeFirstExec,
                long secondsBetweenExec) {
            super();
            this.taskId = taskId;
            this.task = task;
            this.secondsBeforeFirstExec = secondsBeforeFirstExec;
            this.secondsBetweenExec = secondsBetweenExec;
        }

        void setFuture(ScheduledFuture<?> future) {
            this.future = future;
        }

    }

    /** The class logger. */
    final static Log log = LogFactory.getLog(PeriodicTaskExecutor.class);

    /**
     * The actual executor. One thread dedicated to each task.
     */
    private final ScheduledThreadPoolExecutor exec;

    /**
     * Execution status flag, used to control the termination of the checker
     * thread.
     */
    private boolean alive = false;

    /**
     * Separate thread that actively monitors the task executions and catches
     * any {@link ExecutionException} that may occur during an execution.
     */
    private Thread checkerThread = null;

    /**
     * The tasks to run.
     */
    private final PeriodicTask[] tasks;

    /**
     * Builds an executor for a single task.
     * @param taskId the task id string (should be unique)
     * @param task the actual {@link Runnable} object.
     * @param secondsBeforeFirstExec the delay in seconds between starting
     * the executor and the initial task execution.
     * @param secondsBetweenExec the delay in seconds between two successive
     * task executions.
     */
    public PeriodicTaskExecutor(
            String taskId,
            Runnable task,
            long secondsBeforeFirstExec,
            long secondsBetweenExec) {
        this(new PeriodicTask(
                taskId, task, secondsBeforeFirstExec, secondsBetweenExec));
    }

    /**
     * Builds an executor for a set of tasks.
     * @param tasks the task definitions.
     */
    public PeriodicTaskExecutor(PeriodicTask... tasks) {

        this.tasks = tasks;
        this.exec = new ScheduledThreadPoolExecutor(tasks.length);

        alive = true;

        String id = "";
        for (PeriodicTask t : tasks) {
            ScheduledFuture<?> future = exec.scheduleAtFixedRate(
                    t.task,
                    t.secondsBeforeFirstExec,
                    t.secondsBetweenExec,
                    TimeUnit.SECONDS);
            t.setFuture(future);
            id += "_" + t.taskId;
        }

        checkerThread = new Thread(id.hashCode() + "-checker") {
            public void run() {
                while (alive) {
                    checkExecution();
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        if (log.isTraceEnabled()) {
                            log.trace("checkerThread interrupted.");
                        }
                    }
                }
            }
        };

        checkerThread.start();
    }

    /**
     * Checks tasks execution. Called by the checker thread.
     */
    private synchronized void checkExecution() {
        try {
            for (PeriodicTask t : tasks) {
                t.future.get();
            }
        } catch (InterruptedException e) {
            if (log.isTraceEnabled()) {
                log.trace("checkExecution was interrupted.");
            }
        } catch (ExecutionException e) {
            log.error("Task threw exception: " + e.getCause());
        }
    }

    /**
     * Shuts down the executor, attempting to stop any ongoing task execution.
     */
    public void shutdown() {
        alive = false;
        checkerThread.interrupt();
        for (PeriodicTask t : tasks) {
            t.future.cancel(true);
        }
    }

}