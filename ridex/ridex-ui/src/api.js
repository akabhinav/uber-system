import axios from 'axios';

const API_BASE = process.env.REACT_APP_API_URL || 'http://localhost:8080';
const WS_BASE = process.env.REACT_APP_WS_URL || 'ws://localhost:8081';

const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('ridex_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('ridex_token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth
export const sendOtp = (phone) => api.post('/api/auth/otp/send', { phone });
export const verifyOtp = (phone, otp, name) => api.post('/api/auth/otp/verify', { phone, otp, name });
export const getMe = () => api.get('/api/auth/me');
export const logout = () => api.post('/api/auth/logout');

// Trips
export const requestTrip = (data) => api.post('/api/trips', data);
export const getTrip = (tripId) => api.get(`/api/trips/${tripId}`);
export const getRiderTrips = (riderId) => api.get(`/api/trips/rider/${riderId}`);
export const updateTripStatus = (tripId, status) => api.patch(`/api/trips/${tripId}/status`, { status });
export const cancelTrip = (tripId) => api.post(`/api/trips/${tripId}/cancel`);

// Pricing
export const getPriceEstimate = (distanceKm, durationMinutes, geohashCell) =>
  api.get('/api/pricing/estimate', { params: { distanceKm, durationMinutes, geohashCell } });
export const getSurge = (geohashCell) =>
  api.get('/api/pricing/surge', { params: { geohashCell } });

// Payments
export const getPayment = (tripId) => api.get(`/api/payments/trip/${tripId}`);

// WebSocket
export const connectDriverWs = (driverId) => new WebSocket(`${WS_BASE}/ws/driver`);
export const connectRiderWs = (driverId) => new WebSocket(`${WS_BASE}/ws/rider?driverId=${driverId}`);

export default api;
