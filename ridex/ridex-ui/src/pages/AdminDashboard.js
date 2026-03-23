import React, { useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import { getSurge } from '../api';

function AdminDashboard() {
  const [services, setServices] = useState([]);
  const [surge, setSurge] = useState(null);

  const serviceList = [
    { name: 'trip-service', port: 3001 },
    { name: 'location-service', port: 3002 },
    { name: 'matching-service', port: 3003 },
    { name: 'auth-service', port: 3004 },
    { name: 'payment-service', port: 3005 },
    { name: 'pricing-service', port: 3006 },
  ];

  const checkHealth = async () => {
    const results = await Promise.all(
      serviceList.map(async (svc) => {
        try {
          const res = await fetch(`http://localhost:${svc.port}/actuator/health`, {
            signal: AbortSignal.timeout(3000)
          });
          const data = await res.json();
          return { ...svc, status: data.status || 'UP', healthy: true };
        } catch {
          return { ...svc, status: 'DOWN', healthy: false };
        }
      })
    );
    setServices(results);
  };

  const fetchSurge = async () => {
    try {
      const res = await getSurge('default');
      if (res.data.success) setSurge(res.data.data);
    } catch {}
  };

  useEffect(() => {
    checkHealth();
    fetchSurge();
    const interval = setInterval(checkHealth, 10000);
    return () => clearInterval(interval);
  }, []);

  return (
    <div>
      <h2 style={{ marginBottom: '24px' }}>Admin Dashboard</h2>

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3>Service Health</h3>
          <button className="btn btn-secondary" onClick={checkHealth} style={{ fontSize: '14px', padding: '6px 16px' }}>
            Refresh
          </button>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '12px' }}>
          {services.map((svc) => (
            <div key={svc.name} className="card" style={{
              borderLeft: `4px solid ${svc.healthy ? '#28a745' : '#dc3545'}`,
              background: svc.healthy ? '#f0fff0' : '#fff0f0'
            }}>
              <div style={{ fontWeight: 600, marginBottom: '4px' }}>{svc.name}</div>
              <div style={{ fontSize: '13px', color: '#888' }}>Port {svc.port}</div>
              <div style={{ marginTop: '8px', fontWeight: 700, color: svc.healthy ? '#28a745' : '#dc3545' }}>
                {svc.status}
              </div>
            </div>
          ))}
        </div>
      </div>

      {surge && (
        <div className="card">
          <h3 style={{ marginBottom: '12px' }}>Surge Pricing</h3>
          <div style={{ fontSize: '48px', fontWeight: 800 }}>{surge.surgeMultiplier}x</div>
          <div style={{ color: '#888' }}>Cell: {surge.geohashCell}</div>
        </div>
      )}

      <div className="card">
        <h3 style={{ marginBottom: '12px' }}>Quick Links</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '8px' }}>
          {serviceList.map((svc) => (
            <a key={svc.name}
               href={`http://localhost:${svc.port}/actuator/health`}
               target="_blank" rel="noreferrer"
               className="btn btn-secondary" style={{ textAlign: 'center', fontSize: '13px' }}>
              {svc.name} Health
            </a>
          ))}
        </div>
      </div>
    </div>
  );
}

export default AdminDashboard;
