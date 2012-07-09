package com.mosaic.jact.jobqueues;

/**
 *
 */
public class FastStripedMailboxTests extends JobQueueInterfaceTestCases {

    public FastStripedMailboxTests() {
        super( StripedJobQueueFactory.stripeJobQueues( new JobQueue[] {new CASJobQueue(), new CASJobQueue()} ) );

        bulkPopMayReturnLessThanAllJobs = true;
    }

}
