package com.mosaic.jact.jobqueues;

public class SlowStripedMailboxTests extends JobQueueInterfaceTestCases {

    public SlowStripedMailboxTests() {
        super( StripedJobQueueFactory.stripeJobQueues(new JobQueue[] {new CASJobQueue(), new CASJobQueue(), new CASJobQueue()}) );

        isStriped = true;
    }

}