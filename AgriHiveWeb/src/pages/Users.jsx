import React, { useState, useMemo } from 'react';
import { Box, Typography, Avatar, Chip, InputBase, Paper, Divider,
  useTheme, useMediaQuery, CircularProgress, Dialog, Button } from '@mui/material';
import {
  Search as SearchIcon,
  ChevronRight as ChevronRightIcon,
  LocationOn as MapPinIcon,
  Layers as LayersIcon,
  Email as MailIcon,
  ArrowBack as ArrowLeftIcon,
  Warning as WarningIcon,
  CheckCircle as CheckCircleIcon,
  Download as DownloadIcon,
} from '@mui/icons-material';
import { doc, updateDoc, deleteDoc, addDoc, serverTimestamp, collection } from 'firebase/firestore';
import { db } from '../firebase';
import { useAdminData } from '../context/AdminDataContext';

const GREEN = '#1a5c2a';

function getInitial(name) { return (name || 'U')[0].toUpperCase(); }
function getAvatarColor(name) {
  const colors = ['#43a047', '#1565c0', '#f57c00', '#6a1b9a', '#00838f', '#c62828'];
  let sum = 0;
  for (const c of (name || 'U')) sum += c.charCodeAt(0);
  return colors[sum % colors.length];
}
function displayName(u) {
  if (u.firstName && u.lastName) return `${u.firstName} ${u.lastName}`;
  return u.firstName || u.lastName || u.email || 'No name';
}

export default function Users() {
  const { users, usersLoading: loading } = useAdminData();
  const [search, setSearch]           = useState('');
  const [selectedUser, setSelectedUser] = useState(null);
  const [actionMsg, setActionMsg]     = useState('');
  const [confirmDialog, setConfirmDialog] = useState(null); // { type: 'deactivate'|'delete'|'reactivate' }

  // Fixed search: checks firstName, lastName, combined full name, email, farmName
  const filtered = useMemo(() => {
    const q = search.toLowerCase();
    if (!q) return users;
    return users.filter(u => {
      const full = `${u.firstName || ''} ${u.lastName || ''}`.toLowerCase();
      return (
        full.includes(q) ||
        (u.email || '').toLowerCase().includes(q) ||
        (u.farmName || u.farm || '').toLowerCase().includes(q)
      );
    });
  }, [users, search]);

  const activeCount   = users.filter(u => u.active !== false).length;
  const inactiveCount = users.filter(u => u.active === false).length;

  const exportCSV = () => {
    const rows = [
      ['Name', 'Email', 'Farm', 'Location', 'Status'],
      ...users.map(u => [
        displayName(u),
        u.email || '',
        u.farmName || u.farm || '',
        u.farmLocation || u.location || '',
        u.active !== false ? 'Active' : 'Inactive',
      ]),
    ];
    const csv = rows.map(r => r.map(v => `"${String(v).replace(/"/g, '""')}"`).join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `agrihive_users_${new Date().toISOString().split('T')[0]}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  };

  const handleDeactivate = async () => {
    setConfirmDialog(null);
    try {
      await updateDoc(doc(db, 'users', selectedUser.id), { active: false });
      await addDoc(collection(db, 'activity_logs'), {
        action: 'Account Suspended', user: 'Admin',
        target: selectedUser.email, status: 'Warning', timestamp: serverTimestamp(),
      });
      setActionMsg('Account deactivated successfully.');
      setTimeout(() => { setActionMsg(''); setSelectedUser(null); }, 2000);
    } catch (e) { console.error(e); }
  };

  const handleReactivate = async () => {
    setConfirmDialog(null);
    try {
      await updateDoc(doc(db, 'users', selectedUser.id), { active: true });
      await addDoc(collection(db, 'activity_logs'), {
        action: 'Account Reactivated', user: 'Admin',
        target: selectedUser.email, status: 'Success', timestamp: serverTimestamp(),
      });
      setActionMsg('Account reactivated successfully.');
      setTimeout(() => { setActionMsg(''); setSelectedUser(null); }, 2000);
    } catch (e) { console.error(e); }
  };

  const handleDelete = async () => {
    setConfirmDialog(null);
    try {
      await deleteDoc(doc(db, 'users', selectedUser.id));
      await addDoc(collection(db, 'activity_logs'), {
        action: 'Account Deleted', user: 'Admin',
        target: selectedUser.email, status: 'Error', timestamp: serverTimestamp(),
      });
      setActionMsg('Account deleted permanently.');
      setTimeout(() => { setActionMsg(''); setSelectedUser(null); }, 2000);
    } catch (e) { console.error(e); }
  };

  const confirmAction = () => {
    if (confirmDialog?.type === 'delete') return handleDelete();
    if (confirmDialog?.type === 'reactivate') return handleReactivate();
    return handleDeactivate();
  };

  // ── DETAIL VIEW ──────────────────────────────────────────────────────────────
  if (selectedUser) {
    const isActive = selectedUser.active !== false;
    return (
      <Box>
        <Box onClick={() => { setSelectedUser(null); setActionMsg(''); }}
          sx={{ display: 'flex', alignItems: 'center', gap: 0.5, cursor: 'pointer',
            color: GREEN, mb: 3, width: 'fit-content' }}>
          <ArrowLeftIcon sx={{ fontSize: 18 }} />
          <Typography sx={{ fontSize: 14, fontWeight: 600, color: GREEN }}>Back to Users</Typography>
        </Box>

        {actionMsg ? (
          <Paper elevation={0} sx={{ p: 6, textAlign: 'center', borderRadius: 4,
            border: '1px solid rgba(0,0,0,0.07)' }}>
            <CheckCircleIcon sx={{ fontSize: 48, color: '#43a047', mb: 1 }} />
            <Typography variant="h6" color="success.main" fontWeight={700}>{actionMsg}</Typography>
          </Paper>
        ) : (
          <Paper elevation={0} sx={{ maxWidth: 520, mx: 'auto', borderRadius: 4,
            border: '1px solid rgba(0,0,0,0.08)', overflow: 'hidden', bgcolor: 'white' }}>
            {/* Avatar + Name */}
            <Box sx={{ pt: 5, pb: 3, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
              <Box sx={{ width: 72, height: 72, borderRadius: '50%', bgcolor: '#e8f5e9',
                border: '2px solid #c8e6c9', mb: 2, display: 'flex', alignItems: 'center',
                justifyContent: 'center', fontSize: 32 }}>
                🧑
              </Box>
              <Typography variant="h6" fontWeight={700}>{displayName(selectedUser)}</Typography>
              <Chip label={isActive ? 'Active' : 'Inactive'} size="small"
                sx={{ mt: 0.5, bgcolor: isActive ? '#e8f5e9' : '#f5f5f5',
                  color: isActive ? '#2e7d32' : '#777', fontWeight: 600, fontSize: 12 }} />
            </Box>

            <Divider />

            {/* Info Rows */}
            <Box sx={{ px: 3, py: 2 }}>
              {[
                { icon: '🐝', label: 'Farm Name', value: selectedUser.farmName || selectedUser.farm || 'Not set' },
                { icon: <MapPinIcon sx={{ fontSize: 18, color: '#2e7d32' }} />, label: 'Location',
                  value: selectedUser.farmLocation || selectedUser.location || 'Unknown' },
                { icon: <MailIcon sx={{ fontSize: 18, color: '#2e7d32' }} />, label: 'Email',
                  value: selectedUser.email },
                { icon: <LayersIcon sx={{ fontSize: 18, color: '#2e7d32' }} />, label: 'Active Hives',
                  value: selectedUser.hiveCount ?? selectedUser.numHives ?? '—' },
              ].map((row, i) => (
                <Box key={i} sx={{ display: 'flex', alignItems: 'center', gap: 2, py: 1.8,
                  borderBottom: i < 3 ? '1px solid rgba(0,0,0,0.06)' : 'none' }}>
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
            <Box sx={{ display: 'flex', gap: 2, px: 3, pb: 3, pt: 1,
              flexDirection: { xs: 'column', sm: 'row' } }}>
              {isActive ? (
                <Box onClick={() => setConfirmDialog({ type: 'deactivate' })}
                  sx={{ flex: 1, textAlign: 'center', py: 1.5, borderRadius: 2,
                    bgcolor: '#f9a825', color: 'white', fontWeight: 700, fontSize: 15,
                    cursor: 'pointer', '&:hover': { bgcolor: '#e69500' }, transition: 'background 0.2s' }}>
                  Deactivate Account
                </Box>
              ) : (
                <Box onClick={() => setConfirmDialog({ type: 'reactivate' })}
                  sx={{ flex: 1, textAlign: 'center', py: 1.5, borderRadius: 2,
                    bgcolor: '#43a047', color: 'white', fontWeight: 700, fontSize: 15,
                    cursor: 'pointer', '&:hover': { bgcolor: '#2e7d32' }, transition: 'background 0.2s' }}>
                  Reactivate Account
                </Box>
              )}
              <Box onClick={() => setConfirmDialog({ type: 'delete' })}
                sx={{ flex: 1, textAlign: 'center', py: 1.5, borderRadius: 2,
                  bgcolor: '#e53935', color: 'white', fontWeight: 700, fontSize: 15,
                  cursor: 'pointer', '&:hover': { bgcolor: '#c62828' }, transition: 'background 0.2s' }}>
                Delete Account
              </Box>
            </Box>
          </Paper>
        )}

        {/* Confirmation Dialog */}
        <Dialog open={Boolean(confirmDialog)} onClose={() => setConfirmDialog(null)}
          PaperProps={{ sx: { borderRadius: 3, p: 1, mx: 2, minWidth: 300 } }}>
          <Box sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1.5 }}>
              <WarningIcon sx={{
                color: confirmDialog?.type === 'delete' ? '#e53935'
                  : confirmDialog?.type === 'reactivate' ? '#43a047' : '#f9a825',
                fontSize: 28
              }} />
              <Typography variant="h6" fontWeight={700}>
                {confirmDialog?.type === 'delete' ? 'Delete Account?'
                  : confirmDialog?.type === 'reactivate' ? 'Reactivate Account?'
                  : 'Deactivate Account?'}
              </Typography>
            </Box>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              {confirmDialog?.type === 'delete'
                ? `This will permanently delete ${selectedUser?.email}'s account. This cannot be undone.`
                : confirmDialog?.type === 'reactivate'
                ? `This will restore ${selectedUser?.email}'s access to the mobile app.`
                : `This will suspend ${selectedUser?.email}'s access. You can reactivate them later.`}
            </Typography>
            <Box sx={{ display: 'flex', gap: 1.5, justifyContent: 'flex-end' }}>
              <Button onClick={() => setConfirmDialog(null)} color="inherit">Cancel</Button>
              <Button variant="contained" onClick={confirmAction}
                sx={{
                  bgcolor: confirmDialog?.type === 'delete' ? '#e53935'
                    : confirmDialog?.type === 'reactivate' ? '#43a047' : '#f9a825',
                  '&:hover': {
                    bgcolor: confirmDialog?.type === 'delete' ? '#c62828'
                      : confirmDialog?.type === 'reactivate' ? '#2e7d32' : '#e69500'
                  }
                }}>
                {confirmDialog?.type === 'delete' ? 'Yes, Delete'
                  : confirmDialog?.type === 'reactivate' ? 'Yes, Reactivate'
                  : 'Yes, Deactivate'}
              </Button>
            </Box>
          </Box>
        </Dialog>
      </Box>
    );
  }

  // ── LIST VIEW ────────────────────────────────────────────────────────────────
  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between',
        mb: 3, flexDirection: { xs: 'column', sm: 'row' }, gap: 2 }}>
        <Box>
          <Typography variant="h4" fontWeight={700} sx={{ fontSize: { xs: '1.5rem', md: '2.125rem' } }}>
            Registered Users
          </Typography>
          <Typography sx={{ color: '#888', fontSize: 14, mt: 0.5 }}>
            {activeCount} active · {inactiveCount} inactive · {users.length} total
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap', width: { xs: '100%', sm: 'auto' } }}>
          <Paper elevation={0} sx={{ display: 'flex', alignItems: 'center',
            border: '1px solid rgba(0,0,0,0.12)', borderRadius: 3, px: 2, py: 0.8,
            gap: 1, width: { xs: '100%', sm: 260 } }}>
            <SearchIcon sx={{ fontSize: 18, color: '#aaa' }} />
            <InputBase placeholder="Search by name, email, or farm..."
              value={search} onChange={e => setSearch(e.target.value)}
              sx={{ fontSize: 14, flex: 1 }} />
          </Paper>
          <Button
            variant="outlined"
            startIcon={<DownloadIcon />}
            onClick={exportCSV}
            disabled={users.length === 0}
            sx={{ borderRadius: 3, textTransform: 'none', fontWeight: 600, fontSize: 13,
              borderColor: 'rgba(0,0,0,0.2)', color: '#555', '&:hover': { borderColor: '#1a5c2a', color: '#1a5c2a' } }}
          >
            Export CSV
          </Button>
        </Box>      </Box>

      {loading ? (
        <Box sx={{ py: 5, textAlign: 'center' }}>
          <CircularProgress size={32} sx={{ color: '#2e7d32' }} />
        </Box>
      ) : filtered.length === 0 ? (
        <Paper elevation={0} sx={{ p: { xs: 4, md: 8 }, textAlign: 'center',
          border: '1px solid rgba(0,0,0,0.08)', borderRadius: 3 }}>
          <Typography sx={{ fontSize: 36, mb: 1 }}>👤</Typography>
          <Typography variant="h6" color="text.secondary" fontWeight={700}>
            {users.length === 0 ? 'No Users Registered' : 'No Matching Users'}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            {users.length === 0
              ? 'Beekeepers will appear here once they sign up from the mobile app.'
              : 'Try a different search term.'}
          </Typography>
        </Paper>
      ) : (
        <Box sx={{ display: 'grid',
          gridTemplateColumns: { xs: '1fr', sm: 'repeat(auto-fill, minmax(280px, 1fr))' }, gap: 2 }}>
          {filtered.map(user => {
            const isActive = user.active !== false;
            const name = displayName(user);
            const avatarColor = getAvatarColor(name);
            return (
              <Paper key={user.id} elevation={0} onClick={() => setSelectedUser(user)}
                sx={{ border: '1px solid rgba(0,0,0,0.09)', borderRadius: 3,
                  p: 2, display: 'flex', alignItems: 'center', gap: 2,
                  cursor: 'pointer',
                  '&:hover': { boxShadow: '0 4px 20px rgba(0,0,0,0.08)', borderColor: 'rgba(0,0,0,0.15)' },
                  transition: 'all 0.2s', bgcolor: 'white' }}>
                <Box sx={{ width: 46, height: 46, borderRadius: 2, bgcolor: avatarColor,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  color: 'white', fontWeight: 800, fontSize: 20, flexShrink: 0 }}>
                  {getInitial(name)}
                </Box>
                <Box sx={{ flex: 1, overflow: 'hidden' }}>
                  <Typography sx={{ fontWeight: 700, fontSize: 15, whiteSpace: 'nowrap',
                    overflow: 'hidden', textOverflow: 'ellipsis' }}>{name}</Typography>
                  <Typography sx={{ fontSize: 12, color: '#888', whiteSpace: 'nowrap',
                    overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {user.farmName || user.farm || 'No farm'}
                  </Typography>
                  <Typography sx={{ fontSize: 11, color: '#aaa', whiteSpace: 'nowrap',
                    overflow: 'hidden', textOverflow: 'ellipsis' }}>{user.email}</Typography>
                </Box>
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 1 }}>
                  <Chip label={isActive ? 'Active' : 'Inactive'} size="small"
                    sx={{ bgcolor: isActive ? '#e8f5e9' : '#f5f5f5',
                      color: isActive ? '#2e7d32' : '#777', fontWeight: 600, fontSize: 11 }} />
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
