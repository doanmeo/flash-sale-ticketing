// API Response Envelope
export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

// User & Auth
export type Role = 'USER' | 'ORGANIZER' | 'ADMIN';

export interface UserSummary {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  status?: string;
  createdAt?: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserSummary;
}

export interface UserResponse {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  status: 'ACTIVE' | 'BANNED';
  createdAt: string;
}

// Events
export type EventStatus = 'DRAFT' | 'PUBLISHED' | 'CANCELLED' | 'COMPLETED';

export interface TicketTypeResponse {
  id: number;
  eventId: number;
  name: string;
  description?: string;
  price: number;
  totalQuantity: number;
  availableQuantity: number;
  realtimeAvailable?: number;
  saleStart?: string;
  saleEnd?: string;
  minPerOrder?: number;
  maxPerOrder?: number;
  isHidden?: boolean;
  isOnSale?: boolean;
}

export interface EventResponse {
  id: number;
  name: string;
  description: string;
  location: string;
  eventDate: string;
  imageUrl?: string;
  status: EventStatus;
  organizerId: number;
  organizerName: string;
  createdAt: string;
  ticketTypes?: TicketTypeResponse[];
}

// Sessions & Seats
export type SessionStatus = 'SCHEDULED' | 'ONGOING' | 'COMPLETED' | 'CANCELLED';
export type SeatStatus = 'AVAILABLE' | 'LOCKED' | 'BOOKED';

export interface ShowSessionResponse {
  id: number;
  eventId: number;
  sessionName: string;
  startTime: string;
  endTime: string;
  status: SessionStatus;
  ticketTypes?: TicketTypeResponse[];
}

export interface SessionSeatResponse {
  sessionSeatId: number;
  seatId: number;
  rowLabel: string;
  seatNumber: string;
  zoneId?: number;
  zoneName?: string;
  isSeated?: boolean;
  status: SeatStatus;
  price: number;
}

// Orders
export type OrderStatus = 'PENDING' | 'PAID' | 'CANCELLED' | 'EXPIRED';

export interface OrderItemResponse {
  ticketTypeId: number;
  ticketTypeName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface PaymentResponse {
  paymentId: number;
  orderId: number;
  amount: number;
  paymentMethod: string;
  status: string;
  paidAt: string;
}

export interface OrderResponse {
  id: number;
  orderCode: string;
  status: OrderStatus;
  totalAmount: number;
  expiresAt: string;
  createdAt: string;
  items: OrderItemResponse[];
  payment?: PaymentResponse;
}

// Ticket Instances (After Payment)
export type TicketInstanceStatus = 'VALID' | 'USED' | 'CANCELLED' | 'EXPIRED';
export type LineItemType = 'GA' | 'SEATED';

export interface TicketInstanceResponse {
  id: number;
  ticketCode: string;
  qrPayload: string;
  status: TicketInstanceStatus;
  lineType: LineItemType;
  eventName: string;
  sessionName?: string;
  zoneName?: string;
  seatInfo?: string;
  price: number;
  issuedAt: string;
}

// Stats
export interface AdminStatsResponse {
  totalUsers: number;
  totalEvents: number;
  totalPaidOrders: number;
  totalRevenue: number;
}

export interface TicketTypeStat {
  ticketTypeId: number;
  ticketTypeName: string;
  price: number;
  totalQuantity: number;
  soldQuantity: number;
  remainingQuantity: number;
}

export interface OrganizerEventStatsResponse {
  eventId: number;
  eventName: string;
  totalPaidOrders: number;
  totalRevenue: number;
  ticketTypeStats: TicketTypeStat[];
}
