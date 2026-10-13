import React, { useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import {
  Ticket,
  Calendar,
  Layers,
  ShieldAlert,
  LogOut,
  LogIn,
  User,
  Menu,
  X,
  Flame
} from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const navItemClass = ({ isActive }: { isActive: boolean }) =>
    `px-3 py-2 rounded-lg text-sm font-semibold transition-colors duration-150 flex items-center gap-1.5 ${
      isActive
        ? 'text-brand-500 bg-brand-50'
        : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
    }`;

  return (
    <header className="sticky top-0 z-50 bg-white/95 backdrop-blur border-b border-gray-200">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center gap-2.5 group">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-brand-700 via-brand-500 to-orange-400 flex items-center justify-center text-white shadow-md shadow-brand-500/20 group-hover:scale-105 transition-transform">
              <Flame className="w-6 h-6 animate-pulse" />
            </div>
            <div>
              <span className="font-extrabold text-xl tracking-tight text-gray-900 flex items-center gap-1">
                FlashSale <span className="text-brand-500 font-black">Tickets</span>
              </span>
              <p className="text-[10px] text-gray-400 font-semibold tracking-wider uppercase -mt-1">Hybrid Seated & GA</p>
            </div>
          </Link>

          {/* Desktop Nav */}
          <nav className="hidden md:flex items-center gap-1 lg:gap-2">
            <NavLink to="/" end className={navItemClass}>
              <Calendar className="w-4 h-4" />
              <span>Sự kiện</span>
            </NavLink>

            {isAuthenticated && (
              <>
                <NavLink to="/orders" className={navItemClass}>
                  <Layers className="w-4 h-4" />
                  <span>Đơn hàng của tôi</span>
                </NavLink>

                <NavLink to="/tickets" className={navItemClass}>
                  <Ticket className="w-4 h-4" />
                  <span>Vé của tôi</span>
                </NavLink>
              </>
            )}

            {(user?.role === 'ORGANIZER' || user?.role === 'ADMIN') && (
              <NavLink to="/organizer" className={navItemClass}>
                <Layers className="w-4 h-4 text-purple-600" />
                <span className="text-purple-700 font-bold">Organizer Hub</span>
              </NavLink>
            )}

            {user?.role === 'ADMIN' && (
              <NavLink to="/admin" className={navItemClass}>
                <ShieldAlert className="w-4 h-4 text-rose-600" />
                <span className="text-rose-700 font-bold">Quản trị Admin</span>
              </NavLink>
            )}
          </nav>

          {/* User profile / Auth buttons */}
          <div className="hidden md:flex items-center gap-3">
            {isAuthenticated && user ? (
              <div className="flex items-center gap-3 pl-3 border-l border-gray-200">
                <div className="text-right">
                  <div className="text-xs font-bold text-gray-900 leading-tight">{user.fullName || user.email}</div>
                  <div className="text-[11px] font-semibold text-gray-500">
                    <span className={`inline-block px-1.5 py-0.2 rounded text-[10px] font-bold ${
                      user.role === 'ADMIN'
                        ? 'bg-red-100 text-red-700'
                        : user.role === 'ORGANIZER'
                        ? 'bg-purple-100 text-purple-700'
                        : 'bg-green-100 text-green-700'
                    }`}>
                      {user.role}
                    </span>
                  </div>
                </div>
                <button
                  onClick={handleLogout}
                  className="p-2 text-gray-500 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                  title="Đăng xuất"
                >
                  <LogOut className="w-5 h-5" />
                </button>
              </div>
            ) : (
              <div className="flex items-center gap-2">
                <Link
                  to="/login"
                  className="px-4 py-2 text-sm font-semibold text-gray-700 hover:text-brand-500 hover:bg-gray-100 rounded-lg transition"
                >
                  Đăng nhập
                </Link>
                <Link
                  to="/register"
                  className="px-4 py-2 text-sm font-bold text-white bg-brand-500 hover:bg-brand-600 rounded-lg shadow-sm hover:shadow transition"
                >
                  Đăng ký
                </Link>
              </div>
            )}
          </div>

          {/* Mobile hamburger button */}
          <div className="md:hidden flex items-center">
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="p-2 text-gray-600 hover:text-gray-900 focus:outline-none"
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Drawer */}
      {mobileMenuOpen && (
        <div className="md:hidden bg-white border-b border-gray-200 px-4 pt-2 pb-4 space-y-2">
          <NavLink
            to="/"
            end
            onClick={() => setMobileMenuOpen(false)}
            className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-base font-semibold text-gray-700 hover:bg-gray-100"
          >
            <Calendar className="w-5 h-5" /> Sự kiện
          </NavLink>

          {isAuthenticated && (
            <>
              <NavLink
                to="/orders"
                onClick={() => setMobileMenuOpen(false)}
                className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-base font-semibold text-gray-700 hover:bg-gray-100"
              >
                <Layers className="w-5 h-5" /> Đơn hàng của tôi
              </NavLink>
              <NavLink
                to="/tickets"
                onClick={() => setMobileMenuOpen(false)}
                className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-base font-semibold text-gray-700 hover:bg-gray-100"
              >
                <Ticket className="w-5 h-5" /> Vé của tôi
              </NavLink>
            </>
          )}

          {(user?.role === 'ORGANIZER' || user?.role === 'ADMIN') && (
            <NavLink
              to="/organizer"
              onClick={() => setMobileMenuOpen(false)}
              className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-base font-semibold text-purple-700 bg-purple-50"
            >
              <Layers className="w-5 h-5" /> Organizer Dashboard
            </NavLink>
          )}

          {user?.role === 'ADMIN' && (
            <NavLink
              to="/admin"
              onClick={() => setMobileMenuOpen(false)}
              className="flex items-center gap-3 px-3 py-2.5 rounded-lg text-base font-semibold text-rose-700 bg-rose-50"
            >
              <ShieldAlert className="w-5 h-5" /> Admin Portal
            </NavLink>
          )}

          <div className="pt-3 border-t border-gray-200">
            {isAuthenticated && user ? (
              <div className="space-y-2">
                <div className="px-3 py-1 flex items-center justify-between text-sm text-gray-600">
                  <span>{user.fullName || user.email}</span>
                  <span className="font-bold text-xs bg-gray-100 px-2 py-0.5 rounded">{user.role}</span>
                </div>
                <button
                  onClick={() => {
                    handleLogout();
                    setMobileMenuOpen(false);
                  }}
                  className="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg text-sm font-bold text-red-600 bg-red-50 hover:bg-red-100"
                >
                  <LogOut className="w-4 h-4" /> Đăng xuất
                </button>
              </div>
            ) : (
              <div className="grid grid-cols-2 gap-2">
                <Link
                  to="/login"
                  onClick={() => setMobileMenuOpen(false)}
                  className="flex items-center justify-center py-2.5 text-center font-bold text-sm text-gray-700 border border-gray-300 rounded-lg"
                >
                  Đăng nhập
                </Link>
                <Link
                  to="/register"
                  onClick={() => setMobileMenuOpen(false)}
                  className="flex items-center justify-center py-2.5 text-center font-bold text-sm text-white bg-brand-500 rounded-lg"
                >
                  Đăng ký
                </Link>
              </div>
            )}
          </div>
        </div>
      )}
    </header>
  );
};
