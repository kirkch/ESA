package com.mosaic.jact.mailboxes;

import com.mosaic.jact.AsyncJob;
import com.mosaic.utils.SetUtils;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public abstract class MailboxInterfaceTestCases {

    protected Mailbox         mailbox;
    protected MailboxListener mailboxListener;
    private   boolean         guaranteesOrder;


    protected MailboxInterfaceTestCases( Mailbox mb, MailboxListener l, boolean guaranteesOrder ) {
        this.mailbox         = mb;
        this.mailboxListener = l;
        this.guaranteesOrder = guaranteesOrder;
    }


    @Test
    public void givenNoCallsToMailbox_expectEmptyMailboxAndNoCallsToListener() {
        verifyZeroInteractions( mailboxListener );

        assertFalse( mailbox.bulkPop().iterator().hasNext() );
    }

    @Test
    public void pushFirstJob_expectListenerCall() {
        AsyncJob job = mock( AsyncJob.class );

        mailbox.push( job );

        verify(mailboxListener).newPost();
        verifyZeroInteractions( job );
    }

    @Test
    public void givenEmptyMB_pushJob_expectToBeAbleToRetrieveIt() {
        AsyncJob job = mock( AsyncJob.class );

        mailbox.push( job );

        Iterator<AsyncJob> jobs = mailbox.bulkPop().iterator();
        assertTrue( jobs.hasNext() );
        assertTrue( jobs.next() == job );
        assertFalse( jobs.hasNext() );
    }

    @Test
    public void givenEmptyMB_pushTwoJobs_expectListenerCalls() {
        AsyncJob job1 = mock( AsyncJob.class );
        AsyncJob job2 = mock( AsyncJob.class );

        mailbox.push( job1 );
        mailbox.push( job2 );

        if ( guaranteesOrder ) {
            InOrder inorder = inOrder(mailboxListener);
            inorder.verify(mailboxListener, times(2)).newPost();
        } else {
            verify(mailboxListener, times(2)).newPost();
        }

        verifyNoMoreInteractions(mailboxListener);

        verifyZeroInteractions( job1 );
        verifyZeroInteractions( job2 );
    }

    @Test
    public void givenEmptyMB_pushThreeJobs_expectListenerCalls() {
        AsyncJob job1 = mock( AsyncJob.class );
        AsyncJob job2 = mock( AsyncJob.class );
        AsyncJob job3 = mock( AsyncJob.class );

        mailbox.push( job1 );
        mailbox.push( job2 );
        mailbox.push( job3 );

        verify( mailboxListener, times( 3 ) ).newPost();

        verifyNoMoreInteractions(mailboxListener);

        verifyZeroInteractions( job1 );
        verifyZeroInteractions( job2 );
        verifyZeroInteractions( job3 );
    }

    @Test
    public void givenNoneEmptyMB_expectToBeAbleToRetrieveThem() {
        AsyncJob job1 = mock( AsyncJob.class );
        AsyncJob job2 = mock( AsyncJob.class );
        AsyncJob job3 = mock( AsyncJob.class );

        mailbox.push( job1 );
        mailbox.push( job2 );
        mailbox.push( job3 );

        assertMailboxContains( job1, job2, job3 );
    }

    private void assertMailboxContains( AsyncJob...expectedJobs ) {
        Iterator<AsyncJob> jobs = mailbox.bulkPop().iterator();

        if ( guaranteesOrder ) {
            for ( int i=0; i<expectedJobs.length; i++ ) {
                AsyncJob j = expectedJobs[i];

                assertTrue( String.format("index[%s] is missing",i), jobs.hasNext() );

                AsyncJob a = jobs.next();
                assertTrue( String.format("index[%s]: %s != %s",i,a,j), a == j );
            }
        } else {
            assertSetEquals( SetUtils.asSet(expectedJobs), SetUtils.toSet( jobs ) );
        }

        assertFalse( jobs.hasNext() );
    }

    private void assertSetEquals( Set expectedSet, Set actualSet ) {
        SetUtils.SetComparison r = SetUtils.compare( expectedSet, actualSet );

        assertEquals( "The following elements were not expected: "+r.onlyInSetB, 0, r.onlyInSetB.size() );
        assertEquals( "The following elements were expected, but did not occur: "+r.onlyInSetA, 0, r.onlyInSetA.size() );
    }

    @Test
    public void givenMBWhichHasHadItsEntriesPopped_expectItToBeEmpty() {
        AsyncJob job1 = mock( AsyncJob.class );
        AsyncJob job2 = mock( AsyncJob.class );
        AsyncJob job3 = mock( AsyncJob.class );

        mailbox.push( job1 );
        mailbox.push( job2 );
        mailbox.push( job3 );

        mailbox.bulkPop().iterator();


        Iterator<AsyncJob> jobs = mailbox.bulkPop().iterator();
        assertFalse( jobs.hasNext() );
    }

    @Test
    public void givenNonEmptyMB_popJobs_expectListenerCall() {
        AsyncJob job1 = mock( AsyncJob.class );
        AsyncJob job2 = mock( AsyncJob.class );
        AsyncJob job3 = mock( AsyncJob.class );

        mailbox.push( job1 );
        mailbox.push( job2 );
        mailbox.push( job3 );

        mailbox.bulkPop().iterator();


        verify(mailboxListener).postCollected();
    }

    @Test
    public void givenEmptyMB_popJobs_expectNoListenerCall() {
        mailbox.bulkPop().iterator();

        verifyZeroInteractions( mailboxListener );
    }

    @Test
    public void givenJustEmptiedMB_pushTwoJobs_expectCorrectMBListenerCalls() {
        AsyncJob job1 = mock( AsyncJob.class );
        AsyncJob job2 = mock( AsyncJob.class );
        AsyncJob job3 = mock( AsyncJob.class );

        mailbox.push( job1 );
        mailbox.bulkPop().iterator();

        mailbox.push( job2 );
        mailbox.push( job3 );

        if ( guaranteesOrder ) {
            InOrder inorder = inOrder( mailboxListener );
            inorder.verify(mailboxListener).newPost();
            inorder.verify(mailboxListener).postCollected();
            inorder.verify(mailboxListener,times(2)).newPost();
            inorder.verifyNoMoreInteractions();
        } else {
            verify( mailboxListener, times(3) ).newPost();
            verify( mailboxListener ).postCollected();
            verifyNoMoreInteractions( mailboxListener );
        }
    }

    @Test
    public void concurrencyTest_fiveThreads_eachPushAndPopEvents_ensureThatEachThreadSeesConsistentOrderForTheirOwnPushAndPops() throws InterruptedException {
        int numProducerThreads = 5;
        int numJobsPerThread   = 2000;

        CountDownLatch latch = new CountDownLatch( numProducerThreads+1 );

        for ( int i=0; i<numProducerThreads; i++ ) {
            new ProducerThread(mailbox, latch, numJobsPerThread, i).start();
        }

        ConsumerThread consumerThread = new ConsumerThread(mailbox, latch, numJobsPerThread, numProducerThreads);
        consumerThread.start();

        boolean didLatchComplete = latch.await( 2, TimeUnit.SECONDS ); // latch will timeout if the push/pops do not match

        assertTrue( "latched timed out", didLatchComplete );

        if ( guaranteesOrder ) {
            assertEquals( 0, consumerThread.mismatchCount );
        }
    }

    private static class ProducerThread extends Thread {
        private Mailbox        mailbox;
        private CountDownLatch latch;
        private int            numJobsPerThread;
        private int threadNumber;

        public ProducerThread( Mailbox mailbox, CountDownLatch latch, int numJobsPerThread, int threadNumber ) {
            this.mailbox          = mailbox;
            this.latch            = latch;
            this.numJobsPerThread = numJobsPerThread;
            this.threadNumber     = threadNumber;
        }

        @Override
        public void run() {
            for ( int i=0; i<numJobsPerThread; i++ ) {
                mailbox.push( new CTJob(threadNumber, i) );

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

        public String call() throws Exception {
            return jobName;
        }
    }

    private class ConsumerThread extends Thread {
        private final Mailbox        mailbox;
        private final CountDownLatch latch;

        private final int[] perThreadJobMarker;
        private final int   totalNumMessagesExpected;


        public int mismatchCount = 0;

        public ConsumerThread( Mailbox mailbox, CountDownLatch latch, int numJobsPerThread, int numProducerThreads ) {
            this.mailbox = mailbox;
            this.latch   = latch;

            this.perThreadJobMarker       = new int[numProducerThreads];
            this.totalNumMessagesExpected = numJobsPerThread * numProducerThreads;
        }

        @Override
        public void run() {
            int messageCount  = 0;

            while ( messageCount < totalNumMessagesExpected ) {
                for ( AsyncJob j : mailbox.bulkPop() ) {
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
