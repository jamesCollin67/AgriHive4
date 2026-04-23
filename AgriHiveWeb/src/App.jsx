import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import { Box, CircularProgress } from '@mui/material';
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
  const { user, authReady } = useAuth();

  if (!authReady) return <Spinner />;
  if (!user) return <Navigate to="/login" replace />;

  return children;
}

function AppRoutes() {
  const { user, authReady } = useAuth();

  if (!authReady) return <Spinner />;

  return (
    <Box sx={{ width: '100%', minHeight: '100vh' }}>
      <Routes>
        <Route
          path="/login"
          element={user ? <Navigate to="/dashboard" replace /> : <Login />}
        />

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
