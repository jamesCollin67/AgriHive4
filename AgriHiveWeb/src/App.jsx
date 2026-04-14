import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Box, CircularProgress, Typography } from '@mui/material';
import { AuthProvider, useAuth } from './context/AuthContext';
import { AdminDataProvider } from './context/AdminDataContext';

import Login from './pages/Login';
import DashboardLayout from './layouts/DashboardLayout';
import Dashboard from './pages/Dashboard';
import Reports from './pages/Reports';
import Subscriptions from './pages/Subscriptions';
import Users from './pages/Users';

const Spinner = () => (
  <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
    <CircularProgress sx={{ color: '#1a5c2a' }} />
  </Box>
);

/**
 * Guards all admin routes.
 * - Shows spinner while Firebase resolves auth state AND admin check
 * - Redirects to /login if not authenticated
 * - Shows "Access Denied" only if auth is fully resolved AND user is NOT an admin
 */
function AdminRoute({ children }) {
  const { user, isAdmin, authReady, adminChecked } = useAuth();

  // Still waiting for Firebase auth or Firestore admin check
  if (!authReady || (user && !adminChecked)) return <Spinner />;

  if (!user) return <Navigate to="/login" replace />;

  if (!isAdmin) {
    return (
      <Box sx={{
        minHeight: '100vh', display: 'flex', flexDirection: 'column',
        alignItems: 'center', justifyContent: 'center', gap: 2, p: 4,
        background: 'linear-gradient(135deg, #2e7d32 0%, #1b5e20 100%)',
      }}>
        <Typography variant="h5" fontWeight={700} sx={{ color: 'white' }}>Access Denied</Typography>
        <Typography sx={{ color: 'rgba(255,255,255,0.8)', textAlign: 'center', maxWidth: 360 }}>
          Your account does not have admin privileges. Please contact the system administrator.
        </Typography>
      </Box>
    );
  }

  return children;
}

function AppRoutes() {
  const { user, isAdmin, authReady, adminChecked } = useAuth();

  return (
    <Box sx={{ width: '100vw', minHeight: '100vh' }}>
      <Routes>
        <Route
          path="/login"
          element={
            authReady && adminChecked && user && isAdmin
              ? <Navigate to="/dashboard" replace />
              : <Login />
          }
        />

        {/* Admin-only routes wrapped in shared data provider */}
        <Route
          path="/"
          element={
            <AdminRoute>
              <AdminDataProvider>
                <DashboardLayout />
              </AdminDataProvider>
            </AdminRoute>
          }
        >
          <Route index element={<Navigate to="/dashboard" replace />} />
          <Route path="dashboard" element={<Dashboard />} />
          <Route path="reports" element={<Reports />} />
          <Route path="subscriptions" element={<Subscriptions />} />
          <Route path="users" element={<Users />} />
        </Route>

        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Box>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  );
}
