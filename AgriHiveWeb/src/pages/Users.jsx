import React, { useState, useEffect } from 'react';
import { Box, Typography, Avatar, Chip, IconButton, InputBase, Paper, Divider, useTheme, useMediaQuery, CircularProgress } from '@mui/material';
import {
  Search as SearchIcon,
  ChevronRight as ChevronRightIcon,
  LocationOn as MapPinIcon,
  Layers as LayersIcon,
  Email as MailIcon,
  ArrowBack as ArrowLeftIcon
} from '@mui/icons-material';
import { collection, query, onSnapshot, doc, updateDoc, deleteDoc, addDoc, serverTimestamp } from 'firebase/firestore';
import { db } from '../firebase';

const GREEN = '#1a5c2a';

function getInitial(name) {
  return (name || 'U')[0].toUpperCase();
}

function getAvatarColor(name) {
  const colors = ['#43a047', '#1565c0', '#f57c00', '#6a1b9a', '#00838f', '#c62828'];
  let sum = 0;
  for (let c of (name || 'U')) sum += c.charCodeAt(0);
  return colors[sum % colors.length];
}

export default function Users() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [users, setUsers] = useState([]);
  const [search, setSearch] = useState('');
  const [selectedUser, setSelectedUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [actionMsg, setActionMsg] = useState('');

  useEffect(() => {
    const q = query(collection(db, 'users'));
    const unsub = onSnapshot(q, (snapshot) => {
      setUsers(snapshot.docs.map(d => ({ id: d.id, ...d.data() })));
      setLoading(false);
    }, (error) => {
      console.error("Users listener error:", error);
      setLoading(false);
    });
    return () => unsub();
  }, []);

  const filtered = users.filter(u =>
    (u.name || '').toLowerCase().includes(search.toLowerCase()) ||
    (u.email || '').toLowerCase().includes(search.toLowerCase()) ||
    (u.farmName || '').toLowerCase().includes(search.toLowerCase())
  );

  const handleDeactivate = async () => {
    try {
      await updateDoc(doc(db, 'users', selectedUser.id), { active: false });
      await addDoc(collection(db, 'activity_logs'), {
        action: 'Account Suspended', user: 'Admin', target: selectedUser.email, status: 'Warning', timestamp: serverTimestamp()
      });
      setActionMsg('Account deactivated successfully.');
      setTimeout(() => { setActionMsg(''); setSelectedUser(null); }, 2000);
    } catch (e) { console.error(e); }
  };

  const handleDelete = async () => {
    try {
      await deleteDoc(doc(db, 'users', selectedUser.id));
      await addDoc(collection(db, 'activity_logs'), {
        action: 'Account Deleted', user: 'Admin', target: selectedUser.email, status: 'Error', timestamp: serverTimestamp()
      });
      setActionMsg('Account deleted permanently.');
      setTimeout(() => { setActionMsg(''); setSelectedUser(null); }, 2000);
    } catch (e) { console.error(e); }
  };

  // ---- DETAIL VIEW ----
  if (selectedUser) {
    const isActive = selectedUser.active !== false;
    return (
      <Box>
        {/* Back Button */}
        <Box
          onClick={() => { setSelectedUser(null); setActionMsg(''); }}
          sx={{ display: 'flex', alignItems: 'center', gap: 0.5, cursor: 'pointer', color: GREEN, mb: 3, width: 'fit-content' }}
        >
          <ArrowLeftIcon sx={{ fontSize: 18 }} />
          <Typography sx={{ fontSize: 14, fontWeight: 600, color: GREEN }}>Back to Users</Typography>
        </Box>

        {actionMsg ? (
          <Paper elevation={0} sx={{ p: 6, textAlign: 'center', borderRadius: 4, border: '1px solid rgba(0,0,0,0.07)' }}>
            <Typography variant="h6" color="success.main" fontWeight={700}>{actionMsg}</Typography>
          </Paper>
        ) : (
          <Paper elevation={0} sx={{
            maxWidth: 520, mx: 'auto', borderRadius: 4,
            border: '1px solid rgba(0,0,0,0.08)', overflow: 'hidden', bgcolor: 'white'
          }}>
            {/* Avatar + Name */}
            <Box sx={{ pt: 5, pb: 3, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <Avatar sx={{ width: 72, height: 72, bgcolor: '#e8f5e9', border: '2px solid #c8e6c9', mb: 2 }}>
                <span style={{ fontSize: 32 }}>🧑</span>
              </Avatar>
              <Typography variant="h6" fontWeight={700}>
                {selectedUser.firstName && selectedUser.lastName
                  ? `${selectedUser.firstName} ${selectedUser.lastName}`
                  : selectedUser.firstName || selectedUser.lastName || selectedUser.email || 'No name'}
              </Typography>
              <Chip
                label={isActive ? 'Active' : 'Inactive'}
                size="small"
                sx={{
                  mt: 0.5,
                  bgcolor: isActive ? '#e8f5e9' : '#f5f5f5',
                  color: isActive ? '#2e7d32' : '#777',
                  fontWeight: 600, fontSize: 12
                }}
              />
            </Box>

            <Divider />

            {/* Info Rows */}
            <Box sx={{ px: 3, py: 2 }}>
              {[
                { icon: '🐝', label: 'Farm Name', value: selectedUser.farmName || selectedUser.farm || 'Not set' },
                { icon: <MapPinIcon sx={{ fontSize: 18, color: '#2e7d32' }} />, label: 'Location', value: selectedUser.farmLocation || selectedUser.location || 'Unknown' },
                { icon: <MailIcon sx={{ fontSize: 18, color: '#2e7d32' }} />, label: 'Email', value: selectedUser.email },
                { icon: <LayersIcon sx={{ fontSize: 18, color: '#2e7d32' }} />, label: 'Active Hives', value: selectedUser.hiveCount ?? selectedUser.numHives ?? '—' },
              ].map((row, i) => (
                <Box key={i} sx={{ display: 'flex', alignItems: 'center', gap: 2, py: 1.8, borderBottom: i < 3 ? '1px solid rgba(0,0,0,0.06)' : 'none' }}>
                  <Box sx={{ width: 36, display: 'flex', justifyContent: 'center' }}>
                    {typeof row.icon === 'string' ? <span style={{ fontSize: 18 }}>{row.icon}</span> : row.icon}
                  </Box>
                  <Box>
                    <Typography sx={{ fontSize: 11, color: '#43a047', fontWeight: 600, mb: 0.2 }}>{row.label}</Typography>
                    <Typography sx={{ fontSize: 15, fontWeight: 500 }}>{row.value}</Typography>
                  </Box>
                </Box>
              ))}
            </Box>

            {/* Action Buttons */}
            <Box sx={{ display: 'flex', gap: 2, px: 3, pb: 3, pt: 1, flexDirection: { xs: 'column', sm: 'row' } }}>
              <Box
                onClick={handleDeactivate}
                sx={{
                  flex: 1, textAlign: 'center', py: 1.5, borderRadius: 2,
                  bgcolor: '#f9a825', color: 'white', fontWeight: 700, fontSize: 15,
                  cursor: 'pointer', '&:hover': { bgcolor: '#e69500' }, transition: 'background 0.2s'
                }}
              >
                Deactivate Account
              </Box>
              <Box
                onClick={handleDelete}
                sx={{
                  flex: 1, textAlign: 'center', py: 1.5, borderRadius: 2,
                  bgcolor: '#e53935', color: 'white', fontWeight: 700, fontSize: 15,
                  cursor: 'pointer', '&:hover': { bgcolor: '#c62828' }, transition: 'background 0.2s'
                }}
              >
                Delete Account
              </Box>
            </Box>
          </Paper>
        )}
      </Box>
    );
  }

  // ---- LIST VIEW ----
  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', mb: 3, flexDirection: { xs: 'column', sm: 'row' }, gap: 2 }}>
        <Box>
          <Typography variant="h4" fontWeight={700} sx={{ fontSize: { xs: '1.5rem', md: '2.125rem' } }}>Registered Users</Typography>
          <Typography sx={{ color: '#888', fontSize: 14, mt: 0.5 }}>
            {users.length} beekeeper(s) registered
          </Typography>
        </Box>
        {/* Search */}
        <Paper elevation={0} sx={{
          display: 'flex', alignItems: 'center', border: '1px solid rgba(0,0,0,0.12)',
          borderRadius: 3, px: 2, py: 0.8, gap: 1, width: { xs: '100%', sm: 240 }
        }}>
          <SearchIcon sx={{ fontSize: 18, color: '#aaa' }} />
          <InputBase
            placeholder="Search users..."
            value={search}
            onChange={e => setSearch(e.target.value)}
            sx={{ fontSize: 14, flex: 1 }}
          />
        </Paper>
      </Box>

      {/* Cards */}
      {loading ? (
        <Box sx={{ py: 5, textAlign: 'center' }}>
          <CircularProgress size={32} sx={{ color: '#2e7d32' }} />
        </Box>
      ) : filtered.length === 0 ? (
        <Typography color="text.secondary" sx={{ py: 5, textAlign: 'center' }}>No users found.</Typography>
      ) : (
        <Box sx={{
          display: 'grid',
          gridTemplateColumns: { xs: '1fr', sm: 'repeat(auto-fill, minmax(280px, 1fr))' },
          gap: 2
        }}>
          {filtered.map(user => {
            const isActive = user.active !== false;
            const displayName = user.firstName && user.lastName
              ? `${user.firstName} ${user.lastName}`
              : user.firstName || user.lastName || user.email || 'U';
            const avatarColor = getAvatarColor(displayName);
            return (
              <Paper
                key={user.id}
                elevation={0}
                onClick={() => setSelectedUser(user)}
                sx={{
                  border: '1px solid rgba(0,0,0,0.09)', borderRadius: 3,
                  p: 2, display: 'flex', alignItems: 'center', gap: 2,
                  cursor: 'pointer',
                  '&:hover': { boxShadow: '0 4px 20px rgba(0,0,0,0.08)', borderColor: 'rgba(0,0,0,0.15)' },
                  transition: 'all 0.2s', bgcolor: 'white',
                }}
              >
                {/* Avatar */}
                <Box sx={{
                  width: 46, height: 46, borderRadius: 2,
                  bgcolor: avatarColor, display: 'flex', alignItems: 'center', justifyContent: 'center',
                  color: 'white', fontWeight: 800, fontSize: 20, flexShrink: 0
                }}>
                  {getInitial(displayName)}
                </Box>

                {/* Info */}
                <Box sx={{ flex: 1, overflow: 'hidden' }}>
                  <Typography sx={{ fontWeight: 700, fontSize: 15, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {user.firstName && user.lastName
                      ? `${user.firstName} ${user.lastName}`
                      : user.firstName || user.lastName || user.email || 'No name'}
                  </Typography>
                  <Typography sx={{ fontSize: 12, color: '#888', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {user.farmName || user.farm || 'No farm'}
                  </Typography>
                  <Typography sx={{ fontSize: 11, color: '#aaa', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {user.email}
                  </Typography>
                </Box>

                {/* Status + Arrow */}
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 1 }}>
                  <Chip
                    label={isActive ? 'Active' : 'Inactive'}
                    size="small"
                    sx={{
                      bgcolor: isActive ? '#e8f5e9' : '#f5f5f5',
                      color: isActive ? '#2e7d32' : '#777',
                      fontWeight: 600, fontSize: 11,
                    }}
                  />
                  <ChevronRightIcon sx={{ fontSize: 18, color: '#ccc' }} />
                </Box>
              </Paper>
            );
          })}
        </Box>
      )}
    </Box>
  );
}
