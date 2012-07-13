package com.mosaic.jact.jobqueues;

import com.mosaic.jact.AsyncJob;
import com.mosaic.utils.MathUtils;

/**
 * A hybrid job queue. When consumers outpace or keep up with producers this job queue stores messages within
 * a pre-allocated ring buffer (or circular array). However during periods when producers out pace consumers then an extra
 * ring buffer will be allocated and attached to the original via a linked list of ring buffers. <p/>
 *
 * Batch popping from this data structure involves creating a new array to replace the one that was returned by the batch. The
 * number of messages returned in one batch pop will never be more than the length of a single ring
 * buffer. <p/>
 */
public class LinkedRingJobQueue implements JobQueue {

    private Ring insertEnd;
    private Ring popEnd;


    public LinkedRingJobQueue( int ringSize ) {
        ringSize  = MathUtils.roundUpToClosestPowerOf2(ringSize);
        insertEnd = new Ring( ringSize );
        popEnd    = insertEnd;
    }

    private LinkedRingJobQueue( Ring ring ) {
        this.insertEnd = ring;
        this.popEnd    = ring;
    }

    public boolean maintainsOrder() { return true; }
    public boolean isThreadSafe()   { return false; }

    public boolean isEmpty() { return popEnd.isEmpty() && insertEnd == popEnd; }

    public boolean hasContents() { return !isEmpty(); }

    public void push( AsyncJob job ) {
        insertEnd = insertEnd.push(job);
    }

    public AsyncJob pop() {
        if ( popEnd.isEmpty() ) {
            if ( popEnd.nextPoppingRing == null ) {
                return null;
            }

            popEnd = popEnd.nextPoppingRing;
        }

        return popEnd.pop();
    }

    public JobQueue bulkPop() {
        LinkedRingJobQueue spawn = new LinkedRingJobQueue( popEnd );

        popEnd = popEnd.nextPoppingRing;
        spawn.popEnd.nextPoppingRing = null;

        if ( popEnd == null ) {
            insertEnd = new Ring( spawn.popEnd.jobs.length );
            popEnd    = insertEnd;
        }

        return spawn;
    }

    private static class Ring {
        private AsyncJob[] jobs;

        private int insertIndex;
        private int popIndex;

        private int bitmask;

        Ring nextPoppingRing;

        public Ring( int ringSize ) {
            jobs    = new AsyncJob[ringSize];
            bitmask = ringSize-1;
        }

        public final boolean isEmpty() { return insertIndex == popIndex && jobs[0] == null; }
        public final boolean isFull() { return insertIndex == popIndex && jobs[0] != null; }

        public Ring push( AsyncJob job ) {
            jobs[insertIndex] = job;

            insertIndex = (insertIndex+1)&bitmask;

            if ( isFull() ) {
                Ring newRing = new Ring(jobs.length);

                this.nextPoppingRing = newRing;

                return newRing;
            }

            return this;
        }

        public AsyncJob pop() {
            if ( !isEmpty() ) {
                AsyncJob j = jobs[popIndex];
                jobs[popIndex] = null;

                popIndex = (popIndex + 1)&bitmask;

                return j;
            }

            return null;
        }
    }
}
