package com.example.alfcapp.data.chat

import java.util.concurrent.atomic.AtomicReference

/**
 * Process-wide pointer to the chat thread the user is currently viewing.
 * ChatScreen publishes its threadId on enter and clears it on leave; the
 * notifier reads this to suppress the ping/banner for the open conversation.
 */
object ActiveThreadTracker {
    private val current = AtomicReference<Long?>(null)

    fun setActive(threadId: Long) { current.set(threadId) }
    fun clearActive(threadId: Long) {
        // Only clear if we still own it — guards against a fast back-and-forth
        // between threads where the new screen has already set its own id.
        current.compareAndSet(threadId, null)
    }
    fun isActive(threadId: Long): Boolean = current.get() == threadId
}
