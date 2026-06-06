package com.portfolio.tracker.config;

import com.portfolio.tracker.entity.*;
import com.portfolio.tracker.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final StudentRepository studentRepository;
    private final SkillRepository skillRepository;
    private final ProjectRepository projectRepository;
    private final CertificationRepository certificationRepository;
    private final AchievementRepository achievementRepository;
    private final InternshipRepository internshipRepository;
    private final SkillMappingRepository skillMappingRepository;
    private final ProgramOutcomeRepository programOutcomeRepository;
    private final CourseRepository courseRepository;
    private final CourseOutcomeRepository courseOutcomeRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AppUserRepository appUserRepository,
                           StudentRepository studentRepository,
                           SkillRepository skillRepository,
                           ProjectRepository projectRepository,
                           CertificationRepository certificationRepository,
                           AchievementRepository achievementRepository,
                           InternshipRepository internshipRepository,
                           SkillMappingRepository skillMappingRepository,
                           ProgramOutcomeRepository programOutcomeRepository,
                           CourseRepository courseRepository,
                           CourseOutcomeRepository courseOutcomeRepository,
                           PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.studentRepository = studentRepository;
        this.skillRepository = skillRepository;
        this.projectRepository = projectRepository;
        this.certificationRepository = certificationRepository;
        this.achievementRepository = achievementRepository;
        this.internshipRepository = internshipRepository;
        this.skillMappingRepository = skillMappingRepository;
        this.programOutcomeRepository = programOutcomeRepository;
        this.courseRepository = courseRepository;
        this.courseOutcomeRepository = courseOutcomeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        // ============================================================
        //  TEACHER ACCOUNT
        // ============================================================
        AppUser teacher = AppUser.builder()
            .username("Nasimul")
            .password(passwordEncoder.encode("1234"))
            .role(AppUser.Role.TEACHER)
            .teacherName("Mr. Md.Nasimul Kader")
            .teacherDepartment("Computing & information System")
            .teacherEmail("nasimul.cis@diu.edu.bd")
            .teacherPhone("+880-1700-000001")
            .teacherSpecialization("IoT & AI")
            .teacherPhotoUrl("/images/Nasimul_sir.png")
            .build();
        appUserRepository.save(teacher);


        // ============================================================
        //  STUDENT 1 -- Nisha Yamin  (ID: 251-16-022)
        // ============================================================
        Student s1 = Student.builder()
            .studentId("0242510012091022")
            .name("Nisha Yamin")
            .department("Computing & Information System")
            .registrationNo("251-16-022")
            .batch("21")
            .email("251-16-022@diu.edu.bd")
            .phone("01321234456")
            .address("Uttara, Dhaka")
            .githubUrl("https://github.com/nishayamin")
            .linkedinUrl("https://www.linkedin.com/in/nishayamin")
            .portfolioUrl("https://nishayamin.github.io")
            // Social-media-style profile photo via UI Avatars (publicly accessible, no auth required)
            .profilePhotoUrl("/images/nisha.png")
            .bio("Software developer with a strong interest in web development and AI systems. " +
                 "Focused on building clean, efficient, and user-friendly applications. " +
                 "Passionate about learning new technologies and applying them to solve real-world problems.")
            .skills("C, C++, Python, Java, GitHub, Docker, HTML, CSS, SpringBoot, RestAPI, MySQL")
            .build();
        studentRepository.save(s1);

        AppUser u1 = AppUser.builder()
            .username("251-16-022")
            .password(passwordEncoder.encode("Nisha"))
            .role(AppUser.Role.STUDENT)
            .studentId(s1.getId())
            .build();
        appUserRepository.save(u1);

        // --- Projects (Nisha) ---
        projectRepository.save(Project.builder()
            .projectId("PRJ-N001")
            .title("Hospital Management System")
            .description("A web-based hospital management platform built with Spring Boot and MySQL that digitises patient registration, appointment scheduling, doctor-ward assignments, billing, and pharmacy inventory. The system provides role-based dashboards for administrators, doctors, nurses, and receptionists, enabling real-time bed occupancy tracking and automated discharge summaries. Key features include OPD/IPD workflow management, diagnostic report uploads, and email/SMS appointment reminders. The backend exposes a REST API secured with Spring Security, while the Thymeleaf frontend delivers a responsive, mobile-friendly interface. The project targets mid-sized Bangladeshi private hospitals seeking to replace paper-based workflows.")
            .student(s1)
            .projectType(Project.ProjectType.PROJECT)
            .build());

        projectRepository.save(Project.builder()
            .projectId("PRJ-N002")
            .title("Shopie")
            .description("Shopie is a full-stack e-commerce platform designed for Bangladeshi small-to-medium retailers. Built with Spring Boot, Thymeleaf, and MySQL, it supports multi-vendor product listings, category-based browsing, a shopping cart with bKash/SSLCommerz payment gateway integration, and an order-tracking dashboard. Sellers get an analytics panel showing daily revenue, top-selling products, and low-stock alerts. Buyers benefit from a personalised recommendation engine using purchase history. The admin panel handles vendor approval, dispute resolution, and commission management. Shopie differentiates from generic e-commerce templates by focusing on the local SME ecosystem with Bengali-language support and Bangladesh-specific logistics integration.")
            .student(s1)
            .projectType(Project.ProjectType.PROJECT)
            .build());

        projectRepository.save(Project.builder()
            .projectId("PRJ-N003")
            .title("Habit Tracker")
            .description("A personal productivity application that helps users build and sustain positive daily habits through streak-based motivation, visual progress charts, and AI-generated nudges. Developed with Spring Boot and a React frontend, the app lets users define custom habits, set frequency goals (daily/weekly), and log completions. The analytics module visualises streaks, completion rates, and trend graphs over rolling 30/90-day windows. A lightweight rule-based AI engine analyses patterns to send proactive reminders at the user\\'s historically most-productive times. Social features allow friends to join habit challenges, adding an accountability layer. The target audience is DIU students looking to manage study routines alongside personal goals.")
            .student(s1)
            .projectType(Project.ProjectType.PROJECT)
            .build());

        projectRepository.save(Project.builder()
            .projectId("PRJ-N004")
            .title("Trackio")
            .description("Trackio is a comprehensive academic portfolio and skill-tracking platform built as a capstone project for Daffodil International University. The system enables students to log projects, certifications, achievements, and internships in a structured digital portfolio aligned with OBE (Outcome-Based Education) standards. Teachers can monitor cohort progress, view skill attainment heatmaps, generate OBE attainment reports, and endorse student skills. The platform integrates an AI-powered CV generator that tailors resumes to target job descriptions using the Claude API. Built with Spring Boot, Spring Security, Thymeleaf, H2/MySQL, and deployed via Docker, Trackio addresses the gap between academic learning and industry-readiness reporting at Bangladesh universities.")
            .student(s1)
            .projectType(Project.ProjectType.CAPSTONE)
            .build());

        projectRepository.save(Project.builder()
            .projectId("PRJ-N005")
            .title("Flood Rescue Management System")
            .description("A disaster-response coordination platform tailored for Bangladesh\\'s annual flood crisis, enabling government agencies, NGOs, and volunteers to manage rescue operations in real time. The system features an interactive Leaflet.js map showing flooded zones (sourced from BWDB open data), displaced-person registration, volunteer dispatch, and supply-chain tracking for relief goods. Affected families can submit SOS requests via SMS (Twilio) or web form; rescue teams receive optimised routing to reach them. A dashboard aggregates live statistics on rescued persons, available boats, shelter capacity, and food stock. Built with Spring Boot, WebSocket for live updates, and a MySQL backend, the project was validated against the 2024 Sylhet flood scenario.")
            .student(s1)
            .projectType(Project.ProjectType.PROJECT)
            .build());

        projectRepository.save(Project.builder()
            .projectId("PRJ-N006")
            .title("TechSavvy Network Design")
            .description("A network design and simulation assignment for a fictional mid-sized IT company called TechSavvy Ltd. The project documents a complete LAN/WAN topology covering three office floors and a remote branch, designed using Cisco Packet Tracer. The design implements VLAN segmentation for HR, Engineering, and Management departments, OSPF dynamic routing between floors, a DMZ for the public web server, and site-to-site IPSec VPN for the remote branch. IP addressing follows a structured subnetting scheme (192.168.x.x/26 per VLAN). The report includes a risk analysis, bandwidth estimation, and a disaster-recovery plan. The assignment demonstrates proficiency in network design principles covered in CSE-3611 Computer Networks.")
            .student(s1)
            .projectType(Project.ProjectType.ASSIGNMENT)
            .build());

        // --- Certifications (Nisha) ---
        certificationRepository.save(Certification.builder()
            .certificationId("CERT-N001")
            .student(s1)
            .title("Library Award")
            .issuingOrganization("Daffodil International University Library")
            .issueDate(LocalDate.of(2023, 12, 15))
            .verified(true)
            .certificateLink("https://certificates.diu.edu.bd/library-award/nisha-yamin")
            .build());

        certificationRepository.save(Certification.builder()
            .certificationId("CERT-N002")
            .student(s1)
            .title("Dean's Award")
            .issuingOrganization("Faculty of Science & Information Technology, DIU")
            .issueDate(LocalDate.of(2024, 6, 10))
            .verified(true)
            .certificateLink("https://certificates.diu.edu.bd/deans-award/faheem")
            .build());

        certificationRepository.save(Certification.builder()
            .certificationId("CERT-N003")
            .student(s1)
            .title("DIU-DoR Award")
            .issuingOrganization("Daffodil International University \u2013 Department of Research")
            .issueDate(LocalDate.of(2024, 9, 20))
            .verified(true)
            .certificateLink("https://certificates.diu.edu.bd/dor-award/diu-dor-2024")
            .build());

        certificationRepository.save(Certification.builder()
            .certificationId("CERT-N004")
            .student(s1)
            .title("Prompt Engineering")
            .issuingOrganization("Coursera (AI Learning Track)")
            .issueDate(LocalDate.of(2025, 2, 5))
            .expiryDate(LocalDate.of(2028, 2, 5))
            .verified(true)
            .certificateLink("https://coursera.org/verify/prompt-engineering-ai-2025")
            .build());

        certificationRepository.save(Certification.builder()
            .certificationId("CERT-N005")
            .student(s1)
            .title("Mastering Java")
            .issuingOrganization("Udemy")
            .issueDate(LocalDate.of(2024, 11, 12))
            .verified(true)
            .certificateLink("https://udemy.com/certificate/UC-java-mastery-2024")
            .build());

        // --- Achievements (Nisha) ---
        achievementRepository.save(Achievement.builder()
            .achievementId("ACH-N001")
            .student(s1)
            .eventName("National Hackathon \u2013 Top 100")
            .position("Top 100 Finalists")
            .organizer("ICT Division, Government of Bangladesh")
            .date(LocalDate.of(2024, 3, 18))
            .proofLink("https://hackathon.gov.bd/certificates/top100-nisha-yamin")
            .build());

        achievementRepository.save(Achievement.builder()
            .achievementId("ACH-N002")
            .student(s1)
            .eventName("NASA SpaceX Challenge \u2013 Top 100")
            .position("Top 100 Global Participants")
            .organizer("NASA Space Apps Challenge (Global)")
            .date(LocalDate.of(2024, 10, 5))
            .proofLink("https://spaceapps.nasa.gov/2024/top100/faheem")
            .build());

        achievementRepository.save(Achievement.builder()
            .achievementId("ACH-N003")
            .student(s1)
            .eventName("DIU AI Project Showcase")
            .position("Selected Project Showcase Participant")
            .organizer("Daffodil International University (DIU)")
            .date(LocalDate.of(2025, 1, 20))
            .proofLink("https://diu.edu.bd/ai-showcase/2025/nisha-faheem-project")
            .build());

        // --- Internships (Nisha) ---
        internshipRepository.save(Internship.builder()
            .recordId("INT-N001")
            .student(s1)
            .companyName("Cefalo")
            .role("Software Engineer")
            .status(Internship.Status.SELECTED)
            .build());

        internshipRepository.save(Internship.builder()
            .recordId("INT-N002")
            .student(s1)
            .companyName("BrainStation23")
            .role("AI Engineer")
            .status(Internship.Status.SELECTED)
            .build());

        internshipRepository.save(Internship.builder()
            .recordId("INT-N003")
            .student(s1)
            .companyName("Enosis")
            .role("Software Engineer")
            .status(Internship.Status.SELECTED)
            .build());

        // ============================================================
        //  STUDENT 2 -- Faheem  (ID: 251-16-041)
        // ============================================================
        Student s2 = Student.builder()
            .studentId("0242510012091041")
            .name("Mohammad Al Faheem")
            .department("Computing & Information System")
            .registrationNo("251-16-041")
            .batch("21")
            .email("251-16-041@diu.edu.bd")
            .phone("0171675266")
            .address("Uttara, Dhaka")
            .githubUrl("https://github.com/faheemdev")
            .linkedinUrl("https://www.linkedin.com/in/faheemdev")
            .portfolioUrl("https://faheemdev.github.io")
            // Social-media-style profile photo via UI Avatars
            .profilePhotoUrl("/images/Fahim.png")
            .bio("Aspiring software engineer specializing in full-stack development and backend systems. " +
                 "Interested in scalable application design, AI integration, and problem-solving through code. " +
                 "Continuously improving skills through projects and practical development work.")
            .skills("C, C++, Python, Java, GitHub, Docker, HTML, CSS, SpringBoot, RestAPI, MySQL")
            .build();
        studentRepository.save(s2);

        AppUser u2 = AppUser.builder()
            .username("251-16-041")
            .password(passwordEncoder.encode("Faheem"))
            .role(AppUser.Role.STUDENT)
            .studentId(s2.getId())
            .build();
        appUserRepository.save(u2);

        // --- Projects (Faheem) ---
        projectRepository.save(Project.builder()
            .projectId("PRJ-F001")
            .title("Medicare")
            .description("Medicare is a telemedicine and digital health records platform aimed at improving healthcare accessibility in rural Bangladesh. Patients can register, book video consultations with licensed doctors, receive e-prescriptions, and view their complete medical history through a secure portal. Doctors manage appointment queues, write digital notes, and flag high-risk patients for follow-up. The system integrates with a pharmacy module where prescriptions are forwarded automatically to partner dispensaries for home delivery. Built using Spring Boot, MySQL, and WebRTC for video calls, Medicare prioritises low-bandwidth performance optimised for 3G connectivity prevalent in rural areas. The platform includes a health-tip notification system delivering WHO-approved content in Bengali.")
            .student(s2)
            .projectType(Project.ProjectType.PROJECT)
            .build());

        projectRepository.save(Project.builder()
            .projectId("PRJ-F002")
            .title("Nasa SpaceX")
            .description("Nasa SpaceX is a space-data exploration and visualisation capstone project that aggregates live feeds from NASA Open APIs and SpaceX launch data to create an interactive mission-tracking dashboard. Users can explore upcoming and historical rocket launches, view satellite orbital paths rendered with Cesium.js 3D globe, and browse exoplanet candidates classified using a custom ML model trained on the NASA Exoplanet Archive. The backend, built with Spring Boot, polls NASA APOD, Mars Rover Photos, EONET, and Asteroids NeoWs APIs hourly, storing snapshots in MySQL. A leaderboard gamification layer rewards users for correctly predicting launch outcomes. The project demonstrates full-stack development combined with real-world API integration and basic machine learning deployment.")
            .student(s2)
            .projectType(Project.ProjectType.CAPSTONE)
            .build());

        projectRepository.save(Project.builder()
            .projectId("PRJ-F003")
            .title("GreenAI")
            .description("GreenAI is an AI-powered environmental sustainability platform that helps individuals and organisations measure, track, and reduce their carbon footprint. Users input daily activities (transport, energy usage, diet, purchases) and the system calculates equivalent CO2 emissions using IPCC-validated emission factors. A fine-tuned regression model then generates a personalised reduction roadmap with weekly targets. The dashboard visualises emissions trends, compares the user against Bangladeshi national averages, and awards green badges for milestone reductions. An NLP chatbot answers sustainability questions and suggests locally available eco-friendly alternatives. Built with Python (FastAPI) for the ML layer, Spring Boot for the main application, and MySQL for persistence, GreenAI targets environmentally conscious university students and SME sustainability officers.")
            .student(s2)
            .projectType(Project.ProjectType.PROJECT)
            .build());

        projectRepository.save(Project.builder()
            .projectId("PRJ-F004")
            .title("Contact Application")
            .description("A feature-rich contact management application developed as a coursework assignment to demonstrate CRUD operations, search, and data persistence. The app allows users to create, view, edit, and delete contacts with fields for name, phone numbers, email, address, organisation, and a profile photo. Contacts are grouped by category (family, work, university) and are searchable by any field with instant filtering. The application supports CSV import/export for migrating contacts from other platforms and generates a printable contact directory. Built with Spring Boot and Thymeleaf using an H2 in-memory database (swappable to MySQL for production), the assignment focuses on clean MVC architecture, form validation, and responsive UI design as specified in the CSE-2521 Database Systems lab requirements.")
            .student(s2)
            .projectType(Project.ProjectType.ASSIGNMENT)
            .build());

        projectRepository.save(Project.builder()
            .projectId("PRJ-F005")
            .title("Flood Rescue Management System")
            .description("A community-driven flood-response platform focused on Faheem\\'s home district of Sylhet, one of Bangladesh\\'s most flood-prone regions. The system enables local union parishad officials to log affected households, coordinate with upazila rescue teams, and manage relief distribution. A mobile-responsive web app lets community volunteers self-register, declare their skills (swimming, first aid, boat operation), and receive geo-tagged rescue assignments. The platform integrates BWDB river-level APIs to auto-trigger alerts when water levels exceed danger thresholds at monitored stations. A heatmap view overlays flood depth data with population density to prioritise rescue zones. Built with Spring Boot, Leaflet.js, and MySQL, this project extends beyond the generic disaster-management template by incorporating real local government workflows and verified geographic data.")
            .student(s2)
            .projectType(Project.ProjectType.PROJECT)
            .build());

        projectRepository.save(Project.builder()
            .projectId("PRJ-F006")
            .title("Trackio-DIU")
            .description("Trackio-DIU is Faheem\\'s capstone extension of the Trackio platform, specialised for the broader DIU ecosystem with multi-department support. While the original Trackio focuses on CIS department portfolio tracking, Trackio-DIU scales to university-wide deployment with department-specific OBE templates, a centralised teacher-admin hierarchy, and cross-department project discovery. New features include a public student showcase gallery where portfolios are visible to industry recruiters, a skill-endorsement marketplace where alumni can endorse current students, and an automated internship-readiness score computed from skill levels, project count, and certification portfolio. The capstone involved migrating from H2 to PostgreSQL, containerising with Docker Compose, and load-testing with JMeter to support 5,000 concurrent student profiles.")
            .student(s2)
            .projectType(Project.ProjectType.CAPSTONE)
            .build());

        // --- Certifications (Faheem) ---
        certificationRepository.save(Certification.builder()
            .certificationId("CERT-F001")
            .student(s2)
            .title("Library Award")
            .issuingOrganization("Daffodil International University Library")
            .issueDate(LocalDate.of(2023, 12, 15))
            .verified(true)
            .certificateLink("https://certificates.diu.edu.bd/library-award/nisha-yamin")
            .build());

        certificationRepository.save(Certification.builder()
            .certificationId("CERT-F002")
            .student(s2)
            .title("Dean's Award")
            .issuingOrganization("Faculty of Science & Information Technology, DIU")
            .issueDate(LocalDate.of(2024, 6, 10))
            .verified(true)
            .certificateLink("https://certificates.diu.edu.bd/deans-award/faheem")
            .build());

        certificationRepository.save(Certification.builder()
            .certificationId("CERT-F003")
            .student(s2)
            .title("DIU-DoR Award")
            .issuingOrganization("Daffodil International University \u2013 Department of Research")
            .issueDate(LocalDate.of(2024, 9, 20))
            .verified(true)
            .certificateLink("https://certificates.diu.edu.bd/dor-award/diu-dor-2024")
            .build());

        certificationRepository.save(Certification.builder()
            .certificationId("CERT-F004")
            .student(s2)
            .title("Prompt Engineering")
            .issuingOrganization("Coursera (AI Learning Track)")
            .issueDate(LocalDate.of(2025, 2, 5))
            .expiryDate(LocalDate.of(2028, 2, 5))
            .verified(true)
            .certificateLink("https://coursera.org/verify/prompt-engineering-ai-2025")
            .build());

        certificationRepository.save(Certification.builder()
            .certificationId("CERT-F005")
            .student(s2)
            .title("Mastering Java")
            .issuingOrganization("Udemy")
            .issueDate(LocalDate.of(2024, 11, 12))
            .verified(true)
            .certificateLink("https://udemy.com/certificate/UC-java-mastery-2024")
            .build());

        // --- Achievements (Faheem) ---
        achievementRepository.save(Achievement.builder()
            .achievementId("ACH-F001")
            .student(s2)
            .eventName("National Hackathon \u2013 Top 100")
            .position("Top 100 Finalists")
            .organizer("ICT Division, Government of Bangladesh")
            .date(LocalDate.of(2024, 3, 18))
            .proofLink("https://hackathon.gov.bd/certificates/top100-nisha-yamin")
            .build());

        achievementRepository.save(Achievement.builder()
            .achievementId("ACH-F002")
            .student(s2)
            .eventName("NASA SpaceX Challenge \u2013 Top 100")
            .position("Top 100 Global Participants")
            .organizer("NASA Space Apps Challenge (Global)")
            .date(LocalDate.of(2024, 10, 5))
            .proofLink("https://spaceapps.nasa.gov/2024/top100/faheem")
            .build());

        achievementRepository.save(Achievement.builder()
            .achievementId("ACH-F003")
            .student(s2)
            .eventName("DIU AI Project Showcase")
            .position("Selected Project Showcase Participant")
            .organizer("Daffodil International University (DIU)")
            .date(LocalDate.of(2025, 1, 20))
            .proofLink("https://diu.edu.bd/ai-showcase/2025/nisha-faheem-project")
            .build());

        // --- Internships (Faheem) ---
        internshipRepository.save(Internship.builder()
            .recordId("INT-F001")
            .student(s2)
            .companyName("Cefalo")
            .role("DevOps Engineer")
            .status(Internship.Status.SELECTED)
            .build());

        internshipRepository.save(Internship.builder()
            .recordId("INT-F002")
            .student(s2)
            .companyName("BrainStation23")
            .role("AI Engineer")
            .status(Internship.Status.SELECTED)
            .build());

        internshipRepository.save(Internship.builder()
            .recordId("INT-F003")
            .student(s2)
            .companyName("Enosis")
            .role("AI Engineer")
            .status(Internship.Status.SELECTED)
            .build());


        // ============================================================
        //  SKILLS -- Nisha (s1)
        // ============================================================
        String[][] nishaSkills = {
            {"SK-N001", "C",           "Programming Language", "INTERMEDIATE"},
            {"SK-N002", "C++",         "Programming Language", "INTERMEDIATE"},
            {"SK-N003", "Python",      "Programming Language", "ADVANCED"},
            {"SK-N004", "Java",        "Programming Language", "ADVANCED"},
            {"SK-N005", "GitHub",      "DevOps & Tools",       "ADVANCED"},
            {"SK-N006", "Docker",      "DevOps & Tools",       "INTERMEDIATE"},
            {"SK-N007", "HTML",        "Web Development",      "ADVANCED"},
            {"SK-N008", "CSS",         "Web Development",      "ADVANCED"},
            {"SK-N009", "SpringBoot",  "Frameworks",           "ADVANCED"},
            {"SK-N010", "RestAPI",     "API",           "ADVANCED"},
            {"SK-N011", "MySQL",       "Database",             "ADVANCED"},
        };
        for (String[] row : nishaSkills) {
            Skill sk = Skill.builder()
                .skillId(row[0])
                .studentId(s1.getId())
                .skillName(row[1])
                .category(row[2])
                .level(Skill.Level.valueOf(row[3]))
                .verified(true)
                .build();
            skillRepository.save(sk);
        }

        // ============================================================
        //  SKILLS -- Faheem (s2)
        // ============================================================
        String[][] faheemSkills = {
            {"SK-F001", "C",           "Programming Language", "INTERMEDIATE"},
            {"SK-F002", "C++",         "Programming Language", "INTERMEDIATE"},
            {"SK-F003", "Python",      "Programming Language", "ADVANCED"},
            {"SK-F004", "Java",        "Programming Language", "ADVANCED"},
            {"SK-F005", "GitHub",      "DevOps & Tools",       "ADVANCED"},
            {"SK-F006", "Docker",      "DevOps & Tools",       "INTERMEDIATE"},
            {"SK-F007", "HTML",        "Web Development",      "ADVANCED"},
            {"SK-F008", "CSS",         "Web Development",      "ADVANCED"},
            {"SK-F009", "SpringBoot",  "Frameworks",           "ADVANCED"},
            {"SK-F010", "RestAPI",     "API",           "ADVANCED"},
            {"SK-F011", "MySQL",       "Database",             "ADVANCED"},
        };
        for (String[] row : faheemSkills) {
            Skill sk = Skill.builder()
                .skillId(row[0])
                .studentId(s2.getId())
                .skillName(row[1])
                .category(row[2])
                .level(Skill.Level.valueOf(row[3]))
                .verified(true)
                .build();
            skillRepository.save(sk);
        }

        // ============================================================
        //  OBE SEED DATA  --  Washington Accord 12 Program Outcomes
        // ============================================================
        seedProgramOutcomes();
        seedSampleCourses();
    }

    private void seedProgramOutcomes() {
        if (programOutcomeRepository.count() > 0) return;

        programOutcomeRepository.save(new ProgramOutcome("PO1", "Engineering Knowledge",
            "Apply knowledge of mathematics, science, engineering fundamentals, and an engineering specialization to the solution of complex engineering problems."));
        programOutcomeRepository.save(new ProgramOutcome("PO2", "Problem Analysis",
            "Identify, formulate, research literature, and analyse complex engineering problems reaching substantiated conclusions using first principles."));
        programOutcomeRepository.save(new ProgramOutcome("PO3", "Design / Development of Solutions",
            "Design solutions for complex engineering problems and design system components or processes that meet specified needs."));
        programOutcomeRepository.save(new ProgramOutcome("PO4", "Investigation",
            "Use research-based knowledge and methods including design of experiments, analysis and interpretation of data to provide valid conclusions."));
        programOutcomeRepository.save(new ProgramOutcome("PO5", "Modern Tool Usage",
            "Create, select and apply appropriate techniques, resources, and modern engineering and IT tools, including prediction and modeling."));
        programOutcomeRepository.save(new ProgramOutcome("PO6", "The Engineer and Society",
            "Apply reasoning informed by contextual knowledge to assess societal, health, safety, legal, and cultural issues relevant to professional engineering practice."));
        programOutcomeRepository.save(new ProgramOutcome("PO7", "Environment and Sustainability",
            "Understand the impact of professional engineering solutions in societal and environmental contexts, and demonstrate knowledge of sustainable development."));
        programOutcomeRepository.save(new ProgramOutcome("PO8", "Ethics",
            "Apply ethical principles and commit to professional ethics and responsibilities and norms of engineering practice."));
        programOutcomeRepository.save(new ProgramOutcome("PO9", "Individual and Team Work",
            "Function effectively as an individual, and as a member or leader in diverse teams and in multi-disciplinary settings."));
        programOutcomeRepository.save(new ProgramOutcome("PO10", "Communication",
            "Communicate effectively on complex engineering activities with the engineering community and with society at large."));
        programOutcomeRepository.save(new ProgramOutcome("PO11", "Project Management and Finance",
            "Demonstrate knowledge and understanding of engineering and management principles and apply these to one's own work as a member and leader of a team."));
        programOutcomeRepository.save(new ProgramOutcome("PO12", "Life-long Learning",
            "Recognise the need for, and have the preparation and ability to engage in independent and life-long learning in the broadest context of technological change."));
    }

    private void seedSampleCourses() {
        if (courseRepository.count() > 0) return;

        Course c1 = new Course();
        c1.setCourseCode("CSE-3711");
        c1.setCourseName("Software Engineering");
        c1.setDepartment("Computer Science");
        c1.setCredits(3);
        c1.setInstructor("Mr. Md. Nasimul Kader");
        courseRepository.save(c1);

        CourseOutcome co1_1 = new CourseOutcome();
        co1_1.setCourseId(c1.getId());
        co1_1.setCode("CO1");
        co1_1.setStatement("Apply object-oriented design principles to model real-world systems.");
        co1_1.setBloomLevel(CourseOutcome.BloomLevel.APPLY);
        co1_1.setPoMappings("PO1:3,PO3:2,PO5:1");
        courseOutcomeRepository.save(co1_1);

        CourseOutcome co1_2 = new CourseOutcome();
        co1_2.setCourseId(c1.getId());
        co1_2.setCode("CO2");
        co1_2.setStatement("Design and develop software solutions using modern frameworks and tools.");
        co1_2.setBloomLevel(CourseOutcome.BloomLevel.CREATE);
        co1_2.setPoMappings("PO3:3,PO5:3,PO9:2");
        courseOutcomeRepository.save(co1_2);

        CourseOutcome co1_3 = new CourseOutcome();
        co1_3.setCourseId(c1.getId());
        co1_3.setCode("CO3");
        co1_3.setStatement("Evaluate software quality through testing, code review, and design analysis.");
        co1_3.setBloomLevel(CourseOutcome.BloomLevel.EVALUATE);
        co1_3.setPoMappings("PO2:3,PO4:2,PO11:2");
        courseOutcomeRepository.save(co1_3);

        Course c2 = new Course();
        c2.setCourseCode("CSE-3611");
        c2.setCourseName("Computer Networks");
        c2.setDepartment("Computer Science");
        c2.setCredits(3);
        c2.setInstructor("Mr. Md. Nasimul Kader");
        courseRepository.save(c2);

        CourseOutcome co2_1 = new CourseOutcome();
        co2_1.setCourseId(c2.getId());
        co2_1.setCode("CO1");
        co2_1.setStatement("Analyze the OSI and TCP/IP models and explain protocols at each layer.");
        co2_1.setBloomLevel(CourseOutcome.BloomLevel.ANALYZE);
        co2_1.setPoMappings("PO1:3,PO2:2");
        courseOutcomeRepository.save(co2_1);

        CourseOutcome co2_2 = new CourseOutcome();
        co2_2.setCourseId(c2.getId());
        co2_2.setCode("CO2");
        co2_2.setStatement("Design LAN/WAN topologies with appropriate IP addressing and subnetting.");
        co2_2.setBloomLevel(CourseOutcome.BloomLevel.CREATE);
        co2_2.setPoMappings("PO3:3,PO5:2,PO11:1");
        courseOutcomeRepository.save(co2_2);

        Course c3 = new Course();
        c3.setCourseCode("CSE-2521");
        c3.setCourseName("Database Systems");
        c3.setDepartment("Computer Science");
        c3.setCredits(3);
        c3.setInstructor("Faculty");
        courseRepository.save(c3);

        CourseOutcome co3_1 = new CourseOutcome();
        co3_1.setCourseId(c3.getId());
        co3_1.setCode("CO1");
        co3_1.setStatement("Design normalized relational schemas from business requirements.");
        co3_1.setBloomLevel(CourseOutcome.BloomLevel.APPLY);
        co3_1.setPoMappings("PO1:3,PO3:3");
        courseOutcomeRepository.save(co3_1);

        CourseOutcome co3_2 = new CourseOutcome();
        co3_2.setCourseId(c3.getId());
        co3_2.setCode("CO2");
        co3_2.setStatement("Write efficient SQL queries and analyze query performance.");
        co3_2.setBloomLevel(CourseOutcome.BloomLevel.ANALYZE);
        co3_2.setPoMappings("PO2:2,PO5:3");
        courseOutcomeRepository.save(co3_2);
    }
}
