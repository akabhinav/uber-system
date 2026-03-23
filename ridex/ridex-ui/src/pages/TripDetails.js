import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { getTrip, cancelTrip, getPayment } from '../api';

function TripDetails() {
  const { tripId } = useParams();
  const navigate = useNavigate();
  const [trip, setTrip] = useState(null);
  const [payment, setPayment] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchTrip = async () => {
      try {
        const res = await getTrip(tripId);
        setTrip(res.data);
        if (res.data.status === 'COMPLETED') {
          try {
            const payRes = await getPayment(tripId);
            setPayment(payRes.data);
          } catch {}
        }
      } catch (err) {
        toast.error('Failed to load trip');
      } finally {
        setLoading(false);
      }
    };
    fetchTrip();
    const interval = setInterval(fetchTrip, 5000);
    return () => clearInterval(interval);
  }, [tripId]);

  const handleCancel = async () => {
    try {
      await cancelTrip(tripId);
      toast.success('Trip cancelled');
      setTrip((prev) => ({ ...prev, status: 'CANCELLED' }));
    } catch (err) {
      toast.error('Failed to cancel trip');
    }
  };

  if (loading) return <div style={{ textAlign: 'center', padding: '40px' }}>Loading...</div>;
  if (!trip) return <div style={{ textAlign: 'center', padding: '40px' }}>Trip not found</div>;

  const statusClass = `status-badge status-${trip.status?.toLowerCase()}`;

  return (
    <div style={{ maxWidth: '600px', margin: '0 auto' }}>
      <h1 style={{ marginBottom: '24px' }}>Trip Details</h1>

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3>Trip #{tripId.slice(0, 8)}</h3>
          <span className={statusClass}>{trip.status}</span>
        </div>

        <div style={{ marginBottom: '12px' }}>
          <p style={{ color: '#666', fontSize: '14px' }}>Pickup</p>
          <p>{trip.pickupLocation?.lat}, {trip.pickupLocation?.lng}</p>
        </div>

        <div style={{ marginBottom: '12px' }}>
          <p style={{ color: '#666', fontSize: '14px' }}>Dropoff</p>
          <p>{trip.dropoffLocation?.lat}, {trip.dropoffLocation?.lng}</p>
        </div>

        {trip.driverId && (
          <div style={{ marginBottom: '12px' }}>
            <p style={{ color: '#666', fontSize: '14px' }}>Driver</p>
            <p>{trip.driverId}</p>
          </div>
        )}

        {trip.fare && (
          <div style={{ marginBottom: '12px' }}>
            <p style={{ color: '#666', fontSize: '14px' }}>Fare</p>
            <p style={{ fontSize: '24px', fontWeight: '700' }}>${trip.fare.toFixed(2)}</p>
          </div>
        )}

        {payment && (
          <div style={{ marginBottom: '12px', padding: '12px', background: '#f0f9f0', borderRadius: '8px' }}>
            <p style={{ color: '#666', fontSize: '14px' }}>Payment</p>
            <p>Status: <strong>{payment.status}</strong></p>
            <p>Amount: <strong>${payment.amount?.toFixed(2)}</strong></p>
          </div>
        )}

        <div style={{ display: 'flex', gap: '12px', marginTop: '16px' }}>
          {['REQUESTED', 'MATCHED', 'DRIVER_EN_ROUTE'].includes(trip.status) && (
            <button className="btn btn-danger" onClick={handleCancel}>Cancel Trip</button>
          )}
          <button className="btn btn-secondary" onClick={() => navigate('/history')}>View History</button>
        </div>
      </div>
    </div>
  );
}

export default TripDetails;
