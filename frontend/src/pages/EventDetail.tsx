import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { eventApi, orderApi } from '../api';
import { EventResponse, TicketTypeResponse, ShowSessionResponse, SessionSeatResponse } from '../types';
import { SeatMap } from '../components/SeatMap';
import { useAuth } from '../auth/AuthContext';
import {
  Calendar,
  MapPin,
  Clock,
  Ticket,
  Armchair,
  CheckCircle,
  AlertCircle,
  ArrowRight,
  Flame,
  Minus,
  Plus,
  Info
} from 'lucide-react';

export const EventDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const eventId = Number(id);
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  const [event, setEvent] = useState<EventResponse | null>(null);
  const [tickets, setTickets] = useState<TicketTypeResponse[]>([]);
  const [sessions, setSessions] = useState<ShowSessionResponse[]>([]);
  const [activeTab, setActiveTab] = useState<'GA' | 'SEATED'>('GA');

  // GA Booking State
  const [selectedTicketTypeId, setSelectedTicketTypeId] = useState<number | null>(null);
  const [quantity, setQuantity] = useState<number>(1);

  // Seated Booking State
  const [selectedSessionId, setSelectedSessionId] = useState<number | null>(null);
  const [seats, setSeats] = useState<SessionSeatResponse[]>([]);
  const [selectedSeatIds, setSelectedSeatIds] = useState<number[]>([]);
  const [seatsLoading, setSeatsLoading] = useState(false);

  // Submit / Error states
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!eventId) return;
    loadEventData();
  }, [eventId]);

  const loadEventData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [evtData, tixData, sessData] = await Promise.all([
        eventApi.getEventById(eventId),
        eventApi.getEventTickets(eventId).catch(() => []),
        eventApi.getEventSessions(eventId).catch(() => [])
      ]);

      setEvent(evtData);
      setTickets(tixData || []);
      setSessions(sessData || []);

      if (tixData && tixData.length > 0) {
        setSelectedTicketTypeId(tixData[0].id);
      }

      if (sessData && sessData.length > 0) {
        setSelectedSessionId(sessData[0].id);
      }
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Không thể tải thông tin sự kiện');
      }
    } finally {
      setLoading(false);
    }
  };

  // Load seats when a session is selected
  useEffect(() => {
    if (!selectedSessionId) return;
    loadSeats(selectedSessionId);
  }, [selectedSessionId]);

  const loadSeats = async (sessionId: number) => {
    try {
      setSeatsLoading(true);
      const data = await eventApi.getSessionSeats(sessionId);
      setSeats(data || []);
      setSelectedSeatIds([]); // Reset selection when switching session
    } catch (err: unknown) {
      console.error('Failed to load session seats', err);
    } finally {
      setSeatsLoading(false);
    }
  };

  const handleToggleSeat = (seatId: number) => {
    setError(null);
    if (selectedSeatIds.includes(seatId)) {
      setSelectedSeatIds(selectedSeatIds.filter((id) => id !== seatId));
    } else {
      if (selectedSeatIds.length >= 6) {
        setError('Bạn chỉ được chọn tối đa 6 ghế trong mỗi đơn hàng');
        return;
      }
      setSelectedSeatIds([...selectedSeatIds, seatId]);
    }
  };

  // GA Booking Submit
  const handleBookGA = async () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: { pathname: `/events/${eventId}` } } });
      return;
    }
    if (!selectedTicketTypeId) {
      setError('Vui lòng chọn loại vé GA');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      const order = await orderApi.createGaOrder(selectedTicketTypeId, quantity);
      navigate(`/orders/${order.id}`);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Đặt vé thất bại');
      }
    } finally {
      setSubmitting(false);
    }
  };

  // Seated Booking Submit
  const handleBookSeated = async () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: { pathname: `/events/${eventId}` } } });
      return;
    }
    if (!selectedSessionId) {
      setError('Vui lòng chọn suất diễn');
      return;
    }
    if (selectedSeatIds.length === 0) {
      setError('Vui lòng chọn ít nhất 1 ghế trống trên sơ đồ');
      return;
    }

    try {
      setSubmitting(true);
      setError(null);
      const order = await orderApi.createSeatedOrder(selectedSessionId, selectedSeatIds);
      navigate(`/orders/${order.id}`);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Khóa ghế thất bại');
      }
    } finally {
      setSubmitting(false);
    }
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const selectedTicket = tickets.find((t) => t.id === selectedTicketTypeId);

  // Selected seats total price calculation
  const selectedSeatsInfo = seats.filter((s) => selectedSeatIds.includes(s.seatId));
  const totalSeatedPrice = selectedSeatsInfo.reduce((acc, s) => acc + (s.price || 0), 0);

  if (loading) {
    return (
      <div className="max-w-6xl mx-auto px-4 py-16 text-center animate-pulse">
        <div className="h-10 bg-gray-200 rounded-xl w-1/3 mx-auto mb-4" />
        <div className="h-6 bg-gray-100 rounded w-1/4 mx-auto mb-8" />
        <div className="h-96 bg-gray-200 rounded-3xl" />
      </div>
    );
  }

  if (!event) {
    return (
      <div className="max-w-md mx-auto text-center py-20">
        <AlertCircle className="w-16 h-16 text-gray-300 mx-auto mb-4" />
        <h2 className="text-xl font-bold text-gray-800">Không tìm thấy sự kiện</h2>
      </div>
    );
  }

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
      {/* Event Header Banner */}
      <div className="relative rounded-3xl overflow-hidden bg-gray-900 text-white mb-8 shadow-xl">
        {event.imageUrl && (
          <div className="absolute inset-0 opacity-30">
            <img src={event.imageUrl} alt={event.name} className="w-full h-full object-cover" />
          </div>
        )}
        <div className="absolute inset-0 bg-gradient-to-t from-brand-dark via-brand-dark/80 to-transparent" />

        <div className="relative p-6 sm:p-10 max-w-4xl">
          <span className="px-3 py-1 rounded-full text-xs font-bold bg-brand-500 text-white uppercase tracking-wider mb-4 inline-block shadow-md">
            {event.status}
          </span>
          <h1 className="text-3xl sm:text-5xl font-black tracking-tight leading-tight mb-4">
            {event.name}
          </h1>

          <div className="flex flex-wrap gap-y-2 gap-x-6 text-sm text-gray-300 font-semibold mb-6">
            <div className="flex items-center gap-2">
              <Calendar className="w-4 h-4 text-brand-500" />
              <span>{new Date(event.eventDate).toLocaleString('vi-VN')}</span>
            </div>
            <div className="flex items-center gap-2">
              <MapPin className="w-4 h-4 text-brand-500" />
              <span>{event.location}</span>
            </div>
            <div className="flex items-center gap-2">
              <Info className="w-4 h-4 text-brand-500" />
              <span>Bởi: {event.organizerName || 'Ban Tổ Chức'}</span>
            </div>
          </div>

          <p className="text-gray-300 text-sm leading-relaxed max-w-3xl">
            {event.description}
          </p>
        </div>
      </div>

      {/* Global Error Banner if any */}
      {error && (
        <div className="mb-6 p-4 rounded-2xl bg-red-50 border border-red-200 flex items-start gap-3 text-red-700 text-sm">
          <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5" />
          <div className="flex-1">
            <p className="font-bold">Thông báo</p>
            <p>{error}</p>
          </div>
        </div>
      )}

      {/* Booking Mode Selector (GA vs Seated) */}
      <div className="bg-white rounded-3xl border border-gray-200 p-2 sm:p-3 mb-8 shadow-sm max-w-md mx-auto flex items-center gap-2">
        <button
          type="button"
          onClick={() => setActiveTab('GA')}
          className={`flex-1 py-3 px-4 rounded-2xl text-xs sm:text-sm font-black flex items-center justify-center gap-2 transition ${
            activeTab === 'GA'
              ? 'bg-brand-500 text-white shadow-md shadow-brand-500/25'
              : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
          }`}
        >
          <Ticket className="w-4 h-4" />
          <span>Vé Khu Vực (GA)</span>
        </button>

        <button
          type="button"
          onClick={() => setActiveTab('SEATED')}
          className={`flex-1 py-3 px-4 rounded-2xl text-xs sm:text-sm font-black flex items-center justify-center gap-2 transition ${
            activeTab === 'SEATED'
              ? 'bg-brand-500 text-white shadow-md shadow-brand-500/25'
              : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
          }`}
        >
          <Armchair className="w-4 h-4" />
          <span>Vé Ghế Ngồi (Seated)</span>
        </button>
      </div>

      {/* TAB 1: GA BOOKING */}
      {activeTab === 'GA' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Ticket types list */}
          <div className="lg:col-span-2 space-y-4">
            <h2 className="text-xl font-extrabold text-gray-900">Chọn Hạng Vé</h2>
            {tickets.length === 0 ? (
              <div className="bg-white rounded-2xl border border-gray-200 p-8 text-center text-gray-500">
                Chưa có hạng vé GA mở bán cho sự kiện này.
              </div>
            ) : (
              tickets.map((tix) => {
                const isSelected = selectedTicketTypeId === tix.id;
                const isAvailable = (tix.realtimeAvailable ?? tix.availableQuantity) > 0;

                return (
                  <div
                    key={tix.id}
                    onClick={() => isAvailable && setSelectedTicketTypeId(tix.id)}
                    className={`p-6 rounded-2xl border-2 transition-all cursor-pointer flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 ${
                      isSelected
                        ? 'border-brand-500 bg-orange-50/40 shadow-md shadow-brand-500/10'
                        : isAvailable
                        ? 'border-gray-200 bg-white hover:border-gray-300'
                        : 'border-gray-200 bg-gray-50 opacity-60 cursor-not-allowed'
                    }`}
                  >
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span className="font-black text-lg text-gray-900">{tix.name}</span>
                        {isAvailable ? (
                          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-emerald-100 text-emerald-700">
                            Còn {tix.realtimeAvailable ?? tix.availableQuantity} vé
                          </span>
                        ) : (
                          <span className="px-2 py-0.5 rounded text-[10px] font-bold bg-gray-200 text-gray-600">
                            HẾT VÉ / SOLD OUT
                          </span>
                        )}
                      </div>
                      <p className="text-xs text-gray-500">{tix.description || 'Vé vào cổng tự do theo khu'}</p>
                    </div>

                    <div className="text-right sm:flex-shrink-0 flex sm:flex-col items-center sm:items-end justify-between w-full sm:w-auto">
                      <span className="text-xl font-black text-brand-500 tnum">
                        {formatPrice(tix.price)}
                      </span>
                      <span className="text-[11px] text-gray-400">/ vé</span>
                    </div>
                  </div>
                );
              })
            )}
          </div>

          {/* Checkout Box */}
          <div className="bg-white rounded-3xl border border-gray-200 p-6 shadow-lg h-fit sticky top-24">
            <h3 className="text-lg font-black text-gray-900 mb-4 pb-3 border-b border-gray-100">
              Chi Tiết Đặt Vé GA
            </h3>

            {selectedTicket ? (
              <div className="space-y-4">
                <div>
                  <span className="text-xs text-gray-500 uppercase font-bold">Hạng vé đã chọn</span>
                  <div className="font-extrabold text-gray-900 text-base">{selectedTicket.name}</div>
                  <div className="text-brand-500 font-bold text-sm">{formatPrice(selectedTicket.price)}</div>
                </div>

                <div>
                  <span className="text-xs text-gray-500 uppercase font-bold block mb-2">Số lượng</span>
                  <div className="flex items-center gap-3">
                    <button
                      type="button"
                      disabled={quantity <= (selectedTicket.minPerOrder || 1)}
                      onClick={() => setQuantity(Math.max((selectedTicket.minPerOrder || 1), quantity - 1))}
                      className="w-10 h-10 rounded-xl border border-gray-200 flex items-center justify-center text-gray-600 hover:bg-gray-100 disabled:opacity-40"
                    >
                      <Minus className="w-4 h-4" />
                    </button>
                    <span className="text-lg font-black tnum w-8 text-center">{quantity}</span>
                    <button
                      type="button"
                      disabled={quantity >= (selectedTicket.maxPerOrder || 10)}
                      onClick={() => setQuantity(Math.min((selectedTicket.maxPerOrder || 10), quantity + 1))}
                      className="w-10 h-10 rounded-xl border border-gray-200 flex items-center justify-center text-gray-600 hover:bg-gray-100 disabled:opacity-40"
                    >
                      <Plus className="w-4 h-4" />
                    </button>
                  </div>
                  <p className="text-[11px] text-gray-400 mt-1">
                    Tối thiểu {selectedTicket.minPerOrder || 1}, tối đa {selectedTicket.maxPerOrder || 10} vé/đơn
                  </p>
                </div>

                <div className="pt-4 border-t border-gray-100 flex items-center justify-between">
                  <span className="text-sm font-bold text-gray-600">Tổng tiền</span>
                  <span className="text-2xl font-black text-brand-500 tnum">
                    {formatPrice(selectedTicket.price * quantity)}
                  </span>
                </div>

                <button
                  type="button"
                  disabled={submitting || (selectedTicket.realtimeAvailable ?? selectedTicket.availableQuantity) <= 0}
                  onClick={handleBookGA}
                  className="w-full py-4 px-4 bg-brand-500 hover:bg-brand-600 active:bg-brand-700 disabled:opacity-50 text-white font-black rounded-xl shadow-lg shadow-brand-500/25 flex items-center justify-center gap-2 transition"
                >
                  {submitting ? (
                    <span>Đang giữ vé...</span>
                  ) : (
                    <>
                      <span>Giữ Vé GA Ngay</span>
                      <ArrowRight className="w-4 h-4" />
                    </>
                  )}
                </button>
              </div>
            ) : (
              <p className="text-xs text-gray-400">Vui lòng chọn 1 hạng vé bên trái</p>
            )}
          </div>
        </div>
      )}

      {/* TAB 2: SEATED BOOKING */}
      {activeTab === 'SEATED' && (
        <div className="space-y-6">
          {/* Sessions selector */}
          <div className="bg-white rounded-2xl border border-gray-200 p-6 shadow-sm">
            <h2 className="text-lg font-extrabold text-gray-900 mb-3">1. Chọn Suất Diễn (Session)</h2>
            {sessions.length === 0 ? (
              <p className="text-sm text-gray-500">Sự kiện chưa có suất diễn nào được thiết lập.</p>
            ) : (
              <div className="flex flex-wrap gap-3">
                {sessions.map((sess) => {
                  const isSelected = selectedSessionId === sess.id;
                  return (
                    <button
                      key={sess.id}
                      type="button"
                      onClick={() => setSelectedSessionId(sess.id)}
                      className={`px-4 py-3 rounded-xl border text-left transition ${
                        isSelected
                          ? 'border-brand-500 bg-orange-50 text-brand-700 ring-2 ring-brand-500/20'
                          : 'border-gray-200 bg-white hover:bg-gray-50 text-gray-700'
                      }`}
                    >
                      <div className="font-black text-sm">{sess.sessionName}</div>
                      <div className="text-xs text-gray-500 flex items-center gap-1 mt-1">
                        <Clock className="w-3.5 h-3.5" />
                        <span>{new Date(sess.startTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</span>
                        <span>-</span>
                        <span>{new Date(sess.endTime).toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' })}</span>
                      </div>
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          {/* Seat Map Area */}
          <div>
            <div className="flex items-center justify-between mb-3">
              <h2 className="text-lg font-extrabold text-gray-900">2. Chọn Vị Trí Ghế Ngồi</h2>
              <button
                onClick={() => selectedSessionId && loadSeats(selectedSessionId)}
                className="text-xs font-bold text-brand-500 hover:text-brand-600 px-3 py-1.5 rounded-lg border border-brand-500/20 hover:bg-brand-50 transition"
              >
                Cập nhật ghế trống
              </button>
            </div>

            {seatsLoading ? (
              <div className="bg-white rounded-2xl border border-gray-200 p-16 text-center animate-pulse">
                <Armchair className="w-12 h-12 text-gray-300 mx-auto mb-3" />
                <p className="font-bold text-gray-600">Đang tải sơ đồ ghế ngồi...</p>
              </div>
            ) : (
              <SeatMap
                seats={seats}
                selectedSeatIds={selectedSeatIds}
                onToggleSeat={handleToggleSeat}
              />
            )}
          </div>

          {/* Seated Bottom Floating Bar / Checkout Drawer */}
          {selectedSeatIds.length > 0 && (
            <div className="fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur border-t border-gray-200 shadow-2xl p-4 sm:p-6 transition-transform">
              <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
                <div>
                  <div className="text-xs font-bold text-gray-400 uppercase tracking-wider">
                    Đã chọn {selectedSeatIds.length} ghế:
                  </div>
                  <div className="flex flex-wrap gap-1.5 mt-1">
                    {selectedSeatsInfo.map((s) => (
                      <span
                        key={s.seatId}
                        className="px-2.5 py-1 bg-amber-100 text-amber-800 rounded-lg text-xs font-black"
                      >
                        {s.rowLabel}{s.seatNumber} ({s.zoneName || 'Khu A'})
                      </span>
                    ))}
                  </div>
                </div>

                <div className="flex items-center gap-6 w-full sm:w-auto justify-between sm:justify-end">
                  <div className="text-right">
                    <span className="text-[10px] text-gray-400 uppercase font-bold block">Tổng cộng</span>
                    <span className="text-2xl font-black text-brand-500 tnum">
                      {formatPrice(totalSeatedPrice)}
                    </span>
                  </div>

                  <button
                    type="button"
                    disabled={submitting}
                    onClick={handleBookSeated}
                    className="py-3.5 px-8 bg-brand-500 hover:bg-brand-600 active:bg-brand-700 disabled:opacity-50 text-white font-black rounded-xl shadow-lg shadow-brand-500/30 flex items-center gap-2 transition"
                  >
                    {submitting ? (
                      <span>Đang khóa ghế...</span>
                    ) : (
                      <>
                        <span>Xác Nhận Giữ Ghế</span>
                        <ArrowRight className="w-4 h-4" />
                      </>
                    )}
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
