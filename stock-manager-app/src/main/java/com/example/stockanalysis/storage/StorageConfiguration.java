package com.example.stockanalysis.storage;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration(proxyBeanMethods=false)
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfiguration {
    @Bean(destroyMethod="close") public Worker storageWorker(StorageService service){return new Worker(service);}
    public static final class Worker implements AutoCloseable{
        private final StorageService service;private final AtomicBoolean queued=new AtomicBoolean();private volatile boolean ready;
        private final ExecutorService pool=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"snapshot-storage");t.setDaemon(true);return t;});
        Worker(StorageService service){this.service=service;}
        @EventListener(ApplicationReadyEvent.class) public void start(){ready=true;enqueue("STARTUP",()->{service.heartbeat();service.monitor();});}
        @Scheduled(cron="${storage.snapshots.monitor-cron:0 0 * * * *}",zone="Asia/Seoul") public void sample(){enqueue("MONITOR",service::monitor);}
        @Scheduled(cron="${storage.snapshots.archive-cron:0 */5 3-6 * * *}",zone="Asia/Seoul") public void archive(){enqueue("ARCHIVE",service::maintain);}
        @Scheduled(fixedDelay=60000,initialDelay=15000) public void send(){enqueue("HEARTBEAT_DELIVERY",()->{service.heartbeat();service.deliver();});}
        private void enqueue(String phase,Runnable action){if(!ready||!queued.compareAndSet(false,true))return;try{pool.execute(()->{try{action.run();}catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Storage maintenance deferred: phase={}, {}",phase,failureDetails(e));}finally{queued.set(false);}});}catch(RejectedExecutionException e){queued.set(false);}}
        public void close(){ready=false;pool.shutdownNow();}
    }
    // Exception messages and SQL can contain credentials or notification content; log codes and locations only.
    static String failureDetails(Throwable error){
        String sqlCode="NONE",sqlState="NONE",origin="UNKNOWN";
        var visited=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable,Boolean>());
        for(Throwable cause=error;cause!=null&&visited.add(cause);cause=cause.getCause()){
            if(cause instanceof java.sql.SQLException sql){sqlCode=Integer.toString(sql.getErrorCode());String state=sql.getSQLState();sqlState=state!=null&&state.matches("[A-Z0-9]{5}")?state:"UNKNOWN";}
            if(origin.equals("UNKNOWN"))for(var frame:cause.getStackTrace())if(frame.getClassName().startsWith("com.example.stockanalysis.storage.")&&!frame.getClassName().equals(StorageConfiguration.class.getName())){origin=frame.getClassName()+"."+frame.getMethodName()+":"+frame.getLineNumber();break;}
        }
        return "type="+error.getClass().getSimpleName()+", sqlCode="+sqlCode+", sqlState="+sqlState+", origin="+origin;
    }
}
