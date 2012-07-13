package com.mosaic.jact.jobqueues;

import com.mosaic.jact.AsyncJob;
import com.mosaic.utils.MathUtils;

/**
 * Combines multiple job queues into one. This spreads the load generated from many producers over multiple locks, thus
 * reducing contention. However it does so by sacrificing ordering.<p/>
 *
 * Currently optimised for stripes of length 2^n where n is 0 or greater. e.g. 1,2,4,8,16,32 etc.
 */
public class StripedJobQueueFactory {

    public static JobQueue stripeJobQueues( JobQueue[] stripes ) {
        if ( MathUtils.isPowerOf2(stripes.length) ) {
            return new FastStripedJobQueue( stripes );
        } else {
            return new SlowStripedJobQueue( stripes );
        }
    }


    private static class FastStripedJobQueue extends BaseStripedJobQueue {
        private final int bitmask;

        FastStripedJobQueue( JobQueue[] stripes ) {
            super( stripes );

            this.bitmask  = stripes.length - 1;
        }

        protected int roundIndex( int index ) {
            return index & bitmask;
        }
    }

    private static class SlowStripedJobQueue extends BaseStripedJobQueue {
        SlowStripedJobQueue( JobQueue[] stripes ) {
            super( stripes );
        }

        protected int roundIndex( int index ) {
            return index % stripes.length;
        }
    }


    private abstract static class BaseStripedJobQueue implements JobQueue {
        protected final JobQueue[]       stripes;

        protected abstract int roundIndex( int index );

        BaseStripedJobQueue( JobQueue[] stripes ) {
            this.stripes = stripes;
        }

        public boolean maintainsOrder() {
            return false;
        }

        public boolean isThreadSafe() {
            return stripes[0].isThreadSafe();
        }

        public boolean isEmpty() {
            for ( JobQueue m : stripes ) {
                if ( !m.isEmpty() ) {
                    return false;
                }
            }

            return true;
        }

        public boolean hasContents() {
            return !isEmpty();
        }

        public void push( AsyncJob job ) {
            selectMailbox(job.hashCode()).push( job );
        }

        public AsyncJob pop() {
            int startingIndex = roundIndex( 0 );

            int numStripes = stripes.length;
            for ( int i=0; i<numStripes; i++ ) {
                AsyncJob job = selectMailbox( i+startingIndex ).pop();

                if ( job != null ) {
                    return job;
                }
            }

            return null;
        }

        public JobQueue bulkPop() {
            int numStripes = stripes.length;

            JobQueue child = stripes[0].bulkPop();
            if ( !child.isEmpty() ) {
                return child;
            }

            for ( int i=1; i<numStripes; i++ ) {
                child = stripes[i].bulkPop();

                if ( !child.isEmpty() ) {
                    return child;
                }
            }

            return child;
        }

        private JobQueue selectMailbox( int index ) {
            return stripes[ roundIndex(index) ];
        }
    }

}