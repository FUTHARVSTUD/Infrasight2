@Service
@RequiredArgsConstructor
public class NightlyGamifyJob {

    private final RawEventRepository rawRepo;
    private final GamifyConfigRepository cfgRepo;
    private final UserGamifyRepository userRepo;
    private final PointsLogRepository logRepo;
    private final GamificationService calc;   // reuse your existing scoring helpers

    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")  // every day 00:05 UTC
    public void run() {
        GamifyConfigDoc cfg = cfgRepo.findById("default").orElseThrow();

        // 1) pull all yesterday's events
        Instant midnightUtc = Instant.now().truncatedTo(ChronoUnit.DAYS);
        List<RawEvent> events =
            rawRepo.findByTsBetween(midnightUtc.minus(1, ChronoUnit.DAYS), midnightUtc);

        // 2) bucket by userId
        Map<String,List<RawEvent>> byUser =
            events.stream().collect(Collectors.groupingBy(RawEvent::getUserId));

        for (String userId : byUser.keySet()) {
            List<RawEvent> evList = byUser.get(userId);

            int deltaTotal = 0;
            List<String> newBadges = new ArrayList<>();

            for (RawEvent ev : evList) {
                PointsRequest req = toDto(ev);
                PointsResponse pr = calc.computeDeltaOnly(req, userId, cfg); // pure math
                deltaTotal += pr.getDelta();
                newBadges.addAll(pr.getNewBadges());
            }

            // 3) persist cumulative delta to user_gamify
            UserGamify user = userRepo.findById(userId).orElseGet(() -> calc.initUser(userId));
            user.setTotalPoints(user.getTotalPoints() + deltaTotal);
            user.setLastActivity(LocalDate.now());          // or keep max of events
            userRepo.save(user);

            // 4) insert one summary log row
            PointsLog log = new PointsLog(null, userId,
                "batch.daily", "n/a", List.of(), deltaTotal,
                user.getTotalPoints(), user.getStreakDays(),
                LocalDateTime.now(), newBadges);
            logRepo.save(log);
        }

        // 5) archive or delete processed events
        rawRepo.deleteAll(events);
    }

    private PointsRequest toDto(RawEvent ev) {
        PointsRequest dto = new PointsRequest();
        dto.setActionUuid(ev.getId());
        dto.setEvent(ev.getEvent());
        dto.setEnvironment(ev.getEnvironment());
        dto.setServers(ev.getServers());
        dto.setParameters(ev.getParameters());
        return dto;
    }
}
