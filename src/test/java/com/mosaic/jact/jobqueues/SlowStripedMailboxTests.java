package com.mosaic.jact.jobqueues;

public class SlowStripedMailboxTests extends JobQueueInterfaceTestCases {

    private static JobQueue createElementQueue() {
        return new SynchronizedJobQueueWrapper( new LinkedListJobQueue() );
    }

    public SlowStripedMailboxTests() {
        super( StripedJobQueueFactory.stripeJobQueues(new JobQueue[] {createElementQueue(),createElementQueue(),createElementQueue()}) );

        bulkPopMayReturnLessThanAllJobs = true;
    }

}