package autoweka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic WorkerThread that runs for a specific amount of time, then sends an interrupt to the work once a timeout has been hit - if the thread still doesn't stop, it gets killed hard
 */
abstract class WorkerThread extends Thread
{
    final Logger log = LoggerFactory.getLogger(WorkerThread.class);

    private com.sun.management.OperatingSystemMXBean mOSBean = (com.sun.management.OperatingSystemMXBean) java.lang.management.ManagementFactory.getOperatingSystemMXBean();
    private static final int msPollInterval = 5;
    private static final float msTimeoutMultiplyer = 1.5f;
    private static final float msWalltimeMultiplyer = 2.0f;

    private volatile Exception mException = null;
    private volatile boolean mCompleted = false;
    private volatile boolean mTerminated = false;

    /** The run method of the thread */
    public void run()
    {
        try {
            doWork();
            if(!mTerminated)
            {
                mCompleted = true;
            }
        } catch (Exception e) {
            mException = e;
        } catch (Throwable t) {
            mException = new RuntimeException(t);
        }
    }

    /**
     * Gets whatever exception was thrown by this thread
     * @return The exception if it exists.
     */
    public Exception getException()
    {
        return mException;
    }

    /**
     * Subclasses should do all their work in here
     * @throws Exception if anything goes wrong.
     */
    abstract protected void doWork() throws Exception;

    /*
     * For diagnostics, return a string that says what you were trying to do here
     */
    abstract protected String getOpName();

    /**
     * True if the job finished before it was killed hard
     * @return Whether the job completed.
     */
    public boolean completed()
    {
        return mCompleted;
    }

    /**
     * Request that a job be stopped
     */
    public void terminate()
    {
        mTerminated = true;
        this.suspend();
        this.setPriority(Thread.MIN_PRIORITY);
    }

    /**
     * Checks to see if the job was killed hard
     * @return Whether the job was killed.
     */
    public boolean terminated()
    {
        return mTerminated;
    }

    /**
     * Main way of using this class - you should tell it how long you want to run for, and it will return within some multiplier of that time
     * @param timeoutSeconds The timeout in seconds.
     * @return The actual time.
     */
    float runWorker(float timeoutSeconds)
    {
        long timeout = (long)(timeoutSeconds * 1.0e9);
        long wallTime = 0;

        //Record the start time
        long startTime = mOSBean.getProcessCpuTime();
        this.start();

        boolean interrupted = false;
        boolean stopped = false;
        long pollInterval = timeout / 1000000;

        while(true)
        {
            try {
                this.join(pollInterval);
                wallTime += pollInterval;
                pollInterval = msPollInterval;
            } catch(InterruptedException e) {
                //If somehow we get the interrupted exception, we should abort
                break;
            }

            //Did the sucker complete?
            if(this.completed() || this.getException() != null)
            {
                log.debug("{} completed", getOpName());
                break;
            }

            //Are we at a point where we need to kill the sucker?
            if(!interrupted && (mOSBean.getProcessCpuTime() - startTime > timeout /*|| wallTime > timeout * mWalltimeMultiplyer*/))
            {
                //Try to interrupt the bugger
                this.interrupt();
                pollInterval = (long)(timeout * Math.max(0, (msTimeoutMultiplyer - 1)))/1000000;
                log.debug("{} interrupted", getOpName());
                interrupted = true;
            }
            else if(!stopped && (mOSBean.getProcessCpuTime() - startTime > timeout * msTimeoutMultiplyer /*|| wallTime > timeout * mWalltimeMultiplyer * mTimeoutMultiplyer*/))
            {
                //Try to interrupt the bugger
                this.terminate();
                log.debug("{} aborted (it's only been suspended - leaks are likely!)", getOpName());
                stopped = true;
                break;
            }
        }
        long stopTime = mOSBean.getProcessCpuTime();
        return (stopTime - startTime) * 1e-9f;
    }

}

