package com.example.tarea16.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SyncWorker extends Worker {
    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<SyncManager.SyncResult> result = new AtomicReference<>();
        new SyncManager(getApplicationContext()).sincronizar(value -> {
            result.set(value);
            latch.countDown();
        });
        try {
            if (!latch.await(8, TimeUnit.MINUTES)) return Result.retry();
            SyncManager.SyncResult value = result.get();
            return value != null && value.exitoso ? Result.success() : Result.retry();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            return Result.retry();
        }
    }
}
