@RequiredArgsConstructor
public class PointsLogRepositoryCustomImpl
        implements PointsLogRepositoryCustom {

    private final MongoTemplate mongo;

    @Override
    public List<UserLeaderboardRankDTO> findRanksByName(
            String   nameRegex,
            Instant  weekStart,
            Instant  weekEnd,
            String   department) {

        // ---------- Match stage: time-window & (optional) department
        Criteria timeCrit = Criteria.where("timestamp")
                                    .gte(weekStart)
                                    .lt(weekEnd);

        // join department only if filter present
        MatchOperation matchTime = Aggregation.match(timeCrit);

        // ---------- Group: total points per user for the week
        GroupOperation group =
            Aggregation.group("userId")
                       .sum("pointsDelta").as("pts");

        // ---------- $lookup user info from user_gamify
        LookupOperation lookup =
            LookupOperation.newLookup()
                .from("user_gamify")
                .localField("_id")
                .foreignField("userId")
                .as("user");

        UnwindOperation unwind = Aggregation.unwind("user");

        // ---------- Optional department filter
        MatchOperation matchDept = (department == null || department.isBlank())
                ? null
                : Aggregation.match(Criteria.where("user.department").is(department));

        // ---------- Window function: rank by pts desc (tie = same rank)
        Document setWindowFields = new Document("$setWindowFields",
            new Document("sortBy",  new Document("pts", -1).append("_id", 1))
                .append("output",
                    new Document("rank", new Document("$rank", new Document())))
        );

        // ---------- Name search (case-insensitive, partial)
        MatchOperation matchName =
            Aggregation.match(Criteria.where("user.name")
                                      .regex(nameRegex, "i"));

        // ---------- Projection to DTO shape
        ProjectionOperation project = Aggregation.project()
            .and("_id").as("userId")
            .and("user.name").as("name")
            .and("user.department").as("department")
            .and("pts").as("totalPoints")
            .and("rank").as("rank");

        // ---------- Build pipeline
        List<AggregationOperation> ops = new ArrayList<>();
        ops.add(matchTime);
        ops.add(group);
        ops.add(lookup);
        ops.add(unwind);
        if (matchDept != null) ops.add(matchDept);
        ops.add(Aggregation.raw(setWindowFields));
        ops.add(matchName);
        ops.add(project);
        ops.add(Aggregation.sort(Sort.by(Sort.Direction.DESC, "totalPoints")
                                     .and(Sort.by("rank")))); // stable output

        Aggregation agg = Aggregation.newAggregation(ops);

        return mongo.aggregate(agg, "points_log", UserLeaderboardRankDTO.class)
                    .getMappedResults();
    }
}
