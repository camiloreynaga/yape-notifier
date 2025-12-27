// Mock for laravel-echo in tests
const mockChannel = {
  listen: () => mockChannel,
  stopListening: () => mockChannel,
  error: () => mockChannel,
};

const Echo = function() {
  return {
    private: () => mockChannel,
    leave: () => {},
    disconnect: () => {},
    connector: {
      pusher: {
        connection: {
          bind: () => {},
          connect: () => {},
        },
      },
    },
  };
};

export default Echo;

