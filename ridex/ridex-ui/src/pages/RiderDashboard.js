import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { requestTrip, getPriceEstimate, connectRiderWs } from '../api';
import MapView from '../components/MapView';

function RiderDashboard() {
  const navigate = useNavigate();
  const [pickup, setPickup] = useState({ lat: 12.9716, lng: 77.5946 });
  const [dropoff, setDropoff] = useState({ lat: 12.9352, lng: 77.6245 });
  const [priceEstimate, setPriceEstimate] = useState(null);
  const [activeTrip, setActiveTrip] = useState(null);
  const [driverLocation, setDriverLocation] = useState(null);
  const [loading, setLoading] = useState(false);

  const riderId = localStorage.getItem('ridex_userId');

  const handleEstimate = async () => {
    try {
      const distanceKm = haversine(pickup.lat, pickup.lng, dropoff.lat, dropoff.lng);
      const durationMinutes = Math.ceil(distanceKm / 0.5);
      const res = await getPriceEstimate(distanceKm, durationMinutes, 'default');
      if (res.data.success) {
        setPriceEstimate(res.data.data);
      }
    } catch (err) {
      toast.error('Failed to get estimate');
    }
  };

  const handleRequestTrip = async () => {
    setLoading(true);
    try {
      const fareEstimate = priceEstimate ? priceEstimate.totalCents : 50000;
      const res = await requestTrip({
        riderId,
        pickupLat: pickup.lat,
        pickupLng: pickup.lng,
        dropoffLat: dropoff.lat,
        dropoffLng: dropoff.lng,
        fareEstimateCents: fareEstimate,
      });
      if (res.data.success) {
        setActiveTrip(res.data.data);
        toast.success('Trip requested! Looking for a driver...');
      }
    } catch (err) {
      toast.error('Failed to request trip');
    }
    setLoading(false);
  };

  useEffect(() => {
    if (activeTrip?.driverId) {
      const ws = connectRiderWs(activeTrip.driverId);
      ws.onmessage = (event) => {
        try {
          const loc = JSON.parse(event.data);
          setDriverLocation({ lat: loc.lat, lng: loc.lng });
        } catch (e) {}
      };
      return () => ws.close();
    }
  }, [activeTrip?.driverId]);

  useEffect(() => {
    handleEstimate();
  }, [pickup, dropoff]);

  return (
    <div>
      <h2 style={{ marginBottom: '24px' }}>Book a Ride</h2>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '24px' }}>
        <div>
          <div className="card">
            <h3 style={{ marginBottom: '16px' }}>Pickup Location</h3>
            <div className="input-group">
              <label>Latitude</label>
              <input type="number" step="0.0001" value={pickup.lat}
                     onChange={(e) => setPickup({...pickup, lat: parseFloat(e.target.value)})} />
            </div>
            <div className="input-group">
              <label>Longitude</label>
              <input type="number" step="0.0001" value={pickup.lng}
                     onChange={(e) => setPickup({...pickup, lng: parseFloat(e.target.value)})} />
            </div>
          </div>

          <div className="card">
            <h3 style={{ marginBottom: '16px' }}>Dropoff Location</h3>
            <div className="input-group">
              <label>Latitude</label>
              <input type="number" step="0.0001" value={dropoff.lat}
                     onChange={(e) => setDropoff({...dropoff, lat: parseFloat(e.target.value)})} />
            </div>
            <div className="input-group">
              <label>Longitude</label>
              <input type="number" step="0.0001" value={dropoff.lng}
                     onChange={(e) => setDropoff({...dropoff, lng: parseFloat(e.target.value)})} />
            </div>
          </div>

          {priceEstimate && (
            <div className="card" style={{ background: '#f0fff0' }}>
              <h3 style={{ marginBottom: '8px' }}>Price Estimate</h3>
              <div style={{ fontSize: '32px', fontWeight: 800 }}>
                ${(priceEstimate.totalCents / 100).toFixed(2)}
              </div>
              <div style={{ fontSize: '14px', color: '#666', marginTop: '8px' }}>
                Base: ${(priceEstimate.baseFareCents / 100).toFixed(2)} |
                Distance: ${(priceEstimate.distanceFareCents / 100).toFixed(2)} |
                Time: ${(priceEstimate.timeFareCents / 100).toFixed(2)}
                {priceEstimate.surgeMultiplier > 1 && (
                  <span style={{ color: '#dc3545' }}> | Surge: {priceEstimate.surgeMultiplier}x</span>
                )}
              </div>
            </div>
          )}

          {!activeTrip ? (
            <button className="btn btn-primary" style={{ width: '100%', padding: '16px', fontSize: '18px' }}
                    onClick={handleRequestTrip} disabled={loading}>
              {loading ? 'Requesting...' : 'Request Ride'}
            </button>
          ) : (
            <div className="card">
              <h3>Active Trip</h3>
              <p>Trip ID: {activeTrip.id}</p>
              <span className={`status-badge status-${activeTrip.status?.toLowerCase()}`}>
                {activeTrip.status}
              </span>
              <button className="btn btn-secondary" style={{ marginTop: '12px', width: '100%' }}
                      onClick={() => navigate(`/trip/${activeTrip.id}`)}>
                View Details
              </button>
            </div>
          )}
        </div>

        <div>
          <MapView
            pickup={pickup}
            dropoff={dropoff}
            driverLocation={driverLocation}
            onPickupChange={setPickup}
            onDropoffChange={setDropoff}
          />
        </div>
      </div>
    </div>
  );
}

function haversine(lat1, lng1, lat2, lng2) {
  const R = 6371;
  const dLat = (lat2 - lat1) * Math.PI / 180;
  const dLng = (lng2 - lng1) * Math.PI / 180;
  const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
    Math.cos(lat1 * Math.PI/180) * Math.cos(lat2 * Math.PI/180) *
    Math.sin(dLng/2) * Math.sin(dLng/2);
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
}

export default RiderDashboard;
