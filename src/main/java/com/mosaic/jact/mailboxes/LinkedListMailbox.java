package com.mosaic.jact.mailboxes;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.EnhancedIterable;
import com.mosaic.lang.Validate;

import java.util.Iterator;

/**
 *
 */
public class LinkedListMailbox extends Mailbox {

    private MailboxListener mailboxListener;

    private Element head = null;


    public LinkedListMailbox() {
        this( new NullMailboxListener() );
    }

    public LinkedListMailbox( MailboxListener l) {
        Validate.notNull( l, "listener" );

        this.mailboxListener = l;
    }

    public boolean maintainsOrder() {
        return true;
    }

    public boolean isThreadSafe() {
        return false;
    }

    public boolean isEmpty() {
        return head == null;
    }

    public void push( AsyncJob job ) {
        Element e = new Element(job);

        e.next = head;

        head = e;

        mailboxListener.newPost();
    }

    public AsyncJob pop() {
        if ( head == null ) {
            return null;
        }

        AsyncJob job = head.job;
        if ( head.next != null ) {
            head.next.prev = null;
        }

        head = head.next;

        return job;
    }

    public EnhancedIterable<AsyncJob> bulkPop() {
        Element tail = setPreviousPointersAndReturnTail( head );

        head = null;

        if ( tail != null ) {
            mailboxListener.postCollected();
        }

        return new MBEnhancedIterable(tail);
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
