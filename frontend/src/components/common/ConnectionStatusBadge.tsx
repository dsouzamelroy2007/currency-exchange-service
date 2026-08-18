import { useStompConnectionStatus } from '../../hooks/useStompConnectionStatus';

const LABELS: Record<string, string> = {
  connecting: 'Connecting…',
  open: 'Live',
  reconnecting: 'Reconnecting…',
};

export function ConnectionStatusBadge() {
  const status = useStompConnectionStatus();
  return <span className={`connection-status-badge connection-status-${status}`}>{LABELS[status]}</span>;
}
