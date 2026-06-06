package com.portfolio.tracker.controller;

import com.portfolio.tracker.entity.*;
import com.portfolio.tracker.repository.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/teacher")
public class TeacherController {

    private final AppUserRepository appUserRepository;
    private final StudentRepository studentRepository;
    private final SkillRepository skillRepository;
    private final ProjectRepository projectRepository;
    private final CertificationRepository certificationRepository;
    private final AchievementRepository achievementRepository;
    private final InternshipRepository internshipRepository;
    private final SkillMappingRepository skillMappingRepository;
    private final SkillEndorsementRepository skillEndorsementRepository;
    private final PasswordEncoder passwordEncoder;

    public TeacherController(AppUserRepository appUserRepository,
                             StudentRepository studentRepository,
                             SkillRepository skillRepository,
                             ProjectRepository projectRepository,
                             CertificationRepository certificationRepository,
                             AchievementRepository achievementRepository,
                             InternshipRepository internshipRepository,
                             SkillMappingRepository skillMappingRepository,
                             SkillEndorsementRepository skillEndorsementRepository,
                             PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.studentRepository = studentRepository;
        this.skillRepository = skillRepository;
        this.projectRepository = projectRepository;
        this.certificationRepository = certificationRepository;
        this.achievementRepository = achievementRepository;
        this.internshipRepository = internshipRepository;
        this.skillMappingRepository = skillMappingRepository;
        this.skillEndorsementRepository = skillEndorsementRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private AppUser getTeacher(UserDetails u) {
        return appUserRepository.findByUsername(u.getUsername()).orElseThrow();
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails u, Model model) {
        AppUser teacher = getTeacher(u);
        model.addAttribute("teacher", teacher);
        model.addAttribute("studentCount", studentRepository.count());
        long pendingSkillCount = skillRepository.findAll().stream()
                .filter(s -> s.getStudentId() != null && !s.isVerified()).count();
        model.addAttribute("skillCount", skillRepository.findAll().stream()
                .filter(s -> s.getStudentId() != null).count());
        model.addAttribute("pendingSkillCount", pendingSkillCount);
        model.addAttribute("projectCount", projectRepository.count());
        model.addAttribute("certCount", certificationRepository.count());
        model.addAttribute("achievementCount", achievementRepository.count());
        model.addAttribute("internshipCount", internshipRepository.count());
        model.addAttribute("recentStudents", studentRepository.findAll().stream().limit(5).toList());
        model.addAttribute("recentProjects", projectRepository.findAll().stream().limit(5).toList());
        return "teacher/dashboard";
    }

    // ========== TEACHER PROFILE ==========

    @GetMapping("/profile")
    public String teacherProfile(@AuthenticationPrincipal UserDetails u, Model model) {
        model.addAttribute("teacher", getTeacher(u));
        return "teacher/profile";
    }

    @GetMapping("/profile/edit")
    public String teacherEditProfile(@AuthenticationPrincipal UserDetails u, Model model) {
        model.addAttribute("teacher", getTeacher(u));
        return "teacher/profile-edit";
    }

    @PostMapping("/profile/update")
    public String teacherUpdateProfile(@AuthenticationPrincipal UserDetails u,
                                       @ModelAttribute AppUser form,
                                       @RequestParam(value = "photoFile", required = false) org.springframework.web.multipart.MultipartFile photoFile,
                                       @RequestParam(value = "removePhoto", required = false) String removePhoto,
                                       RedirectAttributes ra) {
        AppUser teacher = getTeacher(u);

        teacher.setTeacherName(form.getTeacherName());
        teacher.setTeacherEmail(form.getTeacherEmail());
        teacher.setTeacherPhone(form.getTeacherPhone());
        teacher.setTeacherAddress(emptyToNull(form.getTeacherAddress()));
        teacher.setTeacherDepartment(form.getTeacherDepartment());
        teacher.setTeacherDesignation(emptyToNull(form.getTeacherDesignation()));
        teacher.setTeacherOffice(emptyToNull(form.getTeacherOffice()));
        teacher.setTeacherSpecialization(emptyToNull(form.getTeacherSpecialization()));
        teacher.setTeacherBio(emptyToNull(form.getTeacherBio()));
        teacher.setTeacherLinkedinUrl(emptyToNull(form.getTeacherLinkedinUrl()));
        teacher.setTeacherWebsite(emptyToNull(form.getTeacherWebsite()));
        teacher.setTeacherGithubUrl(emptyToNull(form.getTeacherGithubUrl()));

        if ("yes".equals(removePhoto)) {
            teacher.setTeacherPhotoUrl(null);
        } else {
            try {
                String newPath = com.portfolio.tracker.util.PhotoUploadUtil.save(photoFile, form.getTeacherPhotoUrl());
                if (newPath != null) {
                    teacher.setTeacherPhotoUrl(newPath);
                }
            } catch (IllegalArgumentException ex) {
                ra.addFlashAttribute("error", ex.getMessage());
                return "redirect:/teacher/profile/edit";
            } catch (java.io.IOException ex) {
                ra.addFlashAttribute("error", "Could not save profile photo. Please try again.");
                return "redirect:/teacher/profile/edit";
            }
        }

        appUserRepository.save(teacher);
        ra.addFlashAttribute("success", "Profile updated successfully!");
        return "redirect:/teacher/profile";
    }

    private String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    // ========== STUDENT CRUD ==========
    @GetMapping("/students")
    public String students(Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("students", studentRepository.findAll());
        return "teacher/students";
    }

    @GetMapping("/students/new")
    public String newStudentForm(Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("student", new Student());
        return "teacher/student-form";
    }

    @PostMapping("/students/save")
    public String saveStudent(@ModelAttribute Student student,
                              @RequestParam(required = false) String username,
                              @RequestParam(required = false) String password,
                              RedirectAttributes ra) {
        student.setStudentId("STU-" + System.currentTimeMillis());
        studentRepository.save(student);
        if (username != null && !username.isBlank() && password != null && !password.isBlank()) {
            AppUser user = AppUser.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(AppUser.Role.STUDENT)
                .studentId(student.getId())
                .build();
            appUserRepository.save(user);
        }
        ra.addFlashAttribute("success", "Student created successfully!");
        return "redirect:/teacher/students";
    }

    @GetMapping("/students/view/{id}")
    public String viewStudent(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails u) {
        Student student = studentRepository.findById(id).orElseThrow();
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("student", student);
        model.addAttribute("projects", projectRepository.findByStudentId(id));
        model.addAttribute("certs", certificationRepository.findByStudentId(id));
        model.addAttribute("achievements", achievementRepository.findByStudentId(id));
        model.addAttribute("internships", internshipRepository.findByStudentId(id));
        model.addAttribute("skills", skillRepository.findByStudentId(id));
        return "teacher/student-view";
    }

    /** Teacher verifies a student's self-reported skill. */
    @PostMapping("/skills/verify/{id}")
    public String verifySkill(@PathVariable Long id, @RequestParam boolean verified,
                              RedirectAttributes ra) {
        skillRepository.findById(id).ifPresent(s -> {
            s.setVerified(verified);
            skillRepository.save(s);
        });
        ra.addFlashAttribute("success", verified ? "Skill verified!" : "Skill unverified.");
        return "redirect:/teacher/skills";
    }

    @GetMapping("/students/edit/{id}")
    public String editStudent(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("student", studentRepository.findById(id).orElseThrow());
        return "teacher/student-form";
    }

    @PostMapping("/students/update/{id}")
    public String updateStudent(@PathVariable Long id, @ModelAttribute Student form, RedirectAttributes ra) {
        Student s = studentRepository.findById(id).orElseThrow();
        s.setName(form.getName());
        s.setDepartment(form.getDepartment());
        s.setRegistrationNo(form.getRegistrationNo());
        s.setBatch(form.getBatch());
        s.setEmail(form.getEmail());
        s.setPhone(form.getPhone());
        s.setAddress(form.getAddress());
        studentRepository.save(s);
        ra.addFlashAttribute("success", "Student updated!");
        return "redirect:/teacher/students";
    }

    @PostMapping("/students/delete/{id}")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes ra) {
        studentRepository.deleteById(id);
        ra.addFlashAttribute("success", "Student removed.");
        return "redirect:/teacher/students";
    }

    // ========== SKILL CRUD ==========
    @GetMapping("/skills")
    public String skills(Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        List<com.portfolio.tracker.entity.Skill> allSkills = skillRepository.findAll().stream()
            .filter(s -> s.getStudentId() != null)
            .collect(Collectors.toList());
        List<com.portfolio.tracker.entity.Skill> pendingSkills = allSkills.stream()
            .filter(s -> !s.isVerified())
            .collect(Collectors.toList());
        List<com.portfolio.tracker.entity.Skill> verifiedSkills = allSkills.stream()
            .filter(s -> s.isVerified())
            .collect(Collectors.toList());
        // Build a map of studentId -> studentName for the template to use
        Map<Long, String> studentNameMap = new HashMap<>();
        studentRepository.findAll().forEach(s -> studentNameMap.put(s.getId(), s.getName()));
        model.addAttribute("pendingSkills", pendingSkills);
        model.addAttribute("verifiedSkills", verifiedSkills);
        model.addAttribute("studentNameMap", studentNameMap);
        return "teacher/skills";
    }

    @GetMapping("/skills/new")
    public String newSkillForm(Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("skill", new Skill());
        model.addAttribute("levels", Skill.Level.values());
        model.addAttribute("projects", projectRepository.findAll());
        return "teacher/skill-form";
    }

    @PostMapping("/skills/save")
    public String saveSkill(@ModelAttribute Skill skill,
                            @RequestParam(required = false) String verifiedParam,
                            @RequestParam(required = false) String linkedProjectId,
                            RedirectAttributes ra) {
        skill.setSkillId("SK-" + System.currentTimeMillis());
        skill.setVerified("yes".equalsIgnoreCase(verifiedParam));
        skill.setLinkedProjectId(linkedProjectId);
        skillRepository.save(skill);
        ra.addFlashAttribute("success", "Skill added!");
        return "redirect:/teacher/skills";
    }

    @GetMapping("/skills/edit/{id}")
    public String editSkill(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("skill", skillRepository.findById(id).orElseThrow());
        model.addAttribute("levels", Skill.Level.values());
        model.addAttribute("projects", projectRepository.findAll());
        return "teacher/skill-form";
    }

    @PostMapping("/skills/update/{id}")
    public String updateSkill(@PathVariable Long id, @ModelAttribute Skill form,
                              @RequestParam(required = false) String verifiedParam,
                              @RequestParam(required = false) String linkedProjectId,
                              RedirectAttributes ra) {
        Skill s = skillRepository.findById(id).orElseThrow();
        s.setSkillName(form.getSkillName());
        s.setCategory(form.getCategory());
        s.setLevel(form.getLevel());
        s.setLinkedCourse(form.getLinkedCourse());
        s.setVerified("yes".equalsIgnoreCase(verifiedParam));
        s.setLinkedProjectId(linkedProjectId);
        skillRepository.save(s);
        ra.addFlashAttribute("success", "Skill updated!");
        return "redirect:/teacher/skills";
    }

    @PostMapping("/skills/delete/{id}")
    public String deleteSkill(@PathVariable Long id, RedirectAttributes ra) {
        skillRepository.deleteById(id);
        ra.addFlashAttribute("success", "Skill deleted.");
        return "redirect:/teacher/skills";
    }

    // ========== PROJECT CRUD (teacher view all) ==========
    @GetMapping("/projects")
    public String projects(Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("projects", projectRepository.findAll());
        model.addAttribute("students", studentRepository.findAll());
        return "teacher/projects";
    }

    @GetMapping("/projects/new")
    public String newProjectForm(Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("project", new Project());
        model.addAttribute("students", studentRepository.findAll());
        model.addAttribute("types", Project.ProjectType.values());
        return "teacher/project-form";
    }

    @PostMapping("/projects/save")
    public String saveProject(@ModelAttribute Project project,
                              @RequestParam Long studentId, RedirectAttributes ra) {
        Student student = studentRepository.findById(studentId).orElseThrow();
        project.setStudent(student);
        project.setProjectId("PRJ-" + System.currentTimeMillis());
        projectRepository.save(project);
        ra.addFlashAttribute("success", "Project added!");
        return "redirect:/teacher/projects";
    }

    @GetMapping("/projects/edit/{id}")
    public String editProject(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("project", projectRepository.findById(id).orElseThrow());
        model.addAttribute("students", studentRepository.findAll());
        model.addAttribute("types", Project.ProjectType.values());
        return "teacher/project-form";
    }

    @PostMapping("/projects/update/{id}")
    public String updateProject(@PathVariable Long id, @ModelAttribute Project form,
                                @RequestParam Long studentId, RedirectAttributes ra) {
        Project p = projectRepository.findById(id).orElseThrow();
        p.setTitle(form.getTitle());
        p.setDescription(form.getDescription());
        p.setCourseName(form.getCourseName());
        p.setStartDate(form.getStartDate());
        p.setEndDate(form.getEndDate());
        p.setSupervisor(form.getSupervisor());
        p.setProjectType(form.getProjectType());
        p.setStudent(studentRepository.findById(studentId).orElseThrow());
        projectRepository.save(p);
        ra.addFlashAttribute("success", "Project updated!");
        return "redirect:/teacher/projects";
    }

    @PostMapping("/projects/delete/{id}")
    public String deleteProject(@PathVariable Long id, RedirectAttributes ra) {
        projectRepository.deleteById(id);
        ra.addFlashAttribute("success", "Project deleted.");
        return "redirect:/teacher/projects";
    }

    // ========== CERTIFICATIONS ==========
    @GetMapping("/certifications")
    public String certifications(Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("certs", certificationRepository.findAll());
        return "teacher/certifications";
    }

    @GetMapping("/certifications/new")
    public String newCertForm(Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("cert", new Certification());
        model.addAttribute("students", studentRepository.findAll());
        return "teacher/cert-form";
    }

    @PostMapping("/certifications/save")
    public String saveCert(@ModelAttribute Certification cert,
                           @RequestParam Long studentId,
                           @RequestParam(required = false) String verifiedParam,
                           RedirectAttributes ra) {
        cert.setStudent(studentRepository.findById(studentId).orElseThrow());
        cert.setCertificationId("CERT-" + System.currentTimeMillis());
        cert.setVerified("yes".equalsIgnoreCase(verifiedParam));
        certificationRepository.save(cert);
        ra.addFlashAttribute("success", "Certification added!");
        return "redirect:/teacher/certifications";
    }

    @GetMapping("/certifications/edit/{id}")
    public String editCert(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("cert", certificationRepository.findById(id).orElseThrow());
        model.addAttribute("students", studentRepository.findAll());
        return "teacher/cert-form";
    }

    @PostMapping("/certifications/update/{id}")
    public String updateCert(@PathVariable Long id, @ModelAttribute Certification form,
                             @RequestParam Long studentId,
                             @RequestParam(required = false) String verifiedParam,
                             RedirectAttributes ra) {
        Certification c = certificationRepository.findById(id).orElseThrow();
        c.setTitle(form.getTitle());
        c.setIssuingOrganization(form.getIssuingOrganization());
        c.setIssueDate(form.getIssueDate());
        c.setExpiryDate(form.getExpiryDate());
        c.setCertificateLink(form.getCertificateLink());
        c.setStudent(studentRepository.findById(studentId).orElseThrow());
        c.setVerified("yes".equalsIgnoreCase(verifiedParam));
        certificationRepository.save(c);
        ra.addFlashAttribute("success", "Certification updated!");
        return "redirect:/teacher/certifications";
    }

    @PostMapping("/certifications/delete/{id}")
    public String deleteCert(@PathVariable Long id, RedirectAttributes ra) {
        certificationRepository.deleteById(id);
        ra.addFlashAttribute("success", "Certification removed.");
        return "redirect:/teacher/certifications";
    }

    // Quick verify/unverify for certifications
    @PostMapping("/certifications/verify/{id}")
    public String verifyCert(@PathVariable Long id, @RequestParam boolean verified, RedirectAttributes ra) {
        Certification c = certificationRepository.findById(id).orElseThrow();
        c.setVerified(verified);
        certificationRepository.save(c);
        ra.addFlashAttribute("success", verified ? "Certification verified!" : "Verification removed.");
        return "redirect:/teacher/certifications";
    }

    // ========== ACHIEVEMENTS ==========
    @GetMapping("/achievements")
    public String achievements(Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("achievements", achievementRepository.findAll());
        return "teacher/achievements";
    }

    @GetMapping("/achievements/new")
    public String newAchievementForm(Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("achievement", new Achievement());
        model.addAttribute("students", studentRepository.findAll());
        return "teacher/achievement-form";
    }

    @PostMapping("/achievements/save")
    public String saveAchievement(@ModelAttribute Achievement achievement,
                                  @RequestParam Long studentId,
                                  @RequestParam(required = false) String verifiedParam,
                                  RedirectAttributes ra) {
        achievement.setStudent(studentRepository.findById(studentId).orElseThrow());
        achievement.setAchievementId("ACH-" + System.currentTimeMillis());
        achievement.setVerified("yes".equalsIgnoreCase(verifiedParam));
        achievementRepository.save(achievement);
        ra.addFlashAttribute("success", "Achievement added!");
        return "redirect:/teacher/achievements";
    }

    @GetMapping("/achievements/edit/{id}")
    public String editAchievement(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("achievement", achievementRepository.findById(id).orElseThrow());
        model.addAttribute("students", studentRepository.findAll());
        return "teacher/achievement-form";
    }

    @PostMapping("/achievements/update/{id}")
    public String updateAchievement(@PathVariable Long id, @ModelAttribute Achievement form,
                                    @RequestParam Long studentId,
                                    @RequestParam(required = false) String verifiedParam,
                                    RedirectAttributes ra) {
        Achievement a = achievementRepository.findById(id).orElseThrow();
        a.setEventName(form.getEventName());
        a.setPosition(form.getPosition());
        a.setOrganizer(form.getOrganizer());
        a.setDate(form.getDate());
        a.setProofLink(form.getProofLink());
        a.setStudent(studentRepository.findById(studentId).orElseThrow());
        a.setVerified("yes".equalsIgnoreCase(verifiedParam));
        achievementRepository.save(a);
        ra.addFlashAttribute("success", "Achievement updated!");
        return "redirect:/teacher/achievements";
    }

    @PostMapping("/achievements/delete/{id}")
    public String deleteAchievement(@PathVariable Long id, RedirectAttributes ra) {
        achievementRepository.deleteById(id);
        ra.addFlashAttribute("success", "Achievement deleted.");
        return "redirect:/teacher/achievements";
    }

    // Quick verify/unverify for achievements
    @PostMapping("/achievements/verify/{id}")
    public String verifyAchievement(@PathVariable Long id, @RequestParam boolean verified, RedirectAttributes ra) {
        Achievement a = achievementRepository.findById(id).orElseThrow();
        a.setVerified(verified);
        achievementRepository.save(a);
        ra.addFlashAttribute("success", verified ? "Achievement verified!" : "Verification removed.");
        return "redirect:/teacher/achievements";
    }

    // ========== INTERNSHIPS ==========
    @GetMapping("/internships")
    public String internships(Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("internships", internshipRepository.findAll());
        return "teacher/internships";
    }

    @GetMapping("/internships/new")
    public String newInternshipForm(Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("internship", new Internship());
        model.addAttribute("students", studentRepository.findAll());
        model.addAttribute("statuses", Internship.Status.values());
        return "teacher/internship-form";
    }

    @PostMapping("/internships/save")
    public String saveInternship(@ModelAttribute Internship internship,
                                 @RequestParam Long studentId, RedirectAttributes ra) {
        internship.setStudent(studentRepository.findById(studentId).orElseThrow());
        internship.setRecordId("INT-" + System.currentTimeMillis());
        internshipRepository.save(internship);
        ra.addFlashAttribute("success", "Internship added!");
        return "redirect:/teacher/internships";
    }

    @GetMapping("/internships/edit/{id}")
    public String editInternship(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("internship", internshipRepository.findById(id).orElseThrow());
        model.addAttribute("students", studentRepository.findAll());
        model.addAttribute("statuses", Internship.Status.values());
        return "teacher/internship-form";
    }

    @PostMapping("/internships/update/{id}")
    public String updateInternship(@PathVariable Long id, @ModelAttribute Internship form,
                                   @RequestParam Long studentId, RedirectAttributes ra) {
        Internship i = internshipRepository.findById(id).orElseThrow();
        i.setCompanyName(form.getCompanyName());
        i.setRole(form.getRole());
        i.setStartDate(form.getStartDate());
        i.setEndDate(form.getEndDate());
        i.setStatus(form.getStatus());
        i.setStudent(studentRepository.findById(studentId).orElseThrow());
        internshipRepository.save(i);
        ra.addFlashAttribute("success", "Internship updated!");
        return "redirect:/teacher/internships";
    }

    @PostMapping("/internships/delete/{id}")
    public String deleteInternship(@PathVariable Long id, RedirectAttributes ra) {
        internshipRepository.deleteById(id);
        ra.addFlashAttribute("success", "Internship deleted.");
        return "redirect:/teacher/internships";
    }

    // ========== SKILL MAPPINGS ==========
    @GetMapping("/skill-mappings")
    public String skillMappings(Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("mappings", skillMappingRepository.findAll());
        return "teacher/skill-mappings";
    }

    @GetMapping("/skill-mappings/new")
    public String newMappingForm(Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("mapping", new SkillMapping());
        model.addAttribute("skills", skillRepository.findAll());
        model.addAttribute("levels", SkillMapping.ProficiencyLevel.values());
        return "teacher/mapping-form";
    }

    @PostMapping("/skill-mappings/save")
    public String saveMapping(@ModelAttribute SkillMapping mapping,
                              @RequestParam Long skillId, RedirectAttributes ra) {
        mapping.setSkill(skillRepository.findById(skillId).orElseThrow());
        mapping.setMappingId("MAP-" + System.currentTimeMillis());
        skillMappingRepository.save(mapping);
        ra.addFlashAttribute("success", "Mapping added!");
        return "redirect:/teacher/skill-mappings";
    }

    @GetMapping("/skill-mappings/edit/{id}")
    public String editMapping(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("mapping", skillMappingRepository.findById(id).orElseThrow());
        model.addAttribute("skills", skillRepository.findAll());
        model.addAttribute("levels", SkillMapping.ProficiencyLevel.values());
        return "teacher/mapping-form";
    }

    @PostMapping("/skill-mappings/update/{id}")
    public String updateMapping(@PathVariable Long id, @ModelAttribute SkillMapping form,
                                @RequestParam Long skillId, RedirectAttributes ra) {
        SkillMapping m = skillMappingRepository.findById(id).orElseThrow();
        m.setSkill(skillRepository.findById(skillId).orElseThrow());
        m.setCourseOutcome(form.getCourseOutcome());
        m.setProgramOutcome(form.getProgramOutcome());
        m.setProficiencyLevel(form.getProficiencyLevel());
        skillMappingRepository.save(m);
        ra.addFlashAttribute("success", "Mapping updated!");
        return "redirect:/teacher/skill-mappings";
    }

    @PostMapping("/skill-mappings/delete/{id}")
    public String deleteMapping(@PathVariable Long id, RedirectAttributes ra) {
        skillMappingRepository.deleteById(id);
        ra.addFlashAttribute("success", "Mapping deleted.");
        return "redirect:/teacher/skill-mappings";
    }

    // ========== REPORTS ==========
    @GetMapping("/reports")
    public String reports(Model model, @AuthenticationPrincipal UserDetails u) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("students", studentRepository.findAll());
        return "teacher/reports";
    }

    @GetMapping("/reports/student/{id}")
    public String studentReport(@PathVariable Long id, Model model, @AuthenticationPrincipal UserDetails u) {
        Student student = studentRepository.findById(id).orElseThrow();
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("student", student);
        model.addAttribute("projects", projectRepository.findByStudentId(id));
        model.addAttribute("certs", certificationRepository.findByStudentId(id));
        model.addAttribute("achievements", achievementRepository.findByStudentId(id));
        model.addAttribute("internships", internshipRepository.findByStudentId(id));
        model.addAttribute("skills", skillRepository.findByStudentId(id));
        return "teacher/student-report";
    }

    // ==========================================================
    //  FEATURE 1 -- Skill Endorsement (Teacher side)
    // ==========================================================

    /** Pending + history queue for the logged-in teacher. */
    @GetMapping("/endorsements")
    public String endorsementQueue(@AuthenticationPrincipal UserDetails u, Model model) {
        AppUser teacher = getTeacher(u);
        List<SkillEndorsement> pending = skillEndorsementRepository
                .findByTeacherUserIdAndStatusOrderByRequestedAtDesc(
                        teacher.getId(), SkillEndorsement.Status.PENDING);
        List<SkillEndorsement> all = skillEndorsementRepository
                .findByTeacherUserIdOrderByRequestedAtDesc(teacher.getId());
        List<SkillEndorsement> history = all.stream()
                .filter(e -> e.getStatus() != SkillEndorsement.Status.PENDING)
                .toList();

        model.addAttribute("teacher", teacher);
        model.addAttribute("pending", pending);
        model.addAttribute("history", history);
        return "teacher/endorsements";
    }

    /** Approve an endorsement request -- flips the linked Skill to verified. */
    @PostMapping("/endorsements/{id}/approve")
    public String approveEndorsement(@PathVariable Long id,
                                     @RequestParam(required = false) String proficiency,
                                     @RequestParam(required = false) String teacherNote,
                                     @AuthenticationPrincipal UserDetails u,
                                     RedirectAttributes ra) {
        AppUser teacher = getTeacher(u);
        SkillEndorsement e = skillEndorsementRepository.findById(id).orElse(null);

        if (e == null || !e.getTeacherUserId().equals(teacher.getId())) {
            ra.addFlashAttribute("error", "Endorsement not found.");
            return "redirect:/teacher/endorsements";
        }
        if (e.getStatus() != SkillEndorsement.Status.PENDING) {
            ra.addFlashAttribute("error", "Already responded to.");
            return "redirect:/teacher/endorsements";
        }

        e.setStatus(SkillEndorsement.Status.APPROVED);
        e.setTeacherNote(teacherNote);
        e.setRespondedAt(LocalDateTime.now());
        if (proficiency != null && !proficiency.isBlank()) {
            try { e.setProficiency(SkillEndorsement.Proficiency.valueOf(proficiency)); }
            catch (IllegalArgumentException ignored) {}
        }
        skillEndorsementRepository.save(e);

        // Flip the linked skill to verified
        skillRepository.findById(e.getSkillId()).ifPresent(s -> {
            s.setVerified(true);
            skillRepository.save(s);
        });

        ra.addFlashAttribute("success", "Endorsed \"" + e.getSkillName() + "\" for " + e.getStudentName() + ".");
        return "redirect:/teacher/endorsements";
    }

    /** Decline an endorsement with a reason. */
    @PostMapping("/endorsements/{id}/decline")
    public String declineEndorsement(@PathVariable Long id,
                                     @RequestParam(required = false) String teacherNote,
                                     @AuthenticationPrincipal UserDetails u,
                                     RedirectAttributes ra) {
        AppUser teacher = getTeacher(u);
        SkillEndorsement e = skillEndorsementRepository.findById(id).orElse(null);

        if (e == null || !e.getTeacherUserId().equals(teacher.getId())) {
            ra.addFlashAttribute("error", "Endorsement not found.");
            return "redirect:/teacher/endorsements";
        }
        if (e.getStatus() != SkillEndorsement.Status.PENDING) {
            ra.addFlashAttribute("error", "Already responded to.");
            return "redirect:/teacher/endorsements";
        }
        e.setStatus(SkillEndorsement.Status.DECLINED);
        e.setTeacherNote(teacherNote);
        e.setRespondedAt(LocalDateTime.now());
        skillEndorsementRepository.save(e);

        ra.addFlashAttribute("success", "Endorsement declined.");
        return "redirect:/teacher/endorsements";
    }

    // ==========================================================
    //  FEATURE 2 -- Department Skill Analytics
    // ==========================================================

    /**
     * Industry-target skills used to surface "gaps" (skills the department under-claims
     * relative to what the market expects). Edit this list as needed.
     */
    private static final List<String> TARGET_SKILLS = List.of(
            "Docker", "Kubernetes", "AWS", "Azure", "GCP", "Cloud",
            "Machine Learning", "Deep Learning", "TensorFlow", "PyTorch",
            "React", "Node.js", "TypeScript", "GraphQL",
            "CI/CD", "DevOps", "Microservices", "Kafka",
            "System Design", "Linux", "Git", "REST API"
    );

    @GetMapping("/analytics")
    public String analytics(@AuthenticationPrincipal UserDetails u,
                            @RequestParam(value = "department", required = false) String departmentFilter,
                            @RequestParam(value = "batch", required = false) String batchFilter,
                            Model model) {

        AppUser teacher = getTeacher(u);

        // ---- Pull students (apply filters if given) ----
        List<Student> allStudents = studentRepository.findAll();
        List<Student> students = allStudents.stream()
                .filter(s -> departmentFilter == null || departmentFilter.isBlank()
                        || departmentFilter.equalsIgnoreCase(s.getDepartment()))
                .filter(s -> batchFilter == null || batchFilter.isBlank()
                        || batchFilter.equalsIgnoreCase(s.getBatch()))
                .toList();

        // Unique departments / batches for filter dropdowns
        List<String> departments = allStudents.stream()
                .map(Student::getDepartment).filter(Objects::nonNull).distinct().sorted().toList();
        List<String> batches = allStudents.stream()
                .map(Student::getBatch).filter(Objects::nonNull).distinct().sorted().toList();

        // ---- Skill frequency from student.skills (comma-separated) ----
        Map<String, Integer> skillCounts = new HashMap<>();
        int studentsWithAnySkill = 0;
        for (Student s : students) {
            if (s.getSkills() == null || s.getSkills().isBlank()) continue;
            studentsWithAnySkill++;
            for (String raw : s.getSkills().split(",")) {
                String name = raw.trim();
                if (name.isEmpty()) continue;
                String key = name.toLowerCase();
                skillCounts.merge(key, 1, Integer::sum);
            }
        }

        // Top 15 sorted by count desc
        List<Map<String, Object>> topSkills = skillCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(15)
                .map(en -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", capitalize(en.getKey()));
                    m.put("count", en.getValue());
                    m.put("percent", students.isEmpty() ? 0
                            : (int) Math.round(en.getValue() * 100.0 / students.size()));
                    return m;
                })
                .collect(Collectors.toList());

        // ---- Skill gaps: target skills not represented well ----
        List<Map<String, Object>> skillGaps = new ArrayList<>();
        for (String t : TARGET_SKILLS) {
            int count = skillCounts.getOrDefault(t.toLowerCase(), 0);
            if (students.isEmpty()) break;
            double pct = count * 100.0 / students.size();
            // Gap if fewer than 20% of students claim this skill
            if (pct < 20.0) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", t);
                m.put("count", count);
                m.put("percent", (int) Math.round(pct));
                skillGaps.add(m);
            }
        }

        // ---- Endorsement coverage ----
        List<SkillEndorsement> allEndorsements = skillEndorsementRepository.findAll();
        Set<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
        long approvedForCohort = allEndorsements.stream()
                .filter(e -> e.getStatus() == SkillEndorsement.Status.APPROVED)
                .filter(e -> studentIds.contains(e.getStudentId()))
                .count();
        long pendingForCohort = allEndorsements.stream()
                .filter(e -> e.getStatus() == SkillEndorsement.Status.PENDING)
                .filter(e -> studentIds.contains(e.getStudentId()))
                .count();
        Set<Long> studentsWithEndorsement = allEndorsements.stream()
                .filter(e -> e.getStatus() == SkillEndorsement.Status.APPROVED)
                .filter(e -> studentIds.contains(e.getStudentId()))
                .map(SkillEndorsement::getStudentId)
                .collect(Collectors.toSet());

        // ---- Portfolio completeness ----
        int completeCount = 0;
        for (Student s : students) {
            boolean hasProject  = !projectRepository.findByStudentId(s.getId()).isEmpty();
            boolean hasCert     = !certificationRepository.findByStudentId(s.getId()).isEmpty();
            boolean hasIntern   = !internshipRepository.findByStudentId(s.getId()).isEmpty();
            boolean hasEndorsed = studentsWithEndorsement.contains(s.getId());
            if (hasProject && hasCert && hasIntern && hasEndorsed) completeCount++;
        }
        int completenessPct = students.isEmpty() ? 0
                : (int) Math.round(completeCount * 100.0 / students.size());

        // ---- Top performers (by endorsement count) ----
        Map<Long, Long> endorsementsByStudent = allEndorsements.stream()
                .filter(e -> e.getStatus() == SkillEndorsement.Status.APPROVED)
                .filter(e -> studentIds.contains(e.getStudentId()))
                .collect(Collectors.groupingBy(SkillEndorsement::getStudentId, Collectors.counting()));

        List<Map<String, Object>> topPerformers = students.stream()
                .map(s -> {
                    long endorseCount = endorsementsByStudent.getOrDefault(s.getId(), 0L);
                    long projectCount = projectRepository.findByStudentId(s.getId()).size();
                    long certCount    = certificationRepository.findByStudentId(s.getId()).size();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", s.getId());
                    m.put("name", s.getName());
                    m.put("studentId", s.getStudentId());
                    m.put("department", s.getDepartment());
                    m.put("batch", s.getBatch());
                    m.put("endorsements", endorseCount);
                    m.put("projects", projectCount);
                    m.put("certs", certCount);
                    m.put("score", endorseCount * 3 + projectCount + certCount);
                    return m;
                })
                .sorted((a, b) -> Long.compare(
                        ((Number) b.get("score")).longValue(),
                        ((Number) a.get("score")).longValue()))
                .limit(10)
                .toList();

        // ---- Per-batch breakdown ----
        Map<String, Long> studentsPerBatch = students.stream()
                .filter(s -> s.getBatch() != null && !s.getBatch().isBlank())
                .collect(Collectors.groupingBy(Student::getBatch, TreeMap::new, Collectors.counting()));

        model.addAttribute("teacher", teacher);
        model.addAttribute("departments", departments);
        model.addAttribute("batches", batches);
        model.addAttribute("selectedDepartment", departmentFilter);
        model.addAttribute("selectedBatch", batchFilter);

        model.addAttribute("totalStudents", students.size());
        model.addAttribute("studentsWithAnySkill", studentsWithAnySkill);
        model.addAttribute("totalProjects", students.stream()
                .mapToInt(s -> projectRepository.findByStudentId(s.getId()).size()).sum());
        model.addAttribute("totalCerts", students.stream()
                .mapToInt(s -> certificationRepository.findByStudentId(s.getId()).size()).sum());
        model.addAttribute("approvedEndorsements", approvedForCohort);
        model.addAttribute("pendingEndorsements", pendingForCohort);

        model.addAttribute("topSkills", topSkills);
        model.addAttribute("skillGaps", skillGaps);
        model.addAttribute("topPerformers", topPerformers);
        model.addAttribute("completenessPct", completenessPct);
        model.addAttribute("completeCount", completeCount);
        model.addAttribute("studentsPerBatch", studentsPerBatch);

        return "teacher/analytics";
    }

    /** Export the current analytics cohort to CSV. */
    @GetMapping(value = "/analytics/export", produces = "text/csv")
    @ResponseBody
    public org.springframework.http.ResponseEntity<String> exportAnalyticsCsv(
            @AuthenticationPrincipal UserDetails u,
            @RequestParam(value = "department", required = false) String departmentFilter,
            @RequestParam(value = "batch", required = false) String batchFilter) {

        List<Student> students = studentRepository.findAll().stream()
                .filter(s -> departmentFilter == null || departmentFilter.isBlank()
                        || departmentFilter.equalsIgnoreCase(s.getDepartment()))
                .filter(s -> batchFilter == null || batchFilter.isBlank()
                        || batchFilter.equalsIgnoreCase(s.getBatch()))
                .toList();

        List<SkillEndorsement> allEndorsements = skillEndorsementRepository.findAll();
        Map<Long, Long> approvedByStudent = allEndorsements.stream()
                .filter(e -> e.getStatus() == SkillEndorsement.Status.APPROVED)
                .collect(Collectors.groupingBy(SkillEndorsement::getStudentId, Collectors.counting()));

        StringBuilder sb = new StringBuilder();
        sb.append("Student ID,Name,Department,Batch,Email,Skills Claimed,Projects,Certifications,Internships,Verified Skills (Endorsed)\n");
        for (Student s : students) {
            long projectCount = projectRepository.findByStudentId(s.getId()).size();
            long certCount    = certificationRepository.findByStudentId(s.getId()).size();
            long internCount  = internshipRepository.findByStudentId(s.getId()).size();
            long endorseCount = approvedByStudent.getOrDefault(s.getId(), 0L);

            int skillsClaimed = 0;
            if (s.getSkills() != null && !s.getSkills().isBlank()) {
                for (String t : s.getSkills().split(",")) if (!t.trim().isEmpty()) skillsClaimed++;
            }

            sb.append(csv(s.getStudentId())).append(',')
              .append(csv(s.getName())).append(',')
              .append(csv(s.getDepartment())).append(',')
              .append(csv(s.getBatch())).append(',')
              .append(csv(s.getEmail())).append(',')
              .append(skillsClaimed).append(',')
              .append(projectCount).append(',')
              .append(certCount).append(',')
              .append(internCount).append(',')
              .append(endorseCount).append('\n');
        }

        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"trackio-analytics.csv\"")
                .header("Content-Type", "text/csv; charset=utf-8")
                .body(sb.toString());
    }

    // ---- helpers ----

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        String[] parts = s.split("\\s+");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].isEmpty()) continue;
            b.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) b.append(parts[i].substring(1));
            if (i < parts.length - 1) b.append(' ');
        }
        return b.toString();
    }

    private static String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
