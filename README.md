# 🏛️ Finance Management - Hệ thống Quản lý Tài chính Cá nhân Thông minh

![React](https://img.shields.io/badge/Frontend-ReactJS-61DAFB?style=for-the-badge&logo=react&logoColor=black)
![Spring Boot](https://img.shields.io/badge/Backend-Spring_Boot-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![MySQL](https://img.shields.io/badge/Database-MySQL-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![AI](https://img.shields.io/badge/Integration-AI_Assistant-FF9900?style=for-the-badge&logo=openai&logoColor=white)

## 📖 Giới thiệu dự án
**Finance Managementt** là ứng dụng nền web hỗ trợ quản lý tài chính cá nhân được phát triển dựa trên triết lý **6 chiếc lọ (JARS System)** của T. Harv Eker. Khác với các ứng dụng ghi chép thu chi truyền thống, hệ thống tích hợp **Trí tuệ nhân tạo (AI)** đóng vai trò như một trợ lý ảo, giúp tự động hóa việc phân tích hành vi tiêu dùng, thiết lập kỷ luật dòng tiền và đưa ra các cảnh báo thâm hụt thông minh.

Dự án được phát triển nhằm giải quyết bài toán "chi tiêu cảm xúc" và thiếu kiểm soát ngân sách của giới trẻ hiện nay.

---

## ✨ Tính năng nổi bật

*   **📊 Bảng điều khiển (Dashboard):** Trực quan hóa tổng quan tài sản, số dư ví và biểu đồ thu chi trong tháng.
*   **📝 Quản lý giao dịch (Activity):** Ghi chép linh hoạt các khoản thu/chi. Tích hợp tính năng lọc, phân trang và tìm kiếm thông minh theo thời gian thực.
*   **🏺 Ngân sách 6 chiếc lọ (Budgets):** Tự động phân bổ thu nhập vào 6 quỹ (Thiết yếu, Hưởng thụ, Giáo dục, Tự do tài chính, Tiết kiệm dài hạn, Cho đi).
*   **🤖 Trợ lý phân tích AI (Analytics):** Quét dữ liệu giao dịch trong tháng, đối chiếu với định mức ngân sách và tự động sinh ra các báo cáo cảnh báo, lời khuyên cá nhân hóa.
*   **🔒 Bảo mật cấp độ cao:** Xác thực người dùng qua JWT (JSON Web Token) và mã hóa mật khẩu an toàn bằng BCrypt.

---

## 🛠️ Yêu cầu môi trường (Prerequisites)

Để chạy dự án trên máy cá nhân, bạn cần cài đặt sẵn các công cụ sau:
*   [Node.js](https://nodejs.org/) (Phiên bản 16.x trở lên)
*   [Java JDK](https://www.oracle.com/java/technologies/javase-downloads.html) (Phiên bản 17 trở lên)
*   [MySQL Server](https://dev.mysql.com/downloads/mysql/) (Phiên bản 8.x)
*   Trình duyệt web (Khuyên dùng Google Chrome hoặc Microsoft Edge)

---

## 🚀 Hướng dẫn cài đặt và khởi chạy

Dự án được chia làm 2 phần độc lập: Frontend và Backend.

### 1. Cài đặt Cơ sở dữ liệu (Database)
1. Mở MySQL Workbench hoặc công cụ quản lý MySQL.
2. Tạo một schema mới với tên: `the_architect_db`
   ```sql
   CREATE DATABASE the_architect_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;