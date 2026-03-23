import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { connectDriverWs, updateTripStatus } from '../api';

function DriverDashboard() {
  const navigate = useNavigate();
  const [online, setOnline] = useState(false);
  const [currentTrip, setCurrentTrip] = useState(null);
  const [messages, setMessages] = useState([]);
  const wsRef = useRef(null);

  const goOnline = () => {
    const driverId = localStorage.getItem('ridex_userId');
    try {
      const ws = connectDriverWs(driverId);
      ws.onopen = () => {
        setOnline(true);
        toast.success('You are now online');
        ws.send(JSON.stringify({
          type: 'DRIVER_ONLINE',
          driverId,
          location: { lat: 40.7128, lng: -74.0060 },
        }));
      };
      ws.onmessage = (event) => {
        const data = JSON.parse(event.data);
        setMessages((prev) => [...prev, data]);
        if (data.type === 'TRIP_OFFER' || data.type === 'TRIP_ASSIGNED') {
          setCurrentTrip(data);
          toast('New trip offer!', { icon: '🚗' });
        }
      };
      ws.onclose = () => {
        setOnline(false);
        toast('Disconnected');
      };
      ws.onerror = () => {
        toast.error('WebSocket connection failed');
      };
      wsRef.current = ws;
    } catch (err) {
      toast.error('Failed to connect');
    }
  };

  const goOffline = () => {
    if (wsRef.current) {
      wsRef.current.close();
      wsRef.current = null;
    }
    setOnline(false);
    setCurrentTrip(null);
  };

  const handleAccept = async () => {
    if (!currentTrip) return;
    const tripId = currentTrip.tripId || currentTrip.id;
    try {
      await updateTripStatus(tripId, 'DRIVER_EN_ROUTE');
      toast.success('Trip accepted');
    } catch (err) {
      toast.error('Failed to accept trip');
    }
  };

  const handleArrivedPickup = async () => {
    if (!currentTrip) return;
    const tripId = currentTrip.tripId || currentTrip.id;
    try {
      await updateTripStatus(tripId, 'PICKUP');
      toast.success('Marked arrived at pickup');
    } catch (err) {
      toast.error('Failed to update status');
    }
  };

  const handleStartTrip = async () => {
    if (!currentTrip) return;
    const tripId = currentTrip.tripId || currentTrip.id;
    try {
      await updateTripStatus(tripId, 'IN_PROGRESS');
      toast.success('Trip started');
    } catch (err) {
      toast.error('Failed to start trip');
    }
  };

  const handleCompleteTrip = async () => {
    if (!currentTrip) return;
    const tripId = currentTrip.tripId || currentTrip.id;
    try {
      await updateTripStatus(tripId, 'COMPLETED');
      toast.success('Trip completed');
      setCurrentTrip(null);
    } catch (err) {
      toast.error('Failed to complete trip');
    }
  };

  useEffect(() => {
    return () => {
      if (wsRef.current) wsRef.current.close();
    };
  }, []);

  return (
    <div>
      <h1 style={{ marginBottom: '24px' }}>Driver Dashboard</h1>

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <div>
            <span style={{
              display: 'inline-block', width: '12px', height: '12px',
              borderRadius: '50%', background: online ? '#28a745' : '#dc3545',
              marginRight: '8px'
            }} />
            <strong>{online ? 'Online' : 'Offline'}</strong>
          </div>
          {online ? (
            <button className="btn btn-danger" onClick={goOffline}>Go Offline</button>
          ) : (
            <button className="btn btn-success" onClick={goOnline}>Go Online</button>
          )}
        </div>

        {!online && (
          <p style={{ color: '#666' }}>Go online to start receiving trip requests.</p>
        )}

        {online && !currentTrip && (
          <p style={{ color: '#666' }}>Waiting for trip requests...</p>
        )}

        {currentTrip && (
          <div style={{ marginTop: '16px', padding: '16px', background: '#f9f9f9', borderRadius: '8px' }}>
            <h3>Current Trip</h3>
            <p><strong>Trip ID:</strong> {currentTrip.tripId || currentTrip.id}</p>
            {currentTrip.pickupLocation && (
              <p><strong>Pickup:</strong> {currentTrip.pickupLocation.lat}, {currentTrip.pickupLocation.lng}</p>
            )}
            {currentTrip.dropoffLocation && (
              <p><strong>Dropoff:</strong> {currentTrip.dropoffLocation.lat}, {currentTrip.dropoffLocation.lng}</p>
            )}
            <div style={{ display: 'flex', gap: '8px', marginTop: '12px', flexWrap: 'wrap' }}>
              <button className="btn btn-primary" onClick={handleAccept}>Accept</button>
              <button className="btn btn-secondary" onClick={handleArrivedPickup}>Arrived at Pickup</button>
              <button className="btn btn-secondary" onClick={handleStartTrip}>Start Trip</button>
              <button className="btn btn-success" onClick={handleCompleteTrip}>Complete Trip</button>
            </div>
          </div>
        )}
      </div>

      {messages.length > 0 && (
        <div className="card" style={{ marginTop: '16px' }}>
          <h3 style={{ marginBottom: '12px' }}>Messages</h3>
          <div style={{ maxHeight: '300px', overflow: 'auto' }}>
            {messages.map((msg, i) => (
              <div key={i} style={{ padding: '8px', borderBottom: '1px solid #eee', fontSize: '14px' }}>
                <strong>{msg.type}</strong>: {JSON.stringify(msg)}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

export default DriverDashboard;
