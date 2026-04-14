import React from 'react';
import { Box, Typography, Button } from '@mui/material';
import { ErrorOutline as ErrorIcon } from '@mui/icons-material';

/**
 * Catches unhandled JS errors in any child component tree.
 * Prevents the entire admin panel from going blank on runtime errors.
 */
export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, info) {
    // Log to console for debugging — swap for Sentry in production
    console.error('ErrorBoundary caught:', error, info);
  }

  render() {
    if (this.state.hasError) {
      return (
        <Box sx={{
          minHeight: '100vh',
          display: 'flex', flexDirection: 'column',
          alignItems: 'center', justifyContent: 'center',
          gap: 2, p: 4, bgcolor: '#f4f6f8',
        }}>
          <ErrorIcon sx={{ fontSize: 56, color: '#e53935' }} />
          <Typography variant="h5" fontWeight={700} color="text.primary">
            Something went wrong
          </Typography>
          <Typography color="text.secondary" sx={{ maxWidth: 420, textAlign: 'center', fontSize: 14 }}>
            {this.state.error?.message || 'An unexpected error occurred in the admin panel.'}
          </Typography>
          <Button
            variant="contained"
            onClick={() => window.location.reload()}
            sx={{ bgcolor: '#1a5c2a', '&:hover': { bgcolor: '#0f4020' }, mt: 1 }}
          >
            Reload Page
          </Button>
        </Box>
      );
    }
    return this.props.children;
  }
}
