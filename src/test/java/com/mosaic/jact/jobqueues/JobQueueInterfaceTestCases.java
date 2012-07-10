package com.mosaic.jact.jobqueues;

import com.mosaic.jact.AsyncContext;
import com.mosaic.jact.AsyncJob;
import com.mosaic.utils.SetUtils;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

/**
 *
 */
public abstract class JobQueueInterfaceTestCases {

    protected JobQueue         jobQueue;
    private   boolean          guaranteesOrder;

    protected boolean bulkPopMayReturnLessThanAllJobs = false;

    protected JobQueueInterfaceTestCases( JobQueue mb ) {
        this.jobQueue         = mb;
        this.guaranteesOrder  = jobQueue.maintainsOrder();
    }


    @Test
    public void givenNoCallsToJobQueue_expectEmptyJobQueue() {
        assertTrue( jobQueue.isEmpty() );
        assertNull( jobQueue.pop() );
    }
// todo givenEmptyJobQueue_bulkPop_expectChildToBeEmpty

    @Test
    public void givenEmptyJQ_pushJob_expectToBeAbleToRetrieveIt() {
        AsyncJob job = mock( AsyncJob.class );

        jobQueue.push( job );

        JobQueue child = jobQueue.bulkPop();
        assertFalse( child.isEmpty() );
        assertTrue( child.pop() == job );
        assertTrue( jobQueue.isEmpty() );
    }

    @Test
    public void removeLastJob_expectThatToMarkTheJobQueueAsEmpty() {
        AsyncJob job = mock( AsyncJob.class );

        jobQueue.push( job );

        jobQueue.bulkPop();

        assertTrue( jobQueue.isEmpty() );
    }

    @Test
    public void addMultipleJobs_bulkPop_expectQueueToBeMarkedAsEmpty() {
        AsyncJob job1 = mock( AsyncJob.class );
        AsyncJob job2 = mock( AsyncJob.class );

        jobQueue.push( job1 );
        jobQueue.push( job2 );

        jobQueue.bulkPop();
        if ( bulkPopMayReturnLessThanAllJobs ) {
            jobQueue.bulkPop();
        }

        assertTrue( jobQueue.isEmpty() );
    }

    @Test
    public void givenNoneEmptyJQ_expectToBeAbleToRetrieveThem() {
        AsyncJob job1 = mock( AsyncJob.class );
        AsyncJob job2 = mock( AsyncJob.class );
        AsyncJob job3 = mock( AsyncJob.class );

        jobQueue.push( job1 );
        jobQueue.push( job2 );
        jobQueue.push( job3 );

        assertJobQueueContains( job1, job2, job3 );
    }

    private void assertJobQueueContains( AsyncJob...expectedJobs ) {
        if ( guaranteesOrder ) {
            for ( int i=0; i<expectedJobs.length; i++ ) {
                AsyncJob j = expectedJobs[i];

                assertFalse( String.format( "index[%s] is missing", i ), jobQueue.isEmpty() );

                AsyncJob a = jobQueue.pop();
                assertTrue( String.format("index[%s]: %s != %s",i,a,j), a == j );
            }
        } else {
            List<AsyncJob> jobs = new LinkedList<AsyncJob>();
            while ( jobQueue.hasContents() ) {
                jobs.add( jobQueue.pop() );
            }

            assertSetEquals( SetUtils.asSet(expectedJobs), SetUtils.toSet(jobs.iterator()) );
        }

        assertTrue( jobQueue.isEmpty() );
    }

    private void assertSetEquals( Set expectedSet, Set actualSet ) {
        SetUtils.SetComparison r = SetUtils.compare( expectedSet, actualSet );

        assertEquals( "The following elements were not expected: "+r.onlyInSetB, 0, r.onlyInSetB.size() );
        assertEquals( "The following elements were expected, but did not occur: " + r.onlyInSetA, 0, r.onlyInSetA.size() );
    }

    @Test
    public void givenJQWhichHasHadItsEntriesPopped_expectItToBeEmpty() {
        AsyncJob job1 = mock( AsyncJob.class );
        AsyncJob job2 = mock( AsyncJob.class );
        AsyncJob job3 = mock( AsyncJob.class );

        jobQueue.push( job1 );
        jobQueue.push( job2 );
        jobQueue.push( job3 );

        jobQueue.bulkPop();

        if ( bulkPopMayReturnLessThanAllJobs ) {
            jobQueue.bulkPop();
            jobQueue.bulkPop();
        }


        JobQueue child = jobQueue.bulkPop();
        assertFalse( child.hasContents() );
    }

    @Test
    public void concurrencyTest_fiveThreads_eachPushAndPopEvents_ensureThatEachThreadSeesConsistentOrderForTheirOwnPushAndPops() throws InterruptedException {
        if ( !jobQueue.isThreadSafe() ) {
            return;
        }

        int numProducerThreads = 5;
        int numJobsPerThread   = 2000;

        CountDownLatch latch = new CountDownLatch( numProducerThreads+1 );

        for ( int i=0; i<numProducerThreads; i++ ) {
            new ProducerThread(jobQueue, latch, numJobsPerThread, i).start();
        }

        ConsumerThread consumerThread = new ConsumerThread(jobQueue, latch, numJobsPerThread, numProducerThreads);
        consumerThread.start();

        boolean didLatchComplete = latch.await( 2, TimeUnit.SECONDS ); // latch will timeout if the push/pops do not match

        assertTrue( "latched timed out", didLatchComplete );

        if ( guaranteesOrder ) {
            assertEquals( 0, consumerThread.mismatchCount );
        }
    }

    @Test
    public void pushTwentyJobs_ensureAllTwentyPopAgain() {
        int numMessages = 20;

        for ( int i=0; i<numMessages; i++ ) {
            jobQueue.push( mock(AsyncJob.class) );
        }

        for ( int i=0; i<numMessages; i++ ) {
            assertNotNull( "popping index "+i+" expected job", jobQueue.pop() );
        }

        assertNull( jobQueue.pop() );
    }

    @Test
    public void insertPop_insertPop() {
        AsyncJob job1 = mock(AsyncJob.class);
        AsyncJob job2 = mock(AsyncJob.class);

        jobQueue.push( job1 );
        assertTrue( job1 == jobQueue.pop() );
        assertTrue( jobQueue.isEmpty() );

        jobQueue.push( job2 );
        assertTrue( job2 == jobQueue.pop() );
        assertTrue( jobQueue.isEmpty() );
    }

//    @Test
    public void timePushPop() {
        doTimePushPop();
        doTimePushPop();
        doTimePushPop();
        doTimePushPop();
    }

    private void doTimePushPop() {
        long     startNanos = System.nanoTime();
        AsyncJob job        = new AsyncJob() {
            @Override
            public Object invoke( AsyncContext asyncContext ) throws Exception {
                return null;
            }

            @Override
            public int hashCode() {
                return 77;
            }
        };

        final int numMessages = 1000000;
        for ( int i=0; i<numMessages; i++ ) {
            jobQueue.push( job );
            jobQueue.pop();
        }

        long endNanos       = System.nanoTime();
        long durationMillis = endNanos - startNanos;

        System.out.println("timePushPop:      " + this.getClass().getSimpleName() + " "+ (durationMillis/1000000.0) + "ms");
    }

//    @Test
    public void timePushBulkPop() {
        doTimePushBulkPop();
        doTimePushBulkPop();
        doTimePushBulkPop();
        doTimePushBulkPop();
    }

    private void doTimePushBulkPop() {
        long     startNanos = System.nanoTime();
        AsyncJob job        = mock( AsyncJob.class );

        final int numBatches = 100000;
        final int batchSize  = 10;
        for ( int i=0; i<numBatches; i++ ) {
            for ( int j=0; j<batchSize; j++ ) {
                jobQueue.push( job );
            }

            do {
                JobQueue child = jobQueue.bulkPop();

                AsyncJob j = child.pop();
                while ( j != null ) {
                    j = child.pop();
                }
            } while (jobQueue.hasContents());
        }

        long endNanos       = System.nanoTime();
        long durationMillis = endNanos - startNanos;


        System.out.println("timePushBulkPop: " + this.getClass().getSimpleName() + " "+ (durationMillis/1000000.0) + "ms");
    }

    private static class ProducerThread extends Thread {
        private JobQueue jobQueue;
        private CountDownLatch latch;
        private int            numJobsPerThread;
        private int threadNumber;

        public ProducerThread( JobQueue jobQueue, CountDownLatch latch, int numJobsPerThread, int threadNumber ) {
            this.jobQueue          = jobQueue;
            this.latch            = latch;
            this.numJobsPerThread = numJobsPerThread;
            this.threadNumber     = threadNumber;
        }

        @Override
        public void run() {
            for ( int i=0; i<numJobsPerThread; i++ ) {
                jobQueue.push( new CTJob(threadNumber, i) );

                if ( i % 10 == 0 ) {
                    Thread.yield();
                }
            }

            latch.countDown();
        }
    }

    private static class CTJob extends AsyncJob<String> {
        private final String jobName;
        private final int    threadNumber;
        private final int    jobNumber;

        public CTJob( int threadNumber, int jobNumber ) {
            this.threadNumber = threadNumber;
            this.jobNumber    = jobNumber;
            this.jobName      = threadNumber + " " + jobNumber;
        }

        public String invoke( AsyncContext asyncContext ) throws Exception {
            return jobName;
        }
    }

    private class ConsumerThread extends Thread {
        private final JobQueue jobQueue;
        private final CountDownLatch latch;

        private final int[] perThreadJobMarker;
        private final int   totalNumMessagesExpected;


        public int mismatchCount = 0;

        public ConsumerThread( JobQueue jobQueue, CountDownLatch latch, int numJobsPerThread, int numProducerThreads ) {
            this.jobQueue = jobQueue;
            this.latch   = latch;

            this.perThreadJobMarker       = new int[numProducerThreads];
            this.totalNumMessagesExpected = numJobsPerThread * numProducerThreads;
        }

        @Override
        public void run() {
            int messageCount  = 0;

            while ( messageCount < totalNumMessagesExpected ) {
                AsyncJob j = jobQueue.pop();

                if ( j != null ) {
                    messageCount++;

                    CTJob job = (CTJob) j;
                    int threadNumber = job.threadNumber;
                    int jobNumber    = job.jobNumber;

                    int expectedJobNumber = perThreadJobMarker[threadNumber];
                    if ( expectedJobNumber == jobNumber ) {
                        perThreadJobMarker[threadNumber] = jobNumber+1;
                    } else {
                        mismatchCount++;

                        continue;
                    }

                }
            }

            latch.countDown();
        }
    }
}
