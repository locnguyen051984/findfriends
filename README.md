# FindFriends - Location-Based Real-Time Messaging & Calling Platform

## 📖 Introduction

**FindFriends** là ứng dụng web kết nối bạn bè dựa trên vị trí địa lý, hỗ trợ nhắn tin và gọi thoại/video theo thời gian thực.

Project được xây dựng nhằm áp dụng các kiến thức về **Java Backend, Spring Boot, Database, WebSocket, WebRTC, Authentication và Payment Integration** vào một hệ thống thực tế.

### Mục tiêu chính

* Tìm kiếm người dùng xung quanh dựa trên vị trí.
* Hỗ trợ nhắn tin thời gian thực.
* Hỗ trợ gọi thoại/video trực tiếp giữa các trình duyệt.
* Quản lý thiết bị/trình duyệt tin cậy.
* Phát hiện đăng nhập có vị trí bất thường.
* Hỗ trợ nâng cấp tài khoản Premium thông qua PayOS.
* Cung cấp trang quản trị để theo dõi và quản lý hệ thống.

---

## 🏗️ Architecture

Project sử dụng **Layered Architecture** trong Spring Boot, kết hợp HTTP, WebSocket và WebRTC cho các loại giao tiếp khác nhau.

                         ┌──────────────────────┐
                         │      Web Browser     │
                         └──────────┬───────────┘
                                    │
                  ┌─────────────────┼─────────────────┐
                  │                 │                 │
                HTTP            WebSocket          WebRTC
                  │                 │                 │
                  ▼                 ▼                 │
          ┌─────────────────────────────────┐         │
          │         Spring Boot Backend     │         │
          │                                 │         │
          │  Controller                     │         │
          │      ↓                          │         │
          │  Service                        │         │
          │      ↓                          │         │
          │  Repository                     │         │
          └───────────────┬─────────────────┘         │
                          │                           │
                          ▼                           │
                 ┌─────────────────┐                  │
                 │   PostgreSQL     │                 │
                 └─────────────────┘                  │
                                                      │
                              WebRTC Peer-to-Peer ◄───┘

### Các luồng giao tiếp chính

**HTTP / Thymeleaf**

Browser gửi HTTP request tới Spring Boot để xử lý các chức năng như đăng nhập, đăng ký, quản lý người dùng, thanh toán và Admin Dashboard.

**WebSocket / STOMP**

WebSocket được sử dụng cho giao tiếp hai chiều theo thời gian thực:

* Chat.
* Cập nhật trạng thái cuộc gọi.
* WebRTC signaling.

**WebRTC**

Sau khi quá trình signaling hoàn tất, audio/video được truyền trực tiếp giữa hai trình duyệt theo mô hình **Peer-to-Peer**.

WebSocket chỉ đảm nhiệm việc trao đổi các tín hiệu:

OFFER
ANSWER
ICE_CANDIDATE

**PayOS**

Backend tạo yêu cầu thanh toán thông qua PayOS. Sau khi thanh toán thành công, PayOS gửi Webhook về hệ thống để cập nhật trạng thái giao dịch và nâng cấp tài khoản Premium.

---

## 🚀 Key Features

### 1. Authentication & Account Management

* Đăng ký tài khoản.
* Đăng nhập / đăng xuất.
* Quên mật khẩu.
* Khôi phục mật khẩu.
* Quản lý trạng thái tài khoản.

### 2. Browser Trust

Hệ thống nhận diện trình duyệt đăng nhập thông qua browser token.

Khi phát hiện trình duyệt mới:

New Browser
     │
     ▼
  PENDING
     │
     ├── Approve ──> Trusted Browser
     │
     └── Deny ─────> Access Denied

Cơ chế này giúp hạn chế việc truy cập tài khoản từ các trình duyệt chưa được xác nhận.

### 3. Location & Anomaly Detection

Hệ thống ghi nhận vị trí đăng nhập của người dùng thông qua:

* Latitude.
* Longitude.
* Thời điểm đăng nhập.

Khoảng cách địa lý được tính bằng **Haversine Formula**.

Hệ thống sử dụng lịch sử **10 lần đăng nhập gần nhất** để phát hiện vị trí đăng nhập bất thường.

Nếu vị trí mới lệch quá **100 km** so với vị trí trung bình trong lịch sử, hệ thống đưa ra cảnh báo bảo mật.

Ngoài ra, người dùng có thể tìm kiếm những người dùng khác trong phạm vi **20 km**.

### 4. Real-Time Chat

* Nhắn tin theo thời gian thực.
* Sử dụng WebSocket STOMP.
* Không cần reload trang.
* Lưu trữ lịch sử hội thoại.
* Hiển thị tin nhắn trực tiếp trên chat timeline.

### 5. WebRTC Audio / Video Call

* Gọi thoại trực tiếp trên trình duyệt.
* Gọi video giữa hai người dùng.
* WebSocket được sử dụng làm signaling channel.
* WebRTC thực hiện kết nối Peer-to-Peer.

Hệ thống lưu lại lịch sử cuộc gọi với các trạng thái:

MISSED
REJECTED
COMPLETED
FAILED

Thông tin cuộc gọi sau khi kết thúc được hiển thị trên chat timeline.

### 6. Premium & PayOS

* Hỗ trợ tài khoản thông thường và Premium.
* Tạo yêu cầu thanh toán thông qua PayOS.
* Hỗ trợ thanh toán bằng QR.
* Xử lý PayOS Webhook.
* Cập nhật trạng thái giao dịch sau khi thanh toán.
* Tự động nâng cấp tài khoản Premium khi giao dịch thành công.

### 7. Admin Dashboard

Admin có thể theo dõi các thông tin tổng quan của hệ thống:

* Tổng số người dùng.
* Tổng số người dùng Premium.
* Tổng số cuộc gọi.
* Tổng doanh thu thanh toán thành công.
* Danh sách người dùng.
* Trạng thái Premium.

---

## 🛠️ Tech Stack

| Category              | Technology                      |
| --------------------- | ------------------------------- |
| Language              | Java 21                         |
| Backend Framework     | Spring Boot 3.2.0               |
| Persistence           | Spring Data JPA, Hibernate      |
| Database              | PostgreSQL                      |
| Authentication        | HttpSession                     |
| Real-Time             | Spring WebSocket, STOMP, SockJS |
| Video / Audio         | WebRTC                          |
| Payment               | PayOS Java SDK 2.0.1            |
| Server-Side Rendering | Thymeleaf                       |
| Frontend              | HTML5, CSS3, JavaScript ES6+    |
| UI                    | Bootstrap 5                     |
| Build Tool            | Maven                           |
| Utilities             | Lombok, Spring Boot DevTools    |

---

## 💡 Technical Highlights

### 1. WebSocket Authentication

Sử dụng `HandshakeInterceptor` để kiểm tra `HttpSession` trong quá trình WebSocket handshake.

`userId` từ session được sử dụng để xác định người dùng trên kết nối WebSocket.

Các kết nối không có session hợp lệ sẽ bị từ chối.

### 2. WebRTC Signaling

WebSocket STOMP được sử dụng làm signaling server cho WebRTC.

Các loại signaling message chính:

OFFER
ANSWER
ICE_CANDIDATE

Sau khi signaling hoàn tất, audio/video được truyền trực tiếp giữa hai browser.

### 3. Haversine Distance Calculation

Hệ thống sử dụng công thức Haversine để tính khoảng cách giữa hai tọa độ địa lý.

d = 2R × asin(
    sqrt(
        sin²(Δφ / 2)
        + cos(φ1) × cos(φ2) × sin²(Δλ / 2)
    )
)

Trong đó:

R = 6371 km

Thuật toán được sử dụng cho:

* Tìm người dùng xung quanh.
* Phát hiện vị trí đăng nhập bất thường.

### 4. PayOS Webhook

Backend tiếp nhận Webhook từ PayOS để xử lý kết quả thanh toán.

Quy trình:

User
  │
  ▼
Create Payment
  │
  ▼
PayOS
  │
  ▼
QR Payment
  │
  ▼
Payment Success
  │
  ▼
PayOS Webhook
  │
  ▼
Spring Boot
  │
  ▼
Update Payment
  │
  ▼
Upgrade Premium

---

## 📂 Project Structure

findfriends/
├── pom.xml
│
├── src/
│   └── main/
│       ├── java/com/phaithanhcong/
│       │
│       │   ├── FindFriendsApplication.java
│       │
│       │   ├── config/
│       │   │   └── # WebSocket, PayOS and application configuration
│       │
│       │   ├── controller/
│       │   │   ├── admin/
│       │   │   └── user/
│       │
│       │   ├── database/
│       │   │   └── # DataSeeder
│       │
│       │   ├── dto/
│       │   │   └── # Data Transfer Objects
│       │
│       │   ├── model/
│       │   │   └── # JPA Entities
│       │
│       │   ├── repository/
│       │   │   └── # Spring Data JPA Repositories
│       │
│       │   └── service/
│       │       ├── admin/
│       │       └── user/
│       │
│       └── resources/
│           ├── application.properties
│           ├── static/
│           │   └── js/
│           └── templates/
│
└── README.md

### Layer Responsibilities

| Layer      | Responsibility                              |
| ---------- | ------------------------------------------- |
| Controller | Nhận HTTP/WebSocket request và trả response |
| Service    | Xử lý business logic                        |
| Repository | Tương tác với database                      |
| Model      | Đại diện cho dữ liệu và JPA Entity          |
| DTO        | Truyền dữ liệu giữa các layer               |
| Config     | Cấu hình WebSocket, PayOS và hệ thống       |
| Database   | Khởi tạo dữ liệu mẫu                        |

---

## 📡 API Overview

### HTTP APIs

| Method | Endpoint                 | Description                         |
| ------ | ------------------------ | ----------------------------------- |
| POST   | `/login`                 | Đăng nhập                           |
| POST   | `/register`              | Đăng ký                             |
| POST   | `/location/record`       | Ghi nhận vị trí và kiểm tra anomaly |
| GET    | `/location/distances`    | Tìm người dùng xung quanh           |
| POST   | `/browser/check`         | Kiểm tra browser token              |
| POST   | `/browser/approve`       | Phê duyệt trình duyệt mới           |
| POST   | `/payment/create`        | Tạo thanh toán Premium              |
| POST   | `/payment/payos-webhook` | Nhận PayOS Webhook                  |

### WebSocket

**Send destination**

/app/call.signal

Dùng để gửi signaling message cho WebRTC.

**User queue**

/user/queue/call

Nhận:

OFFER
ANSWER
ICE_CANDIDATE

**Message queue**

/user/queue/message

Dùng cho real-time chat và cập nhật thông tin cuộc gọi.

---

## 🔐 Configuration & Security

Không commit các thông tin nhạy cảm vào GitHub:

* API Key.
* Checksum Key.
* Database password.
* Payment credentials.
* Các secret khác.

### PayOS Configuration

Trong `application.properties`:

payos.client-id=${PAYOS_CLIENT_ID:}
payos.api-key=${PAYOS_API_KEY:}
payos.checksum-key=${PAYOS_CHECKSUM_KEY:}

payos.return-url=http://localhost:8080/payment/success
payos.cancel-url=http://localhost:8080/payment/cancel

Thiết lập các biến môi trường trước khi chạy ứng dụng:

PAYOS_CLIENT_ID
PAYOS_API_KEY
PAYOS_CHECKSUM_KEY


### Database Configuration

spring.datasource.url=jdbc:postgresql://localhost:5432/findfriends
spring.datasource.username=postgres
spring.datasource.password=your_db_password

spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update


---

## 🗃️ Data Seeding

Project có `DataSeeder` để tạo dữ liệu phục vụ development/testing.

Dữ liệu mẫu bao gồm:

* 9 tài khoản test: `test1` → `test9`.
* Password mặc định: `1`.
* Các trạng thái thanh toán.
* Các trạng thái cuộc gọi.



## ⚙️ Setup & Installation

### 1. Prerequisites

Yêu cầu môi trường:

* JDK 21+
* Maven 3.8+
* PostgreSQL
* Git

### 2. Clone Repository

git clone <repository-url>
cd findfriends

### 3. Create Database

Tạo database PostgreSQL:

CREATE DATABASE findfriends;

### 4. Configure Application

Cập nhật database configuration trong:

src/main/resources/application.properties

Thiết lập các biến môi trường PayOS:

PAYOS_CLIENT_ID
PAYOS_API_KEY
PAYOS_CHECKSUM_KEY

### 5. Run Application

mvn clean spring-boot:run

Hoặc:

mvn spring-boot:run

### 6. Access Application

Sau khi ứng dụng khởi động thành công:

http://localhost:8080

Admin Dashboard:

http://localhost:8080/admin

---

## 🧪 Development & Testing

Project sử dụng `DataSeeder` để tạo dữ liệu mẫu giúp kiểm thử nhanh các chức năng:

* Authentication.
* Browser Trust.
* Location.
* Real-time Chat.
* WebRTC Call.
* Payment.
* Admin Dashboard.

---

## 📌 Project Scope

Project tập trung vào việc áp dụng các kiến thức Backend vào một hệ thống web có nhiều thành phần tương tác thời gian thực.

Các nội dung kỹ thuật chính bao gồm:

Spring Boot
     │
     ├── Layered Architecture
     ├── JPA / Hibernate
     ├── PostgreSQL
     ├── Session Authentication
     ├── WebSocket / STOMP
     ├── WebRTC Signaling
     ├── Location Processing
     ├── Anomaly Detection
     ├── Payment Integration
     └── Admin Management

---

## 👨‍💻 Project Purpose

FindFriends được phát triển trong phạm vi **đồ án thực tập Backend**, với mục tiêu áp dụng kiến thức Java/Spring Boot vào việc xây dựng một ứng dụng có các nghiệp vụ thực tế và giao tiếp thời gian thực.

Project tập trung vào việc tìm hiểu và triển khai:

* Thiết kế Backend theo Layered Architecture.
* Xây dựng business logic bằng Spring Boot.
* Làm việc với PostgreSQL và JPA/Hibernate.
* Xử lý authentication và session.
* Xây dựng real-time communication bằng WebSocket.
* Tích hợp WebRTC cho audio/video call.
* Xử lý location và geographic distance.
* Tích hợp hệ thống thanh toán bên thứ ba.
* Xây dựng chức năng quản trị hệ thống.
