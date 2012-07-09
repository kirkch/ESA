package com.mosaic.jact.jobqueues;

import com.mosaic.jact.AsyncJob;
import com.mosaic.utils.MathUtils;

/**
 * An optimised hybrid job queue. When consumers outpace or keep up with producers this job queue stores messages within
 * a pre-allocated ring buffer (or circular array) which out performs linked lists in both throughput and low latency
 * by keeping its impact on the GC low. However during periods when producers out pace consumers then the an extra
 * ring buffer will be allocated and attached to the original via a linked list. Thus giving a hybrid performance of
 * a 'batched' linked list.<p/>
 *
 * Batch popping from this data structure involves creating a new array and copying messages from the ring buffer into
 * the new array. The number of messages returned in one batch pop will never be more than the length of a single ring
 * buffer. When decorated with a synchronizing job queue decorator the performance of popping in batch is greatly improved.
 * Otherwise the overheads are not usually worth it. Copying an array is faster than selecting a subsection of a linked
 * list, but slower than returning the entire linked list. Both approaches to linked lists have different trade offs.<p/>
 *
 * All in all LinkedRingJobQueue offers an excellent balance of trade offs and is extremely fast. Preferable to LinkedRingJobQueue
 * when willing to reserve memory to speed up the job queue.
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
