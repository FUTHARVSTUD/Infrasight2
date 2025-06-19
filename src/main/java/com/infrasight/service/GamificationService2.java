package com.wellsfargo.infrasight.service;

import com.wellsfargo.infrasight.config.GamifyConfig; import com.wellsfargo.infrasight.domain.BadgeMaster; import com.wellsfargo.infrasight.domain.PointsLog; import com.wellsfargo.infrasight.domain.UserGamify; import com.wellsfargo.infrasight.dto.*; import com.wellsfargo.infrasight.repository.BadgeMasterRepository; import com.wellsfargo.infrasight.repository.PointsLogRepository; import com.wellsfargo.infrasight.repository.UserGamifyRepository; import lombok.RequiredArgsConstructor; import org.springframework.expression.ExpressionParser; import org.springframework.expression.spel.standard.SpelExpressionParser; import org.springframework.expression.spel.support.StandardEvaluationContext; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import org.springframework.data.domain.PageRequest;

import java.time.LocalDate; import java.time.LocalDateTime; import java.time.temporal.ChronoUnit; import java.util.*; import java.util.stream.Collectors;

@Service @RequiredArgsConstructor public class GamificationService { private final GamifyConfig cfg; private final UserGamifyRepository userRepo; private final PointsLogRepository logRepo; private final BadgeMasterRepository badgeRepo;

private final ExpressionParser spelParser = new SpelExpressionParser();

/**
 * Award points on login: login bonus + possible welcome-back bonus + streak update
 */
@Transactional
public PointsResponse awardLoginPoints(String userId) {
    UserGamify user = userRepo.findById(userId)
        .orElseGet(() -> initUser(userId));

    int delta = cfg.getLoginPoints();
    // welcome-back
    if (ChronoUnit.DAYS.between(user.getLastActivity(), LocalDate.now()) > cfg.getWelcomeBackGap()) {
        delta += cfg.getWelcomeBackBonus();
    }
    // update streak
    int newStreak = updateStreak(user.getLastActivity(), LocalDate.now());
    user.setStreakDays(newStreak);
    user.setLastActivity(LocalDate.now());
    user.setTotalPoints(user.getTotalPoints() + delta);

    // log event
    PointsLog log = PointsLog.builder()
        .userId(userId)
        .commandType("meta.dailyLogin")
        .environment("n/a")
        .servers(Collections.emptyList())
        .pointsDelta(delta)
        .totalPoints(user.getTotalPoints())
        .streakDays(user.getStreakDays())
        .timestamp(LocalDateTime.now())
        .newBadges(Collections.emptyList())
        .build();
    logRepo.save(log);
    userRepo.save(user);

    return new PointsResponse(delta, user.getTotalPoints(), user.getStreakDays(), Collections.emptyList());
}

/**
 * Award points for a command execution
 */
@Transactional
public PointsResponse awardCommandPoints(PointsRequest req, String userId) {
    UserGamify user = userRepo.findById(userId)
        .orElseGet(() -> initUser(userId));

    // base + complexity
    int baseScore = cfg.getBaseScore();
    int complexity = complexityScore(req.getParameters());

    // command weight (default vs param)
    double cmdWeight = cfg.getCommandWeight().getOrDefault(req.getEvent(), 1.0);
    // access tier weight
    double envWeight = cfg.getAccessTierWeight().getOrDefault(req.getEnvironment(), 1.0);
    // server scaling
    double ss = serverScale(req.getServers().size());
    // streak multiplier
    double sm = streakMultiplier(user.getStreakDays());

    int raw = (int) Math.ceil((baseScore + complexity) * cmdWeight * envWeight * ss * sm);
    int delta = raw;

    // update user fields
    user.setTotalPoints(user.getTotalPoints() + delta);
    user.setStreakDays(updateStreak(user.getLastActivity(), LocalDate.now()));
    user.setLastActivity(LocalDate.now());
    // commands counters
    user.setTotalCommands(user.getTotalCommands() + 1);
    if ("prod".equalsIgnoreCase(req.getEnvironment())) {
        user.setProdCommands(user.getProdCommands() + 1);
    }
    // servers set
    user.getUniqueServers().addAll(req.getServers());

    // badge evaluation
    List<String> newBadges = evaluateBadges(user);
    for (String code : newBadges) {
        user.getBadges().add(code);
        user.setTotalPoints(user.getTotalPoints() + cfg.getBadges().get(code).getBonus());
    }

    // log event
    PointsLog log = PointsLog.builder()
        .userId(userId)
        .commandType(req.getEvent())
        .environment(req.getEnvironment())
        .servers(req.getServers())
        .pointsDelta(delta)
        .totalPoints(user.getTotalPoints())
        .streakDays(user.getStreakDays())
        .timestamp(LocalDateTime.now())
        .newBadges(newBadges)
        .build();
    logRepo.save(log);
    userRepo.save(user);

    return new PointsResponse(delta, user.getTotalPoints(), user.getStreakDays(), newBadges);
}

/**
 * Fetch current user summary
 */
public UserMe getUserMe(String userId) {
    return userRepo.findById(userId)
        .map(u -> new UserMe(u.getTotalPoints(), u.getStreakDays(), u.getBadges()))
        .orElseGet(() -> new UserMe(0, 0, Collections.emptyList()));
}

/**
 * Fetch leaderboard entries
 */
public List<LeaderboardEntry> getLeaderboard(int limit, int offset, String department) {
    PageRequest pr = PageRequest.of(offset / limit, limit);
    List<UserGamify> users = (department != null)
        ? userRepo.findByDepartmentOrderByTotalPointsDesc(department, pr)
        : userRepo.findAllByOrderByTotalPointsDesc(pr);
    return users.stream()
        .map((u) -> new LeaderboardEntry(
            u.getUserId(), u.getName(), u.getDepartment(),
            u.getTotalPoints(), 0, u.getBadges().size()
        ))
        .collect(Collectors.toList());
}

/**
 * Return all badge definitions for catalog
 */
public List<BadgeDef> getAllBadges() {
    return badgeRepo.findAll().stream()
        .map(b -> new BadgeDef(
            b.getCode(), b.getName(), b.getDescription(),
            b.getCondition(), b.getBonus(), b.getIconUrl()
        ))
        .collect(Collectors.toList());
}

// --- helpers ---
private UserGamify initUser(String userId) {
    UserGamify u = new UserGamify(userId, "", "");
    u.setLastActivity(LocalDate.now().minusDays(1));
    u.setBadges(new ArrayList<>());
    return userRepo.save(u);
}

private int complexityScore(List<String> params) {
    return params.stream()
        .mapToInt(p -> cfg.getParameterWeight().getOrDefault(p, 1.0).intValue())
        .sum();
}

private double serverScale(int n) {
    if ("log".equalsIgnoreCase(cfg.getServerScaling().getFunction())) {
        return Math.log(n + 1) / Math.log(cfg.getServerScaling().getLogBase());
    }
    return Math.sqrt(n);
}

private double streakMultiplier(int days) {
    return cfg.getStreakMultiplier().entrySet().stream()
        .filter(e -> e.getKey() <= days)
        .mapToDouble(Map.Entry::getValue)
        .max().orElse(1.0);
}

private int updateStreak(LocalDate last, LocalDate now) {
    return last.plusDays(1).equals(now) ? (last != null ? 1 + cfg.getStreakMultiplier().size() : 1) : 1;
}

private List<String> evaluateBadges(UserGamify user) {
    List<String> earned = new ArrayList<>();
    cfg.getBadges().forEach((code, def) -> {
        if (!user.getBadges().contains(code)) {
            StandardEvaluationContext ctx = new StandardEvaluationContext();
            ctx.setVariable("uniqueServers", user.getUniqueServers().size());
            ctx.setVariable("totalCommands", user.getTotalCommands());
            ctx.setVariable("prodCommands", user.getProdCommands());
            ctx.setVariable("streakDays", user.getStreakDays());
            Boolean ok = spelParser.parseExpression(def.getCondition()).getValue(ctx, Boolean.class);
            if (Boolean.TRUE.equals(ok)) {
                earned.add(code);
            }
        }
    });


