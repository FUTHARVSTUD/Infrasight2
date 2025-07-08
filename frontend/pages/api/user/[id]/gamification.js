export default function handler(req, res) {
  const {
    query: { id },
  } = req;

  res.status(200).json({
    userId: id,
    name: 'Jane Doe',
    avatarUrl: 'https://via.placeholder.com/150',
    totalPoints: 1234,
    streakDays: 5,
    streakMultiplier: 1.2,
    totalCommandsExecuted: 42,
    badges: [
      { id: '1', label: 'Beginner', iconUrl: 'https://via.placeholder.com/32' },
      { id: '2', label: 'Power User', iconUrl: 'https://via.placeholder.com/32' },
      { id: '3', label: 'Veteran', iconUrl: 'https://via.placeholder.com/32' },
    ],
  });
}
