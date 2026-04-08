import React, { useState, useEffect } from 'react';
import { Box, Typography, Paper, Chip, Dialog, Button, Divider, useTheme, useMediaQuery, CircularProgress } from '@mui/material';
import {
  CheckCircle as CheckCircleIcon,
  AccessTime as ClockIcon,
  ChevronRight as ChevronRightIcon
} from '@mui/icons-material';
import { collection, query, onSnapshot, doc, updateDoc, addDoc, serverTimestamp } from 'firebase/firestore';
import { db } from '../firebase';

function getInitial(name) {
  return (name || 'U')[0].toUpperCase();
}

function getAvatarColor(name) {
  const colors = ['#43a047', '#1565c0', '#f57c00', '#6a1b9a', '#00838f'];
  let sum = 0;
  for (let c of (name || 'U')) sum += c.charCodeAt(0);
  return colors[sum % colors.length];
}

export default function Subscriptions() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [subscriptions, setSubscriptions] = useState([]);
  const [confirmingSub, setConfirmingSub] = useState(null);
  const [successMsg, setSuccessMsg] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const q = query(collection(db, 'subscriptions'));
    const unsub = onSnapshot(q, (snapshot) => {
      const liveData = snapshot.docs.map(d => {
        const data = d.data();
        let daysLeft = 0;
        if (data.due) {
          const diff = Math.abs(new Date(data.due) - new Date());
          daysLeft = Math.ceil(diff / (1000 * 60 * 60 * 24));
        }
        return { id: d.id, ...data, daysLeft };
      });
      setSubscriptions(liveData);
      setLoading(false);
    }, (error) => {
      console.error("Subscriptions listener error:", error);
      setLoading(false);
    });
    return () => unsub();
  }, []);

  const confirmed = subscriptions.filter(s => !s.pending);
  const pending = subscriptions.filter(s => s.pending);

  const handleConfirm = async () => {
    try {
      await updateDoc(doc(db, 'subscriptions', confirmingSub.id), {
        pending: false,
        purchased: new Date().toISOString().split('T')[0],
        due: new Date(new Date().setDate(new Date().getDate() + 90)).toISOString().split('T')[0],
      });
      await addDoc(collection(db, 'activity_logs'), {
        action: 'Payment Processed',
        user: confirmingSub.name || 'System User',
        target: confirmingSub.farm || 'Beekeeper Node',
        status: 'Success',
        timestamp: serverTimestamp()
      });
      setSuccessMsg(true);
      setTimeout(() => { setSuccessMsg(false); setConfirmingSub(null); }, 2000);
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: { xs: 2, md: 3 } }}>
        <Typography variant="h4" fontWeight={700} sx={{ fontSize: { xs: '1.5rem', md: '2.125rem' } }}>Verified Subscribers</Typography>
        <Typography sx={{ color: '#888', fontSize: 14, mt: 0.5 }}>
          {subscriptions.length} total subscriber(s)
        </Typography>
      </Box>

      {/* Summary Cards */}
      <Box sx={{
        display: 'grid',
        gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)' },
        gap: { xs: 2, md: 3 },
        mb: 4
      }}>
        {/* Confirmed Card */}
        <Box sx={{
          borderRadius: 3, p: { xs: 2, md: 3 },
          background: 'linear-gradient(135deg, #43a047 0%, #2e7d32 100%)',
          color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          minHeight: { xs: 80, md: 100 },
        }}>
          <Box>
            <Typography sx={{ fontSize: 13, fontWeight: 600, opacity: 0.85, mb: 0.5 }}>Confirmed</Typography>
            <Typography sx={{ fontSize: { xs: 32, md: 40 }, fontWeight: 800, lineHeight: 1 }}>{confirmed.length}</Typography>
          </Box>
          <Box sx={{
            width: 44, height: 44, borderRadius: 2,
            bgcolor: 'rgba(255,255,255,0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center'
          }}>
            <CheckCircleIcon sx={{ fontSize: 24, color: 'white' }} />
          </Box>
        </Box>

        {/* Pending Card */}
        <Box sx={{
          borderRadius: 3, p: { xs: 2, md: 3 },
          background: 'linear-gradient(135deg, #ffa726 0%, #f57c00 100%)',
          color: 'white', display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          minHeight: { xs: 80, md: 100 },
        }}>
          <Box>
            <Typography sx={{ fontSize: 13, fontWeight: 600, opacity: 0.85, mb: 0.5 }}>Pending</Typography>
            <Typography sx={{ fontSize: { xs: 32, md: 40 }, fontWeight: 800, lineHeight: 1 }}>{pending.length}</Typography>
          </Box>
          <Box sx={{
            width: 44, height: 44, borderRadius: 2,
            bgcolor: 'rgba(255,255,255,0.2)', display: 'flex', alignItems: 'center', justifyContent: 'center'
          }}>
            <ClockIcon sx={{ fontSize: 24, color: 'white' }} />
          </Box>
        </Box>
      </Box>

      {/* Subscriber List */}
      {loading ? (
        <Box sx={{ textAlign: 'center', py: 5 }}>
          <CircularProgress size={32} sx={{ color: '#2e7d32' }} />
        </Box>
      ) : subscriptions.length === 0 ? (
        <Paper elevation={0} sx={{ p: { xs: 4, md: 8 }, textAlign: 'center', border: '1px solid rgba(0,0,0,0.08)', borderRadius: 3 }}>
          <Typography variant="h6" color="text.secondary" fontWeight={700}>No Subscribers Yet</Typography>
          <Typography variant="body2" color="text.secondary">Subscription records will appear here when beekeepers subscribe.</Typography>
        </Paper>
      ) : (
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' }, gap: 1.5 }}>
          {subscriptions.map(sub => {
            const isConfirmed = !sub.pending;
            const avatarColor = getAvatarColor(sub.name);
            return (
              <Paper
                key={sub.id}
                elevation={0}
                onClick={() => sub.pending && setConfirmingSub(sub)}
                sx={{
                  border: '1px solid rgba(0,0,0,0.09)', borderRadius: 3,
                  p: { xs: 1.5, md: 2 }, display: 'flex', alignItems: 'center', gap: 2,
                  bgcolor: 'white',
                  cursor: sub.pending ? 'pointer' : 'default',
                  '&:hover': sub.pending ? { boxShadow: '0 4px 16px rgba(0,0,0,0.07)' } : {},
                  transition: 'all 0.2s',
                }}
              >
                {/* Avatar */}
                <Box sx={{
                  width: 46, height: 46, borderRadius: 2,
                  bgcolor: avatarColor, display: 'flex', alignItems: 'center', justifyContent: 'center',
                  color: 'white', fontWeight: 800, fontSize: 20, flexShrink: 0
                }}>
                  {getInitial(sub.name)}
                </Box>

                {/* Info */}
                <Box sx={{ flex: 1, overflow: 'hidden' }}>
                  <Typography sx={{ fontWeight: 700, fontSize: 15, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{sub.name || 'Unknown'}</Typography>
                  <Typography sx={{ fontSize: 12, color: '#2e7d32', fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{sub.farm || 'No Farm'}</Typography>
                  <Typography sx={{ fontSize: 11, color: '#aaa', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{sub.email}</Typography>
                </Box>

                {/* Status + Plan */}
                <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 0.5, flexShrink: 0 }}>
                  <Chip
                    label={isConfirmed ? '✓ Confirmed' : 'Pending'}
                    size="small"
                    sx={{
                      bgcolor: isConfirmed ? '#e8f5e9' : '#fff3e0',
                      color: isConfirmed ? '#2e7d32' : '#e65100',
                      fontWeight: 700, fontSize: 11,
                    }}
                  />
                  <Typography sx={{ fontSize: 12, color: '#aaa' }}>{sub.plan || '3-month'}</Typography>
                </Box>

                {!isMobile && <ChevronRightIcon sx={{ fontSize: 18, color: '#ccc' }} />}
              </Paper>
            );
          })}
        </Box>
      )}

      {/* Confirmation Dialog */}
      <Dialog open={Boolean(confirmingSub)} onClose={() => !successMsg && setConfirmingSub(null)} maxWidth="sm" fullWidth
        PaperProps={{ sx: { borderRadius: 4, overflow: 'hidden' } }}
      >
        {successMsg ? (
          <Box sx={{ p: 6, textAlign: 'center' }}>
            <CheckCircleIcon sx={{ fontSize: 64, color: '#43a047', mb: 2 }} />
            <Typography variant="h5" fontWeight={700} color="success.main">Subscription Confirmed!</Typography>
            <Typography color="text.secondary">Plan activated and recorded in Firebase.</Typography>
          </Box>
        ) : confirmingSub && (
          <Box sx={{ p: 4 }}>
            <Typography variant="h6" fontWeight={700} mb={0.5}>Confirm Subscription</Typography>
            <Typography variant="body2" color="text.secondary" mb={3}>Activate this beekeeper's subscription plan.</Typography>

            <Box sx={{ bgcolor: '#f9f9f9', borderRadius: 2, p: 3, mb: 3, border: '1px solid rgba(0,0,0,0.08)' }}>
              {[
                ['Subscriber', confirmingSub.name],
                ['Farm', confirmingSub.farm || '—'],
                ['Requested Plan', confirmingSub.plan || '3 Months'],
                ['GCash Reference', confirmingSub.refNo || 'Not Provided'],
                ['Amount', `₱${confirmingSub.price || '—'}`],
              ].map(([label, value], i, arr) => (
                <Box key={i}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', py: 1.2 }}>
                    <Typography variant="body2" color="text.secondary">{label}</Typography>
                    <Typography variant="body2" fontWeight={700}>{value}</Typography>
                  </Box>
                  {i < arr.length - 1 && <Box sx={{ height: 1, bgcolor: 'rgba(0,0,0,0.06)' }} />}
                </Box>
              ))}
            </Box>

            <Box sx={{ display: 'flex', gap: 2, justifyContent: 'flex-end' }}>
              <Button onClick={() => setConfirmingSub(null)} color="inherit">Cancel</Button>
              <Button variant="contained" onClick={handleConfirm}
                sx={{ bgcolor: '#2e7d32', '&:hover': { bgcolor: '#1a5c2a' } }}>
                Confirm Subscription
              </Button>
            </Box>
          </Box>
        )}
      </Dialog>
    </Box>
  );
}
