import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { ProtectedRoute } from './auth/ProtectedRoute';
import { Navbar } from './components/Navbar';
import { Home } from './pages/Home';
import { EventDetail } from './pages/EventDetail';
import { Login } from './pages/Login';
import { Register } from './pages/Register';
import { OrderDetail } from './pages/OrderDetail';
import { MyOrders } from './pages/MyOrders';
import { MyTickets } from './pages/MyTickets';
import { OrganizerDashboard } from './pages/OrganizerDashboard';
import { AdminDashboard } from './pages/AdminDashboard';

export const App: React.FC = () => {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="min-h-screen flex flex-col bg-[#faf9fd] text-[#1b1b1f]">
          <Navbar />
          <main className="flex-1">
            <Routes>
              {/* Public Routes */}
              <Route path="/" element={<Home />} />
              <Route path="/events/:id" element={<EventDetail />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />

              {/* User Protected Routes */}
              <Route
                path="/orders/:id"
                element={
                  <ProtectedRoute roles={['USER', 'ORGANIZER', 'ADMIN']}>
                    <OrderDetail />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/orders"
                element={
                  <ProtectedRoute roles={['USER', 'ORGANIZER', 'ADMIN']}>
                    <MyOrders />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/tickets"
                element={
                  <ProtectedRoute roles={['USER', 'ORGANIZER', 'ADMIN']}>
                    <MyTickets />
                  </ProtectedRoute>
                }
              />

              {/* Organizer Protected Routes */}
              <Route
                path="/organizer"
                element={
                  <ProtectedRoute roles={['ORGANIZER', 'ADMIN']}>
                    <OrganizerDashboard />
                  </ProtectedRoute>
                }
              />

              {/* Admin Protected Routes */}
              <Route
                path="/admin"
                element={
                  <ProtectedRoute roles={['ADMIN']}>
                    <AdminDashboard />
                  </ProtectedRoute>
                }
              />

              {/* Catch-all fallback */}
              <Route path="*" element={<Navigate to="/" replace />} />
            </Routes>
          </main>

          {/* Footer */}
          <footer className="bg-white border-t border-gray-200 py-8 px-4 text-center text-xs text-gray-500">
            <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4">
              <p>© 2026 Flash Sale Ticketing Hybrid. Built with Vite, React & Tailwind CSS.</p>
              <div className="flex items-center gap-4 font-semibold text-gray-600">
                <span>VITE_API_BASE_URL: http://localhost:8080</span>
                <span>•</span>
                <span>Code 1000 Envelope</span>
              </div>
            </div>
          </footer>
        </div>
      </BrowserRouter>
    </AuthProvider>
  );
};

export default App;
