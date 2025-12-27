# Maps Implementation Guide

This document explains the map implementation in SmartCBWTF web portal,
including OpenStreetMap/Leaflet integration and reverse geocoding. It also
provides a complete guide for switching to Google Maps.

---

## Current Implementation

### Technology Stack

| Layer             | Technology                                        | Purpose                                 |
| ----------------- | ------------------------------------------------- | --------------------------------------- |
| Map Rendering     | [Leaflet](https://leafletjs.com/)                 | JavaScript library for interactive maps |
| Map Tiles         | [OpenStreetMap](https://www.openstreetmap.org/)   | Free map tiles                          |
| React Bindings    | [react-leaflet](https://react-leaflet.js.org/)    | React wrapper for Leaflet               |
| Reverse Geocoding | [Nominatim](https://nominatim.openstreetmap.org/) | Free address lookup from coordinates    |

### Dependencies

```json
// package.json
{
    "dependencies": {
        "leaflet": "^1.9.x",
        "react-leaflet": "^4.x.x",
        "@types/leaflet": "^1.9.x"
    }
}
```

---

## Files Overview

### Map Components

| File                                 | Purpose                                |
| ------------------------------------ | -------------------------------------- |
| `src/pages/cbwtf/VehicleLiveMap.tsx` | Live vehicle tracking map with markers |
| `src/pages/cbwtf/StaffDetail.tsx`    | Mini map embed showing staff location  |
| `src/components/LocationAddress.tsx` | Reverse geocoding component            |

---

## VehicleLiveMap.tsx Implementation

### 1. Leaflet Setup

```tsx
import { MapContainer, Marker, Popup, TileLayer, useMap } from "react-leaflet";
import L from "leaflet";
import "leaflet/dist/leaflet.css";
```

### 2. Custom Truck Icons

Custom SVG icons for vehicles using Leaflet's `DivIcon`:

```tsx
const createTruckIcon = (color: string) => {
    const svg = `
    <svg viewBox="0 0 24 24" width="32" height="32" xmlns="http://www.w3.org/2000/svg">
      <path fill="${color}" stroke="#fff" stroke-width="1" d="M20,8H17V4H3C1.9,4,1,4.9,1,6V17H3C3,18.7,4.3,20,6,20S9,18.7,9,17H15C15,18.7,16.3,20,18,20S21,18.7,21,17H23V12L20,8M6,18.5C5.2,18.5,4.5,17.8,4.5,17S5.2,15.5,6,15.5S7.5,16.2,7.5,17S6.8,18.5,6,18.5M18,18.5C17.2,18.5,16.5,17.8,16.5,17S17.2,15.5,18,15.5S19.5,16.2,19.5,17S18.8,18.5,18,18.5M19.5,12H17V9.5H18.5L19.5,12Z"/>
    </svg>
  `;

    return L.divIcon({
        html: svg,
        className: "truck-marker",
        iconSize: [32, 32],
        iconAnchor: [16, 32],
        popupAnchor: [0, -32],
    });
};

const onlineTruckIcon = createTruckIcon("#22C55E"); // Green
const offlineTruckIcon = createTruckIcon("#EF4444"); // Red
```

### 3. Map Container

```tsx
<MapContainer
    center={[20.5937, 78.9629]} // India center
    zoom={5}
    style={{ height: "100%", width: "100%" }}
>
    <TileLayer
        attribution="&copy; OpenStreetMap contributors"
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
    />

    {/* Markers */}
    {vehicles.map((vehicle) => (
        <Marker
            key={vehicle.id}
            position={[vehicle.latitude, vehicle.longitude]}
            icon={vehicle.gpsStatus === "ONLINE"
                ? onlineTruckIcon
                : offlineTruckIcon}
        >
            <Popup>Vehicle info here</Popup>
        </Marker>
    ))}
</MapContainer>;
```

### 4. Auto-fit Bounds

Component to automatically fit the map to show all markers:

```tsx
function FitBounds({ vehicles }: { vehicles: Vehicle[] }) {
    const map = useMap();

    useEffect(() => {
        const validVehicles = vehicles.filter((v) => v.latitude && v.longitude);
        if (validVehicles.length > 0) {
            const bounds = L.latLngBounds(
                validVehicles.map((v) => [v.latitude, v.longitude]),
            );
            map.fitBounds(bounds, { padding: [50, 50], maxZoom: 15 });
        }
    }, [vehicles, map]);

    return null;
}
```

---

## StaffDetail.tsx - Embedded Map

Uses OpenStreetMap's iframe embed for a simple static map:

```tsx
<Box
    component="iframe"
    src={`https://www.openstreetmap.org/export/embed.html?bbox=${
        lon - 0.01
    }%2C${lat - 0.01}%2C${lon + 0.01}%2C${
        lat + 0.01
    }&layer=mapnik&marker=${lat}%2C${lon}`}
    sx={{ border: 0, width: "100%", height: 180 }}
    loading="lazy"
    title="Staff Location Map"
/>;
```

---

## Reverse Geocoding (LocationAddress.tsx)

### How It Works

1. Takes lat/lon coordinates as props
2. Calls Nominatim API to get address
3. Caches results to avoid repeated calls
4. Respects rate limits (1 request/second)

### API Call

```tsx
const response = await fetch(
    `https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lon}&zoom=16&addressdetails=1`,
    {
        headers: {
            "User-Agent": "SmartCBWTF/1.0",
        },
    },
);
```

### Caching

Results are cached in memory to avoid hitting the API for the same coordinates:

```tsx
const addressCache: Record<string, string> = {};
```

### Rate Limiting

Nominatim requires max 1 request per second:

```tsx
let lastRequestTime = 0;

// Before making request
const timeSinceLastRequest = Date.now() - lastRequestTime;
if (timeSinceLastRequest < 1100) {
    await new Promise((resolve) =>
        setTimeout(resolve, 1100 - timeSinceLastRequest)
    );
}
lastRequestTime = Date.now();
```

---

## Switching to Google Maps

### Step 1: Get Google Maps API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing
3. Enable **Maps JavaScript API** and **Geocoding API**
4. Create credentials → API Key
5. Restrict the key to your domain for security

### Step 2: Install Google Maps React Library

```bash
npm uninstall leaflet react-leaflet @types/leaflet
npm install @react-google-maps/api
```

### Step 3: Add API Key to Environment

Create/update `.env`:

```env
VITE_GOOGLE_MAPS_API_KEY=your_api_key_here
```

### Step 4: Replace VehicleLiveMap.tsx

```tsx
import {
    GoogleMap,
    InfoWindow,
    LoadScript,
    Marker,
} from "@react-google-maps/api";

const containerStyle = {
    width: "100%",
    height: "100%",
};

const center = { lat: 20.5937, lng: 78.9629 };

function VehicleLiveMap() {
    const [selectedVehicle, setSelectedVehicle] = useState(null);

    return (
        <LoadScript googleMapsApiKey={import.meta.env.VITE_GOOGLE_MAPS_API_KEY}>
            <GoogleMap
                mapContainerStyle={containerStyle}
                center={center}
                zoom={5}
            >
                {vehicles.map((vehicle) => (
                    <Marker
                        key={vehicle.id}
                        position={{
                            lat: vehicle.latitude,
                            lng: vehicle.longitude,
                        }}
                        icon={{
                            url: vehicle.gpsStatus === "ONLINE"
                                ? "/icons/truck-green.png"
                                : "/icons/truck-red.png",
                            scaledSize: new window.google.maps.Size(32, 32),
                        }}
                        onClick={() => setSelectedVehicle(vehicle)}
                    />
                ))}

                {selectedVehicle && (
                    <InfoWindow
                        position={{
                            lat: selectedVehicle.latitude,
                            lng: selectedVehicle.longitude,
                        }}
                        onCloseClick={() => setSelectedVehicle(null)}
                    >
                        <div>
                            <h4>{selectedVehicle.registrationNumber}</h4>
                            <p>{selectedVehicle.vehicleType}</p>
                        </div>
                    </InfoWindow>
                )}
            </GoogleMap>
        </LoadScript>
    );
}
```

### Step 5: Replace LocationAddress.tsx Geocoding

Replace Nominatim with Google Geocoding API:

```tsx
async function reverseGeocode(lat: number, lon: number): Promise<string> {
    const cacheKey = `${lat.toFixed(6)},${lon.toFixed(6)}`;

    if (addressCache[cacheKey]) {
        return addressCache[cacheKey];
    }

    try {
        const response = await fetch(
            `https://maps.googleapis.com/maps/api/geocode/json?latlng=${lat},${lon}&key=${import.meta.env.VITE_GOOGLE_MAPS_API_KEY}`,
        );

        const data = await response.json();

        if (data.results && data.results[0]) {
            const address = data.results[0].formatted_address;
            addressCache[cacheKey] = address;
            return address;
        }

        return `${lat.toFixed(4)}, ${lon.toFixed(4)}`;
    } catch (error) {
        console.error("Geocoding error:", error);
        return `${lat.toFixed(4)}, ${lon.toFixed(4)}`;
    }
}
```

### Step 6: Update StaffDetail.tsx Embed

Replace OpenStreetMap iframe with Google Maps iframe:

```tsx
<Box
    component="iframe"
    src={`https://www.google.com/maps/embed/v1/place?key=${import.meta.env.VITE_GOOGLE_MAPS_API_KEY}&q=${lat},${lon}&zoom=15`}
    sx={{ border: 0, width: "100%", height: 180 }}
    loading="lazy"
    allowFullScreen
    title="Staff Location Map"
/>;
```

---

## Cost Comparison

| Feature          | OpenStreetMap/Nominatim | Google Maps         |
| ---------------- | ----------------------- | ------------------- |
| Map Tiles        | Free                    | $7/1000 loads       |
| Geocoding        | Free (1 req/sec limit)  | $5/1000 requests    |
| API Key Required | No                      | Yes                 |
| Terms of Use     | Must attribute          | Must not cover logo |

---

## Troubleshooting

### Leaflet Markers Not Showing

Leaflet requires explicit icon setup in React. The default icon URLs break with
bundlers. Use `DivIcon` with inline SVG (our approach) or explicitly set icon
paths.

### Nominatim Rate Limiting

If you see 429 errors, you're exceeding 1 request/second. The `LocationAddress`
component handles this automatically.

### Google Maps "For Development Only" Watermark

You need to enable billing on your Google Cloud account (they have a $200/month
free tier).
