import java.time.LocalDateTime;
import java.util.List;

public interface PointsLogRepositoryCustom {
    List<WeeklyLeaderboardEntry> getWeeklyLeaderboard(LocalDateTime weekStart, LocalDateTime weekEnd, int page, int size);
    long countDistinctUsersInWeek(LocalDateTime weekStart, LocalDateTime weekEnd);
}

--------------------------------

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class PointsLogRepositoryCustomImpl implements PointsLogRepositoryCustom {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public List<WeeklyLeaderboardEntry> getWeeklyLeaderboard(LocalDateTime weekStart, LocalDateTime weekEnd, int page, int size) {
        MatchOperation match = Aggregation.match(
                Criteria.where("timestamp").gte(weekStart).lt(weekEnd)
        );
        GroupOperation group = Aggregation.group("userId")
                .sum("pointsDelta").as("weekPoints")
                .first("userId").as("userId")
                .first("name").as("name") // Assumes name is stored in PointsLog
                .first("department").as("department");

        SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "weekPoints");
        SkipOperation skip = Aggregation.skip((long) page * size);
        LimitOperation limit = Aggregation.limit(size);

        Aggregation agg = Aggregation.newAggregation(
                match, group, sort, skip, limit
        );

        return mongoTemplate.aggregate(agg, "points_log", WeeklyLeaderboardEntry.class).getMappedResults();
    }

    @Override
    public long countDistinctUsersInWeek(LocalDateTime weekStart, LocalDateTime weekEnd) {
        MatchOperation match = Aggregation.match(
                Criteria.where("timestamp").gte(weekStart).lt(weekEnd)
        );
        GroupOperation group = Aggregation.group("userId");

        Aggregation agg = Aggregation.newAggregation(match, group);
        return mongoTemplate.aggregate(agg, "points_log", WeeklyLeaderboardEntry.class).getMappedResults().size();
    }
}

-----------------------------------------

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final PointsLogRepository pointsLogRepo;
    private final UserGamifyRepository userGamifyRepo;

    public Page<WeeklyLeaderboardEntry> getWeeklyLeaderboard(
            LocalDateTime weekStart,
            LocalDateTime weekEnd,
            int page,
            int size
    ) {
        // 1. Get paginated weekly leaderboard (userId + weekPoints)
        List<WeeklyLeaderboardEntry> entries = pointsLogRepo.getWeeklyLeaderboard(weekStart, weekEnd, page, size);

        // 2. Batch-fetch user details
        List<String> userIds = entries.stream()
            .map(WeeklyLeaderboardEntry::getUserId)
            .collect(Collectors.toList());
        Map<String, UserGamify> userMap = userGamifyRepo.findAllById(userIds)
            .stream()
            .collect(Collectors.toMap(UserGamify::getUserId, Function.identity()));

        // 3. Fill name/department in results
        for (WeeklyLeaderboardEntry entry : entries) {
            UserGamify user = userMap.get(entry.getUserId());
            if (user != null) {
                entry.setName(user.getName());
                entry.setDepartment(user.getDepartment());
            }
        }

        // 4. Assign rank (1-based across pages)
        int startRank = page * size + 1;
        IntStream.range(0, entries.size())
            .forEach(i -> entries.get(i).setRank(startRank + i));

        // 5. Get total for pagination
        long total = pointsLogRepo.countDistinctUsersInWeek(weekStart, weekEnd);

        // 6. Return as Page DTO
        return new Page<>(entries, total, size, page * size);
    }
}

------------------------------

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/gamify/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping("/weekly")
    public Page<WeeklyLeaderboardEntry> getWeeklyLeaderboard(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime weekStart,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime weekEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return leaderboardService.getWeeklyLeaderboard(weekStart, weekEnd, page, size);
    }
}

-------------------------------

@Override
public List<WeeklyLeaderboardEntry> getWeeklyLeaderboard(
        LocalDateTime weekStart, LocalDateTime weekEnd, int page, int size, String department) {

    MatchOperation match = Aggregation.match(
            Criteria.where("timestamp").gte(weekStart).lt(weekEnd)
    );
    GroupOperation group = Aggregation.group("userId")
            .sum("pointsDelta").as("weekPoints")
            .first("userId").as("userId");

    SortOperation sort = Aggregation.sort(Sort.Direction.DESC, "weekPoints");
    SkipOperation skip = Aggregation.skip((long) page * size);
    LimitOperation limit = Aggregation.limit(size);

    Aggregation agg = Aggregation.newAggregation(
            match, group, sort, skip, limit
    );

    List<WeeklyLeaderboardEntry> entries = mongoTemplate.aggregate(agg, "points_log", WeeklyLeaderboardEntry.class).getMappedResults();

    // Filter by department in Java (after join with UserGamify)
    if (department != null && !department.isEmpty()) {
        entries = entries.stream()
                .filter(e -> department.equals(e.getDepartment()))
                .collect(Collectors.toList());
    }

    return entries;
}

@Override
public long countDistinctUsersInWeek(LocalDateTime weekStart, LocalDateTime weekEnd, String department) {
    // Do aggregation as before, then filter by department after service layer join
    // For simplicity, return 0 here and handle filtering in service layer
    return 0;
}

------------------------------------

public Page<WeeklyLeaderboardEntry> getWeeklyLeaderboard(
        LocalDateTime weekStart,
        LocalDateTime weekEnd,
        int page,
        int size,
        String department // new param
) {
    List<WeeklyLeaderboardEntry> entries = pointsLogRepo.getWeeklyLeaderboard(weekStart, weekEnd, page, size, department);

    // Batch-fetch user details
    List<String> userIds = entries.stream()
        .map(WeeklyLeaderboardEntry::getUserId)
        .collect(Collectors.toList());
    Map<String, UserGamify> userMap = StreamSupport.stream(userGamifyRepo.findAllById(userIds).spliterator(), false)
        .collect(Collectors.toMap(UserGamify::getUserId, Function.identity()));

    // Fill name/department
    for (WeeklyLeaderboardEntry entry : entries) {
        UserGamify user = userMap.get(entry.getUserId());
        if (user != null) {
            entry.setName(user.getName());
            entry.setDepartment(user.getDepartment());
        }
    }

    // Now filter by department
    if (department != null && !department.isEmpty()) {
        entries = entries.stream()
                .filter(e -> department.equals(e.getDepartment()))
                .collect(Collectors.toList());
    }

    // Assign rank
    int startRank = page * size + 1;
    IntStream.range(0, entries.size()).forEach(i -> entries.get(i).setRank(startRank + i));

    // For total, you may want to run a second aggregation or just set to entries.size() for filtered
    long total = entries.size();

    return new Page<>(entries, total, size, page * size);
}

---------------------------------------

public Page<WeeklyLeaderboardEntry> getWeeklyLeaderboard(
        LocalDateTime weekStart,
        LocalDateTime weekEnd,
        int page,
        int size,
        String department) {

    Map<String, Integer> userPoints = new HashMap<>();

    // 1. Stream and aggregate points per user
    try (Stream<PointsLog> logs = pointsLogRepo.streamByTimestampBetween(weekStart, weekEnd)) {
        logs.forEach(log -> userPoints.merge(log.getUserId(), log.getPointsDelta(), Integer::sum));
    }

    // 2. Batch fetch user details
    List<String> userIds = new ArrayList<>(userPoints.keySet());
    Map<String, UserGamify> userMap = StreamSupport
            .stream(userGamifyRepo.findAllById(userIds).spliterator(), false)
            .collect(Collectors.toMap(UserGamify::getUserId, Function.identity()));

    // 3. Map, filter by department, and collect to list
    List<WeeklyLeaderboardEntry> entries = userPoints.entrySet().stream()
        .map(e -> {
            UserGamify user = userMap.get(e.getKey());
            if (user == null) return null;
            return new WeeklyLeaderboardEntry(
                user.getUserId(),
                user.getName(),
                user.getDepartment(),
                e.getValue() // weekPoints
            );
        })
        .filter(Objects::nonNull)
        .filter(entry -> department == null || department.equals(entry.getDepartment()))
        .sorted(Comparator.comparingInt(WeeklyLeaderboardEntry::getWeekPoints).reversed())
        .collect(Collectors.toList());

    // 4. Pagination (calculate offsets)
    int startIdx = page * size;
    int endIdx = Math.min(startIdx + size, entries.size());
    List<WeeklyLeaderboardEntry> pageEntries = (startIdx < endIdx) ? entries.subList(startIdx, endIdx) : List.of();

    // 5. Assign rank
    for (int i = 0; i < pageEntries.size(); i++) {
        pageEntries.get(i).setRank(startIdx + i + 1);
    }

    // 6. Return as Page DTO
    return new Page<>(pageEntries, entries.size(), size, startIdx);
}
