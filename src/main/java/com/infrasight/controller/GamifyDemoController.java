@RestController
@RequestMapping("/api/gamify")
@CrossOrigin(origins = "http://localhost:3000")
public class GamifyController {

    private final UserGamifyRepository userRepo;

    public GamifyController(UserGamifyRepository userRepo) {
        this.userRepo = userRepo;
    }

    /**
     * POST /api/gamify/me
     * Saves whatever UserGamify JSON you send.
     */
    @PostMapping("/me")
    public ResponseEntity<UserGamify> saveUser(@RequestBody UserGamify user) {
        UserGamify saved = userRepo.save(user);
        return ResponseEntity.ok(saved);
    }

    /**
     * GET /api/gamify/me/{userId}
     * Retrieves the UserGamify document by ID.
     */
    @GetMapping("/me/{userId}")
    public ResponseEntity<UserGamify> getUser(@PathVariable String userId) {
        return userRepo.findById(userId)
                       .map(ResponseEntity::ok)
                       .orElse(ResponseEntity.notFound().build());
    }
}
