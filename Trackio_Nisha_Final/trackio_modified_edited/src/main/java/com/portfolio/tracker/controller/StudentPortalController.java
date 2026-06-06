package com.portfolio.tracker.controller;

import com.portfolio.tracker.entity.*;
import com.portfolio.tracker.repository.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.ArrayList;
import java.time.LocalDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Controller
@RequestMapping("/student")

public class StudentPortalController {

    private final AppUserRepository appUserRepository;
    private final StudentRepository studentRepository;
    private final ProjectRepository projectRepository;
    private final CertificationRepository certificationRepository;
    private final AchievementRepository achievementRepository;
    private final InternshipRepository internshipRepository;
    private final SkillRepository skillRepository;
    private final SkillEndorsementRepository skillEndorsementRepository;
    private final CourseRepository courseRepository;
    private final CourseOutcomeRepository courseOutcomeRepository;

    public StudentPortalController(AppUserRepository appUserRepository,
                                   StudentRepository studentRepository,
                                   ProjectRepository projectRepository,
                                   CertificationRepository certificationRepository,
                                   AchievementRepository achievementRepository,
                                   InternshipRepository internshipRepository,
                                   SkillRepository skillRepository,
                                   SkillEndorsementRepository skillEndorsementRepository,
                                   CourseRepository courseRepository,
                                   CourseOutcomeRepository courseOutcomeRepository) {
        this.appUserRepository = appUserRepository;
        this.studentRepository = studentRepository;
        this.projectRepository = projectRepository;
        this.certificationRepository = certificationRepository;
        this.achievementRepository = achievementRepository;
        this.internshipRepository = internshipRepository;
        this.skillRepository = skillRepository;
        this.skillEndorsementRepository = skillEndorsementRepository;
        this.courseRepository = courseRepository;
        this.courseOutcomeRepository = courseOutcomeRepository;
    }

    private Student getCurrentStudent(UserDetails userDetails) {
        AppUser user = appUserRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        return studentRepository.findById(user.getStudentId()).orElseThrow();
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getCurrentStudent(userDetails);
        Long sid = student.getId();
        model.addAttribute("student", student);
        model.addAttribute("projectCount", projectRepository.findByStudentId(sid).size());
        model.addAttribute("certCount", certificationRepository.findByStudentId(sid).size());
        model.addAttribute("achievementCount", achievementRepository.findByStudentId(sid).size());
        model.addAttribute("internshipCount", internshipRepository.findByStudentId(sid).size());
        model.addAttribute("recentProjects", projectRepository.findByStudentId(sid).stream().limit(3).toList());
        model.addAttribute("recentCerts", certificationRepository.findByStudentId(sid).stream().limit(3).toList());
        return "student/dashboard";
    }

    // ---- Profile ----
    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getCurrentStudent(userDetails);
        model.addAttribute("student", student);
        return "student/profile";
    }

    @PostMapping("/profile/update")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                @ModelAttribute Student form,
                                @RequestParam(value = "photoFile", required = false) org.springframework.web.multipart.MultipartFile photoFile,
                                @RequestParam(value = "removePhoto", required = false) String removePhoto,
                                RedirectAttributes ra) {
        Student student = getCurrentStudent(userDetails);

        // Basic / contact
        student.setName(form.getName());
        student.setEmail(form.getEmail());
        student.setPhone(form.getPhone());
        student.setAddress(form.getAddress());

        // Portfolio links
        student.setGithubUrl(emptyToNull(form.getGithubUrl()));
        student.setLinkedinUrl(emptyToNull(form.getLinkedinUrl()));
        student.setPortfolioUrl(emptyToNull(form.getPortfolioUrl()));
        student.setCvUrl(emptyToNull(form.getCvUrl()));

        // Profile content
        student.setBio(emptyToNull(form.getBio()));
        student.setSkills(emptyToNull(form.getSkills()));

        // Photo handling -- three cases:
        //   1. user clicked "Remove" -> clear it
        //   2. user uploaded a new file -> save and replace
        //   3. user pasted a URL (form.profilePhotoUrl) -> keep that
        //   4. nothing -> keep existing
        if ("yes".equals(removePhoto)) {
            student.setProfilePhotoUrl(null);
        } else {
            try {
                String newPath = com.portfolio.tracker.util.PhotoUploadUtil.save(photoFile, form.getProfilePhotoUrl());
                if (newPath != null) {
                    student.setProfilePhotoUrl(newPath);
                }
            } catch (IllegalArgumentException ex) {
                ra.addFlashAttribute("error", ex.getMessage());
                return "redirect:/student/profile/edit";
            } catch (java.io.IOException ex) {
                ra.addFlashAttribute("error", "Could not save profile photo. Please try again.");
                return "redirect:/student/profile/edit";
            }
        }

        studentRepository.save(student);
        ra.addFlashAttribute("success", "Profile updated successfully!");
        return "redirect:/student/profile";
    }

    private String emptyToNull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    // ============================================================
    //  CV generation
    // ============================================================

    @GetMapping("/cv")
    public String cvHub(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getCurrentStudent(userDetails);
        Long sid = student.getId();
        model.addAttribute("student", student);
        model.addAttribute("projectCount", projectRepository.findByStudentId(sid).size());
        model.addAttribute("certCount", certificationRepository.findByStudentId(sid).size());
        model.addAttribute("internshipCount", internshipRepository.findByStudentId(sid).size());
        model.addAttribute("achievementCount", achievementRepository.findByStudentId(sid).size());
        return "student/cv";
    }

    @PostMapping("/cv/generate")
    public String generateCv(@AuthenticationPrincipal UserDetails userDetails,
                             @RequestParam(value = "mode", defaultValue = "deterministic") String mode,
                             @RequestParam(value = "targetRole", required = false) String targetRole,
                             @RequestParam(value = "targetJobDescription", required = false) String targetJobDescription,
                             Model model,
                             RedirectAttributes ra) {

        Student student = getCurrentStudent(userDetails);
        Long sid = student.getId();
        java.util.List<Project> projects = projectRepository.findByStudentId(sid);
        java.util.List<Certification> certs = certificationRepository.findByStudentId(sid);
        java.util.List<Internship> internships = internshipRepository.findByStudentId(sid);
        java.util.List<Achievement> achievements = achievementRepository.findByStudentId(sid);

        model.addAttribute("student", student);
        model.addAttribute("targetRole", targetRole);
        model.addAttribute("targetJobDescription", targetJobDescription);
        model.addAttribute("mode", mode);

        if ("ai".equalsIgnoreCase(mode)) {
            String apiKey = System.getenv("ANTHROPIC_API_KEY");
            try {
                String aiText = com.portfolio.tracker.util.CvGenerator.buildWithClaude(
                    student, projects, certs, internships, achievements,
                    targetRole, targetJobDescription, apiKey);
                model.addAttribute("aiText", aiText);
                model.addAttribute("cv", null);
                return "student/cv-preview";
            } catch (Exception ex) {
                ra.addFlashAttribute("error",
                    "AI generation encountered an issue: " + ex.getMessage()
                    + " \u2014 falling back to standard CV mode.");
                return "redirect:/student/cv";
            }
        }

        // Deterministic mode
        com.portfolio.tracker.util.CvGenerator.CvData cv =
            com.portfolio.tracker.util.CvGenerator.buildDeterministic(
                student, projects, certs, internships, achievements, targetRole);

        model.addAttribute("cv", cv);
        return "student/cv-preview";
    }

    @GetMapping("/profile/edit")
    public String editProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getCurrentStudent(userDetails);
        model.addAttribute("student", student);
        return "student/profile-edit";
    }

    // ---- Projects ----
    @GetMapping("/projects")
    public String projects(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getCurrentStudent(userDetails);
        model.addAttribute("student", student);
        model.addAttribute("projects", projectRepository.findByStudentId(student.getId()));
        return "student/projects";
    }

    @GetMapping("/projects/new")
    public String newProjectForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getCurrentStudent(userDetails);
        model.addAttribute("student", student);
        model.addAttribute("project", new Project());
        model.addAttribute("types", Project.ProjectType.values());
        model.addAttribute("courses", courseRepository.findAllByOrderByCourseCodeAsc());
        model.addAttribute("allCos", courseOutcomeRepository.findAll());
        model.addAttribute("selectedCoIds", new java.util.ArrayList<Long>());
        return "student/project-form";
    }

    @PostMapping("/projects/save")
    public String saveProject(@AuthenticationPrincipal UserDetails userDetails,
                              @ModelAttribute Project project,
                              @RequestParam(value = "selectedCoIds", required = false) List<Long> selectedCoIds,
                              RedirectAttributes ra) {
        Student student = getCurrentStudent(userDetails);
        project.setStudent(student);
        project.setProjectId("PRJ-" + System.currentTimeMillis());
        if (selectedCoIds != null && !selectedCoIds.isEmpty()) {
            project.setCourseOutcomeIds(selectedCoIds.stream()
                    .map(String::valueOf).collect(java.util.stream.Collectors.joining(",")));
        }
        projectRepository.save(project);
        ra.addFlashAttribute("success", "Project saved" +
                (selectedCoIds != null && !selectedCoIds.isEmpty()
                        ? " and tagged to " + selectedCoIds.size() + " course outcome(s)."
                        : "."));
        return "redirect:/student/projects";
    }

    @GetMapping("/projects/edit/{id}")
    public String editProject(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getCurrentStudent(userDetails);
        Project project = projectRepository.findById(id).orElseThrow();
        model.addAttribute("student", student);
        model.addAttribute("project", project);
        model.addAttribute("types", Project.ProjectType.values());
        model.addAttribute("courses", courseRepository.findAllByOrderByCourseCodeAsc());
        model.addAttribute("allCos", courseOutcomeRepository.findAll());

        // Parse the existing CO IDs into a List<Long> for the template to compare against
        List<Long> selectedCoIds = new java.util.ArrayList<>();
        if (project.getCourseOutcomeIds() != null && !project.getCourseOutcomeIds().isBlank()) {
            for (String tok : project.getCourseOutcomeIds().split(",")) {
                try { selectedCoIds.add(Long.parseLong(tok.trim())); } catch (NumberFormatException ignored) {}
            }
        }
        model.addAttribute("selectedCoIds", selectedCoIds);
        return "student/project-form";
    }

    @PostMapping("/projects/update/{id}")
    public String updateProject(@PathVariable Long id, @ModelAttribute Project form,
                                @RequestParam(value = "selectedCoIds", required = false) List<Long> selectedCoIds,
                                @AuthenticationPrincipal UserDetails userDetails, RedirectAttributes ra) {
        Project project = projectRepository.findById(id).orElseThrow();
        project.setTitle(form.getTitle());
        project.setDescription(form.getDescription());
        project.setCourseName(form.getCourseName());
        project.setStartDate(form.getStartDate());
        project.setEndDate(form.getEndDate());
        project.setSupervisor(form.getSupervisor());
        project.setProjectType(form.getProjectType());
        project.setCourseId(form.getCourseId());

        if (selectedCoIds != null && !selectedCoIds.isEmpty()) {
            project.setCourseOutcomeIds(selectedCoIds.stream()
                    .map(String::valueOf).collect(java.util.stream.Collectors.joining(",")));
        } else {
            project.setCourseOutcomeIds(null);
        }
        projectRepository.save(project);
        ra.addFlashAttribute("success", "Project updated!");
        return "redirect:/student/projects";
    }

    @PostMapping("/projects/delete/{id}")
    public String deleteProject(@PathVariable Long id, RedirectAttributes ra) {
        projectRepository.deleteById(id);
        ra.addFlashAttribute("success", "Project deleted.");
        return "redirect:/student/projects";
    }

    // ---- Certifications ----
    @GetMapping("/certifications")
    public String certifications(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getCurrentStudent(userDetails);
        model.addAttribute("student", student);
        model.addAttribute("certs", certificationRepository.findByStudentId(student.getId()));
        return "student/certifications";
    }

    @GetMapping("/certifications/new")
    public String newCertForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("student", getCurrentStudent(userDetails));
        model.addAttribute("cert", new Certification());
        return "student/cert-form";
    }

    @PostMapping("/certifications/save")
    public String saveCert(@AuthenticationPrincipal UserDetails userDetails,
                           @ModelAttribute Certification cert, RedirectAttributes ra) {
        Student student = getCurrentStudent(userDetails);
        cert.setStudent(student);
        cert.setCertificationId("CERT-" + System.currentTimeMillis());
        certificationRepository.save(cert);
        ra.addFlashAttribute("success", "Certification added!");
        return "redirect:/student/certifications";
    }

    @GetMapping("/certifications/edit/{id}")
    public String editCert(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("student", getCurrentStudent(userDetails));
        model.addAttribute("cert", certificationRepository.findById(id).orElseThrow());
        return "student/cert-form";
    }

    @PostMapping("/certifications/update/{id}")
    public String updateCert(@PathVariable Long id, @ModelAttribute Certification form, RedirectAttributes ra) {
        Certification cert = certificationRepository.findById(id).orElseThrow();
        cert.setTitle(form.getTitle());
        cert.setIssuingOrganization(form.getIssuingOrganization());
        cert.setIssueDate(form.getIssueDate());
        cert.setExpiryDate(form.getExpiryDate());
        cert.setCertificateLink(form.getCertificateLink());
        certificationRepository.save(cert);
        ra.addFlashAttribute("success", "Certification updated!");
        return "redirect:/student/certifications";
    }

    @PostMapping("/certifications/delete/{id}")
    public String deleteCert(@PathVariable Long id, RedirectAttributes ra) {
        certificationRepository.deleteById(id);
        ra.addFlashAttribute("success", "Certification removed.");
        return "redirect:/student/certifications";
    }

    // ---- Achievements ----
    @GetMapping("/achievements")
    public String achievements(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getCurrentStudent(userDetails);
        model.addAttribute("student", student);
        model.addAttribute("achievements", achievementRepository.findByStudentId(student.getId()));
        return "student/achievements";
    }

    @GetMapping("/achievements/new")
    public String newAchievementForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("student", getCurrentStudent(userDetails));
        model.addAttribute("achievement", new Achievement());
        return "student/achievement-form";
    }

    @PostMapping("/achievements/save")
    public String saveAchievement(@AuthenticationPrincipal UserDetails userDetails,
                                  @ModelAttribute Achievement achievement, RedirectAttributes ra) {
        Student student = getCurrentStudent(userDetails);
        achievement.setStudent(student);
        achievement.setAchievementId("ACH-" + System.currentTimeMillis());
        achievementRepository.save(achievement);
        ra.addFlashAttribute("success", "Achievement added!");
        return "redirect:/student/achievements";
    }

    @GetMapping("/achievements/edit/{id}")
    public String editAchievement(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("student", getCurrentStudent(userDetails));
        model.addAttribute("achievement", achievementRepository.findById(id).orElseThrow());
        return "student/achievement-form";
    }

    @PostMapping("/achievements/update/{id}")
    public String updateAchievement(@PathVariable Long id, @ModelAttribute Achievement form, RedirectAttributes ra) {
        Achievement a = achievementRepository.findById(id).orElseThrow();
        a.setEventName(form.getEventName());
        a.setPosition(form.getPosition());
        a.setOrganizer(form.getOrganizer());
        a.setDate(form.getDate());
        a.setProofLink(form.getProofLink());
        achievementRepository.save(a);
        ra.addFlashAttribute("success", "Achievement updated!");
        return "redirect:/student/achievements";
    }

    @PostMapping("/achievements/delete/{id}")
    public String deleteAchievement(@PathVariable Long id, RedirectAttributes ra) {
        achievementRepository.deleteById(id);
        ra.addFlashAttribute("success", "Achievement removed.");
        return "redirect:/student/achievements";
    }

    // ---- Internships ----
    @GetMapping("/internships")
    public String internships(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getCurrentStudent(userDetails);
        model.addAttribute("student", student);
        model.addAttribute("internships", internshipRepository.findByStudentId(student.getId()));
        model.addAttribute("statuses", Internship.Status.values());
        return "student/internships";
    }

    @GetMapping("/internships/new")
    public String newInternshipForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("student", getCurrentStudent(userDetails));
        model.addAttribute("internship", new Internship());
        model.addAttribute("statuses", Internship.Status.values());
        return "student/internship-form";
    }

    @PostMapping("/internships/save")
    public String saveInternship(@AuthenticationPrincipal UserDetails userDetails,
                                 @ModelAttribute Internship internship, RedirectAttributes ra) {
        Student student = getCurrentStudent(userDetails);
        internship.setStudent(student);
        internship.setRecordId("INT-" + System.currentTimeMillis());
        internshipRepository.save(internship);
        ra.addFlashAttribute("success", "Internship record added!");
        return "redirect:/student/internships";
    }

    @GetMapping("/internships/edit/{id}")
    public String editInternship(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("student", getCurrentStudent(userDetails));
        model.addAttribute("internship", internshipRepository.findById(id).orElseThrow());
        model.addAttribute("statuses", Internship.Status.values());
        return "student/internship-form";
    }

    @PostMapping("/internships/update/{id}")
    public String updateInternship(@PathVariable Long id, @ModelAttribute Internship form, RedirectAttributes ra) {
        Internship i = internshipRepository.findById(id).orElseThrow();
        i.setCompanyName(form.getCompanyName());
        i.setRole(form.getRole());
        i.setStartDate(form.getStartDate());
        i.setEndDate(form.getEndDate());
        i.setStatus(form.getStatus());
        internshipRepository.save(i);
        ra.addFlashAttribute("success", "Internship updated!");
        return "redirect:/student/internships";
    }

    @PostMapping("/internships/delete/{id}")
    public String deleteInternship(@PathVariable Long id, RedirectAttributes ra) {
        internshipRepository.deleteById(id);
        ra.addFlashAttribute("success", "Record removed.");
        return "redirect:/student/internships";
    }

    // ---- Skills (Student CRUD - read-only verified, can add own skills) ----
    @GetMapping("/skills")
    public String skills(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getCurrentStudent(userDetails);
        model.addAttribute("student", student);
        model.addAttribute("skills", skillRepository.findByStudentId(student.getId()));
        return "student/skills";
    }

    @GetMapping("/skills/new")
    public String newSkillForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("student", getCurrentStudent(userDetails));
        model.addAttribute("skill", new Skill());
        model.addAttribute("levels", Skill.Level.values());
        model.addAttribute("projects", projectRepository.findByStudentId(getCurrentStudent(userDetails).getId()));
        return "student/skill-form";
    }

    @PostMapping("/skills/save")
    public String saveSkill(@AuthenticationPrincipal UserDetails userDetails,
                            @ModelAttribute Skill skill,
                            @RequestParam(required = false) String linkedProjectId,
                            RedirectAttributes ra) {
        Student student = getCurrentStudent(userDetails);
        skill.setSkillId("SK-" + System.currentTimeMillis());
        skill.setVerified(false); // Students cannot self-verify; teacher verifies
        skill.setLinkedProjectId(linkedProjectId);
        skill.setStudentId(student.getId());
        skillRepository.save(skill);
        ra.addFlashAttribute("success", "Skill added! A teacher will verify it.");
        return "redirect:/student/skills";
    }

    @GetMapping("/skills/edit/{id}")
    public String editSkill(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails, Model model) {
        Skill skill = skillRepository.findById(id).orElseThrow();
        // Prevent editing verified skills
        if (skill.isVerified()) {
            return "redirect:/student/skills";
        }
        model.addAttribute("student", getCurrentStudent(userDetails));
        model.addAttribute("skill", skill);
        model.addAttribute("levels", Skill.Level.values());
        model.addAttribute("projects", projectRepository.findByStudentId(getCurrentStudent(userDetails).getId()));
        return "student/skill-form";
    }

    @PostMapping("/skills/update/{id}")
    public String updateSkill(@PathVariable Long id, @ModelAttribute Skill form,
                              @RequestParam(required = false) String linkedProjectId,
                              RedirectAttributes ra) {
        Skill s = skillRepository.findById(id).orElseThrow();
        if (s.isVerified()) {
            ra.addFlashAttribute("error", "Cannot edit a verified skill.");
            return "redirect:/student/skills";
        }
        s.setSkillName(form.getSkillName());
        s.setCategory(form.getCategory());
        s.setLevel(form.getLevel());
        s.setLinkedCourse(form.getLinkedCourse());
        s.setLinkedProjectId(linkedProjectId);
        skillRepository.save(s);
        ra.addFlashAttribute("success", "Skill updated!");
        return "redirect:/student/skills";
    }

    @PostMapping("/skills/delete/{id}")
    public String deleteSkill(@PathVariable Long id, RedirectAttributes ra) {
        Skill s = skillRepository.findById(id).orElseThrow();
        if (s.isVerified()) {
            ra.addFlashAttribute("error", "Cannot delete a verified skill.");
            return "redirect:/student/skills";
        }
        skillRepository.deleteById(id);
        ra.addFlashAttribute("success", "Skill removed.");
        return "redirect:/student/skills";
    }

    // ---- Report ----
    @GetMapping("/report")
    public String report(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getCurrentStudent(userDetails);
        Long sid = student.getId();
        model.addAttribute("student", student);
        model.addAttribute("projects", projectRepository.findByStudentId(sid));
        model.addAttribute("certs", certificationRepository.findByStudentId(sid));
        model.addAttribute("achievements", achievementRepository.findByStudentId(sid));
        model.addAttribute("internships", internshipRepository.findByStudentId(sid));
        model.addAttribute("skills", skillRepository.findAll());
        model.addAttribute("endorsements",
                skillEndorsementRepository.findByStudentIdOrderByRequestedAtDesc(sid));
        return "student/report";
    }

    // ==========================================================
    //  FEATURE 1 -- Skill Endorsement (Student side)
    // ==========================================================

    /** "My Endorsements" page -- see all my requests and their status. */
    @GetMapping("/endorsements")
    public String myEndorsements(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getCurrentStudent(userDetails);
        model.addAttribute("student", student);
        model.addAttribute("endorsements",
                skillEndorsementRepository.findByStudentIdOrderByRequestedAtDesc(student.getId()));
        return "student/endorsements";
    }

    /** Form to request an endorsement for a specific skill. */
    @GetMapping("/skills/{skillId}/request-endorsement")
    public String requestEndorsementForm(@PathVariable Long skillId,
                                         @AuthenticationPrincipal UserDetails userDetails,
                                         Model model) {
        Student student = getCurrentStudent(userDetails);
        Skill skill = skillRepository.findById(skillId).orElseThrow();

        // Only show teachers (so students can pick who to ask)
        List<AppUser> teachers = appUserRepository.findAll().stream()
                .filter(u -> u.getRole() == AppUser.Role.TEACHER)
                .toList();

        model.addAttribute("student", student);
        model.addAttribute("skill", skill);
        model.addAttribute("teachers", teachers);
        model.addAttribute("projects", projectRepository.findByStudentId(student.getId()));
        return "student/endorsement-request";
    }

    /** Save the endorsement request. */
    @PostMapping("/skills/{skillId}/request-endorsement")
    public String submitEndorsementRequest(@PathVariable Long skillId,
                                           @RequestParam Long teacherUserId,
                                           @RequestParam(required = false) String studentNote,
                                           @RequestParam(required = false) String evidenceProjectId,
                                           @AuthenticationPrincipal UserDetails userDetails,
                                           RedirectAttributes ra) {
        Student student = getCurrentStudent(userDetails);
        Skill skill = skillRepository.findById(skillId).orElseThrow();

        // Look up teacher
        AppUser teacher = appUserRepository.findById(teacherUserId).orElse(null);
        if (teacher == null || teacher.getRole() != AppUser.Role.TEACHER) {
            ra.addFlashAttribute("error", "Selected teacher not found.");
            return "redirect:/student/skills";
        }

        // Prevent duplicate pending request to same teacher for same skill
        boolean alreadyPending = skillEndorsementRepository
                .existsBySkillIdAndTeacherUserIdAndStatus(skillId, teacherUserId,
                        SkillEndorsement.Status.PENDING);
        if (alreadyPending) {
            ra.addFlashAttribute("error", "You already have a pending request to this teacher for this skill.");
            return "redirect:/student/endorsements";
        }

        SkillEndorsement e = new SkillEndorsement();
        e.setSkillId(skillId);
        e.setStudentId(student.getId());
        e.setTeacherUserId(teacherUserId);
        e.setStudentName(student.getName());
        e.setTeacherName(teacher.getTeacherName());
        e.setSkillName(skill.getSkillName());
        e.setStudentNote(studentNote);
        e.setEvidenceProjectId(evidenceProjectId);
        e.setStatus(SkillEndorsement.Status.PENDING);
        e.setRequestedAt(LocalDateTime.now());
        skillEndorsementRepository.save(e);

        ra.addFlashAttribute("success", "Endorsement requested. " + teacher.getTeacherName()
                + " will review it.");
        return "redirect:/student/endorsements";
    }

    /** Cancel/withdraw a pending request. */
    @PostMapping("/endorsements/{id}/withdraw")
    public String withdrawEndorsement(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      RedirectAttributes ra) {
        Student student = getCurrentStudent(userDetails);
        SkillEndorsement e = skillEndorsementRepository.findById(id).orElse(null);
        if (e == null || !e.getStudentId().equals(student.getId())) {
            ra.addFlashAttribute("error", "Endorsement not found.");
            return "redirect:/student/endorsements";
        }
        if (e.getStatus() != SkillEndorsement.Status.PENDING) {
            ra.addFlashAttribute("error", "Only pending requests can be withdrawn.");
            return "redirect:/student/endorsements";
        }
        skillEndorsementRepository.deleteById(id);
        ra.addFlashAttribute("success", "Request withdrawn.");
        return "redirect:/student/endorsements";
    }

    // ================================================================
    //  AI PROJECT ADVISOR CHATBOT  (upgraded — streaming + mentor mode)
    // ================================================================

    @GetMapping("/ai/duplicate-checker")
    public String aiChatbotPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getCurrentStudent(userDetails);
        model.addAttribute("student", student);
        return "student/duplicate-checker";
    }

    /**
     * STREAMING endpoint — returns Server-Sent Events so the frontend can
     * display the AI response token-by-token (ChatGPT-style).
     */
    @PostMapping(value = "/ai/duplicate-check-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> aiProjectStream(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody java.util.Map<String, String> body) {

        String projectName  = body.getOrDefault("projectName", "").trim();
        String projectTitle = body.getOrDefault("projectTitle", "").trim();
        String projectIdea  = body.getOrDefault("projectIdea", "").trim();

        // ── Detect input intent before doing anything ─────────────────────────
        String combinedInput = (projectName + " " + projectTitle + " " + projectIdea).trim();
        InputIntent intent   = detectIntent(combinedInput);

        // For greetings and vague inputs, stream a warm conversational reply — no DB lookup
        if (intent == InputIntent.GREETING || intent == InputIntent.VAGUE) {
            String conversationalReply = buildConversationalReply(intent, combinedInput);
            StreamingResponseBody convBody = out -> {
                try {
                    streamTextWordByWord(conversationalReply, out);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                out.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
            };
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(convBody);
        }

        // ── From here: input is a real project idea ───────────────────────────
        List<Project> allProjects = projectRepository.findAll();
        String corpus = buildCorpus(allProjects);
        String systemPrompt = buildMentorSystemPrompt();
        String userMessage  = buildUserMessage(projectName, projectTitle, projectIdea, corpus);
        String apiKey = System.getenv("ANTHROPIC_API_KEY");

        StreamingResponseBody streamBody = out -> {
            try {
                if (apiKey == null || apiKey.isBlank()) {
                    // Fallback: stream the rule-based response word-by-word
                    String fallback = ruleBasedAdvisor(projectName, projectTitle, projectIdea, allProjects);
                    streamTextWordByWord(fallback, out);
                    out.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                    out.flush();
                    return;
                }

                // Build streaming request to Claude
                String requestJson =
                    "{\"model\":\"claude-sonnet-4-20250514\"," +
                    "\"max_tokens\":2000," +
                    "\"stream\":true," +
                    "\"system\":" + toJsonString(systemPrompt) + "," +
                    "\"messages\":[{\"role\":\"user\",\"content\":" + toJsonString(userMessage) + "}]}";

                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.anthropic.com/v1/messages"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

                java.net.http.HttpResponse<java.io.InputStream> response =
                    httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofInputStream());

                try (java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String data = line.substring(6).trim();
                            if (data.equals("[DONE]")) {
                                out.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                                out.flush();
                                break;
                            }
                            // Extract delta text from SSE JSON
                            String delta = extractStreamDelta(data);
                            if (delta != null && !delta.isEmpty()) {
                                // Forward as SSE event
                                String sseEvent = "data: " + toJsonString(delta) + "\n\n";
                                out.write(sseEvent.getBytes(StandardCharsets.UTF_8));
                                out.flush();
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                try {
                    // Fallback to rule-based on any error
                    String fallback = ruleBasedAdvisor(projectName, projectTitle, projectIdea, allProjects);
                    streamTextWordByWord(fallback, out);
                    out.write("data: [DONE]\n\n".getBytes(StandardCharsets.UTF_8));
                    out.flush();
                } catch (Exception ignored) {}
            }
        };

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(streamBody);
    }

    /**
     * Non-streaming fallback endpoint (kept for compatibility).
     */
    @PostMapping(value = "/ai/duplicate-check", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> aiProjectChat(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody java.util.Map<String, String> body) {

        String projectName  = body.getOrDefault("projectName", "").trim();
        String projectTitle = body.getOrDefault("projectTitle", "").trim();
        String projectIdea  = body.getOrDefault("projectIdea", "").trim();

        if (projectName.isEmpty() && projectTitle.isEmpty() && projectIdea.isEmpty()) {
            return ResponseEntity.badRequest()
                .body("{\"error\":\"Please provide at least your project name or idea.\"}");
        }

        List<Project> allProjects = projectRepository.findAll();
        String corpus = buildCorpus(allProjects);
        String apiKey = System.getenv("ANTHROPIC_API_KEY");

        if (apiKey == null || apiKey.isBlank()) {
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"reply\":\"" + jsonEscape(ruleBasedAdvisor(projectName, projectTitle, projectIdea, allProjects)) + "\"}");
        }

        String systemPrompt = buildMentorSystemPrompt();
        String userMessage  = buildUserMessage(projectName, projectTitle, projectIdea, corpus);

        try {
            String requestJson =
                "{\"model\":\"claude-sonnet-4-20250514\"," +
                "\"max_tokens\":2000," +
                "\"system\":" + toJsonString(systemPrompt) + "," +
                "\"messages\":[{\"role\":\"user\",\"content\":" + toJsonString(userMessage) + "}]}";

            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.anthropic.com/v1/messages"))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

            java.net.http.HttpResponse<String> response =
                httpClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

            String aiReply = extractClaudeText(response.body());
            if (aiReply == null || aiReply.isBlank()) aiReply = "I encountered an issue generating the analysis. Please try again.";

            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"reply\":" + toJsonString(aiReply) + "}");

        } catch (Exception ex) {
            String fallback = ruleBasedAdvisor(projectName, projectTitle, projectIdea, allProjects);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"reply\":" + toJsonString(fallback) + ",\"fallback\":true}");
        }
    }


    // ================================================================
    //  AI JOB FINDER  (skill-based job matching with gap analysis)
    // ================================================================

    /** Job role data — company → list of roles, each with required skills */
    private static final java.util.Map<String, java.util.Map<String, List<String>>> JOB_DATA = new java.util.LinkedHashMap<>();
    static {
        java.util.Map<String, List<String>> cefalo = new java.util.LinkedHashMap<>();
        cefalo.put("Software Engineer",      List.of("Java","Spring Boot","GitHub","REST API","MySQL"));
        cefalo.put("DevOps Engineer",        List.of("Java","Spring Boot","Python","DevOps"));
        cefalo.put("Database Engineer",      List.of("HTML","CSS","MySQL","GitHub"));
        JOB_DATA.put("Cefalo", cefalo);

        java.util.Map<String, List<String>> brain = new java.util.LinkedHashMap<>();
        brain.put("AI Engineer",     List.of("Python","NumPy","Pandas"));
        brain.put("DevOps Engineer", List.of("Python","Docker"));
        brain.put("Web Engineer",    List.of("Python","HTML","CSS"));
        JOB_DATA.put("BrainStation23", brain);

        java.util.Map<String, List<String>> enosis = new java.util.LinkedHashMap<>();
        enosis.put("Software Engineer", List.of("Java","Python","Spring Boot","GitHub"));
        enosis.put("DBA",               List.of("MySQL","GitHub"));
        enosis.put("AI Engineer",       List.of("Python","NumPy","Pandas","Machine Learning"));
        JOB_DATA.put("Enosis", enosis);
    }

    @GetMapping("/ai/job-finder")
    public String jobFinderPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        Student student = getCurrentStudent(userDetails);
        List<Skill> skills = skillRepository.findByStudentId(student.getId());
        List<String> skillNames = skills.stream()
            .map(Skill::getSkillName)
            .toList();
        model.addAttribute("student", student);
        model.addAttribute("studentSkills", skillNames);
        return "student/job-finder";
    }

    @PostMapping(value = "/ai/job-finder-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public ResponseEntity<StreamingResponseBody> jobFinderStream(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody java.util.Map<String, String> body) {

        Student student = getCurrentStudent(userDetails);
        List<Skill> skills = skillRepository.findByStudentId(student.getId());
        List<String> studentSkills = skills.stream()
            .map(Skill::getSkillName)
            .map(s -> s.toLowerCase().trim())
            .toList();

        String searchRole    = body.getOrDefault("searchRole", "").trim();
        String searchCompany = body.getOrDefault("searchCompany", "").trim();

        String apiKey = System.getenv("ANTHROPIC_API_KEY");

        StreamingResponseBody streamBody = out -> {
            try {
                String report;
                if (apiKey == null || apiKey.isBlank()) {
                    report = buildJobMatchReport(student.getName(), studentSkills, searchRole, searchCompany);
                } else {
                    String aiReport = callJobFinderAI(student.getName(), studentSkills, searchRole, searchCompany, apiKey);
                    report = (aiReport != null) ? aiReport
                           : buildJobMatchReport(student.getName(), studentSkills, searchRole, searchCompany);
                }
                streamTextWordByWord(report, out);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            out.write("data: [DONE]\n\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.flush();
        };

        return ResponseEntity.ok()
            .contentType(MediaType.TEXT_EVENT_STREAM)
            .body(streamBody);
    }

    private String callJobFinderAI(String studentName, List<String> studentSkills,
                                    String searchRole, String searchCompany, String apiKey) {
        String jobDataStr = buildJobDataString();
        String systemPrompt =
            "You are a Job Advisor AI for Daffodil International University (DIU) students. " +
            "Your job is to match a student's skills against available job roles at partner companies, " +
            "show match percentage, highlight missing skills, and give actionable learning guidance.\n\n" +
            "RESPONSE FORMAT use these exact section headings:\n\n" +
            "## Checked Job Match Results\n" +
            "For each matched role: company name, role name, match %, matched skills, missing skills.\n\n" +
            "## Warning Skill Gap Analysis\n" +
            "Detailed breakdown of which skills are missing and why they matter.\n\n" +
            "## Book Learning Roadmap\n" +
            "Step-by-step plan to acquire missing skills with resources.\n\n" +
            "## Briefcase Best Role Recommendation\n" +
            "Which role fits best right now and why.\n\n" +
            "## Rocket Action Plan\n" +
            "Concrete next steps to get job-ready.\n\n" +
            "## Sparkles Motivation\n" +
            "A short encouraging message.\n\n" +
            "TONE: Warm, professional, honest, encouraging. Be specific about skills.";

        String userMsg =
            "Available Jobs at Partner Companies:\n" + jobDataStr + "\n\n" +
            "Student Name: " + studentName + "\n" +
            "Student's Current Skills: " + (studentSkills.isEmpty() ? "(none added yet)" : String.join(", ", studentSkills)) + "\n" +
            (searchRole.isEmpty() ? "" : "Student is searching for: " + searchRole + "\n") +
            (searchCompany.isEmpty() ? "" : "Preferred company: " + searchCompany + "\n") +
            "\nAnalyze this student's skills against ALL available job roles" +
            (searchRole.isEmpty() ? "" : " with special focus on roles matching '" + searchRole + "'") +
            ". Show match %, missing skills, and a full learning roadmap.";

        try {
            String requestJson =
                "{\"model\":\"claude-sonnet-4-20250514\"," +
                "\"max_tokens\":2000," +
                "\"system\":" + toJsonString(systemPrompt) + "," +
                "\"messages\":[{\"role\":\"user\",\"content\":" + toJsonString(userMsg) + "}]}";

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.anthropic.com/v1/messages"))
                .header("Content-Type","application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version","2023-06-01")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

            java.net.http.HttpResponse<String> resp =
                client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            String rb = resp.body();
            int ti = rb.indexOf("\"text\"");
            if (ti < 0) return null;
            int ci = rb.indexOf(":", ti);
            int qi = rb.indexOf("\"", ci + 1);
            if (qi < 0) return null;
            StringBuilder sb = new StringBuilder();
            int i = qi + 1;
            while (i < rb.length()) {
                char c = rb.charAt(i);
                if (c == '\\' && i + 1 < rb.length()) {
                    char nx = rb.charAt(i + 1);
                    if      (nx == 'n') { sb.append('\n'); i += 2; }
                    else if (nx == 't') { sb.append('\t'); i += 2; }
                    else if (nx == '"'){ sb.append('"'  ); i += 2; }
                    else if (nx == '\\') { sb.append('\\'); i += 2; }
                    else { sb.append(nx); i += 2; }
                } else if (c == '"') break;
                else { sb.append(c); i++; }
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String buildJobMatchReport(String studentName, List<String> studentSkills,
                                        String searchRole, String searchCompany) {
        List<String> normSkills = studentSkills.stream()
            .map(s -> s.toLowerCase().replace("-", " ").replace("_", " ").trim())
            .toList();

        java.util.function.Function<String, Boolean> hasSkill = req -> {
            String r = req.toLowerCase().replace("-", " ").replace("_", " ").trim();
            for (String s : normSkills) {
                if (s.equals(r) || s.contains(r) || r.contains(s)) return true;
                if ((r.equals("spring boot") || r.equals("springboot")) &&
                    (s.equals("spring boot") || s.equals("springboot") || s.equals("spring"))) return true;
                if ((r.equals("rest api") || r.equals("restapi")) &&
                    (s.equals("rest api") || s.equals("rest") || s.equals("restapi"))) return true;
                if (r.equals("github") && (s.equals("git") || s.equals("github"))) return true;
                if (r.equals("numpy") && (s.equals("numpy") || s.equals("num py"))) return true;
                if (r.equals("machine learning") && (s.contains("machine learning") || s.contains("ml"))) return true;
            }
            return false;
        };

        StringBuilder sb = new StringBuilder();
        boolean filterRole    = !searchRole.isEmpty();
        boolean filterCompany = !searchCompany.isEmpty();

        List<String[]> allMatches = new ArrayList<>();

        for (var compEntry : JOB_DATA.entrySet()) {
            String company = compEntry.getKey();
            if (filterCompany && !company.toLowerCase().contains(searchCompany.toLowerCase())) continue;
            for (var roleEntry : compEntry.getValue().entrySet()) {
                String role = roleEntry.getKey();
                List<String> required = roleEntry.getValue();
                if (filterRole && !role.toLowerCase().contains(searchRole.toLowerCase())) continue;
                List<String> matched = new ArrayList<>();
                List<String> missing = new ArrayList<>();
                for (String req : required) {
                    if (hasSkill.apply(req)) matched.add(req);
                    else missing.add(req);
                }
                int pct = required.isEmpty() ? 0 : (matched.size() * 100 / required.size());
                allMatches.add(new String[]{ company, role, String.valueOf(pct),
                    String.join(", ", matched), String.join(", ", missing) });
            }
        }
        allMatches.sort((a, b) -> Integer.parseInt(b[2]) - Integer.parseInt(a[2]));

        sb.append("## Checked Job Match Results\n\n");
        if (allMatches.isEmpty()) {
            sb.append("No matching roles found");
            if (filterRole)    sb.append(" for '").append(searchRole).append("'");
            if (filterCompany) sb.append(" at '").append(searchCompany).append("'");
            sb.append(".\n\nTry a broader search or view all available roles.\n\n");
        } else {
            for (String[] m : allMatches) {
                int pct = Integer.parseInt(m[2]);
                String badge = pct >= 75 ? "Strong Match" : pct >= 40 ? "Partial Match" : "Low Match";
                sb.append("### ").append(m[0]).append(" — ").append(m[1]).append("\n");
                sb.append("**Match: ").append(m[2]).append("%** — ").append(badge).append("\n");
                if (!m[3].isBlank()) sb.append("- You have: ").append(m[3]).append("\n");
                if (!m[4].isBlank()) sb.append("- Missing: **").append(m[4]).append("**\n");
                sb.append("\n");
            }
        }

        sb.append("## Warning Skill Gap Analysis\n\n");
        java.util.Map<String, List<String>> missingToRoles = new java.util.LinkedHashMap<>();
        for (String[] m : allMatches) {
            if (m[4].isBlank()) continue;
            for (String ms : m[4].split(",\\s*")) {
                missingToRoles.computeIfAbsent(ms.trim(), k -> new ArrayList<>())
                              .add(m[0] + " › " + m[1]);
            }
        }
        if (missingToRoles.isEmpty()) {
            sb.append("You already have all the skills for the matched roles!\n\n");
        } else {
            sb.append("Skills you need to acquire:\n\n");
            for (var entry : missingToRoles.entrySet()) {
                sb.append("- **").append(entry.getKey()).append("**")
                  .append(" — needed for: ").append(String.join(", ", entry.getValue())).append("\n");
            }
            sb.append("\n");
        }

        sb.append("## Book Learning Roadmap\n\n");
        if (missingToRoles.isEmpty()) {
            sb.append("No gaps to fill — focus on deepening existing skills and building projects.\n\n");
        } else {
            sb.append("**Step-by-step plan to close your skill gaps:**\n\n");
            int step = 1;
            for (String skill : missingToRoles.keySet()) {
                String lower = skill.toLowerCase();
                String resource;
                if (lower.contains("python"))                                       resource = "Python.org official tutorial or CS50P (Harvard, free)";
                else if (lower.contains("spring"))                                  resource = "Spring.io guides + Baeldung.com Spring Boot tutorials";
                else if (lower.contains("java"))                                    resource = "Java Brains YouTube + Oracle Java tutorials";
                else if (lower.contains("docker"))                                  resource = "Docker official docs + TechWorld with Nana (YouTube)";
                else if (lower.contains("devops"))                                  resource = "DevOps Roadmap (roadmap.sh) + freeCodeCamp DevOps";
                else if (lower.contains("machine learning") || lower.contains("ml")) resource = "Andrew Ng ML course (Coursera, free audit)";
                else if (lower.contains("numpy") || lower.contains("pandas"))      resource = "Kaggle free Python + NumPy/Pandas micro-courses";
                else if (lower.contains("mysql"))                                   resource = "MySQL official docs + W3Schools SQL tutorials";
                else if (lower.contains("git"))                                     resource = "Pro Git book (free) + GitHub Skills";
                else if (lower.contains("html") || lower.contains("css"))          resource = "MDN Web Docs (free) + freeCodeCamp Responsive Web Design";
                else if (lower.contains("rest"))                                    resource = "REST API design guides on restfulapi.net";
                else                                                                resource = "Search for '" + skill + "' on Coursera, Udemy, or YouTube";
                sb.append("**Step ").append(step++).append(" — Learn ").append(skill).append("**\n");
                sb.append("Resource: ").append(resource).append("\n\n");
            }
        }

        sb.append("## Briefcase Best Role Recommendation\n\n");
        if (!allMatches.isEmpty()) {
            String[] best = allMatches.get(0);
            int bestPct = Integer.parseInt(best[2]);
            sb.append("**").append(best[1]).append(" at ").append(best[0]).append("** (").append(best[2]).append("% match)\n\n");
            if (bestPct >= 75)
                sb.append("Strong match — you already meet most requirements. Apply with confidence and showcase your projects.\n\n");
            else if (bestPct >= 40)
                sb.append("This role is within reach. Close the skill gaps above and you will be a competitive candidate.\n\n");
            else
                sb.append("You have foundational overlap. Follow the learning roadmap and revisit in 2–3 months.\n\n");
        } else {
            sb.append("Add your skills in the Skills section first to get personalised recommendations.\n\n");
        }

        sb.append("## Rocket Action Plan\n\n");
        sb.append("1. Go to **My Skills** in Trackio and add all technologies you know\n");
        sb.append("2. Pick 1–2 missing skills and start learning this week\n");
        sb.append("3. Build a mini project using the new skill to demonstrate it\n");
        sb.append("4. Update your Trackio profile with the new skill and project\n");
        sb.append("5. Run the Job Finder again to track your improvement\n\n");

        sb.append("## Sparkles Motivation\n\n");
        if (allMatches.isEmpty()) {
            sb.append("Every expert was once a beginner. Add your skills and the right opportunities will reveal themselves.");
        } else {
            int bestPct = Integer.parseInt(allMatches.get(0)[2]);
            if (bestPct >= 75)
                sb.append("You are closer to job-ready than you think. Stay consistent, keep building, and the right company will notice.");
            else if (bestPct >= 40)
                sb.append("You already have a solid foundation. A few more skills and you will be a strong candidate — keep going!");
            else
                sb.append("The gap between where you are and where you want to be is just a learning plan away. Every skill opens new doors.");
        }

        return sb.toString();
    }

    private String buildJobDataString() {
        StringBuilder sb = new StringBuilder();
        for (var comp : JOB_DATA.entrySet()) {
            sb.append("Company: ").append(comp.getKey()).append("\n");
            for (var role : comp.getValue().entrySet()) {
                sb.append("  Role: ").append(role.getKey())
                  .append(" | Required Skills: ").append(String.join(", ", role.getValue())).append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }


    // ── Intent detection ─────────────────────────────────────────────────────

    enum InputIntent { GREETING, VAGUE, PROJECT_IDEA }

    /**
     * Classifies what the student typed into one of three categories.
     * GREETING    — hi, hello, thanks, ok, small talk
     * VAGUE       — something typed but too thin to analyse meaningfully
     * PROJECT_IDEA — enough substance to run a real duplicate check
     */
    private InputIntent detectIntent(String input) {
        if (input == null || input.isBlank()) return InputIntent.GREETING;
        String s = input.toLowerCase().trim();

        // Exact/near-exact greeting matches
        String[] greetings = {
            "hi","hello","hey","hii","helo","heyo","yo","sup","ok","okay","k",
            "good morning","good afternoon","good evening","good night",
            "how are you","how r u","whats up","what's up","wassup",
            "nice to meet","greetings","howdy","hi there","hello there","hey there",
            "thanks","thank you","thank u","ty","thx","cool","nice","great",
            "awesome","yes","no","sure","maybe","lol","haha","salaam","assalamu"
        };
        for (String g : greetings) {
            String clean = s.replaceAll("[!.?]$","");
            if (clean.equals(g)) return InputIntent.GREETING;
        }

        // Vague patterns checked BEFORE short-input rule so "a system" / "help me"
        // are correctly classified as VAGUE rather than GREETING
        String[] vaguePatterns = {
            "i want to make","i want to build","i want to create","i want to develop",
            "i am going to","i will make","i will build","i will create",
            "my project is","my idea is","an app","a website","a system","a platform",
            "a project","something related to","i don't know","i dont know","not sure",
            "help me","help","i need help","can you help","what should i",
            "suggest me","give me an idea","any idea","i have an idea","my idea"
        };
        for (String v : vaguePatterns) {
            if (s.contains(v) && s.split("\\s+").length < 8) return InputIntent.VAGUE;
        }

        // Single/double word inputs that don't match a greeting exactly could be
        // a project name — treat as VAGUE so the mentor asks for more details,
        // rather than saying "hi" back when a student typed their project name.
        if (s.split("\\s+").length <= 2 && s.length() < 15) return InputIntent.VAGUE;

        // Minimum bar for PROJECT_IDEA
        String[] signals = {
            "system","app","application","platform","website","portal","tool",
            "tracker","manager","monitor","checker","generator","calculator",
            "for","that","which","will","can","help","solve","manage",
            "track","book","order","pay","register","student","doctor","hospital",
            "school","shop","library","farm","bank","expense","food","health",
            "transport","hotel","employee","user","admin","patient","customer"
        };
        int words = s.split("\\s+").length;
        int sigs = 0;
        for (String sig : signals) { if (s.contains(sig)) sigs++; }
        if (words >= 5 && sigs >= 2) return InputIntent.PROJECT_IDEA;
        if (words >= 8) return InputIntent.PROJECT_IDEA;
        return InputIntent.VAGUE;
    }

    /**
     * Returns a warm, human, conversational reply for GREETING and VAGUE inputs.
     * Never an error message. Never robotic. Acts like a mentor whose door is open.
     */
    private String buildConversationalReply(InputIntent intent, String input) {
        String s = (input == null ? "" : input.toLowerCase().trim());

        if (intent == InputIntent.GREETING) {
            if (s.contains("good morning"))
                return "Good morning! Ready to help you check your project idea whenever you are. Just tell me — what problem are you thinking of solving?";
            if (s.contains("good afternoon"))
                return "Good afternoon! Hope the day is going well. Drop your project idea whenever you are ready and I will give you a full analysis.";
            if (s.contains("good evening") || s.contains("good night"))
                return "Good evening! Working late on your project? Tell me what you are thinking of building — I am here to help you figure out if the idea is original and how to make it strong.";
            if (s.contains("how are you") || s.contains("how r u"))
                return "I am doing great, thanks for asking! More importantly — how is your project idea coming along? Tell me what you are thinking of building and I will check it against all existing student projects for you.";
            if (s.contains("thanks") || s.contains("thank"))
                return "You are very welcome! If you have another project idea you would like me to check, just describe it — I am happy to help.";
            if (s.contains("ok") || s.contains("okay") || s.contains("cool") || s.contains("nice") || s.contains("great") || s.contains("awesome"))
                return "Glad to hear it! Whenever you are ready, describe your project idea and I will do a full conceptual analysis for you — comparing it against all existing student projects.";
            return "Hey! Good to see you here.\n\nI am your Project Mentor — I can check your project idea against all existing student projects and tell you exactly how original it is, what overlaps exist, and how to make it stronger.\n\nJust tell me what you are thinking of building. Even a rough description works — what problem are you trying to solve?";
        }

        // VAGUE responses
        if (s.contains("don't know") || s.contains("dont know") || s.contains("not sure") || s.contains("any idea") || s.contains("give me") || s.contains("suggest"))
            return "No worries — that is exactly what I am here for!\n\nLet us figure this out together. Think about something in your daily life that feels unnecessarily difficult or inefficient. That friction is usually where the best project ideas come from.\n\nFor example:\n- Do you forget to track your spending?\n- Is booking an appointment at your clinic still done by phone?\n- Does your campus have no way to find available study rooms?\n\nAny of those ring a bell? Or tell me what domain interests you — health, education, finance, transport — and we can explore from there.";

        if (s.contains("help me") || s.contains("i need help") || s.contains("can you help"))
            return "Of course, that is exactly what I am here for!\n\nTo give you the best analysis, I need to understand your idea a little. Just tell me:\n\n**What problem does your project solve?** Even one sentence is enough to get started. For example: *A system that helps university students find available study rooms on campus* — that is plenty for me to work with.\n\nWhat are you thinking?";

        if (s.contains("i want to make") || s.contains("i want to build") || s.contains("i want to create") || s.contains("i will make") || s.contains("i will build"))
            return "Nice, you have got the motivation — now let us sharpen the idea!\n\nTo check it properly, I need one more thing: **what problem will it solve, and for who?**\n\nFor example, instead of *I want to make an app*, try:\n*An app that helps small restaurant owners manage their daily orders and inventory without needing technical skills.*\n\nThat kind of description lets me compare it meaningfully against existing projects. What is the core problem you are solving?";

        if (s.contains("a system") || s.contains("a website") || s.contains("a platform") || s.contains("an app"))
            return "Good start! I can see you have something in mind.\n\nTo do a proper analysis, I need a bit more to go on. Tell me:\n\n1. **What specific problem does it solve?**\n2. **Who are the users?** (students, doctors, shopkeepers, etc.)\n3. **What will users actually do in it?**\n\nEven two or three sentences covering those points is enough for me to give you a full report. What is the idea?";

        // If input looks like just a project name (short, no spaces, capitalised-ish),
        // ask specifically about that name
        boolean looksLikeName = s.split("\\s+").length <= 3 && !s.contains(" ") == false || s.split("\\s+").length == 1;
        String displayName = input.trim().length() > 0
            ? (input.trim().substring(0,1).toUpperCase() + input.trim().substring(1))
            : "your project";
        if (looksLikeName) {
            return "Got it — **" + displayName + "** sounds like a project name! To check it properly, I need to understand what it actually does.\n\nCould you tell me:\n\n1. **What problem does " + displayName + " solve?**\n2. **Who are the users?**\n3. **What are the main features?**\n\nEven two sentences is enough for me to run a full analysis.";
        }
        return "I can see you have something in mind — let me help you develop it!\n\nTo run a proper duplicate check and give you meaningful feedback, I need to understand the idea a little better. Could you tell me:\n\n**What problem does your project solve, and for who?**\n\nFor example: *A system that helps patients book doctor appointments online instead of waiting in long queues.* That one sentence tells me the problem, the user, and the solution — which is all I need to get started.\n\nWhat are you thinking of building?";    }

    // ── Private helpers ──────────────────────────────────────────────────────

    private String buildCorpus(List<Project> projects) {
        StringBuilder sb = new StringBuilder();
        for (Project p : projects) {
            if (p.getTitle() == null || p.getTitle().isBlank()) continue;
            sb.append("PROJECT TITLE: ").append(p.getTitle()).append("\n");
            if (p.getStudent() != null) {
                String sName = p.getStudent().getName() != null ? p.getStudent().getName() : "Unknown";
                String sReg  = p.getStudent().getRegistrationNo() != null ? p.getStudent().getRegistrationNo() : "";
                sb.append("STUDENT: ").append(sName);
                if (!sReg.isBlank()) sb.append(" (").append(sReg).append(")");
                sb.append("\n");
            }
            if (p.getDescription() != null && !p.getDescription().isBlank())
                sb.append("ABSTRACT: ").append(p.getDescription()).append("\n");
            sb.append("---\n");
        }
        return sb.toString();
    }

    private String buildMentorSystemPrompt() {
        return
            "You are a Project Mentor AI for Daffodil International University (DIU) students. " +
            "Your job is to check a student's project name/title against existing student project titles in the database " +
            "and give them honest, helpful, mentor-quality feedback.\n\n" +

            "MATCHING RULES:\n" +
            "- Compare the submitted project name/title against EXISTING PROJECT TITLES only.\n" +
            "- Exact title match = 100% similarity.\n" +
            "- Very similar title (same words, minor variation) = 80-95%.\n" +
            "- Partial title word overlap = proportional score (max 70%).\n" +
            "- No title overlap = 0%. Do not invent similarity from topic or domain alone.\n" +
            "- NEVER give a high score just because two projects are in the same field.\n" +
            "  Example: 'Hospital Appointment System' vs 'NASA SpaceX' = 0% — completely different titles.\n\n" +

            "RESPONSE FORMAT — use these exact section headings:\n\n" +
            "## 🔍 Similarity Analysis\n" +
            "List each matched project title with similarity % and one-line reason. State Overall Similarity Score.\n\n" +
            "## ⚠️ Duplicate Status\n" +
            "Fully Duplicate / Partially Duplicate / Not Duplicate — explain why in one sentence.\n\n" +
            "## 💡 Mentor Recommendations\n" +
            "Step-by-step improvement plan tailored to the student's situation.\n\n" +
            "## 🚀 Build Guidance\n" +
            "Suggested architecture, tech stack, and development phases.\n\n" +
            "## 🏭 Industry Potential\nLow / Medium / High with explanation.\n\n" +
            "## 🎓 Academic Potential\nLow / Medium / High with explanation.\n\n" +
            "## 📝 Final Advice\nPractical next steps.\n\n" +
            "## ✨ Motivation\nA short, genuine, encouraging note.\n\n" +

            "HANDLING NON-PROJECT INPUTS:\n" +
            "- Greeting (hi, hello, thanks) → respond warmly, ask what they want to build.\n" +
            "- Vague input (I want to make an app) → ask: what problem + who are the users?\n" +
            "- Project name only (trackio) → ask what it does before running analysis.\n" +
            "- NEVER run a similarity report on a greeting or a single vague word.\n\n" +

            "TONE: Human, warm, professional, encouraging. Tailor every response to the student's specific project.";
    }
    private String buildUserMessage(String name, String title, String idea, String corpus) {
        return
            "Here are the existing projects currently in the TrackIO system:\n\n" +
            corpus +
            "\nThe student's proposed project:\n" +
            "Project Name: " + (name.isEmpty() ? "(not provided)" : name) + "\n" +
            "Project Title: " + (title.isEmpty() ? "(same as name)" : title) + "\n" +
            "Project Idea / Description: " + (idea.isEmpty() ? "(The student only provided the project name above — please infer the likely concept from the name and title, then check for similarity against existing projects)" : idea) + "\n\n" +
            "Please perform a deep conceptual analysis — understand the proposed project's problem, users, features, and workflow FIRST, " +
            "then compare with each existing project on those dimensions. Give your full mentor advisory report following the required format.";
    }

    /** Simulates streaming by writing word-by-word (used when API key absent or on fallback). */
    private void streamTextWordByWord(String text, OutputStream out) throws java.io.IOException, InterruptedException {
        String[] words = text.split("(?<=\\s)|(?=\\s)");
        for (String word : words) {
            String sseEvent = "data: " + toJsonString(word) + "\n\n";
            out.write(sseEvent.getBytes(StandardCharsets.UTF_8));
            out.flush();
            if (!word.isBlank()) Thread.sleep(18); // ~55 words/sec
        }
    }

    /** Extracts the delta text from a streaming SSE JSON chunk from Claude. */
    private String extractStreamDelta(String json) {
        try {
            // Look for "delta":{"type":"text_delta","text":"..."}
            int deltaIdx = json.indexOf("\"text_delta\"");
            if (deltaIdx < 0) return null;
            int textIdx = json.indexOf("\"text\"", deltaIdx);
            if (textIdx < 0) return null;
            int colon = json.indexOf(':', textIdx);
            if (colon < 0) return null;
            int quote = json.indexOf('"', colon + 1);
            if (quote < 0) return null;
            StringBuilder sb = new StringBuilder();
            int i = quote + 1;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    if (next == 'n') { sb.append('\n'); i += 2; }
                    else if (next == 't') { sb.append('\t'); i += 2; }
                    else if (next == '"') { sb.append('"'); i += 2; }
                    else if (next == '\\') { sb.append('\\'); i += 2; }
                    else { sb.append(next); i += 2; }
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                    i++;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /** Extracts the text from Claude's JSON response */
    private String extractClaudeText(String json) {
        try {
            int contentIdx = json.indexOf("\"content\"");
            if (contentIdx < 0) return null;
            int textIdx = json.indexOf("\"text\"", contentIdx);
            if (textIdx < 0) return null;
            int colon = json.indexOf(':', textIdx);
            if (colon < 0) return null;
            int quote = json.indexOf('"', colon + 1);
            if (quote < 0) return null;
            StringBuilder sb = new StringBuilder();
            int i = quote + 1;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    if (next == 'n') { sb.append('\n'); i += 2; }
                    else if (next == 't') { sb.append('\t'); i += 2; }
                    else if (next == '"') { sb.append('"'); i += 2; }
                    else if (next == '\\') { sb.append('\\'); i += 2; }
                    else { sb.append(next); i += 2; }
                } else if (c == '"') {
                    break;
                } else {
                    sb.append(c);
                    i++;
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Simple title-match advisor.
     * Compares the submitted project name/title against existing project TITLES only.
     * Exact title match = 100%. Partial word overlap = proportional %.
     * No concept scoring, no keyword groups, no feature matching.
     */
    private String ruleBasedAdvisor(String name, String title, String idea,
                                     List<Project> allProjects) {

        String submitted = (name + " " + title).toLowerCase().trim();
        if (submitted.isBlank()) submitted = idea.toLowerCase().trim();

        // ── Title matching ────────────────────────────────────────────────────
        List<String[]> matches = new ArrayList<>(); // [projTitle, studentName, score%, regNo]
        int highestScore = 0;

        for (Project p : allProjects) {
            if (p.getTitle() == null || p.getTitle().isBlank()) continue;
            String pTitle = p.getTitle().toLowerCase().trim();
            String studentName = (p.getStudent() != null && p.getStudent().getName() != null)
                ? p.getStudent().getName() : "Unknown";
            String studentReg = (p.getStudent() != null && p.getStudent().getRegistrationNo() != null)
                ? p.getStudent().getRegistrationNo() : "";

            int score = computeTitleSimilarity(submitted, pTitle);
            if (score >= 10) {
                matches.add(new String[]{ p.getTitle(), studentName, String.valueOf(score), studentReg });
                if (score > highestScore) highestScore = score;
            }
        }
        matches.sort((a, b) -> Integer.parseInt(b[2]) - Integer.parseInt(a[2]));

        String overallStatus = highestScore >= 75 ? "Fully Duplicate"
                             : highestScore >= 40 ? "Partially Duplicate"
                             : "Not Duplicate";
        String displayName = name.isEmpty() ? (title.isEmpty() ? "Your Project" : capitalize(title)) : capitalize(name);

        StringBuilder sb = new StringBuilder();

        // ── Similarity Analysis ──────────────────────────────────────────────
        sb.append("## 🔍 Similarity Analysis\n\n");
        if (matches.isEmpty()) {
            sb.append("No matching project titles were found in the TrackIO database.\n\n");
            sb.append("**Overall Similarity Score: 0%**\n\n");
        } else {
            sb.append("Here is how your project title compares with existing submissions:\n\n");
            for (String[] m : matches) {
                String studentDisplay = m[1];
                if (m.length > 3 && m[3] != null && !m[3].isBlank())
                    studentDisplay += " (" + m[3] + ")";
                sb.append("- **").append(m[0]).append("** — Submitted by: ").append(studentDisplay).append("\n");
                sb.append("  Similarity: **").append(m[2]).append("%** — ").append(
                    Integer.parseInt(m[2]) >= 75 ? "Fully Duplicate" :
                    Integer.parseInt(m[2]) >= 40 ? "Partially Duplicate" : "Minor Overlap"
                ).append("\n\n");
            }
            sb.append("**Overall Similarity Score: ").append(highestScore).append("%**\n\n");
        }

        // ── Duplicate Status ─────────────────────────────────────────────────
        sb.append("## ⚠️ Duplicate Status\n\n");
        if (overallStatus.equals("Not Duplicate")) {
            sb.append("**Not Duplicate** — No existing project title closely matches yours. Your project name appears original in the current database.\n\n");
        } else if (overallStatus.equals("Partially Duplicate")) {
            sb.append("**Partially Duplicate** — Your project name is similar to one or more existing submissions. Consider refining your title and scope to clearly differentiate your work.\n\n");
        } else {
            sb.append("**Fully Duplicate** — Your project title closely matches an existing submission. You will need to significantly differentiate your project or choose a new direction.\n\n");
        }

        // ── Mentor Recommendations ───────────────────────────────────────────
        sb.append("## 💡 Mentor Recommendations\n\n");
        if (overallStatus.equals("Not Duplicate")) {
            sb.append("Your project title is original — here is how to make it strong:\n\n");
            sb.append("- Define a clear problem statement: what specific pain point does this solve?\n");
            sb.append("- Identify your exact target users and tailor features to their needs\n");
            sb.append("- Add at least one technically advanced feature (AI, real-time, analytics) to elevate academic value\n");
            sb.append("- Document your architecture and design decisions from day one\n\n");
        } else if (overallStatus.equals("Partially Duplicate")) {
            sb.append("**Step 1** — Review the matched project(s) carefully and list what they do\n\n");
            sb.append("**Step 2** — Identify at least 2 features or a different target user group that sets yours apart\n\n");
            sb.append("**Step 3** — Update your project title to reflect your unique angle\n\n");
            sb.append("**Step 4** — Add a novelty statement to your proposal: *Unlike [matched project], mine specifically...*\n\n");
        } else {
            sb.append("**Step 1** — Do not start coding yet — re-scope first\n\n");
            sb.append("**Step 2** — Talk to your supervisor about a feasible pivot direction\n\n");
            sb.append("**Step 3** — Identify what the matched project does NOT do and build around that gap\n\n");
            sb.append("**Step 4** — Run this checker again on your revised idea before committing\n\n");
        }

        // ── Build Guidance ───────────────────────────────────────────────────
        sb.append("## 🚀 Build Guidance\n\n");
        sb.append("**Suggested Stack**: Spring Boot + Spring Security + MySQL + Thymeleaf\n\n");
        sb.append("**Development Phases**:\n");
        sb.append("1. Requirements and UI wireframes (1–2 weeks)\n");
        sb.append("2. Database schema and core entities (1 week)\n");
        sb.append("3. Authentication and basic CRUD (2 weeks)\n");
        sb.append("4. Core business logic and differentiating features (3–4 weeks)\n");
        sb.append("5. Dashboard, analytics, reporting (1–2 weeks)\n");
        sb.append("6. Testing, polish, and documentation (1–2 weeks)\n\n");

        // ── Potential ────────────────────────────────────────────────────────
        sb.append("## 🏭 Industry Potential\n\n");
        sb.append(overallStatus.equals("Not Duplicate") ? "**Rating: Medium–High** — Original ideas in this space have real commercial viability with the right execution.\n\n"
                : overallStatus.equals("Partially Duplicate") ? "**Rating: Medium** — Differentiate the concept and industry potential rises significantly.\n\n"
                : "**Rating: Low (as currently scoped)** — Re-scope around a unique angle and this can improve.\n\n");

        sb.append("## 🎓 Academic Potential\n\n");
        sb.append(overallStatus.equals("Not Duplicate") ? "**Rating: High** — Original problem statements carry strong academic merit.\n\n"
                : overallStatus.equals("Partially Duplicate") ? "**Rating: Medium** — Articulate your novelty clearly in the proposal and this rises to High.\n\n"
                : "**Rating: Low (as currently scoped)** — Apply the re-scoping steps and academic potential can reach Medium or High.\n\n");

        // ── Final Advice ─────────────────────────────────────────────────────
        sb.append("## 📝 Final Advice\n\n");
        sb.append("1. Write your problem statement in 3 sentences: the problem, who it affects, and why existing solutions fall short\n");
        sb.append("2. Sketch your core data model — 4 to 6 main entities\n");
        sb.append("3. Choose the one feature that will make evaluators remember your project\n");
        sb.append("4. Start a project log — document every major decision as you go\n\n");

        // ── Motivation ───────────────────────────────────────────────────────
        sb.append("## ✨ Motivation\n\n");
        if (overallStatus.equals("Not Duplicate"))
            sb.append("You are off to a strong start. An original title means you have identified a space others have not yet claimed — build on that with confidence.");
        else if (overallStatus.equals("Partially Duplicate"))
            sb.append("The overlap is not a setback — it is a map showing where the existing work stops and where your contribution needs to begin. The steps above will get you there.");
        else
            sb.append("A high similarity score is not the end — it is the beginning of a better project. Students who go through re-scoping often produce stronger work than those who never had to question their initial idea.");

        return sb.toString();
    }

    /**
     * Computes title similarity between submitted input and an existing project title.
     * Logic:
     *   - Exact match (after normalisation) = 100%
     *   - Submitted contains the full existing title = 90%
     *   - Existing title contains the full submitted input = 85%
     *   - Word overlap ratio = proportional (max 70%)
     *   - No match = 0%
     */
    private int computeTitleSimilarity(String submitted, String existingTitle) {
        String s = submitted.replaceAll("[^a-z0-9 ]", "").trim();
        String e = existingTitle.replaceAll("[^a-z0-9 ]", "").trim();
        if (s.isBlank() || e.isBlank()) return 0;

        // Exact match
        if (s.equals(e)) return 100;

        // Full containment
        if (s.contains(e)) return 90;
        if (e.contains(s)) return 85;

        // Word overlap
        String[] sWords = s.split("\\s+");
        String[] eWords = e.split("\\s+");
        int matchCount = 0;
        for (String sw : sWords) {
            if (sw.length() < 3) continue;
            for (String ew : eWords) {
                if (sw.equals(ew) || (sw.length() >= 5 && (sw.contains(ew) || ew.contains(sw)))) {
                    matchCount++;
                    break;
                }
            }
        }
        int total = Math.max(sWords.length, eWords.length);
        int wordScore = total > 0 ? (matchCount * 70) / total : 0;
        return Math.min(wordScore, 70);
    }


    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String toJsonString(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\\", "\\\\")
                       .replace("\"", "\\\"")
                       .replace("\n", "\\n")
                       .replace("\r", "\\r")
                       .replace("\t", "\\t") + "\"";
    }

    private String jsonEscape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

}