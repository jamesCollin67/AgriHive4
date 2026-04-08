import React, { useState, useEffect } from 'react';
import { Box, Typography, Paper, Button, TextField, Chip, Dialog, useTheme, useMediaQuery, CircularProgress } from '@mui/material';
import {
  ArrowBack as ArrowLeftIcon,
  Reply as CornerUpLeftIcon,
  Delete as Trash2Icon,
  LocationOn as MapPinIcon,
  Layers as LayersIcon,
  CheckCircle as CheckCircleIcon,
  Send as SendIcon,
  ChevronRight as ChevronRightIcon
} from '@mui/icons-material';
import {
  collection, query, orderBy, onSnapshot,
  doc, deleteDoc, updateDoc, addDoc, serverTimestamp
} from 'firebase/firestore';
import { db } from '../firebase';

// ── Helpers ──────────────────────────────────────────────────────────────────
function getInitial(name) {
  return (name || 'U')[0].toUpperCase();
}
function getAvatarColor(name) {
  const colors = ['#f57c00', '#43a047', '#1565c0', '#6a1b9a', '#00838f'];
  let sum = 0;
  for (const c of (name || 'U')) sum += c.charCodeAt(0);
  return colors[sum % colors.length];
}

// ── Main Component ────────────────────────────────────────────────────────────
export default function Reports() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [reports, setReports] = useState([]);
  const [selectedReport, setSelectedReport] = useState(null);
  const [replyText, setReplyText] = useState('');
  const [showReplyBox, setShowReplyBox] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const q = query(collection(db, 'reports'), orderBy('timestamp', 'desc'));
    const unsub = onSnapshot(q, (snapshot) => {
      const liveData = snapshot.docs.map(d => {
        const data = d.data();
        return {
          id: d.id,
          name: data.name || data.userId || 'Mobile User',
          farm: data.farm || data.farmName || 'Unassigned',
          message: data.description || data.message || 'No description provided.',
          location: data.location || 'Unknown',
          hiveCount: data.hiveCount || data.numHives || '—',
          unread: data.status === 'pending' || data.unread === true,
          reply: data.reply || null,
          ...data,
        };
      });
      setReports(liveData);
      setLoading(false);
    }, (error) => {
      console.error("Reports listener error:", error);
      setLoading(false);
    });
    return () => unsub();
  }, []);

  const pendingCount = reports.filter(r => r.unread).length;

  const handleDelete = async (id) => {
    try {
      await deleteDoc(doc(db, 'reports', id));
      setSelectedReport(null);
    } catch (e) { console.error(e); }
  };

  const handleSendReply = async () => {
    if (!replyText.trim()) return;
    try {
      await updateDoc(doc(db, 'reports', selectedReport.id), {
        reply: replyText,
        repliedAt: serverTimestamp(),
        unread: false, // For admin list
        userRead: false, // NEW: For mobile app to detect new reply
        notified: false, // NEW: For mobile app to trigger local notification
      });
      await addDoc(collection(db, 'activity_logs'), {
        action: 'Report Reply Dispatched',
        user: 'Admin',
        target: selectedReport.farm || selectedReport.name,
        status: 'Info',
        timestamp: serverTimestamp(),
      });
      setShowSuccess(true);
      setTimeout(() => {
        setShowSuccess(false);
        setReplyText('');
        setShowReplyBox(false);
      }, 2000);
    } catch (e) { console.error(e); }
  };

  // ── DETAIL VIEW ─────────────────────────────────────────────────────────────
  if (selectedReport) {
    return (
      <Box>
        {/* Top Action Bar */}
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: { xs: 2, md: 4 }, flexDirection: { xs: 'column', sm: 'row' }, gap: 2 }}>
          {/* Back */}
          <Box
            onClick={() => { setSelectedReport(null); setShowReplyBox(false); setReplyText(''); }}
            sx={{ display: 'flex', alignItems: 'center', gap: 0.5, cursor: 'pointer', alignSelf: 'flex-start' }}
          >
            <ArrowLeftIcon sx={{ fontSize: 20, color: '#1a5c2a' }} />
            <Typography sx={{ fontSize: 14, fontWeight: 600, color: '#1a5c2a' }}>Back</Typography>
          </Box>

          {/* Reply + Delete */}
          <Box sx={{ display: 'flex', gap: 1.5, width: { xs: '100%', sm: 'auto' } }}>
            <Button
              variant="contained"
              fullWidth={isMobile}
              startIcon={<CornerUpLeftIcon />}
              onClick={() => setShowReplyBox(v => !v)}
              sx={{
                bgcolor: '#43a047', '&:hover': { bgcolor: '#2e7d32' },
                borderRadius: 2, fontWeight: 700, fontSize: 13,
                textTransform: 'none', px: 2.5,
              }}
            >
              Reply
            </Button>
            <Button
              variant="contained"
              fullWidth={isMobile}
              startIcon={<Trash2Icon />}
              onClick={() => handleDelete(selectedReport.id)}
              sx={{
                bgcolor: '#e53935', '&:hover': { bgcolor: '#c62828' },
                borderRadius: 2, fontWeight: 700, fontSize: 13,
                textTransform: 'none', px: 2.5,
              }}
            >
              Delete
            </Button>
          </Box>
        </Box>

        {/* Details Card */}
        <Paper elevation={0} sx={{
          maxWidth: 620, mx: 'auto',
          border: '1px solid rgba(0,0,0,0.08)', borderRadius: 3,
          bgcolor: 'white', p: { xs: 2, md: 3 },
        }}>
          <Typography sx={{ fontSize: 18, fontWeight: 700, mb: 2.5 }}>Report Details</Typography>

          {/* Row 1: Beekeeper + Farm Name */}
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2, mb: 2 }}>
            <Box sx={{ bgcolor: '#fafafa', border: '1px solid rgba(0,0,0,0.07)', borderRadius: 2, p: 2 }}>
              <Typography sx={{ fontSize: 11, color: '#aaa', mb: 0.5 }}>Beekeeper</Typography>
              <Typography sx={{ fontWeight: 700, fontSize: 15 }}>{selectedReport.name}</Typography>
            </Box>
            <Box sx={{ bgcolor: '#fafafa', border: '1px solid rgba(0,0,0,0.07)', borderRadius: 2, p: 2 }}>
              <Typography sx={{ fontSize: 11, color: '#aaa', mb: 0.5 }}>Farm Name</Typography>
              <Typography sx={{ fontWeight: 700, fontSize: 15 }}>{selectedReport.farm}</Typography>
            </Box>
          </Box>

          {/* Row 2: Location + Active Hives */}
          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2, mb: 3 }}>
            <Box sx={{ bgcolor: '#fafafa', border: '1px solid rgba(0,0,0,0.07)', borderRadius: 2, p: 2 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 0.5 }}>
                <MapPinIcon sx={{ fontSize: 14, color: '#43a047' }} />
                <Typography sx={{ fontSize: 11, color: '#43a047', fontWeight: 600 }}>Location</Typography>
              </Box>
              <Typography sx={{ fontWeight: 700, fontSize: 15 }}>{selectedReport.location || 'Unknown'}</Typography>
            </Box>
            <Box sx={{ bgcolor: '#fafafa', border: '1px solid rgba(0,0,0,0.07)', borderRadius: 2, p: 2 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 0.5 }}>
                <LayersIcon sx={{ fontSize: 14, color: '#43a047' }} />
                <Typography sx={{ fontSize: 11, color: '#43a047', fontWeight: 600 }}>Active Hives</Typography>
              </Box>
              <Typography sx={{ fontWeight: 700, fontSize: 15 }}>{selectedReport.hiveCount}</Typography>
            </Box>
          </Box>

          {/* Report Message */}
          <Typography sx={{ fontSize: 12, color: '#aaa', mb: 1 }}>Report Message</Typography>
          <Box sx={{ bgcolor: '#fafafa', border: '1px solid rgba(0,0,0,0.07)', borderRadius: 2, p: 2, mb: showReplyBox || selectedReport.reply ? 2 : 0 }}>
            <Typography sx={{ fontSize: 14, color: '#444', lineHeight: 1.75 }}>
              {selectedReport.message}
            </Typography>
          </Box>

          {/* Admin Reply (if already sent) */}
          {selectedReport.reply && (
            <>
              <Typography sx={{ fontSize: 12, color: '#aaa', mb: 1 }}>Admin Reply</Typography>
              <Box sx={{ bgcolor: '#e8f5e9', border: '1px solid #c8e6c9', borderRadius: 2, p: 2 }}>
                <Typography sx={{ fontSize: 14, color: '#1a5c2a', lineHeight: 1.75 }}>
                  {selectedReport.reply}
                </Typography>
              </Box>
            </>
          )}

          {/* Inline Reply Box */}
          {showReplyBox && !selectedReport.reply && (
            <Box sx={{ mt: 2 }}>
              <Typography sx={{ fontSize: 12, color: '#aaa', mb: 1 }}>Your Reply</Typography>
              <TextField
                fullWidth multiline rows={3} variant="outlined"
                placeholder="Type your message to the beekeeper..."
                value={replyText}
                onChange={e => setReplyText(e.target.value)}
                sx={{ mb: 1.5, '& .MuiOutlinedInput-root': { borderRadius: 2, fontSize: 14 } }}
              />
              <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
                <Button onClick={() => setShowReplyBox(false)} color="inherit" sx={{ textTransform: 'none', fontSize: 13 }}>Cancel</Button>
                <Button
                  variant="contained"
                  endIcon={<SendIcon />}
                  onClick={handleSendReply}
                  disabled={!replyText.trim()}
                  sx={{
                    bgcolor: '#2e7d32', '&:hover': { bgcolor: '#1a5c2a' },
                    textTransform: 'none', fontWeight: 700, fontSize: 13, borderRadius: 2,
                  }}
                >
                  Send Reply
                </Button>
              </Box>
            </Box>
          )}
        </Paper>

        {/* Success Dialog */}
        <Dialog open={showSuccess} PaperProps={{ sx: { p: 4, borderRadius: 4, textAlign: 'center', minWidth: 280 } }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <CheckCircleIcon sx={{ fontSize: 56, color: '#43a047', mb: 2 }} />
            <Typography variant="h6" fontWeight={700} color="success.main">Reply Sent!</Typography>
            <Typography color="text.secondary" fontSize={13}>Your message was saved to the database.</Typography>
          </Box>
        </Dialog>
      </Box>
    );
  }

  // ── LIST VIEW ────────────────────────────────────────────────────────────────
  return (
    <Box>
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: 'flex-start', justifyContent: 'space-between', mb: 3, flexDirection: { xs: 'column', sm: 'row' }, gap: 2 }}>
        <Box>
          <Typography variant="h4" fontWeight={700} sx={{ fontSize: { xs: '1.5rem', md: '2.125rem' } }}>Received Reports</Typography>
          <Typography sx={{ color: '#888', fontSize: 14, mt: 0.5 }}>
            {pendingCount} pending report(s)
          </Typography>
        </Box>
        {pendingCount > 0 && (
          <Chip
            label={`${pendingCount} Pending`}
            sx={{
              bgcolor: '#ffebee', color: '#c62828',
              fontWeight: 700, fontSize: 13, px: 0.5, height: 32,
              borderRadius: 5,
            }}
          />
        )}
      </Box>

      {/* Report Cards */}
      {loading ? (
        <Box sx={{ py: 6, textAlign: 'center' }}>
          <CircularProgress size={32} sx={{ color: '#2e7d32' }} />
        </Box>
      ) : reports.length === 0 ? (
        <Paper elevation={0} sx={{ p: { xs: 4, md: 8 }, textAlign: 'center', border: '1px solid rgba(0,0,0,0.08)', borderRadius: 3 }}>
          <Typography variant="h6" color="text.secondary" fontWeight={700}>No Reports Received</Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            Beekeeper reports from the mobile app will appear here.
          </Typography>
        </Paper>
      ) : (
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' }, gap: 2 }}>
          {reports.map(report => {
            const avatarColor = getAvatarColor(report.name);
            return (
              <Paper
                key={report.id}
                elevation={0}
                onClick={() => {
                  setSelectedReport(report);
                  if (report.unread) {
                    updateDoc(doc(db, 'reports', report.id), { unread: false });
                  }
                }}
                sx={{
                  border: '1px solid rgba(0,0,0,0.09)',
                  borderLeft: report.unread ? '4px solid #2e7d32' : '1px solid rgba(0,0,0,0.09)',
                  borderRadius: 3, p: { xs: 2, md: 2.5 },
                  cursor: 'pointer', bgcolor: 'white',
                  '&:hover': { boxShadow: '0 4px 20px rgba(0,0,0,0.08)', transform: 'translateY(-1px)' },
                  transition: 'all 0.2s',
                }}
              >
                {/* Top Row */}
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1.5 }}>
                  {/* Avatar */}
                  <Box sx={{
                    width: 44, height: 44, borderRadius: 2, flexShrink: 0,
                    bgcolor: avatarColor,
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    color: 'white', fontWeight: 800, fontSize: 19,
                  }}>
                    {getInitial(report.name)}
                  </Box>

                  {/* Name + Farm */}
                  <Box sx={{ flex: 1, overflow: 'hidden' }}>
                    <Typography sx={{ fontWeight: 700, fontSize: 15, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{report.name}</Typography>
                    <Typography sx={{ fontSize: 13, color: '#2e7d32', fontWeight: 600, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{report.farm}</Typography>
                  </Box>

                  {/* Arrow */}
                  <Box sx={{ color: '#43a047' }}>
                    <ChevronRightIcon sx={{ fontSize: 18 }} />
                  </Box>
                </Box>

                {/* Location + Hives row */}
                {(report.location !== 'Unknown' || report.hiveCount !== '—') && (
                  <Box sx={{ display: 'flex', gap: 2.5, mb: 1.5, pl: { xs: 0, sm: 7 }, flexWrap: 'wrap' }}>
                    {report.location && report.location !== 'Unknown' && (
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                        <MapPinIcon sx={{ fontSize: 14, color: '#aaa' }} />
                        <Typography sx={{ fontSize: 12, color: '#888' }}>{report.location}</Typography>
                      </Box>
                    )}
                    {report.hiveCount && report.hiveCount !== '—' && (
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                        <LayersIcon sx={{ fontSize: 14, color: '#aaa' }} />
                        <Typography sx={{ fontSize: 12, color: '#888' }}>{report.hiveCount} hives</Typography>
                      </Box>
                    )}
                  </Box>
                )}

                {/* Message preview */}
                <Typography sx={{
                  fontSize: 13, color: '#43a047', pl: { xs: 0, sm: 7 },
                  overflow: 'hidden', display: '-webkit-box',
                  WebkitLineClamp: 2, WebkitBoxOrient: 'vertical',
                  lineHeight: 1.65,
                }}>
                  {report.message}
                </Typography>
              </Paper>
            );
          })}
        </Box>
      )}
    </Box>
  );
}
