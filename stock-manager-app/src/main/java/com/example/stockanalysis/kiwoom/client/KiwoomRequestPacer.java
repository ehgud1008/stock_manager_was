package com.example.stockanalysis.kiwoom.client;

import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** One process-wide request-start budget shared by screener/manual TR and analysis clients, excluding OAuth. */
@Component
public class KiwoomRequestPacer {
    private final long intervalNanos;
    private long lastStart;
    public KiwoomRequestPacer(@Value("${kiwoom.shared-call-interval-millis:350}") long intervalMillis) {
        if(intervalMillis<0 || intervalMillis>60000) throw new IllegalArgumentException("Invalid shared Kiwoom interval");
        intervalNanos=TimeUnit.MILLISECONDS.toNanos(intervalMillis);
    }
    public synchronized void acquire() {
        if(Thread.currentThread().isInterrupted()) throw new CancellationException("Interrupted before external request");
        long remaining;
        while(lastStart!=0 && (remaining=intervalNanos-(System.nanoTime()-lastStart))>0) {
            try { TimeUnit.NANOSECONDS.sleep(remaining); }
            catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new CancellationException("Interrupted before external request"); }
        }
        lastStart=System.nanoTime();
    }
}
