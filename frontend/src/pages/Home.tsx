import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { eventApi } from '../api';
import { EventResponse } from '../types';
import { Calendar, MapPin, Ticket, Flame, ArrowRight, Sparkles, Clock, AlertCircle } from 'lucide-react';

export const Home: React.FC = () => {
  const [events, setEvents] = useState<EventResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchEvents();
  }, []);

  const fetchEvents = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await eventApi.getEvents();
      setEvents(Array.isArray(data) ? data : []);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Không thể tải danh sách sự kiện');
      }
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateString: string) => {
    try {
      const d = new Date(dateString);
      return new Intl.DateTimeFormat('vi-VN', {
        weekday: 'short',
        day: '2-digit',
        month: '2-digit',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      }).format(d);
    } catch {
      return dateString;
    }
  };

  const getLowestPrice = (event: EventResponse) => {
    if (!event.ticketTypes || event.ticketTypes.length === 0) return null;
    const prices = event.ticketTypes.map((t) => t.price);
    const min = Math.min(...prices);
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(min);
  };

  return (
    <div className="pb-20">
      {/* Hero Banner with Stitch Kinetic Aesthetic */}
      <section className="relative overflow-hidden bg-gradient-to-b from-brand-dark via-gray-900 to-brand-dark text-white pt-16 pb-20 px-4 sm:px-6 lg:px-8 mb-12 rounded-3xl shadow-2xl">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(255,77,0,0.18),transparent_50%)]" />
        <div className="relative max-w-5xl mx-auto text-center">
          <div className="inline-flex items-center gap-2 px-3.5 py-1.5 rounded-full bg-brand-500/10 border border-brand-500/30 text-brand-500 text-xs font-bold uppercase tracking-widest mb-6 backdrop-blur">
            <Sparkles className="w-3.5 h-3.5 text-brand-500" />
            Nền tảng vé sự kiện thế hệ mới
          </div>

          <h1 className="text-4xl sm:text-6xl font-black tracking-tight leading-tight mb-6">
            Săn Vé Siêu Tốc <br className="hidden sm:inline" />
            <span className="text-transparent bg-clip-text bg-gradient-to-r from-brand-500 via-orange-400 to-amber-300">
              Flash Sale Ticketing Hybrid
            </span>
          </h1>

          <p className="max-w-2xl mx-auto text-gray-300 text-base sm:text-lg mb-8 font-medium">
            Tận hưởng trải nghiệm giữ vé siêu tốc theo khu vực đứng (GA) hoặc chọn trực tiếp từng ghế ngồi (Seated Seatmap) với Redis lock & thời gian giữ vé tự động.
          </p>

          <div className="flex flex-wrap items-center justify-center gap-3">
            <a
              href="#events-list"
              className="px-6 py-3.5 rounded-xl bg-brand-500 hover:bg-brand-600 text-white font-extrabold text-sm shadow-lg shadow-brand-500/30 flex items-center gap-2 transition"
            >
              <Flame className="w-4 h-4" />
              Khám phá sự kiện ngay
            </a>
          </div>
        </div>
      </section>

      {/* Events List */}
      <div id="events-list" className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between mb-8">
          <div>
            <h2 className="text-2xl sm:text-3xl font-extrabold text-gray-900 tracking-tight">
              Sự Kiện Đang Mở Bán
            </h2>
            <p className="text-sm text-gray-500 mt-1">Đừng bỏ lỡ những đêm diễn đỉnh cao nhất mùa này</p>
          </div>
          <button
            onClick={fetchEvents}
            className="text-xs font-bold text-brand-500 hover:text-brand-600 px-3 py-1.5 rounded-lg border border-brand-500/30 hover:bg-brand-50 transition"
          >
            Làm mới
          </button>
        </div>

        {error && (
          <div className="mb-8 p-4 rounded-2xl bg-red-50 border border-red-200 flex items-start gap-3 text-red-700 text-sm">
            <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5" />
            <div>
              <p className="font-bold">Không thể tải sự kiện từ Backend</p>
              <p className="text-xs mt-1 text-red-600">{error}</p>
            </div>
          </div>
        )}

        {loading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[1, 2, 3].map((n) => (
              <div key={n} className="bg-white rounded-3xl border border-gray-200 p-6 animate-pulse space-y-4">
                <div className="h-48 bg-gray-200 rounded-2xl" />
                <div className="h-6 bg-gray-200 rounded w-3/4" />
                <div className="h-4 bg-gray-100 rounded w-1/2" />
                <div className="h-10 bg-gray-100 rounded-xl" />
              </div>
            ))}
          </div>
        ) : events.length === 0 ? (
          <div className="text-center py-16 bg-white rounded-3xl border border-gray-200">
            <Ticket className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-lg font-bold text-gray-900">Chưa có sự kiện nào được công bố</h3>
            <p className="text-sm text-gray-500 mt-1 max-w-sm mx-auto">
              Hãy quay lại sau hoặc đăng nhập tài khoản Organizer để bắt đầu tạo sự kiện mới!
            </p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 sm:gap-8">
            {events.map((evt) => {
              const lowestPrice = getLowestPrice(evt);
              return (
                <div
                  key={evt.id}
                  className="group bg-white rounded-3xl border border-gray-200 hover:border-brand-500/40 hover:shadow-xl hover:shadow-gray-200/50 transition-all duration-200 flex flex-col overflow-hidden"
                >
                  {/* Event Thumbnail */}
                  <div className="relative h-52 bg-gradient-to-tr from-gray-900 to-gray-800 overflow-hidden">
                    {evt.imageUrl ? (
                      <img
                        src={evt.imageUrl}
                        alt={evt.name}
                        className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                        onError={(e) => {
                          // Fallback if image fails to load
                          (e.currentTarget as HTMLImageElement).style.display = 'none';
                        }}
                      />
                    ) : (
                      <div className="w-full h-full flex flex-col items-center justify-center text-gray-500">
                        <Flame className="w-12 h-12 text-brand-500/40 mb-2" />
                        <span className="text-xs font-bold text-gray-400">FLASHSALE EVENT</span>
                      </div>
                    )}
                    <div className="absolute top-3 left-3">
                      <span className="px-2.5 py-1 rounded-full text-[11px] font-extrabold uppercase tracking-wider bg-white/90 backdrop-blur text-brand-500 shadow-sm">
                        {evt.status}
                      </span>
                    </div>
                  </div>

                  {/* Content */}
                  <div className="p-6 flex-1 flex flex-col justify-between">
                    <div>
                      <h3 className="text-xl font-black text-gray-900 group-hover:text-brand-500 transition-colors line-clamp-1 mb-2">
                        {evt.name}
                      </h3>
                      <p className="text-xs text-gray-500 line-clamp-2 mb-4 leading-relaxed">
                        {evt.description || 'Không có mô tả chi tiết cho sự kiện này.'}
                      </p>

                      <div className="space-y-2 text-xs font-semibold text-gray-600 mb-6">
                        <div className="flex items-center gap-2">
                          <Calendar className="w-4 h-4 text-brand-500 flex-shrink-0" />
                          <span>{formatDate(evt.eventDate)}</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <MapPin className="w-4 h-4 text-gray-400 flex-shrink-0" />
                          <span className="line-clamp-1">{evt.location || 'Địa điểm chưa xác định'}</span>
                        </div>
                      </div>
                    </div>

                    <div className="pt-4 border-t border-gray-100 flex items-center justify-between">
                      <div>
                        <span className="text-[10px] uppercase font-bold text-gray-400 block">Giá từ</span>
                        <span className="text-lg font-black text-brand-500 tnum">
                          {lowestPrice || 'Liên hệ'}
                        </span>
                      </div>

                      <Link
                        to={`/events/${evt.id}`}
                        className="px-4 py-2.5 rounded-xl bg-brand-dark hover:bg-brand-500 text-white font-bold text-xs flex items-center gap-1.5 transition-colors shadow-sm"
                      >
                        <span>Đặt vé</span>
                        <ArrowRight className="w-3.5 h-3.5" />
                      </Link>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
