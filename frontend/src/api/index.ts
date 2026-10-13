import { apiClient } from './client';
import {
  ApiResponse,
  AuthResponse,
  EventResponse,
  PageResponse,
  ShowSessionResponse,
  SessionSeatResponse,
  OrderResponse,
  PaymentResponse,
  TicketInstanceResponse,
  OrganizerEventStatsResponse,
  AdminStatsResponse,
  UserResponse,
  TicketTypeResponse
} from '../types';

export const authApi = {
  login: async (email: string, password: string): Promise<AuthResponse> => {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/api/auth/login', { email, password });
    return res.data.data;
  },
  register: async (payload: { email: string; password: string; fullName: string; role?: 'USER' | 'ORGANIZER' }): Promise<AuthResponse> => {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/api/auth/register', payload);
    return res.data.data;
  },
  refresh: async (refreshToken: string) => {
    const res = await apiClient.post<ApiResponse<{ accessToken: string; refreshToken?: string }>>('/api/auth/refresh', { refreshToken });
    return res.data.data;
  },
  logout: async (refreshToken: string) => {
    const res = await apiClient.post<ApiResponse<string>>('/api/auth/logout', { refreshToken });
    return res.data.data;
  },
  changePassword: async (oldPassword: string, newPassword: string) => {
    const res = await apiClient.post<ApiResponse<string>>('/api/auth/change-password', { oldPassword, newPassword });
    return res.data.data;
  }
};

export const eventApi = {
  getEvents: async (): Promise<EventResponse[]> => {
    const res = await apiClient.get<ApiResponse<PageResponse<EventResponse> | EventResponse[]>>('/api/events');
    const data = res.data.data;
    if (Array.isArray(data)) {
      return data;
    }
    if (data && 'content' in data && Array.isArray(data.content)) {
      return data.content;
    }
    return [];
  },
  getEventById: async (id: number): Promise<EventResponse> => {
    const res = await apiClient.get<ApiResponse<EventResponse>>(`/api/events/${id}`);
    return res.data.data;
  },
  getEventTickets: async (id: number): Promise<TicketTypeResponse[]> => {
    const res = await apiClient.get<ApiResponse<TicketTypeResponse[]>>(`/api/events/${id}/tickets`);
    return res.data.data;
  },
  getEventSessions: async (id: number): Promise<ShowSessionResponse[]> => {
    const res = await apiClient.get<ApiResponse<ShowSessionResponse[]>>(`/api/events/${id}/sessions`);
    return res.data.data;
  },
  getSessionSeats: async (sessionId: number): Promise<SessionSeatResponse[]> => {
    const res = await apiClient.get<ApiResponse<SessionSeatResponse[]>>(`/api/sessions/${sessionId}/seats`);
    return res.data.data;
  }
};

export const orderApi = {
  createGaOrder: async (ticketTypeId: number, quantity: number): Promise<OrderResponse> => {
    const res = await apiClient.post<ApiResponse<OrderResponse>>('/api/orders/ga', { ticketTypeId, quantity });
    return res.data.data;
  },
  createSeatedOrder: async (sessionId: number, seatIds: number[]): Promise<OrderResponse> => {
    const res = await apiClient.post<ApiResponse<OrderResponse>>('/api/orders/seated', { sessionId, seatIds });
    return res.data.data;
  },
  getOrderById: async (orderId: number): Promise<OrderResponse> => {
    const res = await apiClient.get<ApiResponse<OrderResponse>>(`/api/orders/${orderId}`);
    return res.data.data;
  },
  getMyOrders: async (params?: { status?: string; page?: number; size?: number }): Promise<PageResponse<OrderResponse>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<OrderResponse>>>('/api/orders/me', { params });
    return res.data.data;
  },
  cancelOrder: async (orderId: number): Promise<OrderResponse> => {
    const res = await apiClient.post<ApiResponse<OrderResponse>>(`/api/orders/${orderId}/cancel`);
    return res.data.data;
  },
  payOrder: async (orderId: number): Promise<PaymentResponse> => {
    const res = await apiClient.post<ApiResponse<PaymentResponse>>(`/api/orders/${orderId}/pay`);
    return res.data.data;
  },
  getMyTickets: async (): Promise<TicketInstanceResponse[]> => {
    const res = await apiClient.get<ApiResponse<TicketInstanceResponse[]>>('/api/tickets/me');
    return res.data.data;
  }
};

export const organizerApi = {
  getMyEvents: async (page = 0, size = 10): Promise<PageResponse<EventResponse>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<EventResponse>>>('/api/organizer/events', {
      params: { page, size }
    });
    return res.data.data;
  },
  createEvent: async (payload: { name: string; description?: string; location?: string; eventDate: string; imageUrl?: string }): Promise<EventResponse> => {
    const res = await apiClient.post<ApiResponse<EventResponse>>('/api/organizer/events', payload);
    return res.data.data;
  },
  updateEvent: async (id: number, payload: { name: string; description?: string; location?: string; eventDate: string; imageUrl?: string }): Promise<EventResponse> => {
    const res = await apiClient.put<ApiResponse<EventResponse>>(`/api/organizer/events/${id}`, payload);
    return res.data.data;
  },
  publishEvent: async (id: number): Promise<EventResponse> => {
    const res = await apiClient.post<ApiResponse<EventResponse>>(`/api/organizer/events/${id}/publish`);
    return res.data.data;
  },
  deleteEvent: async (id: number): Promise<string> => {
    const res = await apiClient.delete<ApiResponse<string>>(`/api/organizer/events/${id}`);
    return res.data.data;
  },
  createTicketType: async (eventId: number, payload: { name: string; description?: string; price: number; totalQuantity: number; saleStart?: string; saleEnd?: string; minPerOrder?: number; maxPerOrder?: number }): Promise<TicketTypeResponse> => {
    const res = await apiClient.post<ApiResponse<TicketTypeResponse>>(`/api/organizer/events/${eventId}/tickets`, payload);
    return res.data.data;
  },
  updateTicketType: async (ticketTypeId: number, payload: Partial<TicketTypeResponse>): Promise<TicketTypeResponse> => {
    const res = await apiClient.put<ApiResponse<TicketTypeResponse>>(`/api/organizer/events/tickets/${ticketTypeId}`, payload);
    return res.data.data;
  },
  deleteTicketType: async (ticketTypeId: number): Promise<string> => {
    const res = await apiClient.delete<ApiResponse<string>>(`/api/organizer/events/tickets/${ticketTypeId}`);
    return res.data.data;
  },
  createSession: async (eventId: number, payload: { sessionName: string; startTime: string; endTime: string }): Promise<ShowSessionResponse> => {
    const res = await apiClient.post<ApiResponse<ShowSessionResponse>>(`/api/organizer/events/${eventId}/sessions`, payload);
    return res.data.data;
  },
  getSessions: async (eventId: number): Promise<ShowSessionResponse[]> => {
    const res = await apiClient.get<ApiResponse<ShowSessionResponse[]>>(`/api/organizer/events/${eventId}/sessions`);
    return res.data.data;
  },
  getEventOrders: async (eventId: number, status?: string, page = 0, size = 10): Promise<PageResponse<OrderResponse>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<OrderResponse>>>(`/api/organizer/events/${eventId}/orders`, {
      params: { status, page, size }
    });
    return res.data.data;
  },
  getEventStats: async (eventId: number): Promise<OrganizerEventStatsResponse> => {
    const res = await apiClient.get<ApiResponse<OrganizerEventStatsResponse>>(`/api/organizer/events/${eventId}/stats`);
    return res.data.data;
  },
  exportOrdersCsv: async (eventId: number): Promise<Blob> => {
    const res = await apiClient.get(`/api/organizer/events/${eventId}/orders/export`, {
      responseType: 'blob'
    });
    return res.data;
  }
};

export const adminApi = {
  getUsers: async (keyword?: string, page = 0, size = 10): Promise<PageResponse<UserResponse>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<UserResponse>>>('/api/admin/users', {
      params: { keyword, page, size }
    });
    return res.data.data;
  },
  banUser: async (id: number): Promise<UserResponse> => {
    const res = await apiClient.patch<ApiResponse<UserResponse>>(`/api/admin/users/${id}/ban`);
    return res.data.data;
  },
  unbanUser: async (id: number): Promise<UserResponse> => {
    const res = await apiClient.patch<ApiResponse<UserResponse>>(`/api/admin/users/${id}/unban`);
    return res.data.data;
  },
  getAdminStats: async (): Promise<AdminStatsResponse> => {
    const res = await apiClient.get<ApiResponse<AdminStatsResponse>>('/api/admin/stats');
    return res.data.data;
  },
  getAdminEvents: async (keyword?: string, page = 0, size = 10): Promise<PageResponse<EventResponse>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<EventResponse>>>('/api/admin/events', {
      params: { keyword, page, size }
    });
    return res.data.data;
  },
  deleteAdminEvent: async (id: number): Promise<string> => {
    const res = await apiClient.delete<ApiResponse<string>>(`/api/admin/events/${id}`);
    return res.data.data;
  }
};
