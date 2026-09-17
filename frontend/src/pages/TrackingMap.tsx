import { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';
import { getManagerMapOverview, getCustomerMapOverview } from '../api/map';
import { useAuth } from '../context/AuthContext';
import type { MapPin } from '../types';

// Default center: Pune, MH — where every seeded site/technician sits. A real
// deployment with pins elsewhere still works fine; this is just the
// no-pins fallback.
const DEFAULT_CENTER: [number, number] = [18.5204, 73.8567];

// Pins can be spread across a wide area (e.g. Pune and Nashik technicians,
// ~165km apart) — a plain lat/lng average can land the view in empty space
// between two clusters instead of showing either one. Fitting bounds to
// every pin instead guarantees everything is actually visible.
function FitBounds({ pins }: { pins: MapPin[] }) {
  const map = useMap();
  useEffect(() => {
    if (pins.length === 0) return;
    if (pins.length === 1) {
      map.setView([pins[0].latitude, pins[0].longitude], 13);
      return;
    }
    const bounds = L.latLngBounds(pins.map((p) => [p.latitude, p.longitude] as [number, number]));
    map.fitBounds(bounds, { padding: [32, 32] });
  }, [pins, map]);
  return null;
}

const PRIORITY_COLOR: Record<string, string> = {
  CRITICAL: '#d1453b',
  HIGH: '#e08a2c',
  MEDIUM: '#d9b512',
  LOW: '#2fa25a'
};

function pinColor(pin: MapPin): string {
  if (pin.kind === 'TECHNICIAN') return '#5b4fe0';
  if (pin.kind === 'SITE') return '#6b7086';
  return PRIORITY_COLOR[pin.priority ?? ''] ?? '#5b4fe0';
}

function pinIcon(pin: MapPin) {
  const color = pinColor(pin);
  const glyph = pin.kind === 'TECHNICIAN' ? '👤' : pin.kind === 'SITE' ? '📍' : '🔧';
  return L.divIcon({
    className: '',
    html: `<div style="
      width: 30px; height: 30px; border-radius: 50% 50% 50% 0;
      background: ${color}; transform: rotate(-45deg);
      display: flex; align-items: center; justify-content: center;
      box-shadow: 0 2px 6px rgba(0,0,0,0.35); border: 2px solid #fff;
    "><span style="transform: rotate(45deg); font-size: 14px;">${glyph}</span></div>`,
    iconSize: [30, 30],
    iconAnchor: [15, 28],
    popupAnchor: [0, -26]
  });
}

export function TrackingMap() {
  const { user } = useAuth();
  const [pins, setPins] = useState<MapPin[]>([]);
  const [loading, setLoading] = useState(true);
  const [errored, setErrored] = useState(false);

  const isManager = user?.role === 'MANAGER';

  useEffect(() => {
    (async () => {
      setLoading(true);
      setErrored(false);
      try {
        const data = isManager ? await getManagerMapOverview() : await getCustomerMapOverview();
        setPins(data);
      } catch {
        setErrored(true);
      } finally {
        setLoading(false);
      }
    })();
  }, [isManager]);

  return (
    <>
      <div className="page-header">
        <div>
          <h1>Track Map</h1>
          <p>
            {isManager
              ? 'Technician base locations and every open work order, plotted from saved addresses.'
              : 'Your sites and whoever is currently assigned to your open requests.'}
          </p>
        </div>
      </div>

      {loading ? (
        <div className="empty-state">Loading map…</div>
      ) : errored ? (
        <div className="empty-state">Could not load the map.</div>
      ) : pins.length === 0 ? (
        <div className="empty-state">Nothing to show on the map yet.</div>
      ) : (
        <div className="card" style={{ padding: 0, overflow: 'hidden' }}>
          <MapContainer center={DEFAULT_CENTER} zoom={11} style={{ height: 520, width: '100%' }} scrollWheelZoom>
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <FitBounds pins={pins} />
            {pins.map((pin) => (
              <Marker key={`${pin.kind}-${pin.id}`} position={[pin.latitude, pin.longitude]} icon={pinIcon(pin)}>
                <Popup>
                  <div style={{ fontSize: 12.5, fontWeight: 700 }}>{pin.label}</div>
                  {pin.kind === 'WORK_ORDER' && (
                    <div style={{ fontSize: 11.5, marginTop: 2 }}>
                      {pin.status?.replace('_', ' ')} · {pin.priority}
                      {pin.assignedToName && (
                        <>
                          <br />→ {pin.assignedToName}
                        </>
                      )}
                    </div>
                  )}
                  {pin.kind === 'TECHNICIAN' && <div style={{ fontSize: 11.5, marginTop: 2 }}>Technician base</div>}
                  {pin.kind === 'SITE' && <div style={{ fontSize: 11.5, marginTop: 2 }}>Your site</div>}
                </Popup>
              </Marker>
            ))}
          </MapContainer>
        </div>
      )}
    </>
  );
}
