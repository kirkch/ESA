package com.mosaic.jact.mailboxes;

import com.mosaic.jact.AsyncJob;
import com.mosaic.lang.EnhancedIterable;

/**
 *
 */
public interface Mailbox {
    public void push( AsyncJob job );

    public EnhancedIterable<AsyncJob> bulkPop();
}
