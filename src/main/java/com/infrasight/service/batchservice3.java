package com.wellsfargo.infrasight.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NightlyGamifyJob {

    // 1️⃣  Logger bound to this class
    private static final Logger log = LoggerFactory.getLogger(NightlyGamifyJob.class);

    private final RequestModelRepository requestRepo;
    private final GamificationService    gamifySvc;

    /** Runs every day, delegates to the real worker. */
    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    public void executeDailyBatch() {
        log.info("Nightly gamification batch starting…");
        long start = System.currentTimeMillis();
        runDailyAggregation();
        log.info("Nightly batch finished in {} ms",
                 System.currentTimeMillis() - start);
    }

    /** The work you’ll test directly. */
    @Transactional
    public void runDailyAggregation() {
        Instant midnight = Instant.now().truncatedTo(ChronoUnit.DAYS);
        List<RequestModel> reqs = requestRepo
            .findByFinishedTimestampBetween(midnight.minus(1, ChronoUnit.DAYS), midnight);

        log.debug("Found {} requests to process ({} → {})",
                  reqs.size(),
                  midnight.minus(1, ChronoUnit.DAYS), midnight);

        int ok = 0;
        int failed = 0;

        for (RequestModel rm : reqs) {
            try {
                gamifySvc.awardPointsForRequest(rm.getRequestId(), rm.getUserId());
                ok++;
            } catch (Exception ex) {
                failed++;
                log.error("Failed to award points for request {} / user {}",
                          rm.getRequestId(), rm.getUserId(), ex);
            }
        }

        requestRepo.deleteAll(reqs);   // or archive

        log.info("Batch summary: processed={}, succeeded={}, failed={}",
                 reqs.size(), ok, failed);
    }
}
