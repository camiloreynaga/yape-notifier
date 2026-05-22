import { QRCodeSVG } from 'qrcode.react';

interface QRCodeDisplayProps {
  value: string;
  size?: number;
  level?: 'L' | 'M' | 'Q' | 'H';
  className?: string;
}

/**
 * Componente para mostrar un código QR
 * 
 * @param value - El valor a codificar en el QR
 * @param size - Tamaño del QR en píxeles (default: 256)
 * @param level - Nivel de corrección de errores (default: 'M')
 * @param className - Clases CSS adicionales
 */
export default function QRCodeDisplay({
  value,
  size = 256,
  level = 'M',
  className = '',
}: QRCodeDisplayProps) {
  return (
    <div className={`flex justify-center items-center ${className}`}>
      <div className="bg-white p-4 rounded-lg shadow-lg">
        <QRCodeSVG
          value={value}
          size={size}
          level={level}
          includeMargin={true}
        />
      </div>
    </div>
  );
}

