import { useEffect, useRef, useCallback, useState } from 'react';
import { useAuth } from '../auth';
import apiClient from '../api/client';

interface LocationTrackerOptions {
  intervalMs?: number;
  enabled?: boolean;
}

interface LocationState {
  latitude: number | null;
  longitude: number | null;
  accuracy: number | null;
  lastUpdated: Date | null;
  error: string | null;
}

export function useLocationTracker(options: LocationTrackerOptions = {}) {
  const { intervalMs = 5 * 60 * 1000, enabled = true } = options;
  const { user, isAuthenticated } = useAuth();
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);
  const [state, setState] = useState<LocationState>({
    latitude: null,
    longitude: null,
    accuracy: null,
    lastUpdated: null,
    error: null,
  });

  const shouldTrack = useCallback(() => {
    if (!enabled || !isAuthenticated || !user) return false;
    return ['DRIVER', 'PLANT_OPERATOR'].includes(user.role);
  }, [enabled, isAuthenticated, user]);

  const updateLocation = useCallback(async (latitude: number, longitude: number, accuracy: number | null) => {
    try {
      await apiClient.post('/api/location/update', {
        latitude,
        longitude,
        accuracy,
      });
      setState((prev) => ({
        ...prev,
        latitude,
        longitude,
        accuracy,
        lastUpdated: new Date(),
        error: null,
      }));
    } catch (err) {
      console.error('Failed to send location:', err);
      setState((prev) => ({ ...prev, error: 'Failed to sync location' }));
    }
  }, []);

  const getCurrentPosition = useCallback(() => {
    if (!navigator.geolocation) {
      setState((prev) => ({ ...prev, error: 'Geolocation not supported' }));
      return;
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const { latitude, longitude, accuracy } = position.coords;
        updateLocation(latitude, longitude, accuracy);
      },
      (error) => {
        console.error('Geolocation error:', error);
        setState((prev) => ({ ...prev, error: error.message }));
      },
      {
        enableHighAccuracy: true,
        timeout: 30000,
        maximumAge: 60000,
      }
    );
  }, [updateLocation]);

  useEffect(() => {
    if (!shouldTrack()) {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
      return;
    }

    getCurrentPosition();
    intervalRef.current = setInterval(getCurrentPosition, intervalMs);

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
        intervalRef.current = null;
      }
    };
  }, [shouldTrack, getCurrentPosition, intervalMs]);

  const refresh = useCallback(() => {
    if (shouldTrack()) {
      getCurrentPosition();
    }
  }, [shouldTrack, getCurrentPosition]);

  return {
    ...state,
    isTracking: shouldTrack(),
    refresh,
  };
}
