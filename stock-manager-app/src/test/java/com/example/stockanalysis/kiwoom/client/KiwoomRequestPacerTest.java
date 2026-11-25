package com.example.stockanalysis.kiwoom.client;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CancellationException;
import static org.assertj.core.api.Assertions.*;
class KiwoomRequestPacerTest {
    @Test void rejectsInvalidIntervals() {
        assertThatThrownBy(()->new KiwoomRequestPacer(-1)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void interruptedWorkerMustNotSendAnotherRequest() {
        var pacer=new KiwoomRequestPacer(0);
        try {
            Thread.currentThread().interrupt();
            assertThatThrownBy(pacer::acquire).isInstanceOf(CancellationException.class);
        } finally { Thread.interrupted(); }
    }
    @Test void spacesRequestStarts() {
        var pacer=new KiwoomRequestPacer(20); pacer.acquire(); long start=System.nanoTime(); pacer.acquire();
        assertThat(System.nanoTime()-start).isGreaterThanOrEqualTo(18_000_000L);
    }
}
