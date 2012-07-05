package com.mosaic.jact.mailboxes;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.EnhancedIterable;
import com.mosaic.utils.MathUtils;

/**
 * Combines multiple mailboxes into one. This spreads the load generated from many producers over multiple locks, thus
 * reducing contention. However it does so by sacrificing ordering.
 */
public class StripedMailbox {

    public static Mailbox createStripedMailbox( Mailbox[] stripes ) {
        return createStripedMailbox( stripes, new NullMailboxListener() );
    }

    public static Mailbox createStripedMailbox( Mailbox[] stripes, MailboxListener l ) {
        if ( MathUtils.isPowerOf2(stripes.length) ) {
            return new FastStripedMailbox( stripes, l );
        } else {
            return new SlowStripedMailbox( stripes, l );
        }
    }



    private static class FastStripedMailbox extends Mailbox {
        private final Mailbox[]       stripes;
        private final int             bitmask;
        private final MailboxListener listener;

        FastStripedMailbox( Mailbox[] stripes, MailboxListener l ) {
            this.stripes  = stripes;
            this.bitmask  = stripes.length - 1;
            this.listener = l;
        }

        public boolean maintainsOrder() {
            return false;
        }

        public boolean isThreadSafe() {
            return true;
        }

        public boolean isEmpty() {
            for ( Mailbox m : stripes ) {
                if ( !m.isEmpty() ) {
                    return false;
                }
            }

            return true;
        }

        public void push( AsyncJob job ) {
            selectMailbox(job.hashCode()).push( job );

            listener.newPost();
        }

        protected AsyncJob doPop() {
            int startingIndex = ((int) System.currentTimeMillis() & bitmask);

            int numStripes = stripes.length;
            for ( int i=0; i<numStripes; i++ ) {
                AsyncJob job = selectMailbox( i+startingIndex ).pop();

                if ( job != null ) {
                    return job;
                }
            }

            return null;
        }

        private Mailbox selectMailbox( int index ) {
            return stripes[ index & bitmask ];
        }

        protected EnhancedIterable<AsyncJob> doBulkPop() {
            int                          numStripes = stripes.length;
            EnhancedIterable<AsyncJob>[] iterables  = new EnhancedIterable[numStripes];
            for ( int i=0; i<numStripes; i++ ) {
                iterables[i] = stripes[i].bulkPop();
            }


            EnhancedIterable<AsyncJob> poppedMail = EnhancedIterable.combine( iterables );
            if ( !poppedMail.isEmpty() ) {
                listener.postCollected();
            }

            return poppedMail;
        }
    }

    private static class SlowStripedMailbox extends Mailbox {
        private final Mailbox[]       stripes;
        private final MailboxListener listener;

        SlowStripedMailbox( Mailbox[] stripes, MailboxListener l ) {
            this.stripes  = stripes;
            this.listener = l;
        }

        public boolean maintainsOrder() {
            return false;
        }

        public boolean isThreadSafe() {
            return true;
        }

        public boolean isEmpty() {
            for ( Mailbox m : stripes ) {
                if ( !m.isEmpty() ) {
                    return false;
                }
            }

            return true;
        }

        public void push( AsyncJob job ) {
            stripes[job.hashCode() % stripes.length].push( job );

            listener.newPost();
        }

        protected AsyncJob doPop() {
            int startingIndex = ((int) System.currentTimeMillis() % stripes.length);

            int numStripes = stripes.length;
            for ( int i=0; i<numStripes; i++ ) {
                AsyncJob job = selectMailbox( i+startingIndex ).pop();

                if ( job != null ) {
                    return job;
                }
            }

            return null;
        }

        private Mailbox selectMailbox( int index ) {
            return stripes[ index % stripes.length ];
        }

        protected EnhancedIterable<AsyncJob> doBulkPop() {
            int                          numStripes = stripes.length;
            EnhancedIterable<AsyncJob>[] iterables  = new EnhancedIterable[numStripes];
            for ( int i=0; i<numStripes; i++ ) {
                iterables[i] = stripes[i].bulkPop();
            }


            EnhancedIterable<AsyncJob> poppedMail = EnhancedIterable.combine( iterables );
            if ( !poppedMail.isEmpty() ) {
                listener.postCollected();
            }

            return poppedMail;
        }
    }
}
