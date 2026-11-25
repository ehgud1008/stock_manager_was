import com.example.stockanalysis.storage.SnapshotChunks;
import com.example.stockanalysis.unified.eod.EodSnapshotCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.util.*;

/** Read-only measurement against existing compressed recovery files; no database connection. */
public class SnapshotStorageBenchmark {
  public static void main(String[] args)throws Exception{
    var mapper=new ObjectMapper();var chunks=new SnapshotChunks(mapper);var byCode=new TreeMap<String,List<Path>>();
    try(var files=Files.walk(Path.of("eod-storage-backup","20261021"))){files.filter(p->p.toString().endsWith(".json.gz")).forEach(p->byCode.computeIfAbsent(p.getFileName().toString(),k->new ArrayList<>()).add(p));}
    long legacy=0;int documents=0,stocks=0;var objects=new HashMap<String,byte[]>();
    for(var entry:byCode.entrySet()){
      if(entry.getValue().size()<3)continue;
      for(var path:entry.getValue()){
        String[] proof=Files.readString(path.resolveSibling(path.getFileName().toString().replace(".json.gz",".sha256"))).trim().split(" ");
        byte[] gzip=Files.readAllBytes(path);String original=EodSnapshotCodec.decode(gzip,proof[0],Long.parseLong(proof[1]));
        var doc=chunks.split(original);var restored=new HashMap<String,String>();
        doc.objects().forEach((id,e)->{objects.putIfAbsent(id,e.gzip());restored.put(id,EodSnapshotCodec.decode(e.gzip(),id,e.utf8Bytes()));});
        if(!original.equals(chunks.restore(doc.manifest(),restored)))throw new IllegalStateException("ROUNDTRIP_FAILED");
        var manifest=EodSnapshotCodec.encode(mapper.writeValueAsString(doc.manifest()));objects.putIfAbsent(manifest.sha256(),manifest.gzip());legacy+=gzip.length;documents++;
      }
      if(++stocks>=20)break;
    }
    long shared=objects.values().stream().mapToLong(b->b.length).sum();
    System.out.println("STOCKS="+stocks+" DOCUMENTS="+documents+" LEGACY_GZIP_BYTES="+legacy+" SHARED_GZIP_BYTES="+shared+" OBJECTS="+objects.size()+" SAVED_PERCENT="+String.format(Locale.ROOT,"%.2f",100.0*(legacy-shared)/legacy));
  }
}
