import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { orderApi } from '../api';
import { OrderResponse } from '../types';
import { CountdownTimer } from '../components/CountdownTimer';
import {
  CreditCard,
  XCircle,
  CheckCircle2,
  AlertTriangle,
  Clock,
  ArrowLeft,
  Ticket,
  Receipt,
  ShieldCheck,
  Check
} from 'lucide-react';

export const OrderDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const orderId = Number(id);
  const navigate = useNavigate();

  const [order, setOrder] = useState<OrderResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [paying, setPaying] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!orderId) return;
    loadOrder();
  }, [orderId]);

  const loadOrder = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await orderApi.getOrderById(orderId);
      setOrder(data);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Không thể tải thông tin đơn hàng');
      }
    } finally {
      setLoading(false);
    }
  };

  const handlePay = async () => {
    try {
      setPaying(true);
      setError(null);
      await orderApi.payOrder(orderId);
      setSuccessMessage('Thanh toán thành công! Vé điện tử đã được phát hành.');
      // Reload order to update status to PAID
      await loadOrder();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Thanh toán thất bại');
      }
    } finally {
      setPaying(false);
    }
  };

  const handleCancel = async () => {
    if (!window.confirm('Bạn có chắc chắn muốn hủy đơn giữ vé này?')) return;
    try {
      setCancelling(true);
      setError(null);
      await orderApi.cancelOrder(orderId);
      await loadOrder();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Hủy đơn hàng thất bại');
      }
    } finally {
      setCancelling(false);
    }
  };

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  if (loading) {
    return (
      <div className="max-w-2xl mx-auto px-4 py-20 text-center animate-pulse">
        <div className="h-8 bg-gray-200 rounded w-1/3 mx-auto mb-4" />
        <div className="h-64 bg-gray-100 rounded-3xl" />
      </div>
    );
  }

  if (!order) {
    return (
      <div className="max-w-md mx-auto text-center py-20">
        <AlertTriangle className="w-16 h-16 text-gray-300 mx-auto mb-4" />
        <h2 className="text-xl font-bold text-gray-800">Không tìm thấy đơn hàng #{orderId}</h2>
        <Link to="/orders" className="mt-4 inline-block text-brand-500 font-bold text-sm">
          Xem danh sách đơn hàng
        </Link>
      </div>
    );
  }

  const isPending = order.status === 'PENDING';
  const isPaid = order.status === 'PAID';
  const isCancelled = order.status === 'CANCELLED';
  const isExpired = order.status === 'EXPIRED';

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 py-10">
      <Link
        to="/orders"
        className="inline-flex items-center gap-2 text-xs font-bold text-gray-500 hover:text-gray-900 mb-6 transition"
      >
        <ArrowLeft className="w-4 h-4" />
        <span>Quay lại Đơn hàng của tôi</span>
      </Link>

      {/* Main Order Card */}
      <div className="bg-white rounded-3xl border border-gray-200 shadow-xl overflow-hidden">
        {/* Status Header */}
        <div
          className={`p-6 sm:p-8 text-white ${
            isPending
              ? 'bg-gradient-to-r from-gray-900 to-brand-dark'
              : isPaid
              ? 'bg-gradient-to-r from-emerald-800 to-teal-900'
              : 'bg-gradient-to-r from-gray-700 to-gray-800'
          }`}
        >
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
            <div>
              <div className="flex items-center gap-2 mb-1">
                <Receipt className="w-5 h-5 text-gray-300" />
                <span className="text-xs font-bold uppercase tracking-widest text-gray-300">
                  Mã đơn hàng: {order.orderCode}
                </span>
              </div>
              <h1 className="text-2xl sm:text-3xl font-black">
                {isPending && 'Đang Chờ Thanh Toán'}
                {isPaid && 'Đã Thanh Toán Thành Công'}
                {isCancelled && 'Đơn Hàng Đã Bị Hủy'}
                {isExpired && 'Đơn Hàng Đã Quá Hạn'}
              </h1>
            </div>

            {/* Status Badge */}
            <div>
              {isPending && (
                <span className="px-3 py-1 rounded-full text-xs font-extrabold uppercase bg-amber-500 text-white shadow-sm">
                  PENDING
                </span>
              )}
              {isPaid && (
                <span className="px-3 py-1 rounded-full text-xs font-extrabold uppercase bg-emerald-500 text-white shadow-sm">
                  PAID
                </span>
              )}
              {(isCancelled || isExpired) && (
                <span className="px-3 py-1 rounded-full text-xs font-extrabold uppercase bg-rose-500 text-white shadow-sm">
                  {order.status}
                </span>
              )}
            </div>
          </div>

          {/* Countdown banner if PENDING */}
          {isPending && order.expiresAt && (
            <div className="mt-6 pt-6 border-t border-white/10 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
              <CountdownTimer
                expiresAt={order.expiresAt}
                onExpire={loadOrder}
              />
              <p className="text-xs text-gray-300">
                Vé sẽ tự động được hoàn về kho nếu không hoàn tất thanh toán trước khi hết giờ.
              </p>
            </div>
          )}
        </div>

        {/* Alerts */}
        {error && (
          <div className="p-4 mx-6 mt-6 rounded-2xl bg-red-50 border border-red-200 flex items-start gap-3 text-red-700 text-sm">
            <AlertTriangle className="w-5 h-5 flex-shrink-0 mt-0.5" />
            <div>
              <p className="font-bold">Lỗi xử lý</p>
              <p className="text-xs">{error}</p>
            </div>
          </div>
        )}

        {successMessage && (
          <div className="p-4 mx-6 mt-6 rounded-2xl bg-emerald-50 border border-emerald-200 flex items-start gap-3 text-emerald-800 text-sm">
            <CheckCircle2 className="w-5 h-5 flex-shrink-0 mt-0.5" />
            <div>
              <p className="font-bold">Thành công!</p>
              <p className="text-xs">{successMessage}</p>
            </div>
          </div>
        )}

        {/* Order Details Body */}
        <div className="p-6 sm:p-8 space-y-6">
          <div>
            <h3 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-4">
              Danh mục vé đặt
            </h3>
            <div className="divide-y divide-gray-100">
              {order.items && order.items.map((item, idx) => (
                <div key={idx} className="py-3 flex items-center justify-between">
                  <div>
                    <h4 className="font-bold text-gray-900 text-sm">{item.ticketTypeName}</h4>
                    <span className="text-xs text-gray-500">
                      Số lượng: {item.quantity} x {formatPrice(item.unitPrice)}
                    </span>
                  </div>
                  <span className="font-black text-gray-900 tnum">
                    {formatPrice(item.subtotal)}
                  </span>
                </div>
              ))}
            </div>
          </div>

          <div className="pt-4 border-t border-gray-100 space-y-2 text-sm">
            <div className="flex justify-between text-gray-500 text-xs">
              <span>Thời gian tạo đơn:</span>
              <span>{new Date(order.createdAt).toLocaleString('vi-VN')}</span>
            </div>
            {order.payment && (
              <div className="flex justify-between text-gray-500 text-xs">
                <span>Thời gian thanh toán:</span>
                <span>{new Date(order.payment.paidAt).toLocaleString('vi-VN')}</span>
              </div>
            )}
            <div className="flex justify-between items-center pt-2 text-lg font-black text-gray-900">
              <span>Tổng thanh toán:</span>
              <span className="text-2xl text-brand-500 tnum">{formatPrice(order.totalAmount)}</span>
            </div>
          </div>

          {/* Action Buttons */}
          {isPending && (
            <div className="pt-6 border-t border-gray-100 flex flex-col sm:flex-row items-center gap-3">
              <button
                type="button"
                disabled={paying || cancelling}
                onClick={handlePay}
                className="w-full sm:flex-1 py-4 px-6 bg-brand-500 hover:bg-brand-600 active:bg-brand-700 disabled:opacity-50 text-white font-black rounded-xl shadow-lg shadow-brand-500/25 flex items-center justify-center gap-2 transition"
              >
                <CreditCard className="w-5 h-5" />
                <span>{paying ? 'Đang xác nhận thanh toán...' : 'Thanh Toán Ngay (Mock Pay)'}</span>
              </button>

              <button
                type="button"
                disabled={paying || cancelling}
                onClick={handleCancel}
                className="w-full sm:w-auto py-4 px-6 bg-gray-100 hover:bg-rose-50 hover:text-rose-600 text-gray-700 font-bold rounded-xl flex items-center justify-center gap-2 transition"
              >
                <XCircle className="w-5 h-5" />
                <span>{cancelling ? 'Đang hủy...' : 'Hủy Giữ Chỗ'}</span>
              </button>
            </div>
          )}

          {isPaid && (
            <div className="pt-6 border-t border-gray-100 flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="flex items-center gap-2 text-emerald-700 font-bold text-sm">
                <ShieldCheck className="w-5 h-5" />
                <span>Vé đã được lưu vào ví vé điện tử của bạn</span>
              </div>
              <Link
                to="/tickets"
                className="py-3 px-6 bg-brand-dark hover:bg-brand-500 text-white font-bold rounded-xl text-sm flex items-center gap-2 transition shadow-md"
              >
                <Ticket className="w-4 h-4" />
                <span>Xem vé của tôi (My Tickets)</span>
              </Link>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
