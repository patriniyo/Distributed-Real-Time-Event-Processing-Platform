import { useEffect, useMemo, useState } from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

const WINDOWS = ['1m', '5m', '1hr'];
const REFRESH_MS = 1000;

function heatColor(value, max) {
  if (max <= 0) return '#1a2332';
  const intensity = Math.min(1, value / max);
  const r = Math.round(26 + intensity * (59 - 26));
  const g = Math.round(35 + intensity * (130 - 35));
  const b = Math.round(50 + intensity * (246 - 50));
  return `rgb(${r}, ${g}, ${b})`;
}

export default function AnalyticsCharts({ tenant, eventType, apiKey }) {
  const [window, setWindow] = useState('1m');
  const [metrics, setMetrics] = useState([]);
  const [groupedMetrics, setGroupedMetrics] = useState([]);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;

    const load = () => {
      const params = new URLSearchParams({ tenant, window });
      if (eventType) {
        params.set('eventType', eventType);
      }

      const groupedParams = new URLSearchParams({ tenant, window, groupBy: 'eventType' });

      Promise.all([
        fetch(`/api/v1/analytics?${params}`, { headers: { 'X-API-Key': apiKey } }),
        fetch(`/api/v1/analytics?${groupedParams}`, { headers: { 'X-API-Key': apiKey } }),
      ])
        .then(async ([res, groupedRes]) => {
          if (!res.ok) {
            throw new Error(`Analytics request failed (${res.status})`);
          }
          const data = await res.json();
          const groupedData = groupedRes.ok ? await groupedRes.json() : { metrics: [] };
          if (cancelled) return;
          const rows = data.metrics || data.results || data.data || [];
          const groupedRows = groupedData.metrics || groupedData.results || groupedData.data || [];
          setMetrics(Array.isArray(rows) ? rows : []);
          setGroupedMetrics(Array.isArray(groupedRows) ? groupedRows : []);
          setError(null);
        })
        .catch((err) => {
          if (!cancelled) {
            setError(err.message);
            setMetrics([]);
            setGroupedMetrics([]);
          }
        });
    };

    load();
    const interval = setInterval(load, REFRESH_MS);
    return () => {
      cancelled = true;
      clearInterval(interval);
    };
  }, [tenant, eventType, window, apiKey]);

  const chartData = metrics.map((m) => ({
    name: formatBucket(m.bucketStart || m.windowStart || m.eventType),
    count: m.count ?? 0,
    avg: m.avg ?? m.average ?? 0,
    p95: m.p95 ?? 0,
  }));

  const heatmapCells = useMemo(() => {
    return groupedMetrics.map((m) => ({
      eventType: m.eventType || 'unknown',
      bucket: formatBucket(m.bucketStart),
      count: m.count ?? 0,
    }));
  }, [groupedMetrics]);

  const maxHeat = heatmapCells.reduce((max, cell) => Math.max(max, cell.count), 0);

  return (
    <>
      <h2>
        Analytics
        <select
          value={window}
          onChange={(e) => setWindow(e.target.value)}
          style={{ marginLeft: '0.75rem' }}
        >
          {WINDOWS.map((w) => (
            <option key={w} value={w}>
              {w}
            </option>
          ))}
        </select>
      </h2>
      {error && <p className="empty">{error}</p>}
      {!error && chartData.length === 0 && <p className="empty">No metrics yet for this tenant.</p>}
      {chartData.length > 0 && (
        <>
          <ResponsiveContainer width="100%" height={160}>
            <LineChart data={chartData}>
              <CartesianGrid stroke="#2a3544" />
              <XAxis dataKey="name" stroke="#9fb0c5" tick={{ fontSize: 11 }} />
              <YAxis stroke="#9fb0c5" tick={{ fontSize: 11 }} />
              <Tooltip contentStyle={{ background: '#1a2332', border: '1px solid #2a3544' }} />
              <Legend />
              <Line type="monotone" dataKey="count" stroke="#60a5fa" dot={false} isAnimationActive={false} />
              <Line type="monotone" dataKey="p95" stroke="#fbbf24" dot={false} isAnimationActive={false} />
            </LineChart>
          </ResponsiveContainer>
          <ResponsiveContainer width="100%" height={140}>
            <BarChart data={chartData}>
              <CartesianGrid stroke="#2a3544" />
              <XAxis dataKey="name" stroke="#9fb0c5" tick={{ fontSize: 11 }} />
              <YAxis stroke="#9fb0c5" tick={{ fontSize: 11 }} />
              <Tooltip contentStyle={{ background: '#1a2332', border: '1px solid #2a3544' }} />
              <Bar dataKey="avg" isAnimationActive={false}>
                {chartData.map((entry, index) => (
                  <Cell key={`bar-${index}`} fill="#34d399" />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
          {heatmapCells.length > 0 && (
            <div className="heatmap">
              <h3 className="heatmap-title">Event type heatmap (count by bucket)</h3>
              <div className="heatmap-grid">
                {heatmapCells.map((cell) => (
                  <div
                    key={`${cell.eventType}-${cell.bucket}`}
                    className="heatmap-cell"
                    style={{ background: heatColor(cell.count, maxHeat) }}
                    title={`${cell.eventType} @ ${cell.bucket}: ${cell.count}`}
                  >
                    <span className="heatmap-label">{cell.eventType}</span>
                    <span className="heatmap-value">{cell.count}</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </>
      )}
    </>
  );
}

function formatBucket(value) {
  if (!value) return '—';
  if (typeof value === 'string') {
    return value.length > 16 ? value.slice(11, 19) : value;
  }
  return String(value);
}
