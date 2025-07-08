import { useEffect, useState } from 'react';
import { useRouter } from 'next/router';
import {
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Container,
  Grid,
  Typography,
  Alert,
  Snackbar,
} from '@mui/material';
import CountUp from 'react-countup';
import { useQuery } from '@tanstack/react-query';

const fetchGamification = async (id) => {
  const res = await fetch(`/api/user/${id}/gamification`);
  if (!res.ok) throw new Error('Failed to fetch');
  return res.json();
};

export default function GamificationProfile() {
  const router = useRouter();
  const { id } = router.query;

  const { data, isLoading, error, refetch } = useQuery(
    ['gamification', id],
    () => fetchGamification(id),
    { enabled: !!id }
  );

  const [points, setPoints] = useState(0);
  const [toastOpen, setToastOpen] = useState(false);

  useEffect(() => {
    if (data) {
      setPoints(data.totalPoints);
    }
  }, [data]);

  const handleEarnPoint = () => {
    setPoints((p) => p + 1);
    setToastOpen(true);
  };

  const handleClose = (_, reason) => {
    if (reason === 'clickaway') return;
    setToastOpen(false);
  };

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" mt={4}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Box display="flex" flexDirection="column" alignItems="center" mt={4}>
        <Alert severity="error" sx={{ mb: 2 }}>
          Failed to load user profile.
        </Alert>
        <Button variant="contained" onClick={() => refetch()}>
          Retry
        </Button>
      </Box>
    );
  }

  if (!data) return null;

  return (
    <Container maxWidth={false} sx={{ mt: 4 }}>
      <Grid container spacing={2}>
        <Grid item xs={12} md={4}>
          <Box display="flex" flexDirection="column" justifyContent="center" alignItems="center" height="100%">
            <Avatar src={data.avatarUrl} alt={data.name} sx={{ width: 120, height: 120, mb: 2 }} />
            <Typography variant="h6">{data.name}</Typography>
            <Box mt={2}>
              <Button variant="contained" onClick={handleEarnPoint}>Earn Point</Button>
            </Box>
          </Box>
        </Grid>
        <Grid item xs={12} md={8}>
          <Grid container spacing={2} sx={{ mb: 2 }}>
            <Grid item xs={12} sm={6} md={3}>
              <Card sx={{ p: 1, transition: 'box-shadow 0.3s', '&:hover': { boxShadow: 6 } }} elevation={3}>
                <CardContent>
                  <Typography variant="body2">Total Points</Typography>
                  <Typography variant="h5">
                    <CountUp end={points} duration={1} />
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Card sx={{ p: 1, transition: 'box-shadow 0.3s', '&:hover': { boxShadow: 6 } }} elevation={3}>
                <CardContent>
                  <Typography variant="body2">Streak Days</Typography>
                  <Typography variant="h5">
                    <CountUp end={data.streakDays} duration={1} />
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Card sx={{ p: 1, transition: 'box-shadow 0.3s', '&:hover': { boxShadow: 6 } }} elevation={3}>
                <CardContent>
                  <Typography variant="body2">Streak Multiplier</Typography>
                  <Typography variant="h5">
                    <CountUp end={data.streakMultiplier} duration={1} />
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid item xs={12} sm={6} md={3}>
              <Card sx={{ p: 1, transition: 'box-shadow 0.3s', '&:hover': { boxShadow: 6 } }} elevation={3}>
                <CardContent>
                  <Typography variant="body2">Total Commands Executed</Typography>
                  <Typography variant="h5">
                    <CountUp end={data.totalCommandsExecuted} duration={1} />
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>
          <Grid container spacing={2}>
            {data.badges.map((badge) => (
              <Grid item xs={6} sm={4} md={3} key={badge.id}>
                <Card sx={{ p: 2, textAlign: 'center', transition: 'box-shadow 0.3s', '&:hover': { boxShadow: 6 } }} elevation={1}>
                  <Box display="flex" flexDirection="column" alignItems="center">
                    <Avatar src={badge.iconUrl} alt={badge.label} sx={{ mb: 1 }} />
                    <Typography variant="body2">{badge.label}</Typography>
                  </Box>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Grid>
      </Grid>
      <Snackbar
        open={toastOpen}
        autoHideDuration={3000}
        onClose={handleClose}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert onClose={handleClose} severity="success" sx={{ width: '100%' }}>
          +1 point!
        </Alert>
      </Snackbar>
    </Container>
  );
}
