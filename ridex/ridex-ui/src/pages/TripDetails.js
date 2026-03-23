import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { getTrip, updateTripStatus, cancelTrip } from '../api';

function TripDetails() {
  const { tripId } = useParams();
  const navigate = useNavigate();
  const [trip, setTrip] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchTrip = async () => {
    try {
      const res = await getTrip(tripId);
      if (res.data.success) {
        setTrip(res.data.data);
      }
    } catch (err) {
      toast.error('Failed to load trip');
    }
    setLoading(false);
  };

  useEffect(() => {
    fetchTrip();
    const interval = setInterval(fetchTrip, 5000);
    return () => clearInterval(interval);
  }, [tripId]);

  const handleStatusUpdate = async (newStatus) => {
    try {
      const res = await updateTripStatus(tripId, newStatus);
      if (res.data.success) {
        setTrip(res.data.data);
        toast.success(`Status updated to ${newStatus}`);
      }
    } catch (err) {
      toast.error('Failed to update status');
    }
  };

  const handleCancel = async () => {
    try {
      const res = await cancelTrip(tripId);
      if (res.data.success) {
        setTrip(res.data.data);
        toast.success('Trip cancelled');
      }
    } catch (err) {
      toast.error('Failed to cancel trip');
    }
  };

  if (loading) return <div className="card">Loading trip details...</div>;
  if (!trip) return <div className="card">Trip not found</div>;

  const statusFlow = ['REQUESTED', 'MATCHED', 'DRIVER_EN_ROUTE', 'PICKUP', 'IN_PROGRESS', 'COMPLETED'];

  return (
    <div style={{ maxWidth: '700px', margin: '0 auto' }}>
      <button className="btn btn-secondary" onClick={() => navigate(-1)}
              style={{ marginBottom: '16px' }}>
        &larr; Back
      </button>

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h2>Trip Details</h2>
          <span className={`status-badge status-${trip.status?.toLowerCase()}`}>{trip.status}</span>
        </div>

        <div style={{ fontSize: '12px', color: '#888', marginBottom: '24px' }}>ID: {trip.id}</div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px', marginBottom: '24px' }}>
          <div className="card" style={{ background: '#f8f9fa' }}>
            <strong>Pickup</strong>
            <div style={{ fontSize: '14px', color: '#666' }}>
              {trip.pickupLat?.toFixed(4)}, {trip.pickupLng?.toFixed(4)}
            </div>
          </div>
          <div className="card" style={{ background: '#f8f9fa' }}>
            <strong>Dropoff</strong>
            <div style={{ fontSize: '14px', color: '#666' }}>
              {trip.dropoffLat?.toFixed(4)}, {trip.dropoffLng?.toFixed(4)}
            </div>
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '16px', marginBottom: '24px' }}>
          <div>
            <span style={{ color: '#888', fontSize: '14px' }}>Fare</span>
            <div style={{ fontWeight: 700, fontSize: '20px' }}>
              ${(trip.fareCents / 100).toFixed(2)}
            </div>
          </div>
          <div>
            <span style={{ color: '#888', fontSize: '14px' }}>Surge</span>
            <div style={{ fontWeight: 700, fontSize: '20px' }}>{trip.surgeMultiplier}x</div>
          </div>
          <div>
            <span style={{ color: '#888', fontSize: '14px' }}>Driver</span>
            <div style={{ fontWeight: 700, fontSize: '14px' }}>{trip.driverId || 'Searching...'}</div>
          </div>
        </div>

        {/* Status progression buttons (for dev/testing) */}
        {trip.status !== 'COMPLETED' && trip.status !== 'CANCELLED' && (
          <div>
            <h4 style={{ marginBottom: '12px', color: '#888' }}>Dev Controls</h4>
            <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
              {statusFlow.map((s) => (
                <button key={s} className="btn btn-secondary"
                        style={{ fontSize: '12px', padding: '6px 12px' }}
                        onClick={() => handleStatusUpdate(s)}
                        disabled={s === trip.status}>
                  {s}
                </button>
              ))}
              <button className="btn btn-danger" style={{ fontSize: '12px', padding: '6px 12px' }}
                      onClick={handleCancel}>
                CANCEL
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default TripDetails;
