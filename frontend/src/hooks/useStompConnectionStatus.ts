import { useEffect, useState } from 'react';
import { stompConnectionManager } from '../ws/stompConnectionManager';
import type { ConnectionStatus } from '../ws/types';

export function useStompConnectionStatus(): ConnectionStatus {
  const [status, setStatus] = useState<ConnectionStatus>('connecting');

  useEffect(() => stompConnectionManager.onStatusChange(setStatus), []);

  return status;
}
