package com.mosaic.jact.mailboxes;

/**
 * An optimised hybrid mailbox. When consumers outpace or keep up with producers this mailbox stores messages within
 * a pre-allocated ring buffer (or circular array) which out performs linked lists in both throughput and low latency
 * by keeping its impact on the GC low. However during periods when producers out pace consumers then the an extra
 * ring buffer will be allocated and attached to the original via a linked list. Thus giving a hybrid performance of
 * a 'batched' linked list.<p/>
 *
 * Batch popping from this data structure involves creating a new array and copying messages from the ring buffer into
 * the new array. The number of messages returned in one batch pop will never be more than the length of a single ring
 * buffer. When decorated with a synchronizing mailbox decorator the performance of popping in batch is greatly improved.
 * Otherwise the overheads are not usually worth it. Copying an array is faster than selecting a subsection of a linked
 * list, but slower than returning the entire linked list. Both approaches to linked lists have different trade offs.<p/>
 *
 * All in all LinkedRingMailbox offers an excellent balance of trade offs and is extremely fast. Preferable to LinkedListMailbox
 * when willing to reserve memory to speed up the mailbox.
 */
public class LinkedRingMailbox {
}
