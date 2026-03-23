import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { sendOtp, verifyOtp } from '../api';

function Login() {
  const navigate = useNavigate();
  const [step, setStep] = useState('phone');
  const [phone, setPhone] = useState('+919876543210');
  const [otp, setOtp] = useState('');
  const [name, setName] = useState('');
  const [loading, setLoading] = useState(false);
  const [devOtp, setDevOtp] = useState('');

  const handleSendOtp = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await sendOtp(phone);
      if (res.data.success) {
        setDevOtp(res.data.data.otp_dev || '');
        setStep('otp');
        toast.success('OTP sent! Check console for dev OTP');
      } else {
        toast.error(res.data.error || 'Failed to send OTP');
      }
    } catch (err) {
      toast.error('Failed to send OTP');
    }
    setLoading(false);
  };

  const handleVerifyOtp = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const res = await verifyOtp(phone, otp, name);
      if (res.data.success) {
        const { token, userId, name: userName, phone: userPhone } = res.data.data;
        localStorage.setItem('ridex_token', token);
        localStorage.setItem('ridex_userId', userId);
        localStorage.setItem('ridex_name', userName);
        localStorage.setItem('ridex_phone', userPhone);
        toast.success('Logged in!');
        navigate('/rider');
      } else {
        toast.error(res.data.error || 'Invalid OTP');
      }
    } catch (err) {
      toast.error('Verification failed');
    }
    setLoading(false);
  };

  return (
    <div style={{ maxWidth: '420px', margin: '60px auto' }}>
      <div className="card">
        <h2 style={{ marginBottom: '24px', fontSize: '28px' }}>Welcome to RideX</h2>

        {step === 'phone' ? (
          <form onSubmit={handleSendOtp}>
            <div className="input-group">
              <label>Phone Number</label>
              <input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)}
                     placeholder="+919876543210" required />
            </div>
            <div className="input-group">
              <label>Name (optional for new users)</label>
              <input type="text" value={name} onChange={(e) => setName(e.target.value)}
                     placeholder="Your name" />
            </div>
            <button className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
              {loading ? 'Sending...' : 'Send OTP'}
            </button>
          </form>
        ) : (
          <form onSubmit={handleVerifyOtp}>
            <p style={{ marginBottom: '16px', color: '#666' }}>OTP sent to {phone}</p>
            {devOtp && (
              <div style={{ background: '#ffe', padding: '8px 12px', borderRadius: '6px',
                           marginBottom: '16px', fontSize: '14px', border: '1px solid #dda' }}>
                Dev OTP: <strong>{devOtp}</strong>
              </div>
            )}
            <div className="input-group">
              <label>Enter OTP</label>
              <input type="text" value={otp} onChange={(e) => setOtp(e.target.value)}
                     placeholder="123456" maxLength={6} required autoFocus />
            </div>
            <button className="btn btn-primary" style={{ width: '100%' }} disabled={loading}>
              {loading ? 'Verifying...' : 'Verify & Login'}
            </button>
            <button type="button" className="btn btn-secondary"
                    style={{ width: '100%', marginTop: '8px' }}
                    onClick={() => setStep('phone')}>
              Back
            </button>
          </form>
        )}
      </div>
    </div>
  );
}

export default Login;
