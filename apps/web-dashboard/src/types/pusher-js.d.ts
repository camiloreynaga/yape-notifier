declare module 'pusher-js' {
  interface PusherOptions {
    cluster?: string;
    encrypted?: boolean;
    authEndpoint?: string;
    auth?: {
      headers?: Record<string, string>;
    };
    wsHost?: string;
    wsPort?: number;
    wssPort?: number;
    disableStats?: boolean;
    enabledTransports?: string[];
    forceTLS?: boolean;
  }

  interface Channel {
    bind(event: string, callback: (data: any) => void): Channel;
    unbind(event?: string, callback?: (data: any) => void): Channel;
    subscribe(): Channel;
    unsubscribe(): void;
  }

  interface PresenceChannel extends Channel {
    members: {
      each: (callback: (member: any) => void) => void;
      count: number;
    };
  }

  class Pusher {
    constructor(key: string, options?: PusherOptions);
    subscribe(channel: string): Channel;
    unsubscribe(channel: string): void;
    disconnect(): void;
    socket_id(): string | null;
    bind(event: string, callback: (data: any) => void): Pusher;
    unbind(event?: string, callback?: (data: any) => void): Pusher;
  }

  export default Pusher;
}


