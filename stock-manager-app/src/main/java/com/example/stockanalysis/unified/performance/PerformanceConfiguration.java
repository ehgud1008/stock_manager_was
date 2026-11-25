package com.example.stockanalysis.unified.performance;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.*;

@Configuration(proxyBeanMethods=false)
@EnableScheduling
@EnableConfigurationProperties(PerformanceProperties.class)
public class PerformanceConfiguration {
    @Bean(destroyMethod="close") public PerformanceWorker performanceWorker(PerformanceService service){return new PerformanceWorker(service);}
    public static final class PerformanceWorker implements AutoCloseable {
        private final PerformanceService service;
        private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"report-performance");t.setDaemon(true);return t;});
        private final AtomicBoolean active=new AtomicBoolean();
        private volatile boolean ready;
        PerformanceWorker(PerformanceService service){this.service=service;}
        @EventListener(ApplicationReadyEvent.class) public void start(){ready=true;}
        @Scheduled(fixedDelay=60000,initialDelay=15000) public void poll(){
            if(!ready || !active.compareAndSet(false,true))return;
            try{executor.execute(()->{try{service.tick();}catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Performance worker deferred: type={}",e.getClass().getSimpleName());}finally{active.set(false);}});}
            catch(RejectedExecutionException e){active.set(false);}
        }
        public void close(){ready=false;executor.shutdownNow();}
    }
}
