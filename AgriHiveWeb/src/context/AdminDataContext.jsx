import React, { createContext, useContext, useState, useEffect } from 'react';
import { collection, onSnapshot, query, orderBy, limit } from 'firebase/firestore';
import { db } from '../firebase';

const AdminDataContext = createContext(null);

/**
 * Single source of truth for users + reports + subscriptions.
 * All pages consume from here — no duplicate Firestore listeners.
 */
export function AdminDataProvider({ children }) {
  const [users, setUsers] = useState([]);
  const [reports, setReports] = useState([]);
  const [subscriptions, setSubscriptions] = useState([]);
  const [usersLoading, setUsersLoading] = useState(true);
  const [reportsLoading, setReportsLoading] = useState(true);
  const [subsLoading, setSubsLoading] = useState(true);

  useEffect(() => {
    // ── Users listener (latest 500) ────────────────────────────────
    const unsubUsers = onSnapshot(
      query(collection(db, 'users'), limit(500)),
      (snap) => {
        setUsers(snap.docs.map(d => ({ id: d.id, ...d.data() })));
        setUsersLoading(false);
      },
      (err) => { console.error('Users listener:', err); setUsersLoading(false); }
    );

    // ── Reports listener (latest 100, real-time) ────────────────────
    const unsubReports = onSnapshot(
      query(collection(db, 'reports'), orderBy('timestamp', 'desc'), limit(100)),
      (snap) => {
        setReports(snap.docs.map(d => {
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
        }));
        setReportsLoading(false);
      },
      (err) => { console.error('Reports listener:', err); setReportsLoading(false); }
    );

    // ── Subscriptions listener (latest 500) ────────────────────────
    const unsubSubs = onSnapshot(
      query(collection(db, 'subscriptions'), limit(500)),
      (snap) => {
        setSubscriptions(snap.docs.map(d => {
          const data = d.data();
          let daysLeft = 0;
          if (data.due) {
            const diff = new Date(data.due) - new Date();
            daysLeft = Math.ceil(diff / (1000 * 60 * 60 * 24));
          }
          return { id: d.id, ...data, daysLeft };
        }));
        setSubsLoading(false);
      },
      (err) => { console.error('Subs listener:', err); setSubsLoading(false); }
    );

    return () => { unsubUsers(); unsubReports(); unsubSubs(); };
  }, []);

  // Derived: set of farm keys that have pending reports (used by Dashboard)
  const pendingReportFarmKeys = new Set(
    reports
      .filter(r => r.unread)
      .flatMap(r => [r.farm, r.farmName, r.userId].filter(Boolean))
  );

  return (
    <AdminDataContext.Provider value={{
      users, usersLoading,
      reports, reportsLoading,
      subscriptions, subsLoading,
      pendingReportFarmKeys,
    }}>
      {children}
    </AdminDataContext.Provider>
  );
}

export function useAdminData() {
  const ctx = useContext(AdminDataContext);
  if (!ctx) throw new Error('useAdminData must be used inside AdminDataProvider');
  return ctx;
}
