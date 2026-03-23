import React from 'react';
import { Link, useNavigate } from 'react-router-dom';

function Navbar() {
  const navigate = useNavigate();
  const token = localStorage.getItem('ridex_token');
  const userName = localStorage.getItem('ridex_name') || 'User';

  const handleLogout = () => {
    localStorage.removeItem('ridex_token');
    localStorage.removeItem('ridex_userId');
    localStorage.removeItem('ridex_name');
    localStorage.removeItem('ridex_phone');
    navigate('/login');
  };

  return (
    <nav style={{
      position: 'fixed', top: 0, left: 0, right: 0,
      background: '#000', color: '#fff', padding: '16px 24px',
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      zIndex: 1000, boxShadow: '0 2px 10px rgba(0,0,0,0.1)'
    }}>
      <Link to="/" style={{ fontSize: '24px', fontWeight: 800, color: '#fff', letterSpacing: '-1px' }}>
        RideX
      </Link>
      <div style={{ display: 'flex', gap: '20px', alignItems: 'center' }}>
        {token ? (
          <>
            <Link to="/rider" style={{ color: '#ccc', fontWeight: 500 }}>Ride</Link>
            <Link to="/driver" style={{ color: '#ccc', fontWeight: 500 }}>Drive</Link>
            <Link to="/history" style={{ color: '#ccc', fontWeight: 500 }}>History</Link>
            <Link to="/admin" style={{ color: '#ccc', fontWeight: 500 }}>Admin</Link>
            <span style={{ color: '#888', fontSize: '14px' }}>{userName}</span>
            <button onClick={handleLogout} style={{
              background: 'transparent', border: '1px solid #555',
              color: '#fff', padding: '6px 16px', borderRadius: '6px', cursor: 'pointer'
            }}>
              Logout
            </button>
          </>
        ) : (
          <Link to="/login" style={{ color: '#fff', fontWeight: 600 }}>Login</Link>
        )}
      </div>
    </nav>
  );
}

export default Navbar;
