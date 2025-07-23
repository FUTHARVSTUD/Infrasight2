import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeeklyStreakBatchJob {

    private final PointsLogRepository pointsLogRepo;
    private final UserGamifyRepository userGamifyRepo;

    // Runs every Monday at 1:05 AM UTC (adjust as needed)
    @Scheduled(cron = "0 5 1 ? * MON", zone = "UTC")
    public void runWeeklyStreakUpdate() {
        // Get all users
        List<UserGamify> allUsers = userGamifyRepo.findAll();

        for (UserGamify user : allUsers) {
            // 1. Determine user's timezone
            String tzId = user.getTimezone() != null ? user.getTimezone() : "UTC";
            ZoneId zoneId = ZoneId.of(tzId);

            // 2. Calculate the previous week in user’s local time
            ZonedDateTime nowUserTz = ZonedDateTime.now(zoneId);
            // Find previous Monday in user's TZ
            ZonedDateTime thisMonday = nowUserTz.with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS);
            ZonedDateTime prevMonday = thisMonday.minusWeeks(1);
            ZonedDateTime nextMonday = prevMonday.plusWeeks(1);

            // Convert to Instant for querying PointsLog (stored in UTC)
            Instant rangeStart = prevMonday.withZoneSameInstant(ZoneOffset.UTC).toInstant();
            Instant rangeEnd = nextMonday.withZoneSameInstant(ZoneOffset.UTC).toInstant();

            // 3. Fetch all logs for the user in that range
            List<PointsLog> weekLogs = pointsLogRepo.findByUserIdAndTimestampBetween(
                user.getUserId(),
                rangeStart.atZone(ZoneOffset.UTC).toLocalDateTime(),
                rangeEnd.atZone(ZoneOffset.UTC).toLocalDateTime()
            );

            // 4. Collect unique LOCAL dates (in user's TZ) from logs
            Set<LocalDate> activeDays = weekLogs.stream()
                .map(log -> log.getTimestamp()
                    .atZone(ZoneOffset.UTC)
                    .withZoneSameInstant(zoneId)
                    .toLocalDate()
                )
                .collect(Collectors.toSet());

            int daysActive = activeDays.size();

            // 5. Update streak: increment if >=5, else reset
            if (daysActive >= 5) {
                user.setWeeklyStreak(user.getWeeklyStreak() + 1);
                log.info("User {}: streak incremented to {}", user.getUserId(), user.getWeeklyStreak());
            } else {
                if (user.getWeeklyStreak() > 0) {
                    log.info("User {}: streak reset from {}", user.getUserId(), user.getWeeklyStreak());
                }
                user.setWeeklyStreak(0);
            }

            userGamifyRepo.save(user);
        }
        log.info("Weekly streak batch (timezone-aware) complete. Processed {} users.", allUsers.size());
    }
}
