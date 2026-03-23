import React, { useEffect, useRef } from 'react';

function MapView({ pickup, dropoff, driverLocation, onPickupChange, onDropoffChange }) {
  const mapRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const pickupMarkerRef = useRef(null);
  const dropoffMarkerRef = useRef(null);
  const driverMarkerRef = useRef(null);

  useEffect(() => {
    if (!mapRef.current || mapInstanceRef.current) return;

    // Check if Leaflet is available
    if (typeof window.L === 'undefined') {
      // Load Leaflet dynamically
      const script = document.createElement('script');
      script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
      script.onload = () => initMap();
      document.head.appendChild(script);
    } else {
      initMap();
    }

    function initMap() {
      const L = window.L;
      const map = L.map(mapRef.current).setView([pickup.lat, pickup.lng], 13);

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; OpenStreetMap contributors'
      }).addTo(map);

      // Pickup marker (green)
      const pickupIcon = L.divIcon({
        className: '',
        html: '<div style="width:24px;height:24px;background:#28a745;border-radius:50%;border:3px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,0.3)"></div>',
        iconSize: [24, 24],
        iconAnchor: [12, 12]
      });

      // Dropoff marker (red)
      const dropoffIcon = L.divIcon({
        className: '',
        html: '<div style="width:24px;height:24px;background:#dc3545;border-radius:50%;border:3px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,0.3)"></div>',
        iconSize: [24, 24],
        iconAnchor: [12, 12]
      });

      // Driver marker (blue)
      const driverIcon = L.divIcon({
        className: '',
        html: '<div style="width:20px;height:20px;background:#007bff;border-radius:50%;border:3px solid #fff;box-shadow:0 2px 6px rgba(0,0,0,0.3)"></div>',
        iconSize: [20, 20],
        iconAnchor: [10, 10]
      });

      pickupMarkerRef.current = L.marker([pickup.lat, pickup.lng], {
        icon: pickupIcon, draggable: true
      }).addTo(map).bindPopup('Pickup');

      dropoffMarkerRef.current = L.marker([dropoff.lat, dropoff.lng], {
        icon: dropoffIcon, draggable: true
      }).addTo(map).bindPopup('Dropoff');

      driverMarkerRef.current = L.marker([pickup.lat, pickup.lng], {
        icon: driverIcon
      }).bindPopup('Driver');

      pickupMarkerRef.current.on('dragend', (e) => {
        const pos = e.target.getLatLng();
        if (onPickupChange) onPickupChange({ lat: pos.lat, lng: pos.lng });
      });

      dropoffMarkerRef.current.on('dragend', (e) => {
        const pos = e.target.getLatLng();
        if (onDropoffChange) onDropoffChange({ lat: pos.lat, lng: pos.lng });
      });

      // Draw line between pickup and dropoff
      L.polyline([[pickup.lat, pickup.lng], [dropoff.lat, dropoff.lng]], {
        color: '#000', weight: 3, opacity: 0.6, dashArray: '10, 10'
      }).addTo(map);

      // Fit bounds
      map.fitBounds([[pickup.lat, pickup.lng], [dropoff.lat, dropoff.lng]], { padding: [50, 50] });

      mapInstanceRef.current = map;
    }

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    if (pickupMarkerRef.current) {
      pickupMarkerRef.current.setLatLng([pickup.lat, pickup.lng]);
    }
  }, [pickup]);

  useEffect(() => {
    if (dropoffMarkerRef.current) {
      dropoffMarkerRef.current.setLatLng([dropoff.lat, dropoff.lng]);
    }
  }, [dropoff]);

  useEffect(() => {
    if (driverLocation && driverMarkerRef.current && mapInstanceRef.current) {
      driverMarkerRef.current.setLatLng([driverLocation.lat, driverLocation.lng]);
      if (!mapInstanceRef.current.hasLayer(driverMarkerRef.current)) {
        driverMarkerRef.current.addTo(mapInstanceRef.current);
      }
    }
  }, [driverLocation]);

  return (
    <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
      <div ref={mapRef} style={{ height: '500px', width: '100%' }} />
    </div>
  );
}

export default MapView;
