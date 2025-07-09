"use client"

import { useState, useEffect } from "react"
import {
  AppBar,
  Box,
  CssBaseline,
  Drawer,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
  Container,
  Grid,
  Card,
  CardContent,
  CircularProgress,
  Alert,
  Badge,
  Divider,
  Paper,
} from "@mui/material"
import {
  Menu as MenuIcon,
  Dashboard as DashboardIcon,
  Person as PersonIcon,
  Analytics as AnalyticsIcon,
  Settings as SettingsIcon,
  Notifications as NotificationsIcon,
  Stars as StarsIcon,
  PlayArrow as CommandIcon,
  LocalFireDepartment as StreakIcon,
  TrendingUp as MultiplierIcon,
} from "@mui/icons-material"
import { ThemeProvider, createTheme } from "@mui/material/styles"

// Create a modern theme
const theme = createTheme({
  palette: {
    primary: {
      main: "#1976d2",
    },
    secondary: {
      main: "#dc004e",
    },
    background: {
      default: "#f5f5f5",
    },
  },
  typography: {
    h4: {
      fontWeight: 600,
    },
    h6: {
      fontWeight: 600,
    },
  },
  components: {
    MuiCard: {
      styleOverrides: {
        root: {
          boxShadow: "0 2px 10px rgba(0,0,0,0.1)",
          borderRadius: 12,
        },
      },
    },
  },
})

// Types
interface UserData {
  name: string
  department: string
  totalPoints: number
  totalCommandExecutions: number
  streakDays: number
  streakMultiplier: number
}

// Mock API function - replace with your actual API call
const fetchUserData = async (): Promise<UserData> => {
  // Simulate API call
  await new Promise((resolve) => setTimeout(resolve, 1000))
  return {
    name: "Sarah Johnson",
    department: "Engineering",
    totalPoints: 2450,
    totalCommandExecutions: 156,
    streakDays: 12,
    streakMultiplier: 2.5,
  }
}

const drawerWidth = 240

export default function Dashboard() {
  const [mobileOpen, setMobileOpen] = useState(false)
  const [userData, setUserData] = useState<UserData | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const loadUserData = async () => {
      try {
        setLoading(true)
        const data = await fetchUserData()
        setUserData(data)
      } catch (err) {
        setError("Failed to load user data")
        console.error("API Error:", err)
      } finally {
        setLoading(false)
      }
    }

    loadUserData()
  }, [])

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen)
  }

  const drawer = (
    <div>
      <Toolbar>
        <Typography variant="h6" noWrap component="div" sx={{ fontWeight: "bold" }}>
          Dashboard
        </Typography>
      </Toolbar>
      <Divider />
      <List>
        {[
          { text: "Overview", icon: <DashboardIcon />, active: true },
          { text: "Analytics", icon: <AnalyticsIcon />, active: false },
          { text: "Profile", icon: <PersonIcon />, active: false },
          { text: "Settings", icon: <SettingsIcon />, active: false },
        ].map((item) => (
          <ListItem key={item.text} disablePadding>
            <ListItemButton selected={item.active}>
              <ListItemIcon sx={{ color: item.active ? "primary.main" : "inherit" }}>{item.icon}</ListItemIcon>
              <ListItemText primary={item.text} />
            </ListItemButton>
          </ListItem>
        ))}
      </List>
    </div>
  )

  if (loading) {
    return (
      <ThemeProvider theme={theme}>
        <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh">
          <CircularProgress size={60} />
        </Box>
      </ThemeProvider>
    )
  }

  if (error) {
    return (
      <ThemeProvider theme={theme}>
        <Container maxWidth="sm" sx={{ mt: 4 }}>
          <Alert severity="error">{error}</Alert>
        </Container>
      </ThemeProvider>
    )
  }

  const statsData = [
    {
      icon: <StarsIcon sx={{ fontSize: 40, color: "primary.main" }} />,
      title: "Total Points",
      value: userData?.totalPoints || 0,
    },
    {
      icon: <CommandIcon sx={{ fontSize: 40, color: "secondary.main" }} />,
      title: "Command Executions",
      value: userData?.totalCommandExecutions || 0,
    },
    {
      icon: <StreakIcon sx={{ fontSize: 40, color: "warning.main" }} />,
      title: "Streak Days",
      value: userData?.streakDays || 0,
    },
    {
      icon: <MultiplierIcon sx={{ fontSize: 40, color: "success.main" }} />,
      title: "Streak Multiplier",
      value: `${userData?.streakMultiplier || 0}x`,
    },
  ]

  return (
    <ThemeProvider theme={theme}>
      <Box sx={{ display: "flex" }}>
        <CssBaseline />

        {/* App Bar */}
        <AppBar
          position="fixed"
          sx={{
            width: { sm: `calc(100% - ${drawerWidth}px)` },
            ml: { sm: `${drawerWidth}px` },
          }}
        >
          <Toolbar>
            <IconButton
              color="inherit"
              aria-label="open drawer"
              edge="start"
              onClick={handleDrawerToggle}
              sx={{ mr: 2, display: { sm: "none" } }}
            >
              <MenuIcon />
            </IconButton>

            <Box sx={{ flexGrow: 1 }}>
              <Typography variant="h6" noWrap component="div">
                Welcome back, {userData?.name || "User"}
              </Typography>
              <Typography variant="body2" sx={{ opacity: 0.8 }}>
                Here's your performance overview
              </Typography>
            </Box>

            <IconButton color="inherit">
              <Badge badgeContent={4} color="secondary">
                <NotificationsIcon />
              </Badge>
            </IconButton>
          </Toolbar>
        </AppBar>

        {/* Sidebar */}
        <Box component="nav" sx={{ width: { sm: drawerWidth }, flexShrink: { sm: 0 } }}>
          <Drawer
            variant="temporary"
            open={mobileOpen}
            onClose={handleDrawerToggle}
            ModalProps={{ keepMounted: true }}
            sx={{
              display: { xs: "block", sm: "none" },
              "& .MuiDrawer-paper": { boxSizing: "border-box", width: drawerWidth },
            }}
          >
            {drawer}
          </Drawer>
          <Drawer
            variant="permanent"
            sx={{
              display: { xs: "none", sm: "block" },
              "& .MuiDrawer-paper": { boxSizing: "border-box", width: drawerWidth },
            }}
            open
          >
            {drawer}
          </Drawer>
        </Box>

        {/* Main Content */}
        <Box
          component="main"
          sx={{
            flexGrow: 1,
            p: 3,
            width: { sm: `calc(100% - ${drawerWidth}px)` },
            bgcolor: "background.default",
            minHeight: "100vh",
          }}
        >
          <Toolbar />

          <Container maxWidth="xl">
            {/* Main Content Area with 20/80 Split */}
            <Box sx={{ display: "flex", gap: 3, minHeight: "70vh" }}>
              {/* Left Side - 20% - User Info */}
              <Paper
                sx={{
                  width: "20%",
                  minWidth: "200px",
                  p: 3,
                  display: "flex",
                  flexDirection: "column",
                  justifyContent: "center",
                  alignItems: "center",
                  textAlign: "center",
                  bgcolor: "white",
                }}
              >
                <Typography variant="h4" sx={{ fontWeight: "bold", mb: 1, color: "primary.main" }}>
                  {userData?.name}
                </Typography>
                <Typography variant="body1" sx={{ color: "text.secondary", fontWeight: 300 }}>
                  {userData?.department}
                </Typography>
              </Paper>

              {/* Right Side - 80% - Stats Grid */}
              <Box sx={{ width: "80%", display: "flex", alignItems: "center" }}>
                <Grid container spacing={3} sx={{ width: "100%" }}>
                  {statsData.map((stat, index) => (
                    <Grid container xs={12} sm={6} md={6} lg={3} xl={3} key={index} component="div">
                      <Card
                        sx={{
                          height: "100%",
                          transition: "transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out",
                          "&:hover": {
                            transform: "translateY(-4px)",
                            boxShadow: "0 8px 25px rgba(0,0,0,0.15)",
                          },
                        }}
                      >
                        <CardContent sx={{ textAlign: "center", py: 4 }}>
                          <Grid container direction="column" alignItems="center" spacing={2}>
                            <Grid item>{stat.icon}</Grid>
                            <Grid item>
                              <Typography variant="h6" sx={{ fontWeight: "bold", color: "text.primary" }}>
                                {stat.title}
                              </Typography>
                            </Grid>
                            <Grid item>
                              <Typography variant="h4" sx={{ fontWeight: "normal", color: "text.secondary" }}>
                                {stat.value}
                              </Typography>
                            </Grid>
                          </Grid>
                        </CardContent>
                      </Card>
                    </Grid>
                  ))}
                </Grid>
              </Box>
            </Box>
          </Container>
        </Box>
      </Box>
    </ThemeProvider>
  )
}
