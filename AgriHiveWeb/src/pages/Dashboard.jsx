import React, { useState, useEffect } from 'react';
import { Box, Typography, Paper, Chip, CircularProgress } from '@mui/material';
import {
  LocationOn as MapPinIcon,
  Person as UserIcon,
  Layers as LayersIcon,
  CheckCircle as CheckCircleIcon,
  ErrorOutline as AlertCircleIcon
} from '@mui/icons-material';
import { collection, onSnapshot } from 'firebase/firestore';
import { db } from '../firebase';

export default function Dashboard() {
  const [farms, setFarms] = useState([]);
  const [pendingReportFarms, setPendingReportFarms] = useState(new Set());
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Stream all registered users (each user = a beekeeper with a farm)
    const unsubFarms = onSnapshot(collection(db, 'users'), (snap) => {
      const data = snap.docs.map(d => ({ id: d.id, ...d.data() }));
      setFarms(data);
      setLoading(false);
    }, (error) => {
      console.error("Farms listener error:", error);
      setLoading(false);
    });

    // Stream reports to know which farms have pending/unread reports
    const unsubReports = onSnapshot(collection(db, 'reports'), (snap) => {
      const farmNamesWithAlerts = new Set();
      snap.docs.forEach(d => {
        const data = d.data();
        if (data.unread === true || data.status === 'pending') {
          const farmKey = data.farm || data.farmName || data.userId;
          if (farmKey) farmNamesWithAlerts.add(farmKey);
        }
      });
      setPendingReportFarms(farmNamesWithAlerts);
    }, (error) => {
      console.error("Reports listener error:", error);
    });

    return () => { unsubFarms(); unsubReports(); };
  }, []);

  const totalFarms = farms.length;
  const activeFarms = farms.filter(f => f.active !== false).length;
  const totalHives = farms.reduce((sum, f) => {
    const h = parseInt(f.hiveCount || f.numHives || 0, 10);
    return sum + (isNaN(h) ? 0 : h);
  }, 0);

  // Alert = inactive farms + farms with pending reports
  const inactiveFarms = farms.filter(f => f.active === false).length;
  const alertCount = inactiveFarms + pendingReportFarms.size;

  const statCards = [
    {
      label: 'Total Farms',
      value: totalFarms,
      gradient: 'linear-gradient(135deg, #43a047 0%, #2e7d32 100%)',
      icon: '🐝',
    },
    {
      label: 'Active Farms',
      value: activeFarms,
      gradient: 'linear-gradient(135deg, #43a047 0%, #2e7d32 100%)',
      icon: <CheckCircleIcon sx={{ color: 'white', fontSize: 28 }} />,
    },
    {
      label: 'Total Hives',
      value: totalHives,
      gradient: 'linear-gradient(135deg, #ffa726 0%, #f57c00 100%)',
      icon: <LayersIcon sx={{ color: 'white', fontSize: 28 }} />,
    },
  ];

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: { xs: 2, md: 3 } }}>
        <Typography variant="h4" fontWeight={700} sx={{ fontSize: { xs: '1.5rem', md: '2.125rem' } }}>Dashboard</Typography>
        <Typography sx={{ color: '#888', fontSize: { xs: 12, md: 14 }, mt: 0.3 }}>
          Overview of all registered bee farms
        </Typography>
      </Box>

      {/* Stat Cards Row */}
      <Box sx={{
        display: 'grid',
        gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', lg: 'repeat(3, 1fr)' },
        gap: { xs: 2, md: 2.5 },
        mb: 3
      }}>
        {statCards.map((card, i) => (
          <Box
            key={i}
            sx={{
              borderRadius: 3, p: { xs: 2, md: 3 },
              background: card.gradient,
              color: 'white',
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              minHeight: { xs: 90, md: 110 },
            }}
          >
            <Box>
              <Typography sx={{ fontSize: { xs: 11, md: 13 }, fontWeight: 600, opacity: 0.85, mb: 0.5 }}>
                {card.label}
              </Typography>
              <Typography sx={{ fontSize: { xs: 32, md: 44 }, fontWeight: 800, lineHeight: 1 }}>
                {loading ? '—' : card.value}
              </Typography>
            </Box>
            <Box sx={{
              width: 46, height: 46, borderRadius: 2,
              bgcolor: 'rgba(255,255,255,0.2)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 24,
            }}>
              {card.icon}
            </Box>
          </Box>
        ))}
      </Box>

      {/* Alert Banner */}
      {alertCount > 0 && (
        <Box sx={{
          display: 'flex', alignItems: 'center', gap: 1.5,
          bgcolor: '#fff5f5', border: '1px solid #fce4e4',
          borderRadius: 2, px: 2.5, py: 1.5, mb: 3,
        }}>
          <AlertCircleIcon sx={{ color: '#e53935', fontSize: 20 }} />
          <Typography sx={{ fontSize: { xs: 12, md: 14 }, color: '#c62828', fontWeight: 500 }}>
            {alertCount} farm(s) have active alerts requiring attention.
          </Typography>
        </Box>
      )}

      {/* Farm Cards Grid */}
      {loading ? (
        <Box sx={{ textAlign: 'center', py: 6 }}>
          <CircularProgress size={32} sx={{ color: '#2e7d32' }} />
          <Typography color="text.secondary" sx={{ mt: 2, fontSize: 14 }}>
            Loading farms...
          </Typography>
        </Box>
      ) : farms.length === 0 ? (
        <Paper elevation={0} sx={{ p: { xs: 4, md: 8 }, textAlign: 'center', border: '1px solid rgba(0,0,0,0.08)', borderRadius: 3 }}>
          <Typography variant="h6" color="text.secondary" fontWeight={700}>No Farms Registered</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            Registered beekeepers will appear here once they sign up from the mobile app.
          </Typography>
        </Paper>
      ) : (
        <Box sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', sm: 'repeat(auto-fill, minmax(280px, 1fr))' },
          gap: { xs: 2, md: 2.5 }
        }}>
          {farms.map(farm => {
            const isActive = farm.active !== false;
            // Red dot if farm has a pending report
            const hasPendingReport = pendingReportFarms.has(farm.farmName) ||
              pendingReportFarms.has(farm.name) ||
              pendingReportFarms.has(farm.id);

            return (
              <Paper
                key={farm.id}
                elevation={0}
                sx={{
                  border: '1px solid rgba(0,0,0,0.09)',
                  borderRadius: 3, p: { xs: 2, md: 2.5 },
                  bgcolor: 'white',
                  position: 'relative',
                  '&:hover': { boxShadow: '0 6px 24px rgba(0,0,0,0.09)', transform: 'translateY(-1px)' },
                  transition: 'all 0.2s',
                }}
              >
                {/* Red dot for pending report alert */}
                {hasPendingReport && (
                  <Box sx={{
                    position: 'absolute', top: 14, right: 14,
                    width: 10, height: 10, borderRadius: '50%',
                    bgcolor: '#e53935',
                    boxShadow: '0 0 0 2px white, 0 0 0 3px #e5393533',
                  }} />
                )}

                {/* Farm Header: Bee icon + Farm Name */}
                <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.5, mb: 2.5 }}>
                  <Box sx={{
                    width: 42, height: 42, borderRadius: 2, flexShrink: 0,
                    bgcolor: '#fff8e1',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: 20, border: '1px solid #ffe082',
                  }}>
                    🐝
                  </Box>
                  <Box>
                    <Typography sx={{ fontWeight: 700, fontSize: 15, lineHeight: 1.3 }}>
                      {farm.farmName || farm.farm || 'Unnamed Farm'}
                    </Typography>
                    <Typography sx={{ fontSize: 11, color: '#bbb', lineHeight: 1.3 }}>
                      {farm.firstName && farm.lastName
                        ? `${farm.firstName} ${farm.lastName}`
                        : farm.firstName || farm.lastName || farm.email || 'Unknown beekeeper'}
                    </Typography>
                  </Box>
                </Box>

                {/* Info Rows */}
                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.2, mb: 2.5 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <MapPinIcon sx={{ fontSize: 18, color: '#43a047' }} />
                    <Typography sx={{ fontSize: 13, color: '#444' }}>
                      {farm.farmLocation || farm.location || 'Unknown location'}
                    </Typography>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <UserIcon sx={{ fontSize: 18, color: '#43a047' }} />
                    <Typography sx={{ fontSize: 13, color: '#444' }}>
                      {farm.email || 'No email provided'}
                    </Typography>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <LayersIcon sx={{ fontSize: 18, color: '#43a047' }} />
                    <Typography sx={{ fontSize: 13, color: '#444' }}>
                      {farm.hiveCount || farm.numHives || 0} Active Hives
                    </Typography>
                  </Box>
                </Box>

                {/* Status Chip */}
                <Chip
                  label={isActive ? '• Active' : '• Inactive'}
                  size="small"
                  sx={{
                    bgcolor: isActive ? '#e8f5e9' : '#f5f5f5',
                    color: isActive ? '#2e7d32' : '#757575',
                    fontWeight: 600, fontSize: 12,
                    '& .MuiChip-label': { px: 1.5 },
                  }}
                />
              </Paper>
            );
          })}
        </Box>
      )}
    </Box>
  );
}
