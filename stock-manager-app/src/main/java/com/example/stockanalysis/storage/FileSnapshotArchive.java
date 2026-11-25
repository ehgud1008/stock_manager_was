package com.example.stockanalysis.storage;

import com.example.stockanalysis.unified.eod.EodSnapshotCodec;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.UUID;

/** A persistent disk or shared mount outside Oracle. Files are immutable and verified on every read. */
public final class FileSnapshotArchive {
    private final Path root;
    private volatile String expectedIdentity;
    public FileSnapshotArchive(String directory){root=directory==null||directory.isBlank()?null:Path.of(directory).toAbsolutePath().normalize();if(root!=null&&!Path.of(directory).isAbsolute())throw new IllegalArgumentException("Archive directory must be absolute");}
    public boolean configured(){return root!=null;}
    public String identity(){verifyIdentity();return expectedIdentity;}
    public String read(String id,long bytes){
        verifyIdentity();
        try{Path path=path(id);long size=Files.size(path);if(size>32*1024*1024)throw new IllegalStateException("ARCHIVE_TOO_LARGE");return EodSnapshotCodec.decode(Files.readAllBytes(path),id,bytes);}
        catch(IOException e){throw new IllegalStateException("ARCHIVE_READ_FAILED",e);}
    }
    public void put(EodSnapshotCodec.Encoded object){
        checkWritable();
        verifyIdentity();
        Path path=path(object.sha256());
        try{
            Files.createDirectories(path.getParent());
            if(!Files.exists(path)){
                var temporary=path.resolveSibling(path.getFileName()+"."+UUID.randomUUID()+".tmp");
                try{
                    try(var channel=FileChannel.open(temporary,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){var buffer=ByteBuffer.wrap(object.gzip());while(buffer.hasRemaining())channel.write(buffer);channel.force(true);}
                    Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE);
                }finally{Files.deleteIfExists(temporary);}
            }
            String restored=read(object.sha256(),object.utf8Bytes());
            if(!EodSnapshotCodec.hash(restored.getBytes(java.nio.charset.StandardCharsets.UTF_8)).equals(object.sha256()))throw new IllegalStateException("ARCHIVE_VERIFY_FAILED");
        }catch(IOException e){throw new IllegalStateException("ARCHIVE_WRITE_FAILED",e);}
    }
    public void checkWritable(){
        if(root==null)throw new IllegalStateException("ARCHIVE_NOT_CONFIGURED");
        try{if(!Files.isDirectory(root)||!Files.isWritable(root))throw new IOException("Persistent archive directory must exist and be writable");if(Files.getFileStore(root).getUsableSpace()<64L*1024*1024)throw new IOException("Archive disk space is low");}
        catch(IOException e){throw new IllegalStateException("ARCHIVE_DIRECTORY_UNAVAILABLE",e);}
    }
    public String initializeIdentity(){
        checkWritable();Path marker=root.resolve(".stock-snapshot-archive");
        if(Files.isSymbolicLink(marker))throw new IllegalStateException("ARCHIVE_MARKER_INVALID");
        try{
            if(!Files.exists(marker)){
                try(var channel=FileChannel.open(marker,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE)){var buffer=ByteBuffer.wrap(UUID.randomUUID().toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII));while(buffer.hasRemaining())channel.write(buffer);channel.force(true);}
                catch(FileAlreadyExistsException ignored){}
            }
            String id=Files.readString(marker).trim();if(!id.matches("[a-f0-9-]{36}"))throw new IllegalStateException("ARCHIVE_MARKER_INVALID");return id;
        }catch(IOException e){throw new IllegalStateException("ARCHIVE_MARKER_UNAVAILABLE",e);}
    }
    public void expectIdentity(String id){expectedIdentity=id;verifyIdentity();}
    private void verifyIdentity(){
        if(root==null||expectedIdentity==null)throw new IllegalStateException("ARCHIVE_NOT_REGISTERED");
        Path marker=root.resolve(".stock-snapshot-archive");
        try{if(Files.isSymbolicLink(marker)||!Files.readString(marker).trim().equals(expectedIdentity))throw new IllegalStateException("ARCHIVE_MOUNT_CHANGED");}
        catch(IOException e){throw new IllegalStateException("ARCHIVE_MOUNT_UNAVAILABLE",e);}
    }
    private Path path(String id){
        if(root==null)throw new IllegalStateException("ARCHIVE_NOT_CONFIGURED");if(id==null||!id.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("ARCHIVE_KEY_INVALID");
        Path path=root.resolve(id.substring(0,2)).resolve(id+".json.gz");
        // Refuse links below the configured root so corrupted archive keys cannot escape the mount.
        if(Files.isSymbolicLink(root)||Files.isSymbolicLink(path.getParent())||Files.isSymbolicLink(path))throw new IllegalStateException("ARCHIVE_SYMLINK_REFUSED");
        return path;
    }
}
