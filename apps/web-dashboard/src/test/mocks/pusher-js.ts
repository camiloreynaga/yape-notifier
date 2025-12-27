// Mock for pusher-js in tests
const Pusher = function() {
  return {
    connection: {
      bind: () => {},
      connect: () => {},
      state: 'connected',
    },
  };
};

export default Pusher;

