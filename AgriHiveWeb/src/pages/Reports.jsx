import React, { useState, useMemo } from 'react';
import { Box, Typography, Paper, Button, TextField, Chip, Dialog, InputBase,
  useTheme, useMediaQuery, CircularProgress } from '@mui/material';
import { useSearchParams } from 'react-router-dom';
import {
  ArrowBack as ArrowLeftIcon,
  Reply as CornerUpLeftIcon,
  Delete as Trash2Icon,
  LocationOn as MapPinIcon,
  Layers as LayersIcon,
  CheckCircle as CheckCircleIcon,
  Send as SendIcon,
  ChevronRight as ChevronRightIcon,
  AccessTime as ClockIcon,
  Warning as WarningIcon,
  Search as SearchIcon,
  FilterList as FilterIcon,
} from '@mui/icons-material';
import {
  collection, doc, deleteDoc, updateDoc, addDoc, serverTimestamp,
} from 'firebase/firestore';
import { db } from '../firebase';
import { useAdminData } from '../context/AdminDataContext';

function getInitial(name) { return (name || 'U')[0].toUpperCase(); }
function getAvatarColor(name) {
  const colors = ['#f57c00', '#43a047', '#1565c0', '#6a1b9a', '#00838f'];
  let sum = 0;
  for (const c of (name || 'U')) sum += c.charCodeAt(0);
  return colors[sum % colors.length];
}
function formatTs(ts) {
  if (!ts) return '';
  if (typeof ts === 'number') return new Date(ts).toLocaleString();
  if (ts?.toDate) return ts.toDate().toLocaleString();
  return '';
}

export default function Reports() {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [searchParams, setSearchParams] = useSearchParams();

  const { reports, reportsLoading: loading } = useAdminData();

  const [selectedReport, setSelectedReport] = useState(null);
  const [replyText, setReplyText]           = useState('');
  const [showReplyBox, setShowReplyBox]     = useState(false);
  const [showSuccess, setShowSuccess]       = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);

  // Search + filter state — pre-fill farm filter if navigated from Dashboard
  const [search, setSearch]   = useState('');
  const [filter, setFilter]   = useState(searchParams.get('farm') || 'all');
  // 'all' | 'pending' | 'replied' | specific farm name

  const farmNames = useMemo(() => {
    const names = [...new Set(reports.map(r => r.farm).filter(Boolean))];
    return names.sort();
  }, [reports]);

  const filtered = useMemo(() => {
    return reports.filter(r => {
      const matchSearch =
        !search ||
        r.name.toLowerCase().includes(search.toLowerCase()) ||
        r.farm.toLowerCase().includes(search.toLowerCase()) ||
        r.message.toLowerCase().includes(search.toLowerCase());

      const matchFilter =
        filter === 'all' ? true :
        filter === 'pending' ? r.unread :
        filter === 'replied' ? Boolean(r.reply) :
        r.farm === filter;

      return matchSearch && matchFilter;
    });
  }, [reports, search, filter]);

  const pendingCount = reports.filter(r => r.unread).length;

  const handleDelete = async (id) => {
    try {
      await deleteDoc(doc(db, 'reports', id));
      setSelectedReport(null);
      setShowDeleteConfirm(false);
    } catch (e) { console.error(e); }
  };

  const handleSendReply = async () => {
    if (!replyText.trim()) return;
    try {
      await updateDoc(doc(db, 'reports', selectedReport.id), {
        reply: replyText,
        repliedAt: serverTimestamp(),
        unread: false,
        userRead: false,
        notified: false,
      });
      await addDoc(collection(db, 'activity_logs'), {
        action: 'Report Reply Dispatched',
        user: 'Admin',
        target: selectedReport.farm || selectedReport.name,
        status: 'Info',
        timestamp: serverTimestamp(),
      });
      setShowSuccess(true);
      setTimeout(() => { setShowSuccess(false); setReplyText(''); setShowReplyBox(false); }, 2000);
    } catch (e) { console.error(e); }
  };

  // ── DETAIL VIEW ──────────────────────────────────────────────────────────────
  if (selectedReport) {
    return (
      <Box>
        <Box sx={{ display: 'flex', alignItems: { xs: 'flex-start', sm: 'center' }, justifyContent: 'space-between',
          mb: { xs: 2.5, md: 4 }, flexDirection: { xs: 'column', sm: 'row' }, gap: 2 }}>
          <Box onClick={() => { setSelectedReport(null); setShowReplyBox(false); setReplyText(''); }}
            sx={{ display: 'flex', alignItems: 'center', gap: 0.5, cursor: 'pointer' }}>
            <ArrowLeftIcon sx={{ fontSize: 20, color: '#1a5c2a' }} />
            <Typography sx={{ fontSize: 14, fontWeight: 600, color: '#1a5c2a' }}>Back to Reports</Typography>
          </Box>
          <Box sx={{ display: 'flex', gap: 1.5, width: { xs: '100%', sm: 'auto' } }}>
            <Button variant="contained" fullWidth={isMobile} startIcon={<CornerUpLeftIcon />}
              onClick={() => setShowReplyBox(v => !v)}
              sx={{ bgcolor: '#43a047', '&:hover': { bgcolor: '#2e7d32' }, borderRadius: 2,
                fontWeight: 700, fontSize: 13, textTransform: 'none', px: { xs: 1.5, sm: 2.5 }, py: 1 }}>
              {selectedReport.reply ? (isMobile ? 'Follow-up' : 'Send Follow-up') : 'Reply'}
            </Button>
            <Button variant="contained" fullWidth={isMobile} startIcon={<Trash2Icon />}
              onClick={() => setShowDeleteConfirm(true)}
              sx={{ bgcolor: '#e53935', '&:hover': { bgcolor: '#c62828' }, borderRadius: 2,
                fontWeight: 700, fontSize: 13, textTransform: 'none', px: { xs: 1.5, sm: 2.5 }, py: 1 }}>
              Delete
            </Button>
          </Box>
        </Box>

        <Paper elevation={0} sx={{ maxWidth: 620, mx: 'auto', border: '1px solid rgba(0,0,0,0.08)',
          borderRadius: 4, bgcolor: 'white', p: { xs: 2.5, md: 4 }, boxShadow: '0 4px 20px rgba(0,0,0,0.04)' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2.5 }}>
            <Typography sx={{ fontSize: 18, fontWeight: 700 }}>Report Details</Typography>
            {selectedReport.timestamp && (
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
                <ClockIcon sx={{ fontSize: 13, color: '#bbb' }} />
                <Typography sx={{ fontSize: 12, color: '#bbb' }}>{formatTs(selectedReport.timestamp)}</Typography>
              </Box>
            )}
          </Box>

          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2, mb: 2 }}>
            {[
              { label: 'Beekeeper', value: selectedReport.name },
              { label: 'Farm Name', value: selectedReport.farm },
            ].map((item, i) => (
              <Box key={i} sx={{ bgcolor: '#fafafa', border: '1px solid rgba(0,0,0,0.07)', borderRadius: 2, p: 2 }}>
                <Typography sx={{ fontSize: 11, color: '#aaa', mb: 0.5 }}>{item.label}</Typography>
                <Typography sx={{ fontWeight: 700, fontSize: 15 }}>{item.value}</Typography>
              </Box>
            ))}
          </Box>

          <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' }, gap: 2, mb: 3 }}>
            <Box sx={{ bgcolor: '#fafafa', border: '1px solid rgba(0,0,0,0.07)', borderRadius: 3, p: 2 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 0.5 }}>
                <MapPinIcon sx={{ fontSize: 14, color: '#43a047' }} />
                <Typography sx={{ fontSize: 11, color: '#43a047', fontWeight: 600 }}>Location</Typography>
              </Box>
              <Typography sx={{ fontWeight: 700, fontSize: 16 }}>{selectedReport.location || 'Unknown'}</Typography>
            </Box>
            <Box sx={{ bgcolor: '#fafafa', border: '1px solid rgba(0,0,0,0.07)', borderRadius: 3, p: 2 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 0.5 }}>
                <LayersIcon sx={{ fontSize: 14, color: '#43a047' }} />
                <Typography sx={{ fontSize: 11, color: '#43a047', fontWeight: 600 }}>Active Hives</Typography>
              </Box>
              <Typography sx={{ fontWeight: 700, fontSize: 16 }}>{selectedReport.hiveCount}</Typography>
            </Box>
          </Box>

          <Typography sx={{ fontSize: 12, color: '#aaa', mb: 1 }}>Report Message</Typography>
          <Box sx={{ bgcolor: '#fafafa', border: '1px solid rgba(0,0,0,0.07)', borderRadius: 2, p: 2,
            mb: (showReplyBox || selectedReport.reply) ? 2 : 0 }}>
            <Typography sx={{ fontSize: 14, color: '#444', lineHeight: 1.75 }}>{selectedReport.message}</Typography>
          </Box>

          {selectedReport.reply && (
            <>
              <Typography sx={{ fontSize: 12, color: '#aaa', mb: 1, mt: 2 }}>Admin Reply</Typography>
              <Box sx={{ bgcolor: '#e8f5e9', border: '1px solid #c8e6c9', borderRadius: 2, p: 2 }}>
                <Typography sx={{ fontSize: 14, color: '#1a5c2a', lineHeight: 1.75 }}>{selectedReport.reply}</Typography>
              </Box>
            </>
          )}

          {showReplyBox && (
            <Box sx={{ mt: 2 }}>
              <Typography sx={{ fontSize: 12, color: '#aaa', mb: 1 }}>
                {selectedReport.reply ? 'Send Follow-up Reply' : 'Your Reply'}
              </Typography>
              <TextField fullWidth multiline rows={3} variant="outlined"
                placeholder="Type your message to the beekeeper..."
                value={replyText} onChange={e => setReplyText(e.target.value)}
                sx={{ mb: 1.5, '& .MuiOutlinedInput-root': { borderRadius: 2, fontSize: 14 } }} />
              <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
                <Button onClick={() => setShowReplyBox(false)} color="inherit"
                  sx={{ textTransform: 'none', fontSize: 13 }}>Cancel</Button>
                <Button variant="contained" endIcon={<SendIcon />} onClick={handleSendReply}
                  disabled={!replyText.trim()}
                  sx={{ bgcolor: '#2e7d32', '&:hover': { bgcolor: '#1a5c2a' },
                    textTransform: 'none', fontWeight: 700, fontSize: 13, borderRadius: 2 }}>
                  Send Reply
                </Button>
              </Box>
            </Box>
          )}
        </Paper>

        <Dialog open={showSuccess} PaperProps={{ sx: { p: 4, borderRadius: 4, textAlign: 'center', minWidth: 280 } }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <CheckCircleIcon sx={{ fontSize: 56, color: '#43a047', mb: 2 }} />
            <Typography variant="h6" fontWeight={700} color="success.main">Reply Sent!</Typography>
            <Typography color="text.secondary" fontSize={13}>Your message was saved to the database.</Typography>
          </Box>
        </Dialog>

        <Dialog open={showDeleteConfirm} onClose={() => setShowDeleteConfirm(false)}
          PaperProps={{ sx: { borderRadius: 3, p: 1, mx: 2, minWidth: 300 } }}>
          <Box sx={{ p: 2 }}>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 1.5 }}>
              <WarningIcon sx={{ color: '#e53935', fontSize: 28 }} />
              <Typography variant="h6" fontWeight={700}>Delete Report?</Typography>
            </Box>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
              This will permanently delete this report from {selectedReport.name}. This cannot be undone.
            </Typography>
            <Box sx={{ display: 'flex', gap: 1.5, justifyContent: 'flex-end' }}>
              <Button onClick={() => setShowDeleteConfirm(false)} color="inherit">Cancel</Button>
              <Button variant="contained" color="error" onClick={() => handleDelete(selectedReport.id)}>
                Yes, Delete
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
      {/* Header */}
      <Box sx={{ display: 'flex', alignItems: { xs: 'flex-start', sm: 'center' }, justifyContent: 'space-between',
        mb: 4, flexDirection: { xs: 'column', sm: 'row' }, gap: 2 }}>
        <Box>
          <Typography variant="h4" fontWeight={800} sx={{ fontSize: { xs: '1.75rem', md: '2.25rem' }, color: '#1a5c2a' }}>
            Reports Feed
          </Typography>
          <Typography sx={{ color: '#666', fontSize: 14, mt: 0.5 }}>
            {pendingCount > 0 ? `${pendingCount} alerts pending` : 'System healthy'} · {reports.length} total reports
          </Typography>
        </Box>
        {pendingCount > 0 && (
          <Chip label={`${pendingCount} Pending`}
            sx={{ bgcolor: '#fee2e2', color: '#dc2626', fontWeight: 800, fontSize: 13,
              px: 1, height: 36, borderRadius: 2, border: '1px solid #fca5a5' }} />
        )}
      </Box>

      {/* Search + Filter Bar */}
      <Box sx={{ display: 'flex', gap: 2, mb: 4, flexDirection: { xs: 'column', lg: 'row' }, overflow: 'hidden' }}>
        <Paper elevation={0} sx={{ display: 'flex', alignItems: 'center',
          border: '1px solid rgba(0,0,0,0.12)', borderRadius: 3, px: 2.5, py: 1.2,
          gap: 1.5, flex: { lg: 1 }, width: '100%' }}>
          <SearchIcon sx={{ fontSize: 20, color: '#999' }} />
          <InputBase placeholder="Search reports..."
            value={search} onChange={e => setSearch(e.target.value)}
            sx={{ fontSize: 15, flex: 1 }} />
        </Paper>

        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', pb: { xs: 1, lg: 0 } }}>
          {['all', 'pending', 'replied', ...farmNames].map(f => (
            <Chip key={f}
              label={f === 'all' ? 'All' : f === 'pending' ? 'Pending' : f === 'replied' ? 'Replied' : f}
              onClick={() => setFilter(f)}
              sx={{
                cursor: 'pointer',
                bgcolor: filter === f ? '#1a5c2a' : '#fff',
                color: filter === f ? '#fff' : '#555',
                border: '1px solid',
                borderColor: filter === f ? '#1a5c2a' : 'rgba(0,0,0,0.15)',
                fontWeight: filter === f ? 700 : 500,
                fontSize: 13,
                px: 1,
                transition: 'all 0.2s',
                '&:hover': { bgcolor: filter === f ? '#1a5c2a' : '#f5f5f5' }
              }}
            />
          ))}
        </Box>
      </Box>

      {/* Report Cards */}
      {loading ? (
        <Box sx={{ py: 6, textAlign: 'center' }}>
          <CircularProgress size={32} sx={{ color: '#2e7d32' }} />
        </Box>
      ) : filtered.length === 0 ? (
        <Paper elevation={0} sx={{ p: { xs: 4, md: 8 }, textAlign: 'center',
          border: '1px solid rgba(0,0,0,0.08)', borderRadius: 3 }}>
          <Typography sx={{ fontSize: 36, mb: 1 }}>📋</Typography>
          <Typography variant="h6" color="text.secondary" fontWeight={700}>
            {reports.length === 0 ? 'No Reports Received' : 'No Matching Reports'}
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            {reports.length === 0
              ? 'Beekeeper reports from the mobile app will appear here.'
              : 'Try adjusting your search or filter.'}
          </Typography>
        </Paper>
      ) : (
        <Box sx={{ display: 'grid', gridTemplateColumns: { xs: '1fr', lg: '1fr 1fr' }, gap: 2 }}>
          {filtered.map(report => {
            const avatarColor = getAvatarColor(report.name);
            return (
              <Paper key={report.id} elevation={0}
                onClick={() => {
                  setSelectedReport(report);
                  if (report.unread) updateDoc(doc(db, 'reports', report.id), { unread: false });
                }}
                sx={{
                  border: '1px solid rgba(0,0,0,0.09)',
                  borderLeft: report.unread ? '4px solid #2e7d32' : '1px solid rgba(0,0,0,0.09)',
                  borderRadius: 3, p: { xs: 2, md: 2.5 },
                  cursor: 'pointer', bgcolor: 'white',
                  '&:hover': { boxShadow: '0 4px 20px rgba(0,0,0,0.08)', transform: 'translateY(-1px)' },
                  transition: 'all 0.2s',
                }}>
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 1.5 }}>
                  <Box sx={{ width: 44, height: 44, borderRadius: 2, flexShrink: 0,
                    bgcolor: avatarColor, display: 'flex', alignItems: 'center',
                    justifyContent: 'center', color: 'white', fontWeight: 800, fontSize: 19 }}>
                    {getInitial(report.name)}
                  </Box>
                  <Box sx={{ flex: 1, overflow: 'hidden', minWidth: 0 }}>
                    <Typography sx={{ fontWeight: 700, fontSize: 14, whiteSpace: 'nowrap',
                      overflow: 'hidden', textOverflow: 'ellipsis' }}>{report.name}</Typography>
                    <Typography sx={{ fontSize: 12, color: '#2e7d32', fontWeight: 600,
                      whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{report.farm}</Typography>
                  </Box>
                  <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 0.5, flexShrink: 0 }}>
                    {report.reply && (
                      <Chip label="Replied" size="small"
                        sx={{ bgcolor: '#e8f5e9', color: '#2e7d32', fontWeight: 700, fontSize: 10 }} />
                    )}
                    <ChevronRightIcon sx={{ fontSize: 18, color: '#ccc' }} />
                  </Box>
                </Box>

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

                <Typography sx={{ fontSize: { xs: 12, md: 13 }, color: '#555', pl: { xs: 0, sm: 7 },
                  overflow: 'hidden', display: '-webkit-box',
                  WebkitLineClamp: 3, WebkitBoxOrient: 'vertical', lineHeight: 1.6 }}>
                  {report.message}
                </Typography>

                {report.timestamp && (
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mt: 1, pl: { xs: 0, sm: 7 } }}>
                    <ClockIcon sx={{ fontSize: 12, color: '#ccc' }} />
                    <Typography sx={{ fontSize: 11, color: '#bbb' }}>{formatTs(report.timestamp)}</Typography>
                  </Box>
                )}
              </Paper>
            );
          })}
        </Box>
      )}
    </Box>
  );
}
