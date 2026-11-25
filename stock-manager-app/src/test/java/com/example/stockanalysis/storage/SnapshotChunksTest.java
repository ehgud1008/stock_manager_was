package com.example.stockanalysis.storage;

import static org.assertj.core.api.Assertions.*;
import com.example.stockanalysis.unified.eod.EodSnapshotCodec;
import java.util.*;
import org.junit.jupiter.api.Test;

class SnapshotChunksTest {
    final SnapshotChunks chunks=new SnapshotChunks(StorageFixtures.JSON);
    String restore(SnapshotChunks.Document d){var values=new HashMap<String,String>();d.objects().forEach((id,v)->values.put(id,EodSnapshotCodec.decode(v.gzip(),id,v.utf8Bytes())));return chunks.restore(d.manifest(),values);}
    @Test void exactWhitespaceUnicodeEscapesAndNumericLexemesSurviveChunking(){
        String json=StorageFixtures.history(0,"2026-10-20T11:00:00Z");var d=chunks.split(json);
        assertThat(d.objects().size()).isGreaterThan(3);assertThat(restore(d)).isEqualTo(json);
        var values=new HashMap<String,String>();d.objects().forEach((id,v)->values.put(id,EodSnapshotCodec.decode(v.gzip(),id,v.utf8Bytes())));
        values.put(values.keySet().iterator().next(),"corrupt");assertThatThrownBy(()->chunks.restore(d.manifest(),values)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void rollingWindowsAndDifferentReceiptTimesShareHistoricalChunks(){
        var first=chunks.split(StorageFixtures.history(0,"2026-10-20T11:00:00Z"));
        var next=chunks.split(StorageFixtures.history(1,"2026-10-21T11:00:00Z"));
        var common=new HashSet<>(first.objects().keySet());common.retainAll(next.objects().keySet());
        assertThat(common.size()).isGreaterThanOrEqualTo(first.objects().size()/2);
        assertThat(restore(next)).isEqualTo(StorageFixtures.history(1,"2026-10-21T11:00:00Z"));
        assertThat(first.sha256()).isNotEqualTo(next.sha256());
    }
    @Test void literalMarkerCollisionDisablesReceiptTemplatingWithoutChangingInput(){
        String json=StorageFixtures.history(0,"~ua.snapshot.observedAt.v1~");assertThat(restore(chunks.split(json))).isEqualTo(json);
        assertThat(restore(chunks.split("{ \"empty\": [], \"values\": [1,2,3], \"x\":-0.00 }"))).isEqualTo("{ \"empty\": [], \"values\": [1,2,3], \"x\":-0.00 }");
    }
    @Test void receiptBoundariesAndMapFieldOrderDoNotCreateNewHistoricalObjects(){
        String a=StorageFixtures.history(0,"2026-10-20T11:00:00Z");
        String b=a.replaceFirst("2026-10-20T11:00:00Z","2026-10-20T11:01:00Z")
            .replaceAll("\\\"close\\\":\\\"([0-9]+)\\\",\\\"volume\\\":\\\"123456\\\"","\"volume\":\"123456\",\"close\":\"$1\"")
            .replaceAll("\\{\\\"stockCode\\\":\\\"000080\\\",\\\"businessDate\\\":\\\"([^\\\"]+)\\\"","{\"businessDate\":\"$1\",\"stockCode\":\"000080\"");
        var first=chunks.split(a);var second=chunks.split(b);
        assertThat(second.objects().keySet()).containsExactlyInAnyOrderElementsOf(first.objects().keySet());
        assertThat(restore(second)).isEqualTo(b);
        String spaced=b.replace("\"stockCode\":","\"stockCode\" : ");assertThat(restore(chunks.split(spaced))).isEqualTo(spaced);
    }
}
