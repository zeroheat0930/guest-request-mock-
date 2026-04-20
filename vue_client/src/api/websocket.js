import { Client } from '@stomp/stompjs';

let stompClient = null;

export function connectStomp(token, onConnect) {
  if (stompClient && stompClient.connected) {
    if (onConnect) onConnect(stompClient);
    return stompClient;
  }

  const base = resolveWsBase();

  stompClient = new Client({
    brokerURL: base,
    connectHeaders: {
      Authorization: 'Bearer ' + token
    },
    reconnectDelay: 5000,
    onConnect: () => {
      console.log('[WS] STOMP connected');
      if (onConnect) onConnect(stompClient);
    },
    onDisconnect: () => {
      console.log('[WS] STOMP disconnected');
    },
    onStompError: (frame) => {
      console.error('[WS] STOMP error', frame.headers['message']);
    }
  });

  stompClient.activate();
  return stompClient;
}

export function disconnectStomp() {
  if (stompClient) {
    stompClient.deactivate();
    stompClient = null;
  }
}

function resolveWsBase() {
  const loc = window.location;
  const proto = loc.protocol === 'https:' ? 'wss:' : 'ws:';
  // In dev, API is on port 8080
  const host = import.meta.env.VITE_API_BASE
    ? new URL(import.meta.env.VITE_API_BASE).host
    : (loc.port === '5173' ? loc.hostname + ':8080' : loc.host);
  return `${proto}//${host}/ws-ccs`;
}
