import React, { useState, useEffect } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import {
  Box, Drawer, List, ListItem, ListItemButton, ListItemIcon, ListItemText, Typography,
  IconButton, Dialog, Button, useTheme, useMediaQuery, AppBar, Toolbar, Badge, Divider
} from '@mui/material';
import {
  Dashboard as DashboardIcon,
  People as UsersIcon,
  Description as ReportsIcon,
  Subscriptions as SubscriptionsIcon,
  Menu as MenuIcon,
  Logout as LogoutIcon,
  Notifications as BellIcon,
  Close as CloseIcon,
} from '@mui/icons-material';
import { collection, query, onSnapshot, orderBy, limit } from 'firebase/firestore';
import { db } from '../firebase';

const drawerWidth = 240;
const SIDEBAR_GREEN = '#1a5c2a';

export default function DashboardLayout() {
  const navigate = useNavigate();
  const location = useLocation();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [mobileOpen, setMobileOpen] = useState(false);
  const [logoutOpen, setLogoutOpen] = useState(false);
  const [notifOpen, setNotifOpen] = useState(false);
  const [notifications, setNotifications] = useState([]);
  const [lastReadTime, setLastReadTime] = useState(() => {
    return parseInt(localStorage.getItem('notif_last_read') || '0', 10);
  });

  const unreadCount = notifications.filter(n => {
    const ts = n.timestamp?.toDate ? n.timestamp.toDate().getTime() : 0;
    return ts > lastReadTime;
  }).length;

  const handleOpenNotif = () => {
    setNotifOpen(true);
  };

  const handleMarkAllAsRead = () => {
    const now = Date.now();
    setLastReadTime(now);
    localStorage.setItem('notif_last_read', String(now));
  };

  const menuItems = [
    { text: 'Dashboard', path: '/dashboard', icon: <DashboardIcon /> },
    { text: 'Users', path: '/users', icon: <UsersIcon /> },
    { text: 'Reports', path: '/reports', icon: <ReportsIcon /> },
    { text: 'Subscribers', path: '/subscriptions', icon: <SubscriptionsIcon /> },
  ];

  useEffect(() => {
    const q = query(collection(db, 'activity_logs'), orderBy('timestamp', 'desc'), limit(15));
    const unsub = onSnapshot(q, (snap) => {
      setNotifications(snap.docs.map(d => ({ id: d.id, ...d.data() })));
    }, (err) => console.error("Notif error:", err));
    return () => unsub();
  }, []);

  const handleDrawerToggle = () => setMobileOpen(!mobileOpen);

  const sidebarContent = (
    <Box sx={{ height: '100%', display: 'flex', flexDirection: 'column', bgcolor: SIDEBAR_GREEN, color: 'white' }}>
      <Box sx={{ p: 3, display: 'flex', alignItems: 'center', gap: 2 }}>
        <Box sx={{ width: 32, height: 32, bgcolor: '#f9a825', borderRadius: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>🐝</Box>
        <Typography variant="h6" sx={{ fontWeight: 800 }}>AgriHive</Typography>
      </Box>
      <List sx={{ px: 2, flexGrow: 1 }}>
        {menuItems.map((item) => {
          const active = location.pathname === item.path;
          return (
            <ListItem key={item.text} disablePadding>
              <ListItemButton
                onClick={() => { navigate(item.path); if (isMobile) setMobileOpen(false); }}
                sx={{
                  borderRadius: 2, mb: 0.5,
                  bgcolor: active ? 'rgba(255,255,255,0.15)' : 'transparent',
                  '&:hover': { bgcolor: 'rgba(255,255,255,0.1)' }
                }}
              >
                <ListItemIcon sx={{ color: 'white', minWidth: 40 }}>{item.icon}</ListItemIcon>
                <ListItemText primary={item.text} primaryTypographyProps={{ fontWeight: active ? 700 : 500 }} />
              </ListItemButton>
            </ListItem>
          );
        })}
      </List>
      <Box sx={{ p: 2 }}>
        <Button fullWidth startIcon={<LogoutIcon />}
          sx={{ color: 'white', justifyContent: 'flex-start', px: 2 }}
          onClick={() => setLogoutOpen(true)}
        >
          Logout
        </Button>
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: '#f4f6f8' }}>
      {/* Top AppBar */}
      <AppBar position="fixed" sx={{
        bgcolor: 'white', color: 'text.primary',
        boxShadow: 'none', borderBottom: '1px solid #eee',
        zIndex: theme.zIndex.drawer + 1
      }}>
        <Toolbar sx={{ justifyContent: 'space-between', minHeight: { xs: 56, sm: 64 } }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            {isMobile && (
              <IconButton color="inherit" onClick={handleDrawerToggle} edge="start">
                <MenuIcon />
              </IconButton>
            )}
            <Typography variant="h6" noWrap sx={{ fontWeight: 700, color: SIDEBAR_GREEN, fontSize: { xs: '1rem', sm: '1.25rem' } }}>
              AgriHive Admin
            </Typography>
          </Box>
          <IconButton onClick={handleOpenNotif}>
            <Badge badgeContent={unreadCount} color="error">
              <BellIcon />
            </Badge>
          </IconButton>
        </Toolbar>
      </AppBar>

      {/* Sidebar */}
      <Box component="nav" sx={{ width: { md: drawerWidth }, flexShrink: { md: 0 } }}>
        <Drawer
          variant="temporary" open={mobileOpen} onClose={handleDrawerToggle}
          ModalProps={{ keepMounted: true }}
          sx={{
            display: { xs: 'block', md: 'none' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth, border: 'none' },
          }}
        >
          {sidebarContent}
        </Drawer>
        <Drawer
          variant="permanent"
          sx={{
            display: { xs: 'none', md: 'block' },
            '& .MuiDrawer-paper': { boxSizing: 'border-box', width: drawerWidth, border: 'none' },
          }}
          open
        >
          {sidebarContent}
        </Drawer>
      </Box>

      {/* Main Content */}
      <Box component="main" sx={{
        flexGrow: 1,
        p: { xs: 2, md: 4 },
        width: { xs: '100%', md: `calc(100% - ${drawerWidth}px)` },
        mt: { xs: '56px', sm: '64px' },
        pb: { xs: 2, md: 4 },
        overflowX: 'hidden',
      }}>
        <Outlet />
      </Box>

      {/* Notifications Drawer */}
      <Drawer
        anchor="right" open={notifOpen} onClose={() => setNotifOpen(false)}
        sx={{
          '& .MuiDrawer-paper': { width: { xs: '100%', sm: 360 } },
          zIndex: theme.zIndex.modal + 1
        }}
      >
        <Box sx={{ p: 2, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Typography variant="h6" sx={{ fontWeight: 700, px: 1 }}>Activity Logs</Typography>
          <Box sx={{ display: 'flex', gap: 1 }}>
            {unreadCount > 0 && (
              <Button
                size="small"
                onClick={handleMarkAllAsRead}
                sx={{ textTransform: 'none', fontSize: 12, color: SIDEBAR_GREEN }}
              >
                Mark all read
              </Button>
            )}
            <IconButton onClick={() => setNotifOpen(false)}><CloseIcon /></IconButton>
          </Box>
        </Box>
        <Divider />
        <List sx={{ p: 0 }}>
          {notifications.length === 0 ? (
            <Typography sx={{ p: 4, textAlign: 'center', color: 'text.secondary' }}>No recent activity</Typography>
          ) : notifications.map((n) => {
            const ts = n.timestamp?.toDate ? n.timestamp.toDate().getTime() : 0;
            const isUnread = ts > lastReadTime;
            return (
              <ListItem key={n.id} sx={{
                borderBottom: '1px solid #f0f0f0',
                flexDirection: 'column', alignItems: 'flex-start', py: 2,
                bgcolor: isUnread ? '#f1f8e9' : 'transparent',
              }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, width: '100%' }}>
                  {isUnread && <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: '#43a047', flexShrink: 0 }} />}
                  <Typography variant="subtitle2" sx={{ fontWeight: 700, color: SIDEBAR_GREEN }}>{n.action}</Typography>
                </Box>
                <Typography variant="body2" sx={{ my: 0.5, pl: isUnread ? 2.5 : 0 }}>
                  {n.user} {n.target ? `→ ${n.target}` : ''}
                </Typography>
                <Typography variant="caption" sx={{ color: 'text.secondary', pl: isUnread ? 2.5 : 0 }}>
                  {n.timestamp?.toDate ? n.timestamp.toDate().toLocaleString() : 'Just now'}
                </Typography>
              </ListItem>
            );
          })}
        </List>
      </Drawer>

      {/* Logout Dialog */}
      <Dialog open={logoutOpen} onClose={() => setLogoutOpen(false)}
        PaperProps={{ sx: { borderRadius: 3, p: 2, mx: 2 } }}
      >
        <Typography variant="h6" sx={{ mb: 2, fontWeight: 700 }}>Confirm Logout</Typography>
        <Typography sx={{ mb: 3 }}>Are you sure you want to exit the admin panel?</Typography>
        <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
          <Button onClick={() => setLogoutOpen(false)} color="inherit">Cancel</Button>
          <Button variant="contained" color="error" onClick={() => navigate('/login')}>Logout</Button>
        </Box>
      </Dialog>
    </Box>
  );
}
