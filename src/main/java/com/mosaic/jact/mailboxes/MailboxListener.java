package com.mosaic.jact.mailboxes;

/**
 *
 */
public interface MailboxListener {

    /**
     * An item has been pushed onto the mailbox.
     */
    public void newPost();

    /**
     * The mailbox has been cleared.
     */
    public void postCollected();
}
