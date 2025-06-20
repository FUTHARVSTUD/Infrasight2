public class GamificationService {
    private final MongoConfigService configSvc;
    private final UserGamifyRepository userRepo;
    private final PointsLogRepository logRepo;

    public PointsResponse awardLoginPoints(String userId) {
        UserGamify user = initUser(userId);
        GamifyConfigDoc cfg = configSvc.getConfig();

        int delta = cfg.getLoginPoints();
        if (gapDays(user.getLastActivity()) >= cfg.getWelcomeBackGap()) {
            delta += cfg.getWelcomeBackBonus();
        }
        int newStreak = updateStreak(user.getLastActivity(), LocalDate.now(), user.getStreakDays());
        user.setLastActivity(LocalDate.now());
        user.setStreakDays(newStreak);
        user.setTotalPoints(user.getTotalPoints() + delta);
        List<String> newBadges = evaluateBadges(user);
        if (!newBadges.isEmpty()) {
            delta += badgeBonus(newBadges);
            user.getBadges().addAll(newBadges);
            user.setTotalPoints(user.getTotalPoints() + badgeBonus(newBadges));
        }
        userRepo.save(user);
        logRepo.save(PointsLog.builder()
                .userId(userId)
                .commandType("meta.login")
                .pointsDelta(delta)
                .totalPoints(user.getTotalPoints())
                .streakDays(user.getStreakDays())
                .timestamp(LocalDateTime.now())
                .newBadges(newBadges)
                .build());
        return new PointsResponse(delta, user.getTotalPoints(), user.getStreakDays(), newBadges);
    }

public PointsResponse awardCommandPoints(PointsRequest req, String userId) {
        if (logRepo.existsByActionUuid(req.getActionUuid())) {
            return new PointsResponse(0, initUser(userId).getTotalPoints(), initUser(userId).getStreakDays(), List.of());
        }
        UserGamify user = initUser(userId);
        GamifyConfigDoc cfg = configSvc.getConfig();

        int complexity = complexityScore(req.getParameters());
        double cmdWeight = cfg.getCommandWeight().getOrDefault(req.getEvent().contains("param") ? "param" : "default", 1.0);
        double envWeight = cfg.getAccessTierWeight().getOrDefault(req.getEnvironment(), 1.0);
        double scale = serverScale(req.getServers() == null ? 1 : req.getServers().size());
        double streakMult = streakMultiplier(user.getStreakDays());

        int delta = (int)Math.ceil((cfg.getBaseScore() + complexity) * cmdWeight * envWeight * scale * streakMult);

        user.setTotalPoints(user.getTotalPoints() + delta);
        user.setTotalCommands(user.getTotalCommands() + 1);
        if ("prod".equals(req.getEnvironment())) {
            user.setProdCommands(user.getProdCommands() + 1);
        }
        if (req.getServers() != null) {
            user.getUniqueServers().addAll(req.getServers());
        }
        int newStreakCmd = updateStreak(user.getLastActivity(), LocalDate.now(), user.getStreakDays());
        user.setLastActivity(LocalDate.now());
        user.setStreakDays(newStreakCmd);

        List<String> newBadges = evaluateBadges(user);
        if (!newBadges.isEmpty()) {
            delta += badgeBonus(newBadges);
            user.getBadges().addAll(newBadges);
            user.setTotalPoints(user.getTotalPoints() + badgeBonus(newBadges));
        }
        userRepo.save(user);
        logRepo.save(PointsLog.builder()
                .userId(userId)
                .actionUuid(req.getActionUuid())
                .commandType(req.getEvent())
                .environment(req.getEnvironment())
                .servers(req.getServers())
                .pointsDelta(delta)
                .totalPoints(user.getTotalPoints())
                .streakDays(user.getStreakDays())
                .timestamp(LocalDateTime.now())
                .newBadges(newBadges)
                .build());
        return new PointsResponse(delta, user.getTotalPoints(), user.getStreakDays(), newBadges);
    }
public UserMe getUserMe(String userId) {
        UserGamify user = initUser(userId);
        return new UserMe(user.getTotalPoints(), user.getStreakDays(), user.getBadges());
    }

    public List<LeaderboardEntry> getLeaderboard(int limit, int offset, String dept) {
        List<UserGamify> list;
        if (dept == null) {
            list = userRepo.findAllByOrderByTotalPointsDesc(PageRequest.of(offset/limit, limit));
        } else {
            list = userRepo.findByDepartmentOrderByTotalPointsDesc(dept, PageRequest.of(offset/limit, limit));
        }
        List<LeaderboardEntry> res = new ArrayList<>();
        int rank = offset + 1;
        for (UserGamify u : list) {
            res.add(new LeaderboardEntry(u.getUserId(), u.getName(), u.getDepartment(), u.getTotalPoints(), rank++, u.getBadges().size()));
        }
        return res;
    }

    public List<GamifyConfigDoc.BadgeDef> getAllBadges() {
        GamifyConfigDoc cfg = configSvc.getConfig();
        return new ArrayList<>(cfg.getBadges().values());
    }

    private UserGamify initUser(String userId) {
        return userRepo.findById(userId).orElseGet(() -> {
            UserGamify u = UserGamify.builder().userId(userId).build();
            return userRepo.save(u);
        });
    }

    private int complexityScore(List<String> params) {
        if (params == null) return 0;
        int score = 0;
        for (String p : params) {
            if (p.contains("*") || p.contains("?")) score += 10;
            else if (p.matches(".*[\\[\\]\\$\\^\\+].*")) score += 15;
            else score += 5;
        }
        return score;
    }

    private double serverScale(int count) {
        GamifyConfigDoc.ServerScaling ss = configSvc.getConfig().getServerScaling();
        if (ss.getFunction().equalsIgnoreCase("log")) {
            return 1.0 + Math.log(Math.max(count,1)) / Math.log(ss.getLogBase());
        }
        return 1.0;
    }

    private double streakMultiplier(int days) {
        return configSvc.getConfig().getStreakMultiplier().entrySet().stream()
                .filter(e -> days >= e.getKey())
                .mapToDouble(Map.Entry::getValue)
                .max().orElse(1.0);
    }

    private int updateStreak(LocalDate last, LocalDate now, int currentStreak) {
        if (last == null) return 1;
        long diff = ChronoUnit.DAYS.between(last, now);
        if (diff == 0) return currentStreak;
        if (diff == 1) return currentStreak + 1;
        return 1;
    }

    private long gapDays(LocalDate last) {
        if (last == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(last, LocalDate.now());
    }

    private List<String> evaluateBadges(UserGamify user) {
        GamifyConfigDoc cfg = configSvc.getConfig();
        ExpressionParser parser = new SpelExpressionParser();
        List<String> newCodes = new ArrayList<>();
        for (Map.Entry<String, GamifyConfigDoc.BadgeDef> e : cfg.getBadges().entrySet()) {
            if (user.getBadges().contains(e.getKey())) continue;
            Boolean ok = parser.parseExpression(e.getValue().getCondition()).getValue(user, Boolean.class);
            if (Boolean.TRUE.equals(ok)) {
                newCodes.add(e.getKey());
            }
        }
        return newCodes;
    }

private int badgeBonus(List<String> codes) {
        if (codes.isEmpty()) return 0;
        Map<String, GamifyConfigDoc.BadgeDef> defs = configSvc.getConfig().getBadges();
        return codes.stream().mapToInt(c -> defs.get(c).getBonus()).sum();
    }
}
