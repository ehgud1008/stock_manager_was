package com.example.stockanalysis.unified.portfolio;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.*;

@Configuration(proxyBeanMethods=false)
@EnableScheduling
@EnableConfigurationProperties(PortfolioProperties.class)
public class PortfolioConfiguration {
    @Bean(destroyMethod="close") public Worker portfolioWorker(PortfolioService service){return new Worker(service);}
    public static final class Worker implements AutoCloseable {
        private final PortfolioService service;private final AtomicBoolean active=new AtomicBoolean();private volatile boolean ready;
        private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"portfolio-evaluation");t.setDaemon(true);return t;});
        Worker(PortfolioService service){this.service=service;}
        @EventListener(ApplicationReadyEvent.class) public void start(){ready=true;}
        @Scheduled(fixedDelay=60000,initialDelay=20000) public void poll(){if(!ready||!active.compareAndSet(false,true))return;
            try{executor.execute(()->{try{service.tick();}catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Portfolio worker deferred: type={}",e.getClass().getSimpleName());}finally{active.set(false);}});}catch(RejectedExecutionException e){active.set(false);}}
        public void close(){ready=false;executor.shutdownNow();}
    }
}
