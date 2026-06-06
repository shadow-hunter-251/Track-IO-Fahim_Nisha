# 📚 Portfolio & Skill Tracker

A full-stack Spring Boot web application for managing student academic portfolios, skills, projects, certifications, achievements, and internships — with separate role-based portals for **Students** and **Teachers**.

---

## 🎨 Theme
Custom dark palette: `#170c10` · `#f79aaf` · `#dd9faf` · `#8e4256` · `#6a3f4a`

---

## 🚀 Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+

### Run the App

```bash
cd portfolio-tracker
mvn spring-boot:run
```

Then open: **http://localhost:8080**

---

## 🔑 Demo Login Accounts

| Role    | Username  | Password     | Portal              |
|---------|-----------|--------------|---------------------|
| Teacher | `teacher` | `teacher123` | `/teacher/dashboard`|
| Student | `aisha`   | `student123` | `/student/dashboard`|
| Student | `rafiq`   | `student123` | `/student/dashboard`|
| Student | `nadia`   | `student123` | `/student/dashboard`|

---

## 🗂 Project Structure

```
src/main/java/com/portfolio/tracker/
├── config/
│   ├── SecurityConfig.java       # Spring Security + role-based routing
│   └── DataInitializer.java      # Demo data seeding on startup
├── controller/
│   ├── AuthController.java       # Login / root redirect
│   ├── StudentPortalController.java  # All student CRUD routes
│   └── TeacherController.java        # All teacher CRUD routes
├── entity/
│   ├── AppUser.java              # Login user (student or teacher)
│   ├── Student.java              # Student profile
│   ├── Skill.java                # Skill definitions
│   ├── Project.java              # Projects / assignments / capstone
│   ├── Certification.java        # Certificates
│   ├── Achievement.java          # Competitions & awards
│   ├── Internship.java           # Internship & placement records
│   └── SkillMapping.java         # Skill → learning outcome mapping
└── repository/                   # Spring Data JPA repos for all entities

src/main/resources/
├── application.properties        # H2 DB, JPA, Thymeleaf config
├── static/css/main.css           # Full custom CSS (dark theme)
└── templates/
    ├── auth/login.html            # Login page
    ├── fragments/
    │   ├── student-sidebar.html   # Reusable student nav
    │   └── teacher-sidebar.html   # Reusable teacher nav
    ├── student/                   # 10 student portal templates
    │   ├── dashboard.html
    │   ├── profile.html / profile-edit.html
    │   ├── projects.html / project-form.html
    │   ├── certifications.html / cert-form.html
    │   ├── achievements.html / achievement-form.html
    │   ├── internships.html / internship-form.html
    │   └── report.html
    └── teacher/                   # 17 teacher portal templates
        ├── dashboard.html
        ├── students.html / student-form.html / student-view.html
        ├── projects.html / project-form.html
        ├── certifications.html / cert-form.html
        ├── achievements.html / achievement-form.html
        ├── internships.html / internship-form.html
        ├── skills.html / skill-form.html
        ├── skill-mappings.html / mapping-form.html
        ├── reports.html / student-report.html
```

---

## ✅ Features

### Student Portal
- View personal dashboard with stats
- Edit profile (name, email, phone, address)
- **Projects** — Full CRUD (Assignment / Project / Capstone)
- **Certifications** — Full CRUD with issue/expiry dates
- **Achievements** — Full CRUD (competitions, awards)
- **Internships** — Full CRUD with status tracking
- **Competency Report** — Auto-generated from all data, printable

### Teacher Portal
- Overview dashboard with system-wide statistics
- **Student Management** — Full CRUD + create login credentials
- **Skills** — Full CRUD with categories and levels
- **Projects** — Full CRUD across all students
- **Certifications** — Full CRUD across all students
- **Achievements** — Full CRUD across all students
- **Internships** — Full CRUD across all students
- **Skill Mappings** — Map skills to course/program outcomes
- **Reports** — Generate and print per-student competency reports

---

## 🛠 Tech Stack

| Layer       | Technology                  |
|-------------|-----------------------------|
| Backend     | Spring Boot 3.2, Spring MVC |
| Security    | Spring Security 6           |
| Database    | H2 (in-memory)              |
| ORM         | Spring Data JPA / Hibernate |
| Templates   | Thymeleaf                   |
| Code Gen    | Lombok                      |
| Dev Tools   | Spring Boot DevTools         |
| Frontend    | Custom CSS (no frameworks)  |

---

## 🗄 H2 Console
Access the database at: **http://localhost:8080/h2-console**  
JDBC URL: `jdbc:h2:mem:portfoliodb`  
Username: `sa` | Password: *(empty)*

---

## 📝 Notes
- Data is in-memory and resets on each restart (H2 `create-drop`)
- To persist data, change `spring.jpa.hibernate.ddl-auto=update` and switch to a file-based H2 or external DB
- To add more teacher accounts, seed them in `DataInitializer.java`
