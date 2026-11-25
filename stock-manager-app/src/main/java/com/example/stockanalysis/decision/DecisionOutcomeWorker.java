package com.example.stockanalysis.decision;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;

@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix="trade-decision",name="enabled",havingValue="true")
public class DecisionOutcomeWorker {
    private final DecisionOutcomeService service;
    public DecisionOutcomeWorker(DecisionOutcomeService service){this.service=service;}
    @Scheduled(fixedDelayString="${trade-decision.outcome-poll-ms:300000}",initialDelay=60000)
    public void poll(){try{service.tick();}catch(RuntimeException e){org.slf4j.LoggerFactory.getLogger(getClass()).warn("Decision outcome storage unavailable: type={}",e.getClass().getSimpleName());}}
}
