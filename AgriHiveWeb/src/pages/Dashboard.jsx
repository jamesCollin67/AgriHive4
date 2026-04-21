import React, { useState } from 'react';
import { Box, Typography, Paper, Chip, CircularProgress, IconButton, Divider } from '@mui/material';
import { useNavigate } from 'react-router-dom';
import {
  LocationOn as MapPinIcon,
  Person as UserIcon,
  Layers as LayersIcon,
  CheckCircle as CheckCircleIcon,
  ErrorOutline as AlertCircleIcon,
  ChevronRight as ChevronRightIcon,
  ArrowBack as ArrowBackIcon,
  Thermostat as ThermostatIcon,
  WaterDrop as HumidityIcon,
  FitnessCenter as WeightIcon,
  Lock as LockIcon,
  LockOpen as LockOpenIcon,
} from '@mui/icons-material';
import { useAdminData } from '../context/AdminDataContext';

const GREEN = '#1a5c2a';

function SensorCard({ icon, label, value, unit, status, statusColor, online }) {
  return (
    <Paper elevation={0} sx={{
      border: '1px solid rgba(0,0,0,0.09)', borderRadius: 3, p: 2,
      display: 'flex', flexDirection: 'column', gap: 0.5, minWidth: 130,
    }}>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, color: '#43a047' }}>
        {icon}
        <Typography sx={{ fontSize: 12, color: '#888' }}>{label}</Typography>
      </Box>
      <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 0.5, mt: 0.5 }}>
        <Typography sx={{ fontSize: 28, fontWeight: 800, lineHeight: 1, color: online ? '#111' : '#bbb' }}>
          {online ? value : '--'}
        </Typography>
        {online && unit && (
          <Typography sx={{ fontSize: 13, color: '#888' }}>{unit}</Typography>
        )}
      </Box>
      <Typography sx={{ fontSize: 12, fontWeight: 700, color: online ? statusColor : '#bbb' }}>
        {online ? status : 'No Data'}
      </Typography>
    </Paper>
  );
}

function HiveDetailPanel({ farm, apiaries, onBack }) {
  const hives = apiaries.filter(a => a.ownerId === farm.id);

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 3 }}>
        <IconButton onClick={onBack} size="small" sx={{ color: GREEN }}>
          <ArrowBackIcon />
        </IconButton>
        <Box>
          <Typography variant="h6" fontWeight={700}>
            {farm.farmName || farm.farm || 'Unnamed Farm'}
          </Typography>
          <Typography sx={{ fontSize: 12, color: '#888' }}>
            {farm.firstName} {farm.lastName} · {farm.farmLocation || farm.location || 'Unknown location'}
          </Typography>
        </Box>
      </Box>

      {hives.length === 0 ? (
        <Paper elevation={0} sx={{
          p: 6, textAlign: 'center',
          border: '1px solid rgba(0,0,0,0.08)', borderRadius: 3,
        }}>
          <Typography sx={{ fontSize: 36, mb: 1 }}>🐝</Typography>
          <Typography fontWeight={700} color="text.secondary">No hives registered</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            This farm has no hives linked yet. The beekeeper needs to add apiaries from the mobile app.
          </Typography>
        </Paper>
      ) : (
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {hives.map(hive => {
            const online = hive.isConnected === true;
            const lidOpen = (hive.moisture ?? 0) >= 5;
            const temp = hive.temperature ?? 0;
            const hum = hive.humidity ?? 0;
            const wt = hive.weight ?? 0;

            const tempStatus = !online ? 'No Data' : temp < 34 ? 'Too Cold' : temp > 36 ? 'Too Hot' : 'Normal';
            const tempColor  = tempStatus === 'Normal' ? '#43a047' : tempStatus === 'No Data' ? '#bbb' : '#e53935';
            const humStatus  = !online ? 'No Data' : hum < 50 ? 'Too Dry' : hum > 80 ? 'Too Humid' : 'Normal';
            const humColor   = humStatus === 'Normal' ? '#43a047' : humStatus === 'No Data' ? '#bbb' : '#e53935';
            const wtStatus   = !online ? 'No Data' : wt <= 0 ? 'No Data' : wt < 5 ? 'Low — Check Hive' : 'Normal';
            const wtColor    = wtStatus === 'Normal' ? '#43a047' : wtStatus === 'No Data' ? '#bbb' : '#e53935';
            const lidStatus  = !online ? 'No Data' : lidOpen ? '⚠ Check Hive' : 'Secure';
            const lidColor   = !online ? '#bbb' : lidOpen ? '#FF9800' : '#43a047';

            const lastUpdated = hive.lastUpdate
              ? new Date(hive.lastUpdate?.seconds ? hive.lastUpdate.seconds * 1000 : hive.lastUpdate)
                  .toLocaleTimeString()
              : '—';

            return (
              <Paper key={hive.id} elevation={0} sx={{
                border: `1px solid ${online ? 'rgba(67,160,71,0.3)' : 'rgba(0,0,0,0.09)'}`,
                borderRadius: 3, p: { xs: 2, md: 2.5 },
                bgcolor: online ? '#f9fdf9' : 'white',
              }}>
                {/* Hive header */}
                <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                  <Box>
                    <Typography fontWeight={700} sx={{ fontSize: 15 }}>{hive.name}</Typography>
                    <Typography sx={{ fontSize: 11, color: '#888' }}>Node: {hive.nodeId || '—'}</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Box sx={{
                      width: 8, height: 8, borderRadius: '50%',
                      bgcolor: online ? '#43a047' : '#e53935',
                    }} />
                    <Typography sx={{ fontSize: 12, fontWeight: 600, color: online ? '#43a047' : '#e53935' }}>
                      {online ? 'Online' : 'Offline'}
                    </Typography>
                    <Typography sx={{ fontSize: 11, color: '#bbb', ml: 1 }}>
                      Last: {lastUpdated}
                    </Typography>
                  </Box>
                </Box>

                {/* Sensor grid */}
                <Box sx={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(130px, 1fr))',
                  gap: 1.5,
                }}>
                  <SensorCard
                    icon={<ThermostatIcon sx={{ fontSize: 16 }} />}
                    label="Temperature"
                    value={temp.toFixed(1)}
                    unit="°C"
                    status={tempStatus}
                    statusColor={tempColor}
                    online={online}
                  />
                  <SensorCard
                    icon={<HumidityIcon sx={{ fontSize: 16 }} />}
                    label="Humidity"
                    value={hum.toFixed(1)}
                    unit="%"
                    status={humStatus}
                    statusColor={humColor}
                    online={online}
                  />
                  <SensorCard
                    icon={<WeightIcon sx={{ fontSize: 16 }} />}
                    label="Weight"
                    value={wt.toFixed(1)}
                    unit="kg"
                    status={wtStatus}
                    statusColor={wtColor}
                    online={online}
                  />
                  <SensorCard
                    icon={lidOpen
                      ? <LockOpenIcon sx={{ fontSize: 16, color: '#FF9800' }} />
                      : <LockIcon sx={{ fontSize: 16 }} />}
                    label="Hive Lid"
                    value={lidOpen ? 'OPEN' : 'CLOSED'}
                    unit=""
                    status={lidStatus}
                    statusColor={lidColor}
                    online={online}
                  />
                </Box>
              </Paper>
            );
          })}
        </Box>
      )}
    </Box>
  );
}

export default function Dashboard() {
  const navigate = useNavigate();
  const { users: farms, usersLoading: loading, reports, pendingReportFarmKeys, apiaries } = useAdminData();
  const [selectedFarm, setSelectedFarm] = useState(null);

  const totalFarms     = farms.length;
  const activeFarms    = farms.filter(f => f.active !== false).length;
  const totalHives     = farms.reduce((sum, f) => {
    const h = parseInt(f.hiveCount || f.numHives || 0, 10);
    return sum + (isNaN(h) ? 0 : h);
  }, 0);
  const pendingReports = reports.filter(r => r.unread).length;
  const inactiveFarms  = farms.filter(f => f.active === false).length;
  const alertCount     = inactiveFarms + pendingReportFarmKeys.size;

  const statCards = [
    {
      label: 'Total Farms', value: totalFarms,
      gradient: 'linear-gradient(135deg, #1565c0 0%, #0d47a1 100%)', icon: '🐝',
    },
    {
      label: 'Active Farms', value: activeFarms,
      gradient: 'linear-gradient(135deg, #43a047 0%, #2e7d32 100%)',
      icon: <CheckCircleIcon sx={{ color: 'white', fontSize: 28 }} />,
    },
    {
      label: 'Total Hives', value: totalHives,
      gradient: 'linear-gradient(135deg, #ffa726 0%, #f57c00 100%)',
      icon: <LayersIcon sx={{ color: 'white', fontSize: 28 }} />,
    },
    {
      label: 'Pending Reports', value: pendingReports,
      gradient: pendingReports > 0
        ? 'linear-gradient(135deg, #e53935 0%, #c62828 100%)'
        : 'linear-gradient(135deg, #78909c 0%, #546e7a 100%)',
      icon: '📋',
      onClick: () => navigate('/reports'),
    },
  ];

  // Show hive detail panel when a farm is selected
  if (selectedFarm) {
    return (
      <HiveDetailPanel
        farm={selectedFarm}
        apiaries={apiaries}
        onBack={() => setSelectedFarm(null)}
      />
    );
  }

  return (
    <Box>
      <Box sx={{ mb: { xs: 2, md: 3 } }}>
        <Typography variant="h4" fontWeight={700} sx={{ fontSize: { xs: '1.5rem', md: '2.125rem' } }}>
          Dashboard
        </Typography>
        <Typography sx={{ color: '#888', fontSize: { xs: 12, md: 14 }, mt: 0.3 }}>
          Overview of all registered bee farms
        </Typography>
      </Box>

      {/* Stat Cards */}
      <Box sx={{
        display: 'grid',
        gridTemplateColumns: { xs: '1fr 1fr', md: 'repeat(4, 1fr)' },
        gap: { xs: 1.5, md: 2.5 }, mb: 3,
      }}>
        {statCards.map((card, i) => (
          <Box key={i} onClick={card.onClick} sx={{
            borderRadius: 3, p: { xs: 2, md: 3 },
            background: card.gradient, color: 'white',
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            minHeight: { xs: 80, md: 110 },
            cursor: card.onClick ? 'pointer' : 'default',
            transition: 'transform 0.15s, box-shadow 0.15s',
            '&:hover': card.onClick ? { transform: 'translateY(-2px)', boxShadow: '0 8px 24px rgba(0,0,0,0.18)' } : {},
          }}>
            <Box>
              <Typography sx={{ fontSize: { xs: 10, md: 13 }, fontWeight: 600, opacity: 0.85, mb: 0.5 }}>
                {card.label}
              </Typography>
              <Typography sx={{ fontSize: { xs: 28, md: 44 }, fontWeight: 800, lineHeight: 1 }}>
                {loading ? '—' : card.value}
              </Typography>
            </Box>
            <Box sx={{
              width: { xs: 36, md: 46 }, height: { xs: 36, md: 46 }, borderRadius: 2,
              bgcolor: 'rgba(255,255,255,0.2)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: { xs: 18, md: 24 },
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

      <Box sx={{ mb: 2 }}>
        <Typography sx={{ fontWeight: 700, fontSize: 15, color: '#444', mb: 1.5 }}>
          Registered Farms
        </Typography>
      </Box>

      {loading ? (
        <Box sx={{ textAlign: 'center', py: 6 }}>
          <CircularProgress size={32} sx={{ color: '#2e7d32' }} />
          <Typography color="text.secondary" sx={{ mt: 2, fontSize: 14 }}>Loading farms...</Typography>
        </Box>
      ) : farms.length === 0 ? (
        <Paper elevation={0} sx={{
          p: { xs: 4, md: 8 }, textAlign: 'center',
          border: '1px solid rgba(0,0,0,0.08)', borderRadius: 3,
        }}>
          <Typography sx={{ fontSize: 40, mb: 1 }}>🐝</Typography>
          <Typography variant="h6" color="text.secondary" fontWeight={700}>No Farms Registered</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            Registered beekeepers will appear here once they sign up from the mobile app.
          </Typography>
        </Paper>
      ) : (
        <Box sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', sm: 'repeat(auto-fill, minmax(280px, 1fr))' },
          gap: { xs: 2, md: 2.5 },
        }}>
          {farms.map(farm => {
            const isActive = farm.active !== false;
            const hasPendingReport =
              pendingReportFarmKeys.has(farm.farmName) ||
              pendingReportFarmKeys.has(farm.name) ||
              pendingReportFarmKeys.has(farm.id);
            const farmHives = apiaries.filter(a => a.ownerId === farm.id);
            const onlineHives = farmHives.filter(a => a.isConnected === true).length;

            return (
              <Paper key={farm.id} elevation={0}
                onClick={() => setSelectedFarm(farm)}
                sx={{
                  border: '1px solid rgba(0,0,0,0.09)',
                  borderRadius: 3, p: { xs: 2, md: 2.5 },
                  bgcolor: 'white', position: 'relative',
                  cursor: 'pointer',
                  '&:hover': { boxShadow: '0 6px 24px rgba(0,0,0,0.09)', transform: 'translateY(-2px)' },
                  transition: 'all 0.2s',
                }}
              >
                {hasPendingReport && (
                  <Box sx={{
                    position: 'absolute', top: 14, right: 44,
                    width: 10, height: 10, borderRadius: '50%',
                    bgcolor: '#e53935',
                    boxShadow: '0 0 0 2px white, 0 0 0 3px #e5393533',
                  }} />
                )}
                <Box sx={{ position: 'absolute', top: '50%', right: 14, transform: 'translateY(-50%)' }}>
                  <ChevronRightIcon sx={{ fontSize: 18, color: '#ccc' }} />
                </Box>

                <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.5, mb: 2 }}>
                  <Box sx={{
                    width: 42, height: 42, borderRadius: 2, flexShrink: 0,
                    bgcolor: '#fff8e1', display: 'flex', alignItems: 'center',
                    justifyContent: 'center', fontSize: 20, border: '1px solid #ffe082',
                  }}>
                    🐝
                  </Box>
                  <Box sx={{ pr: 3 }}>
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

                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1, mb: 2 }}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <MapPinIcon sx={{ fontSize: 16, color: '#43a047' }} />
                    <Typography sx={{ fontSize: 12, color: '#555' }}>
                      {farm.farmLocation || farm.location || 'Unknown location'}
                    </Typography>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <UserIcon sx={{ fontSize: 16, color: '#43a047' }} />
                    <Typography sx={{ fontSize: 12, color: '#555' }}>{farm.email || 'No email'}</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <LayersIcon sx={{ fontSize: 16, color: '#43a047' }} />
                    <Typography sx={{ fontSize: 12, color: '#555' }}>
                      {farmHives.length} hive{farmHives.length !== 1 ? 's' : ''}
                      {farmHives.length > 0 && (
                        <span style={{ color: onlineHives > 0 ? '#43a047' : '#bbb', marginLeft: 6 }}>
                          · {onlineHives} online
                        </span>
                      )}
                    </Typography>
                  </Box>
                </Box>

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
