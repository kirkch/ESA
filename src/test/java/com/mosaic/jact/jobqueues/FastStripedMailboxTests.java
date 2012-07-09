package com.mosaic.jact.jobqueues;

/**
 *
 */
public class FastStripedMailboxTests extends JobQueueInterfaceTestCases {

    private static JobQueue createElementQueue() {
//        return new SynchronizedJobQueueWrapper( new LinkedListJobQueue() );
        return new CASJobQueue(); // 50ms (over 100k jobs) faster than above
    }

    public FastStripedMailboxTests() {
        super( StripedJobQueueFactory.stripeJobQueues( new JobQueue[] {createElementQueue(), createElementQueue()} ) );

        bulkPopMayReturnLessThanAllJobs = true;
    }

}
