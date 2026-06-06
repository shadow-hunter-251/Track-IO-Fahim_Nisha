package com.portfolio.tracker.controller;

import com.portfolio.tracker.entity.*;
import com.portfolio.tracker.repository.*;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.http.ResponseEntity;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Outcome-Based Education (OBE) Engine.
 *
 * Implements the standard accreditation hierarchy used by BAETE/NBA/ABET:
 *
 *      Program Outcomes (POs)  <- what graduates achieve
 *           ^ weighted aggregation
 *      Course Outcomes (COs)   <- what students achieve per course
 *           ^ tagged contributions
 *      Artifacts (Projects)    <- evidence that the COs were attained
 *
 * Attainment is computed dynamically -- no stored counts -- so it stays
 * correct as students add/remove projects.
 */
@Controller
public class OBEController {

    private final AppUserRepository appUserRepository;
    private final StudentRepository studentRepository;
    private final ProgramOutcomeRepository poRepository;
    private final CourseRepository courseRepository;
    private final CourseOutcomeRepository coRepository;
    private final ProjectRepository projectRepository;

    public OBEController(AppUserRepository appUserRepository,
                         StudentRepository studentRepository,
                         ProgramOutcomeRepository poRepository,
                         CourseRepository courseRepository,
                         CourseOutcomeRepository coRepository,
                         ProjectRepository projectRepository) {
        this.appUserRepository = appUserRepository;
        this.studentRepository = studentRepository;
        this.poRepository = poRepository;
        this.courseRepository = courseRepository;
        this.coRepository = coRepository;
        this.projectRepository = projectRepository;
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    private AppUser getTeacher(UserDetails u) {
        return appUserRepository.findByUsername(u.getUsername()).orElseThrow();
    }

    private Student getCurrentStudent(UserDetails u) {
        AppUser user = appUserRepository.findByUsername(u.getUsername()).orElseThrow();
        return studentRepository.findById(user.getStudentId()).orElseThrow();
    }

    /** Parse "PO1:3,PO3:2,PO5:1" -> Map{PO1->3, PO3->2, PO5->1}. Robust to nulls/spaces. */
    private Map<String, Integer> parsePoMappings(String poMappings) {
        Map<String, Integer> m = new LinkedHashMap<>();
        if (poMappings == null || poMappings.isBlank()) return m;
        for (String token : poMappings.split(",")) {
            String t = token.trim();
            if (t.isEmpty()) continue;
            String[] kv = t.split(":");
            if (kv.length != 2) continue;
            String po = kv[0].trim().toUpperCase();
            try {
                int weight = Integer.parseInt(kv[1].trim());
                if (weight < 1) weight = 1;
                if (weight > 3) weight = 3;
                m.put(po, weight);
            } catch (NumberFormatException ignored) {}
        }
        return m;
    }

    /** Parse "1,3,7" -> [1L, 3L, 7L]. */
    private List<Long> parseCoIds(String csv) {
        List<Long> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) return out;
        for (String tok : csv.split(",")) {
            try { out.add(Long.parseLong(tok.trim())); } catch (NumberFormatException ignored) {}
        }
        return out;
    }

    // ==================================================================
    //  TEACHER -- Manage Courses
    // ==================================================================

    @GetMapping("/teacher/obe/courses")
    public String courses(@AuthenticationPrincipal UserDetails u, Model model) {
        AppUser teacher = getTeacher(u);
        List<Course> all = courseRepository.findAllByOrderByCourseCodeAsc();

        // Attach CO counts for the table
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Course c : all) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("course", c);
            r.put("coCount", coRepository.countByCourseId(c.getId()));
            rows.add(r);
        }
        model.addAttribute("teacher", teacher);
        model.addAttribute("rows", rows);
        return "teacher/obe-courses";
    }

    @GetMapping("/teacher/obe/courses/new")
    public String newCourseForm(@AuthenticationPrincipal UserDetails u, Model model) {
        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("course", new Course());
        return "teacher/obe-course-form";
    }

    @PostMapping("/teacher/obe/courses/save")
    public String saveCourse(@ModelAttribute Course course, RedirectAttributes ra) {
        if (course.getCourseCode() == null || course.getCourseCode().isBlank()
                || course.getCourseName() == null || course.getCourseName().isBlank()) {
            ra.addFlashAttribute("error", "Course code and name are required.");
            return "redirect:/teacher/obe/courses/new";
        }
        if (courseRepository.existsByCourseCode(course.getCourseCode().trim())) {
            ra.addFlashAttribute("error", "A course with that code already exists.");
            return "redirect:/teacher/obe/courses/new";
        }
        course.setCourseCode(course.getCourseCode().trim());
        course.setCourseName(course.getCourseName().trim());
        courseRepository.save(course);
        ra.addFlashAttribute("success", "Course \"" + course.getCourseCode() + "\" created.");
        return "redirect:/teacher/obe/courses/" + course.getId() + "/outcomes";
    }

    @PostMapping("/teacher/obe/courses/{id}/delete")
    public String deleteCourse(@PathVariable Long id, RedirectAttributes ra) {
        // Delete the COs first to avoid orphans
        coRepository.deleteByCourseId(id);
        courseRepository.deleteById(id);
        ra.addFlashAttribute("success", "Course deleted along with its outcomes.");
        return "redirect:/teacher/obe/courses";
    }

    // ==================================================================
    //  TEACHER -- Manage Course Outcomes (COs) for a course
    // ==================================================================

    @GetMapping("/teacher/obe/courses/{courseId}/outcomes")
    public String outcomes(@PathVariable Long courseId,
                           @AuthenticationPrincipal UserDetails u, Model model) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        List<CourseOutcome> cos = coRepository.findByCourseIdOrderByCodeAsc(courseId);
        List<ProgramOutcome> pos = poRepository.findAllByOrderByCodeAsc();

        // Build a structured CO×PO matrix for the matrix view
        List<Map<String, Object>> matrixRows = new ArrayList<>();
        for (CourseOutcome co : cos) {
            Map<String, Integer> mappings = parsePoMappings(co.getPoMappings());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("co", co);
            // For each PO, store the weight (or 0)
            Map<String, Integer> weights = new LinkedHashMap<>();
            for (ProgramOutcome po : pos) {
                weights.put(po.getCode(), mappings.getOrDefault(po.getCode(), 0));
            }
            row.put("weights", weights);
            matrixRows.add(row);
        }

        model.addAttribute("teacher", getTeacher(u));
        model.addAttribute("course", course);
        model.addAttribute("cos", cos);
        model.addAttribute("pos", pos);
        model.addAttribute("matrixRows", matrixRows);
        model.addAttribute("newCo", new CourseOutcome());
        return "teacher/obe-outcomes";
    }

    @PostMapping("/teacher/obe/courses/{courseId}/outcomes/save")
    public String saveOutcome(@PathVariable Long courseId,
                              @ModelAttribute CourseOutcome co,
                              @RequestParam(value = "poMap", required = false) List<String> poMap,
                              RedirectAttributes ra) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        if (co.getCode() == null || co.getCode().isBlank()
                || co.getStatement() == null || co.getStatement().isBlank()) {
            ra.addFlashAttribute("error", "CO code and statement are required.");
            return "redirect:/teacher/obe/courses/" + courseId + "/outcomes";
        }

        co.setCourseId(courseId);
        co.setCode(co.getCode().trim().toUpperCase());

        // Convert poMap (list of "PO1:3" entries from checkboxes) -> comma-separated string
        if (poMap != null && !poMap.isEmpty()) {
            String joined = poMap.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(","));
            co.setPoMappings(joined);
        }

        if (co.getTargetAttainment() == null || co.getTargetAttainment() < 1) {
            co.setTargetAttainment(60);
        }

        coRepository.save(co);
        ra.addFlashAttribute("success", "Outcome \"" + co.getCode() + "\" saved for " + course.getCourseCode() + ".");
        return "redirect:/teacher/obe/courses/" + courseId + "/outcomes";
    }

    @PostMapping("/teacher/obe/outcomes/{id}/delete")
    public String deleteOutcome(@PathVariable Long id, RedirectAttributes ra) {
        CourseOutcome co = coRepository.findById(id).orElseThrow();
        Long courseId = co.getCourseId();
        coRepository.deleteById(id);
        ra.addFlashAttribute("success", "Outcome deleted.");
        return "redirect:/teacher/obe/courses/" + courseId + "/outcomes";
    }

    // ==================================================================
    //  TEACHER -- Attainment Dashboard
    // ==================================================================

    @GetMapping("/teacher/obe/attainment")
    public String attainment(@AuthenticationPrincipal UserDetails u,
                             @RequestParam(value = "department", required = false) String department,
                             @RequestParam(value = "batch", required = false) String batch,
                             Model model) {

        AppUser teacher = getTeacher(u);

        // ----- cohort -----
        List<Student> allStudents = studentRepository.findAll();
        List<Student> students = allStudents.stream()
                .filter(s -> department == null || department.isBlank() || department.equalsIgnoreCase(s.getDepartment()))
                .filter(s -> batch == null || batch.isBlank() || batch.equalsIgnoreCase(s.getBatch()))
                .toList();

        List<String> departments = allStudents.stream()
                .map(Student::getDepartment).filter(Objects::nonNull).distinct().sorted().toList();
        List<String> batches = allStudents.stream()
                .map(Student::getBatch).filter(Objects::nonNull).distinct().sorted().toList();

        // ----- compute attainment -----
        Map<String, Object> attainment = computeAttainment(students);

        model.addAttribute("teacher", teacher);
        model.addAttribute("departments", departments);
        model.addAttribute("batches", batches);
        model.addAttribute("selectedDepartment", department);
        model.addAttribute("selectedBatch", batch);
        model.addAttribute("totalStudents", students.size());
        model.addAttribute("poAttainment", attainment.get("poAttainment"));
        model.addAttribute("coAttainment", attainment.get("coAttainment"));
        model.addAttribute("courses", attainment.get("courses"));
        model.addAttribute("overallPoAvg", attainment.get("overallPoAvg"));
        model.addAttribute("posAboveThreshold", attainment.get("posAboveThreshold"));
        model.addAttribute("posBelowThreshold", attainment.get("posBelowThreshold"));
        model.addAttribute("totalProjectsTagged", attainment.get("totalProjectsTagged"));
        return "teacher/obe-attainment";
    }

    /**
     * Core attainment computation. Returns a Map with:
     *  - poAttainment:  list of { code, shortName, percent, count, weighted }
     *  - coAttainment:  list of { code, courseCode, statement, contributions, target, attained, percent }
     *  - courses:       list of courses for filter display
     *  - overallPoAvg:  average PO attainment across the 12
     *  - posAboveThreshold / posBelowThreshold: counts
     */
    private Map<String, Object> computeAttainment(List<Student> students) {
        List<ProgramOutcome> pos = poRepository.findAllByOrderByCodeAsc();
        List<CourseOutcome> allCos = coRepository.findAll();
        List<Course> allCourses = courseRepository.findAllByOrderByCourseCodeAsc();

        // Index courses by id for lookup
        Map<Long, Course> courseById = allCourses.stream()
                .collect(Collectors.toMap(Course::getId, c -> c));

        // Gather all relevant projects (across the cohort)
        Set<Long> studentIds = students.stream().map(Student::getId).collect(Collectors.toSet());
        List<Project> projects = projectRepository.findAll().stream()
                .filter(p -> p.getStudent() != null && studentIds.contains(p.getStudent().getId()))
                .toList();

        long projectsTagged = projects.stream()
                .filter(p -> p.getCourseOutcomeIds() != null && !p.getCourseOutcomeIds().isBlank())
                .count();

        // Index COs by id and gather "contribution count" per CO
        Map<Long, CourseOutcome> coById = allCos.stream()
                .collect(Collectors.toMap(CourseOutcome::getId, c -> c));
        Map<Long, Long> contributionsPerCo = new HashMap<>();
        for (Project p : projects) {
            for (Long coId : parseCoIds(p.getCourseOutcomeIds())) {
                contributionsPerCo.merge(coId, 1L, Long::sum);
            }
        }

        // ---- CO attainment ----
        // Attainment % = min(100, contributions × 20)  -- i.e. 5 tagged contributions = 100%
        // This is a simple, defensible rule of thumb. Each contribution counts as
        // ~20% toward the CO. In real OBE you'd weight by assessment marks, but
        // for a portfolio system the count-of-evidence is the natural proxy.
        List<Map<String, Object>> coAttainment = new ArrayList<>();
        for (CourseOutcome co : allCos) {
            long contributions = contributionsPerCo.getOrDefault(co.getId(), 0L);
            int attainmentPct = (int) Math.min(100, contributions * 20);
            int target = co.getTargetAttainment() != null ? co.getTargetAttainment() : 60;
            Course c = courseById.get(co.getCourseId());

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", co.getId());
            row.put("code", co.getCode());
            row.put("statement", co.getStatement());
            row.put("bloom", co.getBloomLevel());
            row.put("contributions", contributions);
            row.put("attained", attainmentPct);
            row.put("target", target);
            row.put("metTarget", attainmentPct >= target);
            row.put("courseCode", c != null ? c.getCourseCode() : "?");
            row.put("courseName", c != null ? c.getCourseName() : "?");
            row.put("poMappings", co.getPoMappings());
            coAttainment.add(row);
        }

        // ---- PO attainment ----
        // For each PO, compute weighted attainment from contributing COs:
        //   PO_attainment = Σ(CO_attainment × weight) / Σ(weight)
        // Standard OBE formula used by NBA/BAETE.
        List<Map<String, Object>> poAttainment = new ArrayList<>();
        int totalPct = 0; int above = 0; int below = 0;
        final int PO_THRESHOLD = 60;

        for (ProgramOutcome po : pos) {
            double weightedSum = 0.0;
            double totalWeight = 0.0;
            int contributingCos = 0;

            for (CourseOutcome co : allCos) {
                Map<String, Integer> mapping = parsePoMappings(co.getPoMappings());
                Integer weight = mapping.get(po.getCode());
                if (weight == null) continue;
                contributingCos++;

                long contributions = contributionsPerCo.getOrDefault(co.getId(), 0L);
                int coAttained = (int) Math.min(100, contributions * 20);

                weightedSum += coAttained * weight;
                totalWeight += weight;
            }

            int poPct = totalWeight == 0 ? 0 : (int) Math.round(weightedSum / totalWeight);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", po.getCode());
            row.put("shortName", po.getShortName());
            row.put("statement", po.getStatement());
            row.put("percent", poPct);
            row.put("contributingCos", contributingCos);
            row.put("metTarget", poPct >= PO_THRESHOLD);
            poAttainment.add(row);

            totalPct += poPct;
            if (poPct >= PO_THRESHOLD) above++; else below++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("poAttainment", poAttainment);
        result.put("coAttainment", coAttainment);
        result.put("courses", allCourses);
        result.put("overallPoAvg", pos.isEmpty() ? 0 : totalPct / pos.size());
        result.put("posAboveThreshold", above);
        result.put("posBelowThreshold", below);
        result.put("totalProjectsTagged", projectsTagged);
        return result;
    }

    /** CSV export of PO attainment -- what assessors want. */
    @GetMapping(value = "/teacher/obe/attainment/export", produces = "text/csv")
    @ResponseBody
    public ResponseEntity<String> exportAttainment(
            @RequestParam(value = "department", required = false) String department,
            @RequestParam(value = "batch", required = false) String batch) {

        List<Student> students = studentRepository.findAll().stream()
                .filter(s -> department == null || department.isBlank() || department.equalsIgnoreCase(s.getDepartment()))
                .filter(s -> batch == null || batch.isBlank() || batch.equalsIgnoreCase(s.getBatch()))
                .toList();

        Map<String, Object> result = computeAttainment(students);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> poAttainment = (List<Map<String, Object>>) result.get("poAttainment");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> coAttainment = (List<Map<String, Object>>) result.get("coAttainment");

        StringBuilder sb = new StringBuilder();
        sb.append("=== PROGRAM OUTCOME ATTAINMENT ===\n");
        sb.append("PO Code,Short Name,Attainment %,Contributing COs,Met Target (60%)\n");
        for (Map<String, Object> r : poAttainment) {
            sb.append(r.get("code")).append(',')
              .append(csv((String) r.get("shortName"))).append(',')
              .append(r.get("percent")).append(',')
              .append(r.get("contributingCos")).append(',')
              .append(((Boolean) r.get("metTarget")) ? "YES" : "NO").append('\n');
        }
        sb.append("\n=== COURSE OUTCOME ATTAINMENT ===\n");
        sb.append("Course,CO Code,Statement,Bloom Level,Evidence Count,Attainment %,Target %,Met Target\n");
        for (Map<String, Object> r : coAttainment) {
            sb.append(csv((String) r.get("courseCode"))).append(',')
              .append(r.get("code")).append(',')
              .append(csv((String) r.get("statement"))).append(',')
              .append(r.get("bloom")).append(',')
              .append(r.get("contributions")).append(',')
              .append(r.get("attained")).append(',')
              .append(r.get("target")).append(',')
              .append(((Boolean) r.get("metTarget")) ? "YES" : "NO").append('\n');
        }

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"obe-attainment-report.csv\"")
                .header("Content-Type", "text/csv; charset=utf-8")
                .body(sb.toString());
    }

    // ==================================================================
    //  STUDENT -- My Outcomes
    // ==================================================================

    @GetMapping("/student/outcomes")
    public String studentOutcomes(@AuthenticationPrincipal UserDetails u, Model model) {
        Student student = getCurrentStudent(u);
        Map<String, Object> attainment = computeStudentAttainment(student);

        model.addAttribute("student", student);
        model.addAttribute("poAttainment", attainment.get("poAttainment"));
        model.addAttribute("coAttainment", attainment.get("coAttainment"));
        model.addAttribute("overallPoAvg", attainment.get("overallPoAvg"));
        model.addAttribute("posStrongCount", attainment.get("posStrongCount"));
        model.addAttribute("posWeakCount", attainment.get("posWeakCount"));
        model.addAttribute("strongestPo", attainment.get("strongestPo"));
        model.addAttribute("weakestPo", attainment.get("weakestPo"));
        model.addAttribute("totalEvidence", attainment.get("totalEvidence"));
        return "student/outcomes";
    }

    /**
     * Computes a single student's attainment. Same formula as cohort but scoped to one student.
     * Per-student rule: each tagged project = 25% toward CO (4 projects -> 100%).
     */
    private Map<String, Object> computeStudentAttainment(Student student) {
        List<ProgramOutcome> pos = poRepository.findAllByOrderByCodeAsc();
        List<CourseOutcome> allCos = coRepository.findAll();
        Map<Long, CourseOutcome> coById = allCos.stream()
                .collect(Collectors.toMap(CourseOutcome::getId, c -> c));
        Map<Long, Course> courseById = courseRepository.findAll().stream()
                .collect(Collectors.toMap(Course::getId, c -> c));

        // The student's own projects
        List<Project> myProjects = projectRepository.findByStudentId(student.getId());

        // Count contributions per CO (only from this student's projects)
        Map<Long, Long> contributionsPerCo = new HashMap<>();
        long totalEvidence = 0;
        for (Project p : myProjects) {
            List<Long> coIds = parseCoIds(p.getCourseOutcomeIds());
            for (Long coId : coIds) {
                contributionsPerCo.merge(coId, 1L, Long::sum);
                totalEvidence++;
            }
        }

        // CO attainment per student (25% per piece of evidence)
        List<Map<String, Object>> coAttainment = new ArrayList<>();
        for (CourseOutcome co : allCos) {
            long contributions = contributionsPerCo.getOrDefault(co.getId(), 0L);
            if (contributions == 0) continue; // only show COs the student has touched
            int pct = (int) Math.min(100, contributions * 25);

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", co.getCode());
            row.put("statement", co.getStatement());
            row.put("bloom", co.getBloomLevel());
            row.put("contributions", contributions);
            row.put("attained", pct);
            Course c = courseById.get(co.getCourseId());
            row.put("courseCode", c != null ? c.getCourseCode() : "?");
            row.put("courseName", c != null ? c.getCourseName() : "?");
            coAttainment.add(row);
        }

        // PO attainment per student
        List<Map<String, Object>> poAttainment = new ArrayList<>();
        int totalPct = 0;
        int strong = 0, weak = 0;
        String strongestPo = "\u2014", weakestPo = "\u2014";
        int maxPct = -1, minPct = 101;

        for (ProgramOutcome po : pos) {
            double weightedSum = 0.0;
            double totalWeight = 0.0;
            int evidenceCount = 0;

            for (CourseOutcome co : allCos) {
                Map<String, Integer> mapping = parsePoMappings(co.getPoMappings());
                Integer w = mapping.get(po.getCode());
                if (w == null) continue;

                long contributions = contributionsPerCo.getOrDefault(co.getId(), 0L);
                int coPct = (int) Math.min(100, contributions * 25);

                weightedSum += coPct * w;
                totalWeight += w;
                evidenceCount += contributions;
            }

            int poPct = totalWeight == 0 ? 0 : (int) Math.round(weightedSum / totalWeight);

            String tier;
            if (poPct >= 75) { tier = "STRONG"; strong++; }
            else if (poPct >= 40) tier = "DEVELOPING";
            else { tier = "EMERGING"; weak++; }

            if (poPct > maxPct) { maxPct = poPct; strongestPo = po.getCode() + " \u2014 " + po.getShortName(); }
            if (poPct < minPct) { minPct = poPct; weakestPo = po.getCode() + " \u2014 " + po.getShortName(); }

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("code", po.getCode());
            row.put("shortName", po.getShortName());
            row.put("statement", po.getStatement());
            row.put("percent", poPct);
            row.put("evidence", evidenceCount);
            row.put("tier", tier);
            poAttainment.add(row);

            totalPct += poPct;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("poAttainment", poAttainment);
        result.put("coAttainment", coAttainment);
        result.put("overallPoAvg", pos.isEmpty() ? 0 : totalPct / pos.size());
        result.put("posStrongCount", strong);
        result.put("posWeakCount", weak);
        result.put("strongestPo", maxPct < 0 ? "\u2014" : strongestPo);
        result.put("weakestPo", minPct > 100 ? "\u2014" : weakestPo);
        result.put("totalEvidence", totalEvidence);
        return result;
    }

    // ==================================================================
    //  Helpers
    // ==================================================================

    private static String csv(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
