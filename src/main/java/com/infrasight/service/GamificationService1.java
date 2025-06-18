@Service
@RequiredArgsConstructor
public class GamificationService {
    // inject your repos & config
    public PointsResponse awardLoginPoints(String userId) {
        // TODO: lookup or init UserGamify, compute delta, save log & user
        return new PointsResponse( /* delta, newTotal, streak, emptyBadges */ );
    }

    public PointsResponse awardCommandPoints(PointsRequest req, String userId) {
        // TODO: compute command pts + serverScale + streak + bonuses
        return new PointsResponse( /* ... */ );
    }

    public UserMe getUserMe(String userId) {
        // TODO: fetch UserGamify → map to UserMe
        return new UserMe( /* totalPoints, streakDays, badges */ );
    }

    public List<LeaderboardEntry> getLeaderboard(int limit, int offset, String dept) {
        // TODO: query UserGamify sorted, map to entries
        return List.of();
    }

    public List<BadgeDef> getAllBadges() {
        // TODO: fetch from config or badges_master
        return List.of();
    }
}
