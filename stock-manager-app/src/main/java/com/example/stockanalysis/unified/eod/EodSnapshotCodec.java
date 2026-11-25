package com.example.stockanalysis.unified.eod;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.sql.*;
import java.util.HexFormat;
import java.util.zip.*;

/** Lossless UTF-8 GZIP snapshots, with a legacy CLOB reader and bounded, verified decoding. */
public final class EodSnapshotCodec {
    private static final int MAX_BYTES=32*1024*1024;
    private EodSnapshotCodec() {}
    public record Encoded(byte[] gzip,String sha256,int utf8Bytes) {}
    public static Encoded encode(String json) {
        if(json==null)throw new IllegalArgumentException("SNAPSHOT_MISSING");
        byte[] bytes=json.getBytes(StandardCharsets.UTF_8);
        if(bytes.length>MAX_BYTES)throw new IllegalArgumentException("SNAPSHOT_TOO_LARGE");
        try {
            var out=new ByteArrayOutputStream();
            try(var gzip=new GZIPOutputStream(out)){gzip.write(bytes);}
            return new Encoded(out.toByteArray(),hash(bytes),bytes.length);
        }catch(IOException e){throw new IllegalArgumentException("SNAPSHOT_ENCODING_FAILED",e);}
    }
    public static String decode(byte[] gzip,String sha256,long utf8Bytes) {
        if(gzip==null||gzip.length>MAX_BYTES||utf8Bytes<0||utf8Bytes>MAX_BYTES||sha256==null)
            throw new IllegalArgumentException("SNAPSHOT_METADATA_INVALID");
        try(var in=new GZIPInputStream(new ByteArrayInputStream(gzip))){
            byte[] bytes=in.readNBytes(MAX_BYTES+1);
            if(bytes.length!=utf8Bytes||!hash(bytes).equals(sha256))throw new IllegalArgumentException("SNAPSHOT_INTEGRITY_FAILED");
            return new String(bytes,StandardCharsets.UTF_8);
        }catch(IOException e){throw new IllegalArgumentException("SNAPSHOT_DECODING_FAILED",e);}
    }
    public static String read(ResultSet row)throws SQLException {
        byte[] gzip=row.getBytes("input_gzip");
        if(gzip==null)return row.getString("input_json");
        long size=row.getLong("input_utf8_bytes");
        if(row.wasNull())throw new IllegalArgumentException("SNAPSHOT_METADATA_INVALID");
        return decode(gzip,row.getString("input_sha256"),size);
    }
    public static String hash(byte[] bytes){
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
        catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
}
