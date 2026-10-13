import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { orderApi } from '../api';
import { OrderResponse } from '../types';
import { Layers, ArrowRight, Clock, AlertCircle, RefreshCw } from 'lucide-react';

export const MyOrders: React.FC = () => {
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [statusFilter, setStatusFilter] = useState<string>('');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadOrders();
  }, [statusFilter]);

  const loadOrders = async () => {
    try {
      setLoading(true);
      setError(null);
      const res = await orderApi.getMyOrders({
        status: statusFilter || undefined,
        page: 0,
        size: 20,
      });
      setOrders(res.content || []);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Không thể tải lịch sử đơn hàng');
      }
    } finally {
      setLoading(false);
    }
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
        <div>
          <h1 className="text-2xl sm:text-3xl font-black text-gray-900 tracking-tight">
            Đơn Hàng Của Tôi
          </h1>
          <p className="text-sm text-gray-500 mt-1">Theo dõi trạng thái và thanh toán các đơn vé đang giữ</p>
        </div>

        {/* Status Filter */}
        <div className="flex items-center gap-2">
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="px-3.5 py-2 bg-white border border-gray-200 rounded-xl text-xs font-bold text-gray-700 focus:outline-none focus:border-brand-500"
          >
            <option value="">Tất cả trạng thái</option>
            <option value="PENDING">Đang chờ (PENDING)</option>
            <option value="PAID">Đã thanh toán (PAID)</option>
            <option value="CANCELLED">Đã hủy (CANCELLED)</option>
            <option value="EXPIRED">Hết hạn (EXPIRED)</option>
          </select>
          <button
            onClick={loadOrders}
            className="p-2 border border-gray-200 bg-white rounded-xl text-gray-600 hover:text-brand-500 transition"
            title="Làm mới"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-2xl bg-red-50 border border-red-200 flex items-start gap-3 text-red-700 text-sm">
          <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {loading ? (
        <div className="space-y-4">
          {[1, 2, 3].map((n) => (
            <div key={n} className="bg-white rounded-2xl border border-gray-200 p-6 animate-pulse space-y-3">
              <div className="h-6 bg-gray-200 rounded w-1/4" />
              <div className="h-4 bg-gray-100 rounded w-1/2" />
            </div>
          ))}
        </div>
      ) : orders.length === 0 ? (
        <div className="bg-white rounded-3xl border border-gray-200 p-12 text-center">
          <Layers className="w-14 h-14 text-gray-300 mx-auto mb-3" />
          <h3 className="text-base font-bold text-gray-800">Bạn chưa có đơn đặt vé nào</h3>
          <p className="text-xs text-gray-500 mt-1 mb-6">Hãy tham gia ngay các sự kiện hot đang mở bán!</p>
          <Link
            to="/"
            className="px-5 py-2.5 bg-brand-500 text-white font-bold rounded-xl text-xs hover:bg-brand-600 transition"
          >
            Khám phá sự kiện
          </Link>
        </div>
      ) : (
        <div className="space-y-4">
          {orders.map((ord) => {
            const isPending = ord.status === 'PENDING';
            const isPaid = ord.status === 'PAID';

            return (
              <div
                key={ord.id}
                className="bg-white rounded-2xl border border-gray-200 hover:border-brand-500/30 p-6 transition flex flex-col sm:flex-row sm:items-center justify-between gap-4 shadow-sm"
              >
                <div>
                  <div className="flex items-center gap-3 mb-2">
                    <span className="font-mono text-xs font-black text-gray-500 bg-gray-100 px-2 py-0.5 rounded">
                      #{ord.orderCode}
                    </span>
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-[11px] font-black uppercase ${
                        isPaid
                          ? 'bg-emerald-100 text-emerald-800'
                          : isPending
                          ? 'bg-amber-100 text-amber-800'
                          : 'bg-gray-100 text-gray-600'
                      }`}
                    >
                      {ord.status}
                    </span>
                    <span className="text-xs text-gray-400">
                      {new Date(ord.createdAt).toLocaleDateString('vi-VN')}
                    </span>
                  </div>

                  {/* Items summary */}
                  <div className="text-sm font-bold text-gray-900">
                    {ord.items && ord.items.map((i) => `${i.ticketTypeName} (x${i.quantity})`).join(', ')}
                  </div>
                </div>

                <div className="flex items-center justify-between sm:justify-end gap-6 pt-3 sm:pt-0 border-t sm:border-0 border-gray-100">
                  <div className="text-left sm:text-right">
                    <span className="text-[10px] text-gray-400 uppercase font-bold block">Tổng tiền</span>
                    <span className="text-xl font-black text-brand-500 tnum">
                      {formatPrice(ord.totalAmount)}
                    </span>
                  </div>

                  <Link
                    to={`/orders/${ord.id}`}
                    className={`px-4 py-2.5 rounded-xl font-bold text-xs flex items-center gap-1.5 transition ${
                      isPending
                        ? 'bg-brand-500 text-white hover:bg-brand-600 shadow-md shadow-brand-500/20'
                        : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                    }`}
                  >
                    <span>{isPending ? 'Thanh toán' : 'Chi tiết'}</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </Link>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
