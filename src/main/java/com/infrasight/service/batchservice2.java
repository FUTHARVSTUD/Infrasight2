@Service
@RequiredArgsConstructor
public class NightlyGamifyJob {

    private final RequestModelRepository requestRepo;
    private final GamificationService    gamifySvc;   // << inject it

    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    public void run() {

        // yesterday in UTC
        Instant midnight = Instant.now().truncatedTo(ChronoUnit.DAYS);
        Instant start    = midnight.minus(1, ChronoUnit.DAYS);

        // pull all finished requests in that 24-h window
        List<RequestModel> requests =
            requestRepo.findByFinishedTimestampBetween(start, midnight);

        for (RequestModel rm : requests) {
            gamifySvc.awardPointsForRequest(rm.getRequestId(), rm.getUserId());
        }

        // optional: delete or mark processed
        requestRepo.deleteAll(requests);
    }
}
