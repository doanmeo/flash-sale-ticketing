# Flash Sale Ticketing Hybrid — Frontend Client

Frontend ứng dụng đặt vé Flash Sale thế hệ mới hỗ trợ mô hình Hybrid: **Vé theo khu vực (GA - General Admission)** và **Vé chọn theo vị trí ghế ngồi (Seated Seatmap)**. Giao diện được thiết kế hiện đại, bám sát Design System từ [Google Stitch Project](https://stitch.withgoogle.com/projects/7637641550825937016), tối ưu trải nghiệm trên cả Desktop và Điện thoại (Responsive Mobile).

---

## 🚀 Công Nghệ Sử Dụng
- **Core:** Vite + React 19 + TypeScript
- **Styling:** Tailwind CSS (Phông chữ *Plus Jakarta Sans*, bảng màu Kinetic Urgency Flame `#FF4D00`)
- **Routing:** React Router v7
- **API & Network:** Axios (với Request Interceptor tự động gắn Bearer Token, Response Interceptor tự động làm mới `refreshToken` và bắt lỗi từ Envelope `code != 1000`)
- **QR Code:** `qrcode.react` (Tạo vé điện tử check-in cho người dùng)
- **Icons:** `lucide-react`

---

## 📁 Cấu Trúc Thư Mục `src/`

```
frontend/
├── .env.example
├── .env                        # Chứa VITE_API_BASE_URL=http://localhost:8080
├── package.json
├── tailwind.config.js          # Stitch Theme & Token config
└── src/
    ├── api/
    │   ├── client.ts           # Axios instance, Bearer token interceptor, 401 refresh queue
    │   └── index.ts            # Typed endpoints: authApi, eventApi, orderApi, organizerApi, adminApi
    ├── auth/
    │   ├── AuthContext.tsx     # Context quản lý đăng nhập, lưu token & phân quyền user.role
    │   └── ProtectedRoute.tsx  # Route Guard bảo vệ theo vai trò (USER, ORGANIZER, ADMIN)
    ├── components/
    │   ├── Navbar.tsx          # Thanh điều hướng đa nhiệm, drawer menu trên điện thoại
    │   ├── CountdownTimer.tsx  # Đếm ngược giữ vé PENDING chuẩn tabular-nums
    │   └── SeatMap.tsx         # Sơ đồ ghế ngồi sân khấu, trạng thái AVAILABLE/LOCKED/BOOKED
    ├── pages/
    │   ├── Home.tsx            # Trang chủ: Danh sách sự kiện public
    │   ├── EventDetail.tsx     # Chi tiết sự kiện: Tabs đặt vé GA & chọn ghế ngồi Seated
    │   ├── Login.tsx           # Đăng nhập (kèm nút chọn nhanh tài khoản Seed)
    │   ├── Register.tsx        # Đăng ký tài khoản (chọn vai trò USER hoặc ORGANIZER)
    │   ├── OrderDetail.tsx     # Chi tiết đơn hàng: Đếm ngược, Mock Pay, Hủy giữ chỗ
    │   ├── MyOrders.tsx        # Lịch sử đơn hàng cá nhân
    │   ├── MyTickets.tsx       # Ví vé điện tử cá nhân với mã QR check-in
    │   ├── OrganizerDashboard.tsx # Quản lý sự kiện, vé, suất diễn, thống kê, xuất CSV
    │   └── AdminDashboard.tsx  # Quản trị hệ thống: thống kê, ban/unban người dùng
    ├── types/
    │   └── index.ts            # Khớp 100% Backend DTOs & ApiResponse envelope
    ├── App.tsx
    ├── main.tsx
    └── index.css
```

---

## 🛠️ Cài Đặt & Khởi Chạy

### 1. Di chuyển vào thư mục frontend:
```bash
cd frontend
```

### 2. Cài đặt các thư viện:
```bash
npm install
```

### 3. Cấu hình biến môi trường:
Tạo file `.env` (hoặc sao chép từ `.env.example`):
```env
VITE_API_BASE_URL=http://localhost:8080
```

### 4. Chạy ứng dụng:
```bash
npm run dev
```
Ứng dụng sẽ mở tại `http://localhost:5173`.

---

## 👥 Danh Sách Tài Khoản Mẫu (Seed Accounts)

Giao diện trang Đăng nhập tích hợp sẵn các nút bấm chọn nhanh tài khoản mẫu:

| Role | Email | Mật khẩu |
| :--- | :--- | :--- |
| **User** | `user@flashsale.local` | `User@123` |
| **Organizer** | `organizer@flashsale.local` | `Organizer@123` |
| **Admin** | `admin@flashsale.local` | `Admin@123` |

---

## 🎬 Kịch Bản Demo End-to-End

### Flow 1: Đặt vé Khu vực (GA)
1. Truy cập trang chủ, nhấn vào một sự kiện có sẵn.
2. Chọn tab **"Vé Khu Vực (GA)"**.
3. Chọn hạng vé mong muốn (ví dụ: *VIP Early Bird*), chỉnh số lượng vé.
4. Bấm **"Giữ Vé GA Ngay"**.
5. Hệ thống chuyển sang trang **Checkout**:
   - Hiển thị đồng hồ đếm ngược thời gian giữ vé (`expiresAt`).
   - Bấm **"Thanh Toán Ngay (Mock Pay)"** hoặc **"Hủy Giữ Chỗ"**.
6. Sau khi bấm thanh toán thành công, bấm **"Xem vé của tôi"** để xem vé điện tử chứa mã QR kiểm soát.

### Flow 2: Đặt vé Chỗ ngồi (Seated Seatmap)
1. Tại trang chi tiết sự kiện, chọn tab **"Vé Ghế Ngồi (Seated)"**.
2. Chọn Suất diễn (Show Session). Sơ đồ ghế sẽ tự động tải trạng thái từ API:
   - Màu xanh lá: Ghế trống (`AVAILABLE`)
   - Màu vàng hổ phách: Ghế đang được chọn
   - Màu đỏ nhạt: Ghế đang bị khóa bởi người khác (`LOCKED`)
   - Màu xám: Ghế đã bán (`BOOKED`)
3. Chọn các ghế bạn muốn đặt (thanh tổng kết nổi ở chân trang sẽ tính tổng tiền theo thời gian thực).
4. Bấm **"Xác Nhận Giữ Ghế"** → chuyển sang trang Checkout và thanh toán.

### Flow 3: Organizer Hub
1. Đăng nhập bằng tài khoản `organizer@flashsale.local` / `Organizer@123`.
2. Truy cập mục **"Organizer Hub"** trên thanh menu:
   - **Tạo sự kiện mới**: Nhập tên, địa điểm, thời gian, link ảnh.
   - **Công bố sự kiện (Publish)**: Đưa sự kiện từ `DRAFT` lên `PUBLISHED` để người mua có thể thấy và đặt vé.
   - **Thêm hạng vé (Ticket Types)**: Thiết lập giá vé, tổng số lượng phát hành.
   - **Thêm suất diễn (Sessions)**: Thiết lập giờ bắt đầu và kết thúc của đêm diễn.
   - **Xuất CSV**: Bấm nút **"Xuất CSV đơn hàng"** để tải về báo cáo doanh số file `.csv`.

### Flow 4: Quản Trị Hệ Thống (Admin Portal)
1. Đăng nhập bằng tài khoản `admin@flashsale.local` / `Admin@123`.
2. Truy cập mục **"Quản trị Admin"**:
   - Theo dõi tổng số người dùng, tổng sự kiện, tổng đơn thanh toán và tổng doanh thu nền tảng.
   - Tìm kiếm người dùng theo tên hoặc email.
   - Thực hiện thao tác **Khóa tài khoản (Ban)** hoặc **Mở khóa (Unban)** với phản hồi tức thì.
