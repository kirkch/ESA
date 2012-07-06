package com.mosaic.jact.mailboxes;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.EnhancedIterable;
import com.mosaic.lang.Validate;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

/**
 *
 */
public class CASMailbox implements Mailbox {
    private final AtomicReference<Element> jobQueueRef = new AtomicReference<Element>( null );

    private MailboxListener mailboxListener;

    public CASMailbox() {
        this( new NullMailboxListener() );
    }

    public CASMailbox( MailboxListener l ) {
        Validate.notNull( l, "listener" );

        this.mailboxListener = l;
    }

    public boolean maintainsOrder() {
        return true;
    }

    public boolean isThreadSafe() {
        return true;
    }

    public boolean isEmpty() {
        Element head = jobQueueRef.get();

        return head == null;
    }

    public void push( AsyncJob job ) {
        Element e = new Element(job);

        while ( true ) {
            Element currentHead = jobQueueRef.get();

            e.next = currentHead;

            boolean wasSuccessful = jobQueueRef.compareAndSet( currentHead, e );
            if ( wasSuccessful ) {
                mailboxListener.newPost();

                return;
            }
        }
    }

    public AsyncJob pop() {
        Element head;
        boolean wasSuccessful;

        while (true) {
            head = jobQueueRef.get();
            if ( head == null ) {
                return null;
            }

            wasSuccessful = jobQueueRef.compareAndSet( head, head.next );
            if ( wasSuccessful ) {
                if ( head.next != null ) {
                    head.next.prev = null;
                }

                return head.job;
            }
        }
    }

    public  EnhancedIterable<AsyncJob> bulkPop() {
        while ( true ) {
            Element head          = jobQueueRef.get();
            boolean wasSuccessful = jobQueueRef.compareAndSet( head, null );

            Element tail = setPreviousPointersAndReturnTail( head );


            if ( wasSuccessful ) {
                if ( tail != null ) {
                    mailboxListener.postCollected();
                }

                return new MBEnhancedIterable(tail);
            }
        }
    }

    private Element setPreviousPointersAndReturnTail( Element e ) {
        while ( e != null && e.next != null ) {
            e.next.prev = e;

            e = e.next;
        }

        return e;
    }


    private static class Element {
        public final AsyncJob job;

        public Element next;
        public Element prev;

        public Element( AsyncJob job ) {
            Validate.notNull( job, "job" );

            this.job = job;
        }
    }

    private class MBEnhancedIterable extends EnhancedIterable<AsyncJob> {
        private Element tail;

        public MBEnhancedIterable( Element tail ) {
            this.tail = tail;
        }

        public boolean isEmpty() {
            return tail == null;
        }

        public Iterator<AsyncJob> iterator() {
            return new Iterator<AsyncJob>() {
                private Element next = tail;

                public boolean hasNext() {
                    return next != null;
                }

                public AsyncJob next() {
                    AsyncJob job = next.job;

                    this.next = this.next.prev;

                    return job;
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
