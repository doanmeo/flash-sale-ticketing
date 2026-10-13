import React, { useEffect, useState } from 'react';
import { organizerApi } from '../api';
import {
  EventResponse,
  OrganizerEventStatsResponse,
  OrderResponse,
  ShowSessionResponse,
  TicketTypeResponse
} from '../types';
import {
  Plus,
  BarChart3,
  Calendar,
  Layers,
  Download,
  Trash2,
  Send,
  Armchair,
  CheckCircle,
  AlertCircle,
  Clock,
  Ticket,
  ChevronRight
} from 'lucide-react';

export const OrganizerDashboard: React.FC = () => {
  const [events, setEvents] = useState<EventResponse[]>([]);
  const [selectedEventId, setSelectedEventId] = useState<number | null>(null);
  const [stats, setStats] = useState<OrganizerEventStatsResponse | null>(null);
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [sessions, setSessions] = useState<ShowSessionResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  // Modal / Form state for creating an Event
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newEvent, setNewEvent] = useState({
    name: '',
    description: '',
    location: '',
    eventDate: '',
    imageUrl: '',
  });

  // Form for creating a Ticket Type
  const [showTicketModal, setShowTicketModal] = useState(false);
  const [newTicket, setNewTicket] = useState({
    name: '',
    description: '',
    price: 150000,
    totalQuantity: 100,
    saleStart: '',
    saleEnd: '',
    minPerOrder: 1,
    maxPerOrder: 5,
  });

  // Form for creating a Session
  const [showSessionModal, setShowSessionModal] = useState(false);
  const [newSession, setNewSession] = useState({
    sessionName: 'Đêm diễn Chính (Main Night)',
    startTime: '',
    endTime: '',
  });

  useEffect(() => {
    loadMyEvents();
  }, []);

  const loadMyEvents = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await organizerApi.getMyEvents(0, 50);
      setEvents(res.content || []);
      if (res.content && res.content.length > 0 && !selectedEventId) {
        setSelectedEventId(res.content[0].id);
      }
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

  // Load details for selected event
  useEffect(() => {
    if (!selectedEventId) return;
    loadEventDetails(selectedEventId);
  }, [selectedEventId]);

  const loadEventDetails = async (eventId: number) => {
    try {
      const [statsData, ordersData, sessionsData] = await Promise.all([
        organizerApi.getEventStats(eventId).catch(() => null),
        organizerApi.getEventOrders(eventId, undefined, 0, 20).catch(() => ({ content: [] })),
        organizerApi.getSessions(eventId).catch(() => []),
      ]);
      setStats(statsData);
      setOrders(ordersData.content || []);
      setSessions(sessionsData || []);
    } catch (err) {
      console.error(err);
    }
  };

  const handleCreateEvent = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setActionLoading(true);
      setError(null);
      const created = await organizerApi.createEvent(newEvent);
      setSuccess(`Đã tạo sự kiện: ${created.name}`);
      setShowCreateModal(false);
      setNewEvent({ name: '', description: '', location: '', eventDate: '', imageUrl: '' });
      await loadMyEvents();
      setSelectedEventId(created.id);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Tạo sự kiện thất bại');
      }
    } finally {
      setActionLoading(false);
    }
  };

  const handlePublishEvent = async (eventId: number) => {
    try {
      setActionLoading(true);
      setError(null);
      await organizerApi.publishEvent(eventId);
      setSuccess('Đã công bố sự kiện thành công! Người dùng có thể mua vé ngay.');
      await loadMyEvents();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Công bố sự kiện thất bại');
      }
    } finally {
      setActionLoading(false);
    }
  };

  const handleCreateTicketType = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedEventId) return;
    try {
      setActionLoading(true);
      setError(null);

      // Default saleStart to now, saleEnd to eventDate or 7 days later if empty
      const now = new Date();
      const defaultStart = now.toISOString().slice(0, 19);
      const defaultEnd = selectedEvent?.eventDate
        ? selectedEvent.eventDate.slice(0, 19)
        : new Date(now.getTime() + 7 * 24 * 60 * 60 * 1000).toISOString().slice(0, 19);

      const payload = {
        ...newTicket,
        saleStart: newTicket.saleStart ? newTicket.saleStart.slice(0, 19) : defaultStart,
        saleEnd: newTicket.saleEnd ? newTicket.saleEnd.slice(0, 19) : defaultEnd,
      };

      await organizerApi.createTicketType(selectedEventId, payload);
      setSuccess('Đã thêm loại vé mới!');
      setShowTicketModal(false);
      setNewTicket({
        name: '',
        description: '',
        price: 150000,
        totalQuantity: 100,
        saleStart: '',
        saleEnd: '',
        minPerOrder: 1,
        maxPerOrder: 5,
      });
      await loadEventDetails(selectedEventId);
      await loadMyEvents();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Thêm loại vé thất bại');
      }
    } finally {
      setActionLoading(false);
    }
  };

  const handleCreateSession = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedEventId) return;
    try {
      setActionLoading(true);
      setError(null);
      await organizerApi.createSession(selectedEventId, newSession);
      setSuccess('Đã tạo suất diễn mới!');
      setShowSessionModal(false);
      await loadEventDetails(selectedEventId);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Tạo suất diễn thất bại');
      }
    } finally {
      setActionLoading(false);
    }
  };

  const handleExportCsv = async () => {
    if (!selectedEventId) return;
    try {
      const blob = await organizerApi.exportOrdersCsv(selectedEventId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `orders-event-${selectedEventId}.csv`;
      document.body.appendChild(a);
      a.click();
      window.URL.revokeObjectURL(url);
      document.body.removeChild(a);
      setSuccess('Tải danh sách đơn hàng CSV thành công!');
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Không thể xuất CSV');
      }
    }
  };

  const formatPrice = (price?: number) => {
    if (price === undefined || price === null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  const selectedEvent = events.find((e) => e.id === selectedEventId);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-gray-900 tracking-tight">
            Organizer Hub (Quản Lý Sự Kiện)
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Tạo và công bố sự kiện, cấu hình vé GA, thiết lập suất diễn và theo dõi doanh thu.
          </p>
        </div>

        <button
          onClick={() => setShowCreateModal(true)}
          className="px-5 py-3 rounded-xl bg-brand-500 hover:bg-brand-600 text-white font-black text-sm flex items-center gap-2 shadow-lg shadow-brand-500/25 transition self-start sm:self-auto"
        >
          <Plus className="w-5 h-5" />
          <span>Tạo Sự Kiện Mới</span>
        </button>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-2xl bg-red-50 border border-red-200 flex items-start gap-3 text-red-700 text-sm">
          <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {success && (
        <div className="mb-6 p-4 rounded-2xl bg-emerald-50 border border-emerald-200 flex items-start gap-3 text-emerald-800 text-sm">
          <CheckCircle className="w-5 h-5 flex-shrink-0 mt-0.5" />
          <span>{success}</span>
        </div>
      )}

      {/* Grid: Left Event List, Right Event Details & Actions */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left: Events Selector */}
        <div className="space-y-4">
          <h2 className="text-xs font-bold text-gray-400 uppercase tracking-wider">
            Sự Kiện Của Tôi ({events.length})
          </h2>

          {loading ? (
            <div className="space-y-3">
              {[1, 2].map((n) => (
                <div key={n} className="h-20 bg-gray-200 rounded-2xl animate-pulse" />
              ))}
            </div>
          ) : events.length === 0 ? (
            <div className="bg-white rounded-2xl border border-gray-200 p-8 text-center text-gray-500">
              <Calendar className="w-10 h-10 text-gray-300 mx-auto mb-2" />
              <p className="text-xs font-semibold">Chưa có sự kiện nào. Hãy nhấn nút tạo sự kiện ở trên!</p>
            </div>
          ) : (
            events.map((evt) => {
              const isSelected = selectedEventId === evt.id;
              return (
                <div
                  key={evt.id}
                  onClick={() => setSelectedEventId(evt.id)}
                  className={`p-4 rounded-2xl border-2 transition cursor-pointer ${
                    isSelected
                      ? 'border-brand-500 bg-orange-50/40 shadow-md'
                      : 'border-gray-200 bg-white hover:border-gray-300'
                  }`}
                >
                  <div className="flex items-center justify-between mb-1">
                    <span
                      className={`px-2 py-0.5 rounded text-[10px] font-black uppercase ${
                        evt.status === 'PUBLISHED'
                          ? 'bg-emerald-100 text-emerald-700'
                          : 'bg-amber-100 text-amber-700'
                      }`}
                    >
                      {evt.status}
                    </span>
                    <span className="text-[11px] text-gray-400">
                      {new Date(evt.eventDate).toLocaleDateString('vi-VN')}
                    </span>
                  </div>
                  <h3 className="font-bold text-gray-900 text-sm line-clamp-1">{evt.name}</h3>
                  <p className="text-xs text-gray-500 line-clamp-1 mt-0.5">{evt.location}</p>
                </div>
              );
            })
          )}
        </div>

        {/* Right: Selected Event Details & Management */}
        {selectedEvent ? (
          <div className="lg:col-span-2 space-y-6">
            {/* Header with Publish button */}
            <div className="bg-white rounded-3xl border border-gray-200 p-6 sm:p-8 shadow-sm">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-6">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-xs font-bold text-gray-400">ID #{selectedEvent.id}</span>
                    <span className="px-2 py-0.5 bg-gray-100 rounded text-xs font-bold">{selectedEvent.status}</span>
                  </div>
                  <h2 className="text-2xl font-black text-gray-900">{selectedEvent.name}</h2>
                </div>

                <div className="flex items-center gap-2">
                  {selectedEvent.status === 'DRAFT' && (
                    <button
                      type="button"
                      disabled={actionLoading}
                      onClick={() => handlePublishEvent(selectedEvent.id)}
                      className="px-4 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl flex items-center gap-1.5 shadow transition"
                    >
                      <Send className="w-3.5 h-3.5" />
                      <span>Công bố (Publish)</span>
                    </button>
                  )}
                  <button
                    type="button"
                    onClick={handleExportCsv}
                    className="px-4 py-2.5 bg-gray-800 hover:bg-gray-900 text-white font-bold text-xs rounded-xl flex items-center gap-1.5 shadow transition"
                  >
                    <Download className="w-3.5 h-3.5" />
                    <span>Xuất CSV đơn hàng</span>
                  </button>
                </div>
              </div>

              {/* Stats overview cards */}
              {stats && (
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 pt-4 border-t border-gray-100">
                  <div className="p-4 bg-gray-50 rounded-2xl">
                    <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">
                      Đơn thanh toán
                    </span>
                    <span className="text-xl font-black text-gray-900 tnum">
                      {stats.totalPaidOrders}
                    </span>
                  </div>
                  <div className="p-4 bg-gray-50 rounded-2xl">
                    <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">
                      Tổng doanh thu
                    </span>
                    <span className="text-xl font-black text-brand-500 tnum">
                      {formatPrice(Number(stats.totalRevenue))}
                    </span>
                  </div>
                  <div className="p-4 bg-gray-50 rounded-2xl col-span-2 sm:col-span-1">
                    <span className="text-[10px] font-bold text-gray-400 uppercase tracking-wider block">
                      Hạng vé mở bán
                    </span>
                    <span className="text-xl font-black text-gray-900 tnum">
                      {stats.ticketTypeStats ? stats.ticketTypeStats.length : 0}
                    </span>
                  </div>
                </div>
              )}
            </div>

            {/* Ticket Types Management */}
            <div className="bg-white rounded-3xl border border-gray-200 p-6 sm:p-8 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-extrabold text-base text-gray-900 flex items-center gap-2">
                  <Ticket className="w-4 h-4 text-brand-500" />
                  <span>Quản Lý Hạng Vé (Ticket Types)</span>
                </h3>
                <button
                  type="button"
                  onClick={() => setShowTicketModal(true)}
                  className="px-3 py-1.5 bg-brand-50 text-brand-600 hover:bg-brand-100 font-bold text-xs rounded-lg transition"
                >
                  + Thêm hạng vé
                </button>
              </div>

              {stats?.ticketTypeStats && stats.ticketTypeStats.length > 0 ? (
                <div className="divide-y divide-gray-100">
                  {stats.ticketTypeStats.map((tts) => (
                    <div key={tts.ticketTypeId} className="py-3 flex items-center justify-between">
                      <div>
                        <div className="font-bold text-gray-900 text-sm">{tts.ticketTypeName}</div>
                        <div className="text-xs text-gray-500">
                          {formatPrice(tts.price)} • Tổng: {tts.totalQuantity} vé
                        </div>
                      </div>
                      <div className="text-right">
                        <span className="text-xs font-bold text-emerald-600">
                          Đã bán: {tts.soldQuantity}
                        </span>
                        <span className="text-xs text-gray-400 block">
                          Còn lại: {tts.remainingQuantity}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-gray-400 py-3">Chưa có hạng vé nào được tạo.</p>
              )}
            </div>

            {/* Sessions Management */}
            <div className="bg-white rounded-3xl border border-gray-200 p-6 sm:p-8 shadow-sm">
              <div className="flex items-center justify-between mb-4">
                <h3 className="font-extrabold text-base text-gray-900 flex items-center gap-2">
                  <Armchair className="w-4 h-4 text-brand-500" />
                  <span>Quản Lý Suất Diễn (Show Sessions)</span>
                </h3>
                <button
                  type="button"
                  onClick={() => setShowSessionModal(true)}
                  className="px-3 py-1.5 bg-brand-50 text-brand-600 hover:bg-brand-100 font-bold text-xs rounded-lg transition"
                >
                  + Thêm suất diễn
                </button>
              </div>

              {sessions.length > 0 ? (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  {sessions.map((sess) => (
                    <div key={sess.id} className="p-3.5 bg-gray-50 rounded-2xl border border-gray-100">
                      <div className="font-bold text-sm text-gray-900">{sess.sessionName}</div>
                      <div className="text-xs text-gray-500 flex items-center gap-1 mt-1">
                        <Clock className="w-3.5 h-3.5" />
                        <span>{new Date(sess.startTime).toLocaleString('vi-VN')}</span>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-gray-400 py-3">Chưa có suất diễn nào được thiết lập.</p>
              )}
            </div>
          </div>
        ) : (
          <div className="lg:col-span-2 bg-white rounded-3xl border border-gray-200 p-12 text-center text-gray-500">
            Chọn một sự kiện từ cột bên trái để xem chi tiết và quản lý
          </div>
        )}
      </div>

      {/* CREATE EVENT MODAL */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl">
            <h2 className="text-xl font-black text-gray-900 mb-4">Tạo Sự Kiện Mới</h2>
            <form onSubmit={handleCreateEvent} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">Tên sự kiện</label>
                <input
                  type="text"
                  required
                  value={newEvent.name}
                  onChange={(e) => setNewEvent({ ...newEvent, name: e.target.value })}
                  placeholder="Ví dụ: Summer Music Festival 2026"
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">Mô tả</label>
                <textarea
                  rows={3}
                  value={newEvent.description}
                  onChange={(e) => setNewEvent({ ...newEvent, description: e.target.value })}
                  placeholder="Thông tin chi tiết về sự kiện..."
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">Địa điểm</label>
                <input
                  type="text"
                  required
                  value={newEvent.location}
                  onChange={(e) => setNewEvent({ ...newEvent, location: e.target.value })}
                  placeholder="Sân vận động Mỹ Đình, Hà Nội"
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">Thời gian diễn ra</label>
                <input
                  type="datetime-local"
                  required
                  value={newEvent.eventDate}
                  onChange={(e) => setNewEvent({ ...newEvent, eventDate: e.target.value })}
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">Link Ảnh Banner (URL)</label>
                <input
                  type="url"
                  value={newEvent.imageUrl}
                  onChange={(e) => setNewEvent({ ...newEvent, imageUrl: e.target.value })}
                  placeholder="https://images.unsplash.com/photo-..."
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2.5 text-xs font-bold text-gray-600 hover:bg-gray-100 rounded-xl"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="px-5 py-2.5 text-xs font-black bg-brand-500 hover:bg-brand-600 text-white rounded-xl shadow-md"
                >
                  {actionLoading ? 'Đang tạo...' : 'Lưu Sự Kiện'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* CREATE TICKET TYPE MODAL */}
      {showTicketModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl">
            <h2 className="text-xl font-black text-gray-900 mb-4">Thêm Hạng Vé Mới</h2>
            <form onSubmit={handleCreateTicketType} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">Tên hạng vé</label>
                <input
                  type="text"
                  required
                  value={newTicket.name}
                  onChange={(e) => setNewTicket({ ...newTicket, name: e.target.value })}
                  placeholder="VIP Early Bird / Standard GA"
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">Giá vé (VNĐ)</label>
                <input
                  type="number"
                  required
                  min={0}
                  step={10000}
                  value={newTicket.price}
                  onChange={(e) => setNewTicket({ ...newTicket, price: Number(e.target.value) })}
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">Số lượng phát hành</label>
                <input
                  type="number"
                  required
                  min={1}
                  value={newTicket.totalQuantity}
                  onChange={(e) => setNewTicket({ ...newTicket, totalQuantity: Number(e.target.value) })}
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase mb-1">Bắt đầu mở bán</label>
                  <input
                    type="datetime-local"
                    value={newTicket.saleStart}
                    onChange={(e) => setNewTicket({ ...newTicket, saleStart: e.target.value })}
                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase mb-1">Kết thúc mở bán</label>
                  <input
                    type="datetime-local"
                    value={newTicket.saleEnd}
                    onChange={(e) => setNewTicket({ ...newTicket, saleEnd: e.target.value })}
                    className="w-full px-3 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setShowTicketModal(false)}
                  className="px-4 py-2.5 text-xs font-bold text-gray-600 hover:bg-gray-100 rounded-xl"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="px-5 py-2.5 text-xs font-black bg-brand-500 hover:bg-brand-600 text-white rounded-xl shadow-md"
                >
                  {actionLoading ? 'Đang lưu...' : 'Thêm Hạng Vé'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* CREATE SESSION MODAL */}
      {showSessionModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl">
            <h2 className="text-xl font-black text-gray-900 mb-4">Thêm Suất Diễn (Session)</h2>
            <form onSubmit={handleCreateSession} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">Tên suất diễn</label>
                <input
                  type="text"
                  required
                  value={newSession.sessionName}
                  onChange={(e) => setNewSession({ ...newSession, sessionName: e.target.value })}
                  placeholder="Đêm 1 - 20:00"
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">Bắt đầu lúc</label>
                <input
                  type="datetime-local"
                  required
                  value={newSession.startTime}
                  onChange={(e) => setNewSession({ ...newSession, startTime: e.target.value })}
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm"
                />
              </div>
              <div>
                <label className="block text-xs font-bold text-gray-700 uppercase mb-1">Kết thúc lúc</label>
                <input
                  type="datetime-local"
                  required
                  value={newSession.endTime}
                  onChange={(e) => setNewSession({ ...newSession, endTime: e.target.value })}
                  className="w-full px-3.5 py-2.5 bg-gray-50 border border-gray-200 rounded-xl text-sm"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-gray-100">
                <button
                  type="button"
                  onClick={() => setShowSessionModal(false)}
                  className="px-4 py-2.5 text-xs font-bold text-gray-600 hover:bg-gray-100 rounded-xl"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={actionLoading}
                  className="px-5 py-2.5 text-xs font-black bg-brand-500 hover:bg-brand-600 text-white rounded-xl shadow-md"
                >
                  {actionLoading ? 'Đang tạo...' : 'Tạo Suất Diễn'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
