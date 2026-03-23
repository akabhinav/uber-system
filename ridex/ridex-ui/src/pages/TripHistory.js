import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { getRiderTrips } from '../api';

function TripHistory() {
  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(true);
  const riderId = localStorage.getItem('ridex_userId');

  useEffect(() => {
    const fetchTrips = async () => {
      try {
        const res = await getRiderTrips(riderId);
        if (res.data.success) {
          setTrips(res.data.data);
        }
      } catch (err) {
        toast.error('Failed to load trip history');
      }
      setLoading(false);
    };
    if (riderId) fetchTrips();
    else setLoading(false);
  }, [riderId]);

  if (loading) return <div className="card">Loading trip history...</div>;

  return (
    <div style={{ maxWidth: '800px', margin: '0 auto' }}>
      <h2 style={{ marginBottom: '24px' }}>Trip History</h2>

      {trips.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '48px' }}>
          <p style={{ color: '#888', fontSize: '18px' }}>No trips yet</p>
          <Link to="/rider" className="btn btn-primary" style={{ marginTop: '16px' }}>
            Book your first ride
          </Link>
        </div>
      ) : (
        trips.map((trip) => (
          <Link to={`/trip/${trip.id}`} key={trip.id}>
            <div className="card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer' }}>
              <div>
                <div style={{ fontWeight: 600, marginBottom: '4px' }}>
                  ({trip.pickupLat?.toFixed(3)}, {trip.pickupLng?.toFixed(3)}) &rarr;
                  ({trip.dropoffLat?.toFixed(3)}, {trip.dropoffLng?.toFixed(3)})
                </div>
                <div style={{ fontSize: '13px', color: '#888' }}>
                  {trip.createdAt ? new Date(trip.createdAt).toLocaleString() : 'N/A'}
                </div>
              </div>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontWeight: 700, fontSize: '18px' }}>
                  ${(trip.fareCents / 100).toFixed(2)}
                </div>
                <span className={`status-badge status-${trip.status?.toLowerCase()}`}>
                  {trip.status}
                </span>
              </div>
            </div>
          </Link>
        ))
      )}
    </div>
  );
}

export default TripHistory;
