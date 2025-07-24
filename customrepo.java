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

