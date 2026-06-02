import { useCallback, useEffect, useState } from 'react';

export default function DlqInspector({ apiKey }) {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    fetch('/admin/dlq?limit=50', { headers: { 'X-API-Key': apiKey } })
      .then((res) => {
        if (!res.ok) {
          throw new Error(`DLQ request failed (${res.status})`);
        }
        return res.json();
      })
      .then((data) => {
        setEvents(Array.isArray(data) ? data : []);
        setMessage('');
      })
      .catch((err) => setMessage(err.message))
      .finally(() => setLoading(false));
  }, [apiKey]);

  useEffect(() => {
    load();
  }, [load]);

  const replayAll = () => {
    fetch('/admin/dlq/replay?limit=50', {
      method: 'POST',
      headers: { 'X-API-Key': apiKey },
    })
      .then((res) => res.json())
      .then((data) => {
        setMessage(`Replayed ${data.replayed ?? 0} events`);
        load();
      })
      .catch((err) => setMessage(err.message));
  };

  const replayOne = (dlqId) => {
    fetch(`/admin/dlq/${dlqId}/replay`, {
      method: 'POST',
      headers: { 'X-API-Key': apiKey },
    })
      .then((res) => {
        if (!res.ok) {
          throw new Error(`Replay failed (${res.status})`);
        }
        return res.json();
      })
      .then(() => {
        setMessage(`Replayed ${dlqId}`);
        load();
      })
      .catch((err) => setMessage(err.message));
  };

  return (
    <>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2>DLQ inspector</h2>
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button type="button" className="btn secondary" onClick={load} disabled={loading}>
            Refresh
          </button>
          <button type="button" className="btn" onClick={replayAll}>
            Replay all
          </button>
        </div>
      </div>
      {message && <p className="status">{message}</p>}
      {events.length === 0 ? (
        <p className="empty">No DLQ events.</p>
      ) : (
        <table className="dlq-table">
          <thead>
            <tr>
              <th>DLQ ID</th>
              <th>Tenant</th>
              <th>Event type</th>
              <th>Reason</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {events.map((event) => (
              <tr key={event.dlqId}>
                <td>{event.dlqId}</td>
                <td>{event.tenantId}</td>
                <td>{event.eventType}</td>
                <td>{event.failureReason || event.reason}</td>
                <td>
                  <button type="button" className="btn secondary" onClick={() => replayOne(event.dlqId)}>
                    Replay
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </>
  );
}
