import { useState, useEffect } from 'react';
import { Typography, Skeleton, Tooltip } from '@mui/material';

// Cache for geocoded addresses to avoid repeated API calls
const addressCache: Record<string, string> = {};

// Rate limit: Nominatim requires max 1 request per second
let lastRequestTime = 0;

/**
 * Reverse geocode coordinates to human-readable address using Nominatim (OpenStreetMap).
 * Free service with rate limiting (1 req/sec max).
 */
async function reverseGeocode(lat: number, lon: number): Promise<string> {
  const cacheKey = `${lat.toFixed(6)},${lon.toFixed(6)}`;
  
  if (addressCache[cacheKey]) {
    return addressCache[cacheKey];
  }

  // Rate limiting - wait if needed
  const now = Date.now();
  const timeSinceLastRequest = now - lastRequestTime;
  if (timeSinceLastRequest < 1100) {
    await new Promise(resolve => setTimeout(resolve, 1100 - timeSinceLastRequest));
  }
  lastRequestTime = Date.now();

  try {
    const response = await fetch(
      `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}&zoom=16&addressdetails=1`,
      {
        headers: {
          'User-Agent': 'SmartCBWTF/1.0 (contact@smartcbwtf.com)',
        },
      }
    );

    if (!response.ok) {
      throw new Error('Geocoding failed');
    }

    const data = await response.json();
    
    // Build a short, readable address
    const address = data.address;
    let shortAddress = '';
    
    if (address) {
      // Try to get a reasonable short address
      const parts: string[] = [];
      
      // Add locality/suburb/neighbourhood if available
      if (address.neighbourhood) parts.push(address.neighbourhood);
      else if (address.suburb) parts.push(address.suburb);
      else if (address.village) parts.push(address.village);
      else if (address.town) parts.push(address.town);
      
      // Add city/district
      if (address.city) parts.push(address.city);
      else if (address.district) parts.push(address.district);
      else if (address.county) parts.push(address.county);
      
      // Add state code or state
      if (address.state) parts.push(address.state);
      
      shortAddress = parts.slice(0, 3).join(', ') || data.display_name?.split(',').slice(0, 3).join(', ') || 'Unknown location';
    } else {
      shortAddress = data.display_name?.split(',').slice(0, 3).join(', ') || 'Unknown location';
    }

    addressCache[cacheKey] = shortAddress;
    return shortAddress;
  } catch (error) {
    console.error('Reverse geocoding error:', error);
    return `${lat.toFixed(4)}, ${lon.toFixed(4)}`;
  }
}

interface LocationAddressProps {
  latitude: number | null;
  longitude: number | null;
  showCoords?: boolean;
}

/**
 * Component that displays a human-readable address from GPS coordinates.
 * Uses OpenStreetMap Nominatim for reverse geocoding.
 */
export function LocationAddress({ latitude, longitude, showCoords = false }: LocationAddressProps) {
  const [address, setAddress] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (latitude && longitude) {
      setLoading(true);
      reverseGeocode(latitude, longitude)
        .then(setAddress)
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
      setAddress(null);
    }
  }, [latitude, longitude]);

  if (!latitude || !longitude) {
    return (
      <Typography variant="body2" color="text.secondary">
        -
      </Typography>
    );
  }

  if (loading) {
    return <Skeleton variant="text" width={120} />;
  }

  const coordsText = `${latitude.toFixed(6)}, ${longitude.toFixed(6)}`;

  return (
    <Tooltip title={coordsText} arrow placement="top">
      <Typography 
        variant="body2" 
        sx={{ 
          cursor: 'help',
        }}
      >
        {address || coordsText}
        {showCoords && address && (
          <Typography variant="caption" display="block" color="text.secondary" sx={{ fontFamily: 'monospace' }}>
            {coordsText}
          </Typography>
        )}
      </Typography>
    </Tooltip>
  );
}

export default LocationAddress;
