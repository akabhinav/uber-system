import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import Navbar from './components/Navbar';
import Login from './pages/Login';
import RiderDashboard from './pages/RiderDashboard';
import DriverDashboard from './pages/DriverDashboard';
import TripDetails from './pages/TripDetails';
import TripHistory from './pages/TripHistory';
import AdminDashboard from './pages/AdminDashboard';

function App() {
  const token = localStorage.getItem('ridex_token');

  return (
    <Router>
      <Toaster position="top-right" />
      <Navbar />
      <div className="container" style={{ paddingTop: '80px' }}>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/rider" element={token ? <RiderDashboard /> : <Navigate to="/login" />} />
          <Route path="/driver" element={token ? <DriverDashboard /> : <Navigate to="/login" />} />
          <Route path="/trip/:tripId" element={token ? <TripDetails /> : <Navigate to="/login" />} />
          <Route path="/history" element={token ? <TripHistory /> : <Navigate to="/login" />} />
          <Route path="/admin" element={<AdminDashboard />} />
          <Route path="/" element={<Navigate to="/rider" />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
