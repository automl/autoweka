package autoweka;

/**
 * Generic WorkerThread that runs for a specific amount of time, then sends an interrupt to the work once a timeout has been hit - if the thread still doesn't stop, it gets killed hard
 */
abstract class WorkerThread extends Thread
{
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
     */
    public Exception getException()
    {
        return mException;
    }

    /**
     * Subclasses should do all their work in here
     */
    abstract protected void doWork() throws Exception;

    /**
     * For diagnostics, return a string that says what you were trying to do here
     */
    abstract protected String getOpName();

    /**
     * True if the job finished before it was killed hard
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
     */
    public boolean terminated()
    {
        return mTerminated;
    }

    /**
     * Main way of using this class - you should tell it how long you want to run for, and it will return withing some multiplyer of that time
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
                System.out.println(getOpName() + " completed");
                break;
            }

            //Are we at a point where we need to kill the sucker?
            if(!interrupted && (mOSBean.getProcessCpuTime() - startTime > timeout /*|| wallTime > timeout * mWalltimeMultiplyer*/))
            {
                //Try to interrupt the bugger
                this.interrupt();
                pollInterval = (long)(timeout * Math.max(0, (msTimeoutMultiplyer - 1)))/1000000;
                System.out.println(getOpName() + " interrupted");
                interrupted = true;
            }
            else if(!stopped && (mOSBean.getProcessCpuTime() - startTime > timeout * msTimeoutMultiplyer /*|| wallTime > timeout * mWalltimeMultiplyer * mTimeoutMultiplyer*/))
            {
                //Try to interrupt the bugger
                this.terminate();
                System.out.println(getOpName() + " aborted (it's only been suspended - leaks are likely!)");
                stopped = true;
                break;
            }
        }
        long stopTime = mOSBean.getProcessCpuTime();
        return (stopTime - startTime) * 1e-9f;
    }

}

