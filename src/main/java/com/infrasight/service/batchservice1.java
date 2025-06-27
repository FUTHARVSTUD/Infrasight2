@Service
@RequiredArgsConstructor
public class NightlyGamifyJob {

    private final RequestModelRepository reqRepo;   // <-- use this
    private final GamifyConfigRepository cfgRepo;
    private final UserGamifyRepository   userRepo;
    private final PointsLogRepository    logRepo;
    private final GamificationCalculator calc;      // pure math helper

    @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
    public void run() {
        Instant midnightUtc = Instant.now().truncatedTo(ChronoUnit.DAYS);

        List<RequestModel> events =
            reqRepo.findByFinishedTimestampBetween(
                midnightUtc.minus(1, ChronoUnit.DAYS), midnightUtc);

        Map<String,List<RequestModel>> byUser =
            events.stream().collect(Collectors.groupingBy(RequestModel::getUserId));

        for (String userId : byUser.keySet()) {
            int deltaTotal = 0;
            List<String> newBadges = new ArrayList<>();

            for (RequestModel rm : byUser.get(userId)) {
                PointsRequest dto = toPointsRequest(rm);
                PointsResponse pr = calc.computeDelta(dto); // returns delta+badges
                deltaTotal += pr.getDelta();
                newBadges.addAll(pr.getNewBadges());
            }

            // update user_gamify and write one PointsLog (same as before) …
        }
    }

    private PointsRequest toPointsRequest(RequestModel rm) {
        PointsRequest dto = new PointsRequest();
        dto.setActionUuid(rm.getRequestId());
        dto.setEvent(rm.getEvent());
        dto.setEnvironment(rm.getEnvironment());
        dto.setServers(rm.getServerList());
        dto.setParameters(rm.getParameters());
        return dto;
    }
}
