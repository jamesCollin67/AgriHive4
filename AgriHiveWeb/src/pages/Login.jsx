import React, { useState } from 'react';
import { Box, Card, Typography, TextField, Button, InputAdornment, Container } from '@mui/material';
import { Lock, Email as Mail, Agriculture as Hexagon } from '@mui/icons-material';
import { useNavigate } from 'react-router-dom';

import { signInWithEmailAndPassword } from 'firebase/auth';
import { auth } from '../firebase';

export default function Login() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [errorMsg, setErrorMsg] = useState('');
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    if (!email || !password) return;
    
    // Quick Testing Sandbox Account Bypass
    if (email === 'admin@agrihive.com' && password === 'admin123') {
       navigate('/dashboard');
       return;
    }

    try {
      await signInWithEmailAndPassword(auth, email, password);
      navigate('/dashboard');
    } catch (e) {
      setErrorMsg("Invalid credentials or user not found on Firebase.");
    }
  };

  return (
    <Box 
      sx={{ 
        minHeight: '100vh', 
        display: 'flex', 
        alignItems: 'center', 
        justifyContent: 'center',
        background: 'linear-gradient(135deg, #2e7d32 0%, #1b5e20 100%)',
        p: 2
      }}
    >
      <Container maxWidth="xs">
        <Card 
          sx={{ 
            p: { xs: 3, sm: 5 }, 
            width: '100%', 
            borderRadius: 4,
            boxShadow: '0 20px 40px rgba(0,0,0,0.2)',
            textAlign: 'center'
          }}
        >
          <Box sx={{ mb: 3, display: 'flex', justifyContent: 'center', color: '#2e7d32' }}>
            <Hexagon sx={{ fontSize: 48 }} />
          </Box>
          <Typography variant="h4" fontWeight="bold" sx={{ color: '#1a5c2a', mb: 1, fontSize: { xs: '1.75rem', sm: '2.125rem' } }}>
            AgriHive Admin
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
            Control Center Login
          </Typography>

          {errorMsg && (
            <Typography color="error" variant="body2" fontWeight="bold" sx={{ mb: 3 }}>
              {errorMsg}
            </Typography>
          )}

          <form onSubmit={handleLogin}>
            <TextField
              fullWidth
              placeholder="Admin Email"
              variant="outlined"
              margin="normal"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Mail sx={{ fontSize: 20, color: "#757575" }} />
                  </InputAdornment>
                ),
                sx: { borderRadius: 2 }
              }}
            />
            <TextField
              fullWidth
              placeholder="Password"
              type="password"
              variant="outlined"
              margin="normal"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              sx={{ mb: 4 }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <Lock sx={{ fontSize: 20, color: "#757575" }} />
                  </InputAdornment>
                ),
                sx: { borderRadius: 2 }
              }}
            />
            <Button 
              type="submit" 
              fullWidth 
              variant="contained" 
              size="large"
              sx={{ 
                py: 1.5, 
                fontSize: '1.1rem', 
                borderRadius: 2,
                bgcolor: '#1a5c2a',
                '&:hover': { bgcolor: '#0f4020' }
              }}
            >
              Login
            </Button>
          </form>
        </Card>
      </Container>
    </Box>
  );
}
