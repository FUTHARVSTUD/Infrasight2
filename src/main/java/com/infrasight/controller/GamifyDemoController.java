@RestController
@RequestMapping("${apiOpenPrefix:/api}/gamify")
@CrossOrigin(origins = "http://localhost:3000")
@RequiredArgsConstructor
public class GamifyController {

    private final GamificationService svc;

    // 1. Login points
    @PostMapping("/login")
    public ResponseEntity<PointsResponse> loginPoints(
            @AuthenticationPrincipal String userId) {
        PointsResponse resp = svc.awardLoginPoints(userId);
        return ResponseEntity.ok(resp);
    }

    // 2. Command points
    @PostMapping("/points")
    public ResponseEntity<PointsResponse> commandPoints(
            @RequestBody PointsRequest req,
            @AuthenticationPrincipal String userId) {
        PointsResponse resp = svc.awardCommandPoints(req, userId);
        return ResponseEntity.ok(resp);
    }

    // 3. Fetch “me”
    @GetMapping("/me/{userId}")
    public ResponseEntity<UserMe> getMe(@PathVariable String userId) {
        UserMe me = svc.getUserMe(userId);
        return ResponseEntity.of(Optional.ofNullable(me));
    }

    // 4. Leaderboard
    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> leaderboard(
            @RequestParam(defaultValue="20") int limit,
            @RequestParam(defaultValue="0") int offset,
            @RequestParam(required=false) String department) {
        List<LeaderboardEntry> board = svc.getLeaderboard(limit, offset, department);
        return ResponseEntity.ok(board);
    }

    // 5. Badge catalog
    @GetMapping("/badges")
    public ResponseEntity<List<BadgeDef>> badges() {
        return ResponseEntity.ok(svc.getAllBadges());
    }
}
