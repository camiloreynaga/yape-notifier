declare module 'laravel-echo' {
  import { Channel, PresenceChannel } from 'laravel-echo/dist/channel';
  import { Connector } from 'laravel-echo/dist/connector';

  interface EchoOptions {
    broadcaster: string;
    key?: string;
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
    namespace?: string;
  }

  class Echo<T = any> {
    constructor(options: EchoOptions);

    channel(channel: string): Channel;
    private(channel: string): Channel;
    presence(channel: string): PresenceChannel;
    leave(channel: string): void;
    leaveChannel(channel: string): void;
    disconnect(): void;
    socketId(): string | null;
    listen(channel: string, event: string, callback: (data: any) => void): Echo<T>;
    connector?: {
      pusher?: {
        connection?: {
          bind: (event: string, callback: (data?: any) => void) => void;
        };
        connect?: () => void;
        config?: {
          auth?: {
            headers?: Record<string, string>;
          };
        };
      };
    };
  }

  export = Echo;
  export { Channel, PresenceChannel, Connector };
}

