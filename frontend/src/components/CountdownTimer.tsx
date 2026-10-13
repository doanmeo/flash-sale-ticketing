import React, { useState, useEffect } from 'react';
import { Timer, AlertTriangle } from 'lucide-react';

interface CountdownTimerProps {
  expiresAt: string;
  onExpire?: () => void;
  className?: string;
}

export const CountdownTimer: React.FC<CountdownTimerProps> = ({ expiresAt, onExpire, className = '' }) => {
  const [timeLeft, setTimeLeft] = useState<{ minutes: number; seconds: number; isExpired: boolean }>({
    minutes: 0,
    seconds: 0,
    isExpired: false,
  });

  useEffect(() => {
    const calculateTime = () => {
      // Backend LocalDateTime string e.g. 2026-10-13T16:20:00
      const targetTime = new Date(expiresAt).getTime();
      const now = new Date().getTime();
      const difference = targetTime - now;

      if (difference <= 0) {
        setTimeLeft({ minutes: 0, seconds: 0, isExpired: true });
        if (onExpire) onExpire();
        return;
      }

      const minutes = Math.floor((difference % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((difference % (1000 * 60)) / 1000);

      setTimeLeft({ minutes, seconds, isExpired: false });
    };

    calculateTime();
    const interval = setInterval(calculateTime, 1000);
    return () => clearInterval(interval);
  }, [expiresAt, onExpire]);

  if (timeLeft.isExpired) {
    return (
      <div className={`inline-flex items-center gap-1.5 px-3 py-1.5 bg-red-100 text-red-700 text-xs font-bold rounded-lg ${className}`}>
        <AlertTriangle className="w-4 h-4" />
        <span>Đã hết thời gian giữ vé</span>
      </div>
    );
  }

  const isUrgent = timeLeft.minutes < 3;

  return (
    <div
      className={`inline-flex items-center gap-2 px-3 py-1.5 rounded-lg border text-sm font-bold tnum ${
        isUrgent
          ? 'bg-brand-dark text-brand-500 border-brand-500 animate-pulse'
          : 'bg-orange-50 text-brand-600 border-orange-200'
      } ${className}`}
    >
      <Timer className="w-4 h-4" />
      <span>Thời gian giữ vé:</span>
      <span className="text-base font-black tracking-wider">
        {String(timeLeft.minutes).padStart(2, '0')}:{String(timeLeft.seconds).padStart(2, '0')}
      </span>
    </div>
  );
};
