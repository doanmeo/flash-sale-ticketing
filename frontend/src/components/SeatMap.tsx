import React, { useMemo } from 'react';
import { SessionSeatResponse } from '../types';
import { Armchair, Check, Lock } from 'lucide-react';

interface SeatMapProps {
  seats: SessionSeatResponse[];
  selectedSeatIds: number[];
  onToggleSeat: (seatId: number) => void;
  maxSeats?: number;
}

export const SeatMap: React.FC<SeatMapProps> = ({
  seats,
  selectedSeatIds,
  onToggleSeat,
  maxSeats = 6,
}) => {
  // Group seats by zone and then by row
  const groupedSeats = useMemo(() => {
    const zones: Record<string, Record<string, SessionSeatResponse[]>> = {};

    seats.forEach((seat) => {
      const zoneKey = seat.zoneName || 'Khu vực chính';
      const rowKey = seat.rowLabel || 'A';

      if (!zones[zoneKey]) {
        zones[zoneKey] = {};
      }
      if (!zones[zoneKey][rowKey]) {
        zones[zoneKey][rowKey] = [];
      }
      zones[zoneKey][rowKey].push(seat);
    });

    // Sort seats in each row by seatNumber
    Object.keys(zones).forEach((z) => {
      Object.keys(zones[z]).forEach((r) => {
        zones[z][r].sort((a, b) => {
          const numA = parseInt(a.seatNumber, 10);
          const numB = parseInt(b.seatNumber, 10);
          if (!isNaN(numA) && !isNaN(numB)) return numA - numB;
          return a.seatNumber.localeCompare(b.seatNumber);
        });
      });
    });

    return zones;
  }, [seats]);

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  return (
    <div className="bg-white rounded-2xl border border-gray-200 p-4 sm:p-6 shadow-sm overflow-hidden">
      {/* Stage indicator */}
      <div className="mb-8">
        <div className="w-full max-w-md mx-auto py-2.5 bg-gradient-to-r from-gray-800 via-gray-900 to-gray-800 text-white text-center font-bold text-xs uppercase tracking-widest rounded-xl shadow-md border-b-4 border-brand-500">
          SÂN KHẤU / STAGE
        </div>
        <p className="text-center text-xs text-gray-400 mt-2">Hướng nhìn lên sân khấu</p>
      </div>

      {/* Legend */}
      <div className="flex flex-wrap items-center justify-center gap-4 sm:gap-6 mb-8 text-xs font-semibold text-gray-600 bg-gray-50 py-3 px-4 rounded-xl border border-gray-100">
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-md border-2 border-emerald-500 bg-white flex items-center justify-center text-[10px] text-emerald-600 font-bold">
            01
          </div>
          <span>Còn trống</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-md bg-amber-500 text-white flex items-center justify-center shadow-sm">
            <Check className="w-3.5 h-3.5" />
          </div>
          <span>Đang chọn ({selectedSeatIds.length}/{maxSeats})</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-md bg-rose-100 border border-rose-300 text-rose-500 flex items-center justify-center">
            <Lock className="w-3.5 h-3.5" />
          </div>
          <span>Đang khóa</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-6 h-6 rounded-md bg-gray-200 text-gray-400 flex items-center justify-center cursor-not-allowed">
            ✕
          </div>
          <span>Đã bán</span>
        </div>
      </div>

      {/* Seat Map grid with responsive horizontal scroll */}
      <div className="overflow-x-auto pb-4 pt-2 -mx-2 px-2">
        <div className="min-w-[640px] flex flex-col items-center gap-8">
          {Object.entries(groupedSeats).map(([zoneName, rows]) => (
            <div key={zoneName} className="w-full text-center">
              <div className="inline-block px-3 py-1 bg-gray-100 text-gray-700 text-xs font-bold uppercase rounded-full mb-4">
                {zoneName}
              </div>

              <div className="flex flex-col gap-2.5 items-center">
                {Object.entries(rows).map(([rowLabel, rowSeats]) => (
                  <div key={rowLabel} className="flex items-center gap-3">
                    <span className="w-6 text-right font-black text-xs text-gray-400 select-none">
                      {rowLabel}
                    </span>

                    <div className="flex items-center gap-1.5 sm:gap-2">
                      {rowSeats.map((seat) => {
                        const isSelected = selectedSeatIds.includes(seat.seatId);
                        const isAvailable = seat.status === 'AVAILABLE';
                        const isLocked = seat.status === 'LOCKED';
                        const isBooked = seat.status === 'BOOKED';

                        let btnClasses =
                          'relative w-8 h-8 sm:w-9 sm:h-9 rounded-lg font-bold text-xs flex items-center justify-center transition-all duration-150 select-none ';

                        if (isSelected) {
                          btnClasses +=
                            'bg-amber-500 text-white shadow-md shadow-amber-500/30 ring-2 ring-amber-600 scale-105';
                        } else if (isAvailable) {
                          btnClasses +=
                            'bg-white border-2 border-emerald-500 text-emerald-700 hover:bg-emerald-500 hover:text-white hover:scale-105 cursor-pointer';
                        } else if (isLocked) {
                          btnClasses +=
                            'bg-rose-50 border border-rose-200 text-rose-400 cursor-not-allowed';
                        } else {
                          // Booked
                          btnClasses +=
                            'bg-gray-100 border border-gray-200 text-gray-400 cursor-not-allowed';
                        }

                        return (
                          <button
                            key={seat.sessionSeatId}
                            type="button"
                            disabled={!isAvailable && !isSelected}
                            onClick={() => onToggleSeat(seat.seatId)}
                            title={`${rowLabel}${seat.seatNumber} - ${seat.status} - ${formatPrice(seat.price)}`}
                            className={btnClasses}
                          >
                            {isSelected ? (
                              <Check className="w-4 h-4" />
                            ) : isLocked ? (
                              <Lock className="w-3.5 h-3.5 text-rose-400" />
                            ) : isBooked ? (
                              <span className="text-[10px]">✕</span>
                            ) : (
                              <span>{seat.seatNumber}</span>
                            )}
                          </button>
                        );
                      })}
                    </div>

                    <span className="w-6 text-left font-black text-xs text-gray-400 select-none">
                      {rowLabel}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          ))}
        </div>
      </div>

      {seats.length === 0 && (
        <div className="py-12 text-center text-gray-500">
          <Armchair className="w-12 h-12 mx-auto text-gray-300 mb-2" />
          <p className="font-semibold text-sm">Chưa có dữ liệu ghế cho suất diễn này.</p>
        </div>
      )}
    </div>
  );
};
