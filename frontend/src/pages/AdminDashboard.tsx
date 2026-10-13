import React, { useEffect, useState } from 'react';
import { adminApi } from '../api';
import { AdminStatsResponse, UserResponse } from '../types';
import {
  ShieldAlert,
  Users,
  DollarSign,
  Calendar,
  Layers,
  Ban,
  CheckCircle,
  AlertTriangle,
  Search,
  RefreshCw
} from 'lucide-react';

export const AdminDashboard: React.FC = () => {
  const [stats, setStats] = useState<AdminStatsResponse | null>(null);
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [keyword, setKeyword] = useState('');
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      const [statsData, usersData] = await Promise.all([
        adminApi.getAdminStats().catch(() => null),
        adminApi.getUsers(keyword || undefined, 0, 50).catch(() => ({ content: [] })),
      ]);
      setStats(statsData);
      setUsers(usersData.content || []);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Không thể tải dữ liệu admin');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleBan = async (userId: number) => {
    if (!window.confirm(`Xác nhận khóa (BAN) tài khoản người dùng ID #${userId}?`)) return;
    try {
      setActionLoading(userId);
      setError(null);
      await adminApi.banUser(userId);
      setSuccess(`Đã khóa người dùng #${userId}`);
      await loadData();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Khóa tài khoản thất bại');
      }
    } finally {
      setActionLoading(null);
    }
  };

  const handleUnban = async (userId: number) => {
    try {
      setActionLoading(userId);
      setError(null);
      await adminApi.unbanUser(userId);
      setSuccess(`Đã mở khóa tài khoản #${userId}`);
      await loadData();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Mở khóa tài khoản thất bại');
      }
    } finally {
      setActionLoading(null);
    }
  };

  const formatPrice = (price?: number) => {
    if (price === undefined || price === null) return '0 ₫';
    return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(price);
  };

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-rose-50 text-rose-700 text-xs font-black uppercase tracking-wider mb-2">
            <ShieldAlert className="w-4 h-4" />
            Hệ Thống Quản Trị Cấp Cao
          </div>
          <h1 className="text-2xl sm:text-3xl font-black text-gray-900 tracking-tight">
            Admin Dashboard & User Moderation
          </h1>
          <p className="text-sm text-gray-500 mt-1">
            Theo dõi toàn bộ chỉ số nền tảng, doanh thu và kiểm soát tài khoản người dùng.
          </p>
        </div>

        <button
          onClick={loadData}
          className="p-2.5 border border-gray-200 bg-white rounded-xl text-gray-600 hover:text-brand-500 transition self-start sm:self-auto flex items-center gap-2 text-xs font-bold"
        >
          <RefreshCw className="w-4 h-4" />
          <span>Làm mới số liệu</span>
        </button>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-2xl bg-red-50 border border-red-200 flex items-start gap-3 text-red-700 text-sm">
          <AlertTriangle className="w-5 h-5 flex-shrink-0 mt-0.5" />
          <span>{error}</span>
        </div>
      )}

      {success && (
        <div className="mb-6 p-4 rounded-2xl bg-emerald-50 border border-emerald-200 flex items-start gap-3 text-emerald-800 text-sm">
          <CheckCircle className="w-5 h-5 flex-shrink-0 mt-0.5" />
          <span>{success}</span>
        </div>
      )}

      {/* High-level stats cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-10">
        <div className="bg-white rounded-3xl border border-gray-200 p-6 shadow-sm">
          <div className="w-12 h-12 bg-blue-50 text-blue-600 rounded-2xl flex items-center justify-center mb-4">
            <Users className="w-6 h-6" />
          </div>
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">
            Tổng người dùng
          </span>
          <span className="text-3xl font-black text-gray-900 tnum mt-1 block">
            {stats ? stats.totalUsers : 0}
          </span>
        </div>

        <div className="bg-white rounded-3xl border border-gray-200 p-6 shadow-sm">
          <div className="w-12 h-12 bg-purple-50 text-purple-600 rounded-2xl flex items-center justify-center mb-4">
            <Calendar className="w-6 h-6" />
          </div>
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">
            Tổng sự kiện
          </span>
          <span className="text-3xl font-black text-gray-900 tnum mt-1 block">
            {stats ? stats.totalEvents : 0}
          </span>
        </div>

        <div className="bg-white rounded-3xl border border-gray-200 p-6 shadow-sm">
          <div className="w-12 h-12 bg-amber-50 text-amber-600 rounded-2xl flex items-center justify-center mb-4">
            <Layers className="w-6 h-6" />
          </div>
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">
            Đơn đã thanh toán
          </span>
          <span className="text-3xl font-black text-gray-900 tnum mt-1 block">
            {stats ? stats.totalPaidOrders : 0}
          </span>
        </div>

        <div className="bg-white rounded-3xl border border-gray-200 p-6 shadow-sm">
          <div className="w-12 h-12 bg-emerald-50 text-emerald-600 rounded-2xl flex items-center justify-center mb-4">
            <DollarSign className="w-6 h-6" />
          </div>
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider block">
            Tổng doanh thu hệ thống
          </span>
          <span className="text-2xl font-black text-emerald-600 tnum mt-1 block">
            {stats ? formatPrice(Number(stats.totalRevenue)) : '0 ₫'}
          </span>
        </div>
      </div>

      {/* Users moderation table */}
      <div className="bg-white rounded-3xl border border-gray-200 shadow-sm overflow-hidden">
        <div className="p-6 border-b border-gray-100 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h2 className="text-xl font-black text-gray-900">Danh Sách Tài Khoản Người Dùng</h2>
            <p className="text-xs text-gray-500 mt-0.5">Kiểm soát và phân quyền người dùng trong hệ thống</p>
          </div>

          {/* Search bar */}
          <form
            onSubmit={(e) => {
              e.preventDefault();
              loadData();
            }}
            className="flex items-center gap-2"
          >
            <div className="relative">
              <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
              <input
                type="text"
                placeholder="Tìm email, họ tên..."
                value={keyword}
                onChange={(e) => setKeyword(e.target.value)}
                className="pl-9 pr-4 py-2 bg-gray-50 border border-gray-200 rounded-xl text-xs font-medium focus:bg-white focus:outline-none focus:border-brand-500"
              />
            </div>
            <button
              type="submit"
              className="px-4 py-2 bg-brand-dark text-white rounded-xl text-xs font-bold hover:bg-black transition"
            >
              Tìm kiếm
            </button>
          </form>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse text-xs">
            <thead>
              <tr className="bg-gray-50/75 border-b border-gray-100 text-gray-400 uppercase tracking-wider font-extrabold">
                <th className="py-3.5 px-6">ID</th>
                <th className="py-3.5 px-6">Họ tên & Email</th>
                <th className="py-3.5 px-6">Vai trò</th>
                <th className="py-3.5 px-6">Trạng thái</th>
                <th className="py-3.5 px-6 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {loading ? (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-gray-400">
                    Đang tải danh sách người dùng...
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={5} className="py-8 text-center text-gray-400">
                    Không tìm thấy người dùng nào phù hợp.
                  </td>
                </tr>
              ) : (
                users.map((u) => {
                  const isBanned = u.status === 'BANNED';
                  const isActing = actionLoading === u.id;

                  return (
                    <tr key={u.id} className="hover:bg-gray-50/50 transition">
                      <td className="py-4 px-6 font-mono font-bold text-gray-400">#{u.id}</td>
                      <td className="py-4 px-6">
                        <div className="font-bold text-gray-900 text-sm">{u.fullName || 'Chưa đặt tên'}</div>
                        <div className="text-gray-500 font-mono text-[11px]">{u.email}</div>
                      </td>
                      <td className="py-4 px-6">
                        <span
                          className={`px-2 py-0.5 rounded text-[10px] font-black uppercase ${
                            u.role === 'ADMIN'
                              ? 'bg-rose-100 text-rose-700'
                              : u.role === 'ORGANIZER'
                              ? 'bg-purple-100 text-purple-700'
                              : 'bg-blue-100 text-blue-700'
                          }`}
                        >
                          {u.role}
                        </span>
                      </td>
                      <td className="py-4 px-6">
                        <span
                          className={`px-2.5 py-1 rounded-full text-[11px] font-bold ${
                            isBanned
                              ? 'bg-rose-100 text-rose-700'
                              : 'bg-emerald-100 text-emerald-800'
                          }`}
                        >
                          {u.status}
                        </span>
                      </td>
                      <td className="py-4 px-6 text-right">
                        {u.role !== 'ADMIN' && (
                          <>
                            {isBanned ? (
                              <button
                                type="button"
                                disabled={isActing}
                                onClick={() => handleUnban(u.id)}
                                className="px-3 py-1.5 rounded-lg bg-emerald-50 hover:bg-emerald-100 text-emerald-700 font-bold transition disabled:opacity-50"
                              >
                                {isActing ? '...' : 'Mở khóa (Unban)'}
                              </button>
                            ) : (
                              <button
                                type="button"
                                disabled={isActing}
                                onClick={() => handleBan(u.id)}
                                className="px-3 py-1.5 rounded-lg bg-rose-50 hover:bg-rose-100 text-rose-700 font-bold transition disabled:opacity-50"
                              >
                                {isActing ? '...' : 'Khóa tài khoản (Ban)'}
                              </button>
                            )}
                          </>
                        )}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
