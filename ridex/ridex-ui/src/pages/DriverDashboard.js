import React, { useState, useEffect, useRef } from 'react';
import toast from 'react-hot-toast';
import { connectDriverWs } from '../api';

function DriverDashboard() {
  const [isOnline, setIsOnline] = useState(false);
  const [location, setLocation] = useState({ lat: 12.9716, lng: 77.5946 });
  const [status, setStatus] = useState('OFFLINE');
  const wsRef = useRef(null);
  const intervalRef = useRef(null);

  const driverId = localStorage.getItem('ridex_userId');

  const goOnline = () => {
    try {
      wsRef.current = connectDriverWs(driverId);
      wsRef.current.onopen = () => {
        setIsOnline(true);
        setStatus('AVAILABLE');
        toast.success('You are now online!');
        startLocationUpdates();
      };
      wsRef.current.onclose = () => {
        setIsOnline(false);
        setStatus('OFFLINE');
      };
      wsRef.current.onerror = () => {
        toast.error('WebSocket connection failed');
      };
    } catch (err) {
      toast.error('Failed to connect');
    }
  };

  const goOffline = () => {
    if (wsRef.current) {
      wsRef.current.close();
    }
    if (intervalRef.current) {
      clearInterval(intervalRef.current);
    }
    setIsOnline(false);
    setStatus('OFFLINE');
    toast('You are now offline');
  };

  const startLocationUpdates = () => {
    intervalRef.current = setInterval(() => {
      if (wsRef.current?.readyState === WebSocket.OPEN) {
        // Simulate small movement
        const newLat = location.lat + (Math.random() - 0.5) * 0.001;
        const newLng = location.lng + (Math.random() - 0.5) * 0.001;
        setLocation({ lat: newLat, lng: newLng });

        wsRef.current.send(JSON.stringify({
          driverId,
          lat: newLat,
          lng: newLng,
          speed: 30 + Math.random() * 20,
          heading: Math.random() * 360,
          timestamp: new Date().toISOString(),
        }));
      }
    }, 4000);
  };

  useEffect(() => {
    return () => {
      if (wsRef.current) wsRef.current.close();
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, []);

  return (
    <div style={{ maxWidth: '600px', margin: '0 auto' }}>
      <h2 style={{ marginBottom: '24px' }}>Driver Dashboard</h2>

      <div className="card" style={{ textAlign: 'center' }}>
        <div style={{
          width: '120px', height: '120px', borderRadius: '50%',
          margin: '0 auto 16px',
          background: isOnline ? '#28a745' : '#6c757d',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          color: '#fff', fontSize: '18px', fontWeight: 700
        }}>
          {status}
        </div>

        <h3 style={{ marginBottom: '8px' }}>Driver ID</h3>
        <p style={{ fontSize: '12px', color: '#888', marginBottom: '24px', wordBreak: 'break-all' }}>
          {driverId}
        </p>

        {!isOnline ? (
          <button className="btn btn-success" style={{ width: '100%', padding: '16px', fontSize: '18px' }}
                  onClick={goOnline}>
            Go Online
          </button>
        ) : (
          <button className="btn btn-danger" style={{ width: '100%', padding: '16px', fontSize: '18px' }}
                  onClick={goOffline}>
            Go Offline
          </button>
        )}
      </div>

      {isOnline && (
        <div className="card">
          <h3 style={{ marginBottom: '12px' }}>Current Location</h3>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            <div>
              <span style={{ color: '#888', fontSize: '14px' }}>Latitude</span>
              <div style={{ fontWeight: 600 }}>{location.lat.toFixed(6)}</div>
            </div>
            <div>
              <span style={{ color: '#888', fontSize: '14px' }}>Longitude</span>
              <div style={{ fontWeight: 600 }}>{location.lng.toFixed(6)}</div>
            </div>
          </div>
          <p style={{ marginTop: '12px', fontSize: '14px', color: '#888' }}>
            Sending location updates every 4 seconds...
          </p>
        </div>
      )}

      <div className="card">
        <h3 style={{ marginBottom: '12px' }}>Today's Stats</h3>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px', textAlign: 'center' }}>
          <div>
            <div style={{ fontSize: '28px', fontWeight: 800 }}>0</div>
            <div style={{ fontSize: '14px', color: '#888' }}>Trips</div>
          </div>
          <div>
            <div style={{ fontSize: '28px', fontWeight: 800 }}>$0.00</div>
            <div style={{ fontSize: '14px', color: '#888' }}>Earnings</div>
          </div>
          <div>
            <div style={{ fontSize: '28px', fontWeight: 800 }}>0h</div>
            <div style={{ fontSize: '14px', color: '#888' }}>Online</div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default DriverDashboard;
