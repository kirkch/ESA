package com.mosaic.jact.mailboxes;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.EnhancedIterable;
import com.mosaic.lang.Factory;
import com.mosaic.lang.reflect.JavaClass;
import com.mosaic.lang.reflect.JavaConstructor;
import com.mosaic.utils.ArrayUtils;
import com.mosaic.utils.MathUtils;

/**
 * Combines multiple mailboxes into one. This spreads the load generated from many producers over multiple locks, thus
 * reducing contention. However it does so by sacrificing ordering.
 */
public class StripedMailbox extends Mailbox {

    private final Mailbox[]       stripes;
    private final int             bitmask;
    private final MailboxListener listener;

    public StripedMailbox( final Class subMailbox, final int numBuckets, final MailboxListener l ) {
        this(
            (Mailbox[]) ArrayUtils.fill(
                subMailbox,
                MathUtils.roundUpToClosestMultipleOf2(numBuckets),
                new Factory() {
                    private JavaConstructor<Mailbox> constructor = JavaClass.toJavaClass(subMailbox).getConstructorFor();

                    public Mailbox create() {
                        return constructor.newInstance();
                    }
                }
            ),
            l
        );
    }

    private StripedMailbox( Mailbox[] stripes, MailboxListener l ) {
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

    public void push( AsyncJob job ) {
        stripes[job.hashCode() & bitmask].push( job );

        listener.newPost();
    }

    protected EnhancedIterable<AsyncJob> doPop() {
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
