package com.portfolio.tracker.controller;

import com.portfolio.tracker.entity.*;
import com.portfolio.tracker.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class AuthController {

    private final AppUserRepository appUserRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AppUserRepository appUserRepository,
                          StudentRepository studentRepository,
                          PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------------------- Public landing routes ----------------------

    @GetMapping("/")
    public String root() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }

    // ---------------------- Signup entry ----------------------

    @GetMapping("/signup")
    public String signupHub() {
        return "redirect:/signup/student";
    }

    // ---------------------- STUDENT SIGNUP ----------------------

    @GetMapping("/signup/student")
    public String showStudentSignup(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new StudentSignupForm());
        }
        return "auth/signup_std";
    }

    @PostMapping("/signup/student")
    public String processStudentSignup(@ModelAttribute("form") StudentSignupForm form,
                                       @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
                                       Model model) {

        // ---- Validation: required fields ----
        if (isBlank(form.getFullName()) || isBlank(form.getUsername())
                || isBlank(form.getPassword()) || isBlank(form.getEmail())
                || isBlank(form.getPhone()) || isBlank(form.getAddress())
                || isBlank(form.getDepartment()) || isBlank(form.getBatch())
                || isBlank(form.getRegistrationNo())) {
            return errorBack("auth/signup_std", "Please fill in all required fields.", form, model);
        }

        if (form.getPassword().length() < 6) {
            return errorBack("auth/signup_std", "Password must be at least 6 characters long.", form, model);
        }

        if (form.getConfirmPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            return errorBack("auth/signup_std", "Passwords do not match.", form, model);
        }

        // ---- Validation: duplicates ----
        if (appUserRepository.existsByUsername(form.getUsername())) {
            return errorBack("auth/signup_std", "That username is already taken. Pick another one.", form, model);
        }
        if (studentRepository.existsByEmail(form.getEmail())) {
            return errorBack("auth/signup_std", "An account already exists with that email address.", form, model);
        }
        if (studentRepository.existsByRegistrationNo(form.getRegistrationNo())) {
            return errorBack("auth/signup_std", "That registration number is already registered.", form, model);
        }

        // ---- Handle file upload (optional) ----
        String photoPath;
        try {
            photoPath = saveUploadedPhoto(photoFile, form.getProfilePhotoUrl());
        } catch (IllegalArgumentException ex) {
            return errorBack("auth/signup_std", ex.getMessage(), form, model);
        } catch (IOException ex) {
            return errorBack("auth/signup_std", "Could not save profile photo. Please try again.", form, model);
        }

        // ---- Generate unique student id ----
        long count = studentRepository.count() + 1;
        String yearPart = form.getBatch().replaceAll("\\D", "");
        if (yearPart.isEmpty()) yearPart = "2025";
        String generatedStudentId = String.format("STU-%s-%04d", yearPart, count);
        while (studentRepository.existsByStudentId(generatedStudentId)) {
            count++;
            generatedStudentId = String.format("STU-%s-%04d", yearPart, count);
        }

        // ---- Create Student ----
        Student student = Student.builder()
                .studentId(generatedStudentId)
                .name(form.getFullName().trim())
                .username(form.getUsername().trim())
                .password(passwordEncoder.encode(form.getPassword()))
                .email(form.getEmail().trim())
                .phone(form.getPhone().trim())
                .address(form.getAddress().trim())
                .department(form.getDepartment().trim())
                .batch(form.getBatch().trim())
                .registrationNo(form.getRegistrationNo().trim())
                .profilePhotoUrl(photoPath)
                .githubUrl(nullIfBlank(form.getGithubUrl()))
                .linkedinUrl(nullIfBlank(form.getLinkedinUrl()))
                .portfolioUrl(nullIfBlank(form.getPortfolioUrl()))
                .bio(nullIfBlank(form.getBio()))
                .skills(null)
                .cvUrl(nullIfBlank(form.getCvUrl()))
                .role(Student.Role.STUDENT)
                .build();

        Student saved = studentRepository.save(student);

        AppUser appUser = AppUser.builder()
                .username(form.getUsername().trim())
                .password(passwordEncoder.encode(form.getPassword()))
                .role(AppUser.Role.STUDENT)
                .studentId(saved.getId())
                .build();

        appUserRepository.save(appUser);
        return "redirect:/login?registered=true&role=student";
    }

    // ---------------------- TEACHER SIGNUP ----------------------

    @GetMapping("/signup/teacher")
    public String showTeacherSignup(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new TeacherSignupForm());
        }
        return "auth/signup_teacher";
    }

    @PostMapping("/signup/teacher")
    public String processTeacherSignup(@ModelAttribute("form") TeacherSignupForm form,
                                       @RequestParam(value = "photoFile", required = false) MultipartFile photoFile,
                                       Model model) {

        if (isBlank(form.getFullName()) || isBlank(form.getUsername())
                || isBlank(form.getPassword()) || isBlank(form.getEmail())
                || isBlank(form.getPhone()) || isBlank(form.getAddress())
                || isBlank(form.getDepartment()) || isBlank(form.getDesignation())) {
            return errorBack("auth/signup_teacher", "Please fill in all required fields.", form, model);
        }

        if (form.getPassword().length() < 6) {
            return errorBack("auth/signup_teacher", "Password must be at least 6 characters long.", form, model);
        }

        if (form.getConfirmPassword() != null
                && !form.getPassword().equals(form.getConfirmPassword())) {
            return errorBack("auth/signup_teacher", "Passwords do not match.", form, model);
        }

        if (appUserRepository.existsByUsername(form.getUsername())) {
            return errorBack("auth/signup_teacher", "That username is already taken. Pick another one.", form, model);
        }
        if (appUserRepository.existsByTeacherEmail(form.getEmail())) {
            return errorBack("auth/signup_teacher", "An account already exists with that email address.", form, model);
        }

        String photoPath;
        try {
            photoPath = saveUploadedPhoto(photoFile, form.getProfilePhotoUrl());
        } catch (IllegalArgumentException ex) {
            return errorBack("auth/signup_teacher", ex.getMessage(), form, model);
        } catch (IOException ex) {
            return errorBack("auth/signup_teacher", "Could not save profile photo. Please try again.", form, model);
        }

        AppUser appUser = AppUser.builder()
                .username(form.getUsername().trim())
                .password(passwordEncoder.encode(form.getPassword()))
                .role(AppUser.Role.TEACHER)
                .teacherName(form.getFullName().trim())
                .teacherEmail(form.getEmail().trim())
                .teacherPhone(form.getPhone().trim())
                .teacherDepartment(form.getDepartment().trim())
                .teacherDesignation(form.getDesignation().trim())
                .teacherAddress(form.getAddress().trim())
                .teacherOffice(nullIfBlank(form.getOfficeLocation()))
                .teacherBio(nullIfBlank(form.getBio()))
                .teacherPhotoUrl(photoPath)
                .teacherLinkedinUrl(nullIfBlank(form.getLinkedinUrl()))
                .teacherWebsite(nullIfBlank(form.getWebsiteUrl()))
                .teacherGithubUrl(nullIfBlank(form.getGithubUrl()))
                .teacherSpecialization(nullIfBlank(form.getSpecialization()))
                .build();

        appUserRepository.save(appUser);
        return "redirect:/login?registered=true&role=teacher";
    }

    // ---------------------- File upload helper ----------------------

    private String saveUploadedPhoto(MultipartFile photoFile, String fallbackUrl) throws IOException {
        return com.portfolio.tracker.util.PhotoUploadUtil.save(photoFile, fallbackUrl);
    }

    // ---------------------- Helpers ----------------------

    private String errorBack(String view, String message, Object form, Model model) {
        model.addAttribute("error", message);
        model.addAttribute("form", form);
        return view;
    }

    private boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private String nullIfBlank(String s) { return isBlank(s) ? null : s.trim(); }

    // ---------------------- Form-backing DTOs ----------------------

    public static class StudentSignupForm {
        private String fullName;
        private String username;
        private String password;
        private String confirmPassword;
        private String email;
        private String phone;
        private String address;
        private String profilePhotoUrl;
        private String githubUrl;
        private String linkedinUrl;
        private String portfolioUrl;
        private String department;
        private String batch;
        private String registrationNo;
        private String bio;
        private String skills;
        private String cvUrl;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getProfilePhotoUrl() { return profilePhotoUrl; }
        public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
        public String getGithubUrl() { return githubUrl; }
        public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }
        public String getLinkedinUrl() { return linkedinUrl; }
        public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
        public String getPortfolioUrl() { return portfolioUrl; }
        public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public String getBatch() { return batch; }
        public void setBatch(String batch) { this.batch = batch; }
        public String getRegistrationNo() { return registrationNo; }
        public void setRegistrationNo(String registrationNo) { this.registrationNo = registrationNo; }
        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
        public String getSkills() { return skills; }
        public void setSkills(String skills) { this.skills = skills; }
        public String getCvUrl() { return cvUrl; }
        public void setCvUrl(String cvUrl) { this.cvUrl = cvUrl; }
    }

    public static class TeacherSignupForm {
        private String fullName;
        private String username;
        private String password;
        private String confirmPassword;
        private String email;
        private String phone;
        private String address;
        private String profilePhotoUrl;
        private String linkedinUrl;
        private String department;
        private String designation;
        private String officeLocation;
        private String bio;
        private String websiteUrl;
        private String githubUrl;
        private String specialization;

        public String getFullName() { return fullName; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getConfirmPassword() { return confirmPassword; }
        public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getProfilePhotoUrl() { return profilePhotoUrl; }
        public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }
        public String getLinkedinUrl() { return linkedinUrl; }
        public void setLinkedinUrl(String linkedinUrl) { this.linkedinUrl = linkedinUrl; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public String getDesignation() { return designation; }
        public void setDesignation(String designation) { this.designation = designation; }
        public String getOfficeLocation() { return officeLocation; }
        public void setOfficeLocation(String officeLocation) { this.officeLocation = officeLocation; }
        public String getBio() { return bio; }
        public void setBio(String bio) { this.bio = bio; }
        public String getWebsiteUrl() { return websiteUrl; }
        public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }
        public String getGithubUrl() { return githubUrl; }
        public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }
        public String getSpecialization() { return specialization; }
        public void setSpecialization(String specialization) { this.specialization = specialization; }
    }
}
