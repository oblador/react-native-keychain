package com.oblador.keychain.decryptionHandler;

import com.oblador.keychain.cipherStorage.CipherStorage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

interface AuthCb {
    void run(final CompletableFuture<CipherStorage.DecryptionResult> cb);
}

public class AuthSerialQueue {
    private final ConcurrentLinkedQueue<Runnable> queue = new ConcurrentLinkedQueue<>();
    /**
     * Lock is needed to not call a job is there is already one in progress
     */
    private final AtomicBoolean lock = new AtomicBoolean(false);

    /**
     * So we want to run auth related things in serial order
     * but at the same time we don't want to lock a thread.
     * For that reason we utilize a queue that stores `Runnable`s
     * for every `Runnable` we create a `CompletableFuture`,
     * that must be `completed` by that callback.
     * Only after that the next `Runnable` will be started.
     */
    public CompletableFuture<CipherStorage.DecryptionResult> add(AuthCb cb) {
        final CompletableFuture<CipherStorage.DecryptionResult> intention = new CompletableFuture<>();
        CompletableFuture<CipherStorage.DecryptionResult> queueLoop = intention.handle(((decryptionResult, throwable) -> {
            lock.set(false);
            this.flush();
            if (throwable != null) {
                if(throwable instanceof CompletionException)
                    throwable = throwable.getCause();
                throw new CompletionException(throwable);
            }
            return decryptionResult;
        }));

        queue.add(() -> {
            try {
                cb.run(intention);
            } catch (Throwable err) {
                intention.completeExceptionally(err);
            }
        });

        flush();

        return queueLoop;
    }

    private void flush() {
        if (lock.get()) {
            return;
        }

        final Runnable job = queue.poll();

        // All work is done for now
        if (job == null) {
            return;
        }

        lock.set(true);
        job.run();
    }
}