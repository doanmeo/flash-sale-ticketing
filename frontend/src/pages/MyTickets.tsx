import React, { useEffect, useState } from 'react';
import { orderApi } from '../api';
import { TicketInstanceResponse } from '../types';
import { QRCodeSVG } from 'qrcode.react';
import {
  Ticket,
  Calendar,
  MapPin,
  Clock,
  Sparkles,
  AlertCircle,
  Armchair,
  CheckCircle2,
  Download,
  Share2
} from 'lucide-react';

export const MyTickets: React.FC = () => {
  const [tickets, setTickets] = useState<TicketInstanceResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadTickets();
  }, []);

  const loadTickets = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await orderApi.getMyTickets();
      setTickets(data || []);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Không thể tải danh sách vé');
      }
    } finally {
      setLoading(false);
    }
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div className="mb-8">
        <h1 className="text-2xl sm:text-3xl font-black text-gray-900 tracking-tight flex items-center gap-2">
          <span>Ví Vé Điện Tử (My Tickets)</span>
          <span className="px-2.5 py-0.5 rounded-full text-xs font-black bg-brand-50 text-brand-600 border border-brand-200">
            {tickets.length} vé
          </span>
        </h1>
        <p className="text-sm text-gray-500 mt-1">
          Vé hợp lệ sau khi thanh toán thành công. Xuất trình mã QR tại cổng kiểm soát để check-in.
        </p>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-2xl bg-red-50 border border-red-200 flex items-start gap-3 text-red-700 text-sm">
          <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="space-y-6">
          {[1, 2].map((n) => (
            <div key={n} className="bg-white rounded-3xl border border-gray-200 p-6 animate-pulse space-y-4">
              <div className="h-6 bg-gray-200 rounded w-1/3" />
              <div className="h-32 bg-gray-100 rounded-2xl" />
            </div>
          ))}
        </div>
      ) : tickets.length === 0 ? (
        <div className="bg-white rounded-3xl border border-gray-200 p-12 text-center">
          <Ticket className="w-16 h-16 text-gray-300 mx-auto mb-3" />
          <h3 className="text-base font-bold text-gray-800">Bạn chưa có vé nào</h3>
          <p className="text-xs text-gray-500 mt-1">
            Các vé sau khi thanh toán thành công (PAID) sẽ tự động xuất hiện tại đây cùng mã QR check-in!
          </p>
        </div>
      ) : (
        <div className="space-y-8">
          {tickets.map((tix) => {
            const isSeated = tix.lineType === 'SEATED';
            return (
              <div
                key={tix.id}
                className="relative bg-white rounded-3xl border-2 border-gray-200 shadow-xl overflow-hidden flex flex-col md:flex-row hover:border-brand-500/50 transition-all"
              >
                {/* Left Ticket Stub */}
                <div className="flex-1 p-6 sm:p-8 flex flex-col justify-between border-b md:border-b-0 md:border-r border-dashed border-gray-300 relative">
                  <div>
                    <div className="flex items-center justify-between gap-2 mb-3">
                      <span className="px-3 py-1 rounded-full text-[11px] font-black uppercase tracking-wider bg-brand-50 text-brand-600 border border-brand-200">
                        {tix.lineType} TICKET
                      </span>
                      <span className="flex items-center gap-1 text-xs font-bold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded">
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        {tix.status}
                      </span>
                    </div>

                    <h2 className="text-xl sm:text-2xl font-black text-gray-900 mb-2">
                      {tix.eventName}
                    </h2>

                    <div className="space-y-1.5 text-xs text-gray-600 font-semibold mb-6">
                      {tix.sessionName && (
                        <div className="flex items-center gap-2">
                          <Clock className="w-4 h-4 text-brand-500" />
                          <span>Suất diễn: {tix.sessionName}</span>
                        </div>
                      )}
                      {tix.zoneName && (
                        <div className="flex items-center gap-2">
                          <MapPin className="w-4 h-4 text-gray-400" />
                          <span>Khu vực: {tix.zoneName}</span>
                        </div>
                      )}
                    </div>
                  </div>

                  {/* Seat / Position Detail Callout */}
                  <div className="bg-gray-50 rounded-2xl p-4 border border-gray-100 flex items-center justify-between">
                    <div>
                      <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">
                        {isSeated ? 'Vị trí ghế ngồi' : 'Loại vé / Khu vực'}
                      </span>
                      <span className="text-base sm:text-lg font-black text-brand-dark flex items-center gap-1.5">
                        {isSeated && <Armchair className="w-4 h-4 text-brand-500" />}
                        {tix.seatInfo || tix.zoneName || 'General Admission'}
                      </span>
                    </div>
                    <div className="text-right">
                      <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">
                        Mệnh giá
                      </span>
                      <span className="text-base font-black text-brand-500 tnum">
                        {formatPrice(tix.price)}
                      </span>
                    </div>
                  </div>
                </div>

                {/* Right QR Code Stub */}
                <div className="w-full md:w-64 bg-gray-50/80 p-6 sm:p-8 flex flex-col items-center justify-center text-center">
                  <div className="bg-white p-3 rounded-2xl shadow-md border border-gray-200 mb-3">
                    <QRCodeSVG
                      value={tix.qrPayload || tix.ticketCode}
                      size={130}
                      level="H"
                      includeMargin={false}
                    />
                  </div>
                  <span className="font-mono text-xs font-black tracking-widest text-gray-700 mb-1">
                    {tix.ticketCode}
                  </span>
                  <p className="text-[10px] font-semibold text-gray-400">
                    Quét tại cửa kiểm soát vé
                  </p>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
