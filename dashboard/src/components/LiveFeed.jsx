import { useEffect, useRef, useState } from 'react';

const MAX_EVENTS = 50;

export default function LiveFeed({ wsUrl, tenant }) {
  const [events, setEvents] = useState([]);
  const [connected, setConnected] = useState(false);
  const wsRef = useRef(null);

  useEffect(() => {
    setEvents([]);
    const ws = new WebSocket(wsUrl);
    wsRef.current = ws;

    ws.onopen = () => setConnected(true);
    ws.onclose = () => setConnected(false);
    ws.onerror = () => setConnected(false);
    ws.onmessage = (message) => {
      try {
        const event = JSON.parse(message.data);
        setEvents((prev) => [event, ...prev].slice(0, MAX_EVENTS));
      } catch {
        // ignore malformed payloads
      }
    };

    return () => ws.close();
  }, [wsUrl]);

  return (
    <>
      <h2>
        Live feed — {tenant}{' '}
        <span className={`status ${connected ? '' : 'disconnected'}`}>
          {connected ? 'connected' : 'disconnected'}
        </span>
      </h2>
      {events.length === 0 ? (
        <p className="empty">Waiting for processed events…</p>
      ) : (
        <ul className="feed-list">
          {events.map((event) => (
            <li key={event.eventId || `${event.traceId}-${event.processedAt}`} className="feed-item">
              <span className="badge">{event.eventType}</span>
              <strong>{event.eventId}</strong>
              <div>trace: {event.traceId}</div>
              <div>at: {event.processedAt || event.timestamp}</div>
            </li>
          ))}
        </ul>
      )}
    </>
  );
}
