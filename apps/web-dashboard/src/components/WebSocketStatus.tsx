// src/components/WebSocketStatus.tsx
import { useEffect, useState } from "react";
import { Wifi, WifiOff, AlertCircle } from "lucide-react";
import { getConnectionState } from "@/services/echo";

export default function WebSocketStatus() {
  const [status, setStatus] = useState<
    "connected" | "disconnected" | "connecting" | "error"
  >(getConnectionState());

  useEffect(() => {
    const updateStatus = () => {
      setStatus(getConnectionState());
    };

    window.addEventListener("echo:connected", updateStatus);
    window.addEventListener("echo:disconnected", updateStatus);
    window.addEventListener("echo:error", updateStatus);

    return () => {
      window.removeEventListener("echo:connected", updateStatus);
      window.removeEventListener("echo:disconnected", updateStatus);
      window.removeEventListener("echo:error", updateStatus);
    };
  }, []);

  const statusConfig = {
    connected: {
      icon: Wifi,
      color: "text-green-500",
      label: "Conectado",
    },
    disconnected: {
      icon: WifiOff,
      color: "text-gray-400",
      label: "Desconectado",
    },
    connecting: {
      icon: Wifi,
      color: "text-yellow-500",
      label: "Conectando...",
    },
    error: {
      icon: AlertCircle,
      color: "text-red-500",
      label: "Error de conexión",
    },
  };

  const config = statusConfig[status];
  const Icon = config.icon;

  return (
    <div
      className={`flex items-center gap-2 ${config.color}`}
      title={config.label}
    >
      <Icon className="w-4 h-4" />
      <span className="text-xs">{config.label}</span>
    </div>
  );
}

