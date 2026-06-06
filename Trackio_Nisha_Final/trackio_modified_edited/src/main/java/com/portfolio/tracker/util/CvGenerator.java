package com.portfolio.tracker.util;

import com.portfolio.tracker.entity.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates an ATS-optimized CV from a student's stored portfolio.
 *
 * Two modes:
 *   1. Deterministic -- builds the CV from structured data with no external calls.
 *   2. AI-enhanced  -- passes the structured data to Claude with the master prompt.
 */
public final class CvGenerator {

    private CvGenerator() {}

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("MMM yyyy");

    /* ============================================================
       Public entry points \u2014 returns the CV as a Map of sections
       ready to render in the Thymeleaf preview.
       ============================================================ */

    public static CvData buildDeterministic(Student s,
                                            List<Project> projects,
                                            List<Certification> certs,
                                            List<Internship> internships,
                                            List<Achievement> achievements,
                                            String targetRole) {
        CvData cv = new CvData();

        // ---- Header ----
        cv.fullName = orDash(s.getName());
        cv.title = inferTitle(s, targetRole);
        cv.location = compactLocation(s.getAddress());
        cv.phone = orBlank(s.getPhone());
        cv.email = orBlank(s.getEmail());
        cv.linkedinUrl = orBlank(s.getLinkedinUrl());
        cv.githubUrl = orBlank(s.getGithubUrl());
        cv.portfolioUrl = orBlank(s.getPortfolioUrl());

        // ---- Hero summary ----
        cv.summary = buildSummary(s, projects, certs, internships, achievements, targetRole);

        // ---- Core competencies ----
        cv.competencies = buildCompetencies(s, projects);

        // ---- Experience (internships) ----
        for (Internship i : internships) {
            CvExperience exp = new CvExperience();
            exp.company = orDash(i.getCompanyName());
            exp.title = orDash(i.getRole());
            exp.period = fmtPeriod(i.getStartDate(), i.getEndDate());
            exp.bullets = synthesizeInternshipBullets(i);
            cv.experience.add(exp);
        }

        // ---- Projects ----
        int projectsCap = Math.min(projects.size(), 6);
        for (int idx = 0; idx < projectsCap; idx++) {
            Project p = projects.get(idx);
            CvProject proj = new CvProject();
            proj.title = orDash(p.getTitle());
            proj.context = p.getCourseName() != null ? p.getCourseName() : (p.getProjectType() != null ? p.getProjectType().name() : "");
            proj.period = fmtPeriod(p.getStartDate(), p.getEndDate());
            proj.bullet = projectBullet(p, s.getSkills());
            cv.projects.add(proj);
        }

        // ---- Certifications & Achievements ----
        for (Certification c : certs) {
            cv.credentials.add(
                orDash(c.getTitle())
                + (notBlank(c.getIssuingOrganization()) ? " \u2014 " + c.getIssuingOrganization() : "")
                + (c.getIssueDate() != null ? " (" + c.getIssueDate().getYear() + ")" : "")
            );
        }
        for (Achievement a : achievements) {
            cv.credentials.add(
                orDash(a.getPosition())
                + (notBlank(a.getEventName()) ? " \u2014 " + a.getEventName() : "")
                + (notBlank(a.getOrganizer()) ? ", " + a.getOrganizer() : "")
                + (a.getDate() != null ? " (" + a.getDate().getYear() + ")" : "")
            );
        }

        // ---- Education ----
        CvEducation edu = new CvEducation();
        edu.degree = (notBlank(s.getDepartment()) ? "B.Sc. in " + s.getDepartment() : "Bachelor's Degree");
        edu.institution = "Daffodil International University";
        edu.year = notBlank(s.getBatch()) ? "Batch " + s.getBatch() : "";
        cv.education.add(edu);

        return cv;
    }

    /* ============================================================
       AI-enhanced generation \u2014 calls Claude API with the master prompt.
       Falls back to deterministic if the API key is missing or call fails.
       ============================================================ */

    public static String buildWithClaude(Student s,
                                         List<Project> projects,
                                         List<Certification> certs,
                                         List<Internship> internships,
                                         List<Achievement> achievements,
                                         String targetRole,
                                         String targetJobDescription,
                                         String apiKey) throws Exception {

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                "ANTHROPIC_API_KEY environment variable is not set. " +
                "Set it before using AI mode, or use deterministic mode.");
        }

        String dataDump = serializeForLlm(s, projects, certs, internships, achievements,
                                          targetRole, targetJobDescription);

        // The master prompt provided by the user
        String systemPrompt = ATS_CV_PROMPT;

        // JSON body (escape carefully)
        String body = "{"
            + "\"model\":\"claude-sonnet-4-20250514\","
            + "\"max_tokens\":2000,"
            + "\"system\":" + jsonString(systemPrompt) + ","
            + "\"messages\":[{\"role\":\"user\",\"content\":" + jsonString(dataDump) + "}]"
            + "}";

        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.anthropic.com/v1/messages"))
            .timeout(Duration.ofSeconds(60))
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("content-type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Claude API returned " + response.statusCode() + ": " + response.body());
        }
        return extractTextFromClaudeResponse(response.body());
    }

    /* ============================================================
       Helpers
       ============================================================ */

    private static String inferTitle(Student s, String targetRole) {
        if (notBlank(targetRole)) return targetRole;
        String skills = s.getSkills() != null ? s.getSkills().toLowerCase() : "";
        if (skills.contains("spring") || skills.contains("java")) return "Java / Spring Boot Developer";
        if (skills.contains("react") || skills.contains("nextjs") || skills.contains("next.js")) return "Frontend / React Developer";
        if (skills.contains("python") && skills.contains("ml")) return "Machine Learning Engineer";
        if (skills.contains("android") || skills.contains("kotlin")) return "Android Developer";
        if (s.getDepartment() != null) return s.getDepartment() + " Student";
        return "Software Engineer";
    }

    private static String compactLocation(String address) {
        if (!notBlank(address)) return "";
        String[] parts = address.split(",");
        if (parts.length >= 2) {
            return parts[parts.length - 2].trim() + ", " + parts[parts.length - 1].trim();
        }
        return address.trim();
    }

    private static List<String> buildSummary(Student s,
                                             List<Project> projects,
                                             List<Certification> certs,
                                             List<Internship> internships,
                                             List<Achievement> achievements,
                                             String targetRole) {

        java.util.ArrayList<String> bullets = new java.util.ArrayList<>();

        String role = inferTitle(s, targetRole);
        String pcount = projects.size() + (projects.size() == 1 ? " project" : " projects");
        bullets.add(
            (s.getDepartment() != null ? s.getDepartment() + " student" : "Engineering student")
            + " specializing in " + role.toLowerCase()
            + (notBlank(s.getBatch()) ? ", Batch " + s.getBatch() : "")
            + "."
        );

        if (!projects.isEmpty()) {
            bullets.add("Delivered " + pcount + " spanning "
                + topKeywords(s.getSkills(), 3) + ", with focus on production-quality code and measurable outcomes.");
        }

        if (!internships.isEmpty()) {
            Internship i = internships.get(0);
            bullets.add("Industry exposure as " + orDash(i.getRole())
                + " at " + orDash(i.getCompanyName())
                + ", applying engineering best practices in a real-world team.");
        }

        if (!certs.isEmpty() || !achievements.isEmpty()) {
            bullets.add("Recognized contributor with "
                + certs.size() + " certification" + (certs.size() == 1 ? "" : "s")
                + " and " + achievements.size() + " achievement" + (achievements.size() == 1 ? "" : "s") + ".");
        }

        if (notBlank(s.getBio())) {
            // Take first sentence of bio if short enough.
            String bio = s.getBio().trim();
            int dot = bio.indexOf('.');
            String first = dot > 0 && dot < 180 ? bio.substring(0, dot + 1) : (bio.length() < 200 ? bio : bio.substring(0, 197) + "...");
            bullets.add(first);
        }

        return bullets;
    }

    private static String buildCompetencies(Student s, List<Project> projects) {
        java.util.LinkedHashSet<String> set = new java.util.LinkedHashSet<>();
        if (notBlank(s.getSkills())) {
            for (String tok : s.getSkills().split(",")) {
                String t = tok.trim();
                if (!t.isEmpty()) set.add(t);
            }
        }
        // Pull a few extra keywords from project descriptions
        String[] techHints = {"Java","Spring Boot","Spring","REST","API","SQL","PostgreSQL","MySQL",
                              "MongoDB","Redis","Docker","Kubernetes","AWS","GCP","Git","Linux",
                              "Python","JavaScript","TypeScript","React","Node.js","HTML","CSS",
                              "Tailwind","Thymeleaf","JPA","Hibernate","Maven","JUnit","Kotlin",
                              "Android","Firebase","Figma"};
        for (Project p : projects) {
            String txt = ((p.getTitle() != null ? p.getTitle() : "") + " " + (p.getDescription() != null ? p.getDescription() : "")).toLowerCase();
            for (String hint : techHints) {
                if (txt.contains(hint.toLowerCase())) set.add(hint);
                if (set.size() >= 20) break;
            }
            if (set.size() >= 20) break;
        }
        return String.join(", ", set);
    }

    private static List<String> synthesizeInternshipBullets(Internship i) {
        java.util.ArrayList<String> b = new java.util.ArrayList<>();
        String role = orDash(i.getRole());
        String company = orDash(i.getCompanyName());
        b.add("Contributed as " + role + " at " + company + ", working alongside cross-functional engineering teams.");
        if (i.getStatus() != null) {
            switch (i.getStatus().name()) {
                case "COMPLETED":
                    b.add("Completed full internship cycle, delivering all assigned modules within deadlines.");
                    break;
                case "ONGOING":
                    b.add("Currently delivering production features in an active engineering team.");
                    break;
                default:
                    b.add("Engaged in real-world engineering workflows with version control and code review.");
            }
        }
        b.add("Strengthened domain expertise through hands-on implementation and stakeholder collaboration.");
        return b;
    }

    private static String projectBullet(Project p, String skills) {
        String stack = topKeywords(skills, 3);
        String desc = p.getDescription();
        if (notBlank(desc)) {
            String shortDesc = desc.trim();
            // If description fits cleanly, use it; otherwise skip it entirely — no truncation
            if (shortDesc.length() > 160) {
                // Try to cut at the last sentence boundary within 160 chars
                int lastDot = shortDesc.lastIndexOf('.', 160);
                int lastComma = shortDesc.lastIndexOf(',', 160);
                int cutAt = Math.max(lastDot, lastComma);
                if (cutAt > 60) {
                    shortDesc = shortDesc.substring(0, cutAt + 1).trim();
                } else {
                    // No good sentence boundary — omit description, just show title + stack
                    return "Built " + p.getTitle() + (notBlank(stack) ? " using " + stack : "")
                         + (p.getCourseName() != null ? " for " + p.getCourseName() : "") + ".";
                }
            }
            return "Built " + p.getTitle() + " \u2014 " + shortDesc + (notBlank(stack) ? " (Stack: " + stack + ")" : "");
        }
        return "Built " + p.getTitle() + (notBlank(stack) ? " using " + stack : "")
             + (p.getCourseName() != null ? " for " + p.getCourseName() : "")
             + ".";
    }

    private static String topKeywords(String skills, int n) {
        if (!notBlank(skills)) return "Java, Spring Boot, REST APIs";
        String[] parts = skills.split(",");
        StringBuilder sb = new StringBuilder();
        int taken = 0;
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(t);
            taken++;
            if (taken >= n) break;
        }
        return sb.toString();
    }

    private static String fmtPeriod(java.time.LocalDate start, java.time.LocalDate end) {
        if (start == null && end == null) return "";
        String s = start != null ? D.format(start) : "\u2014";
        String e = end != null ? D.format(end) : "Present";
        return s + " \u2014 " + e;
    }

    private static String serializeForLlm(Student s, List<Project> projects, List<Certification> certs,
                                          List<Internship> internships, List<Achievement> achievements,
                                          String targetRole, String targetJobDescription) {
        StringBuilder b = new StringBuilder();
        b.append("CANDIDATE DATA\n==============\n");
        b.append("Name: ").append(orDash(s.getName())).append("\n");
        b.append("Email: ").append(orBlank(s.getEmail())).append("\n");
        b.append("Phone: ").append(orBlank(s.getPhone())).append("\n");
        b.append("Address: ").append(orBlank(s.getAddress())).append("\n");
        b.append("Department: ").append(orBlank(s.getDepartment())).append("\n");
        b.append("Batch: ").append(orBlank(s.getBatch())).append("\n");
        b.append("Registration No: ").append(orBlank(s.getRegistrationNo())).append("\n");
        b.append("GitHub: ").append(orBlank(s.getGithubUrl())).append("\n");
        b.append("LinkedIn: ").append(orBlank(s.getLinkedinUrl())).append("\n");
        b.append("Portfolio: ").append(orBlank(s.getPortfolioUrl())).append("\n");
        b.append("Skills: ").append(orBlank(s.getSkills())).append("\n");
        b.append("Bio: ").append(orBlank(s.getBio())).append("\n\n");

        b.append("PROJECTS (").append(projects.size()).append(")\n");
        for (Project p : projects) {
            b.append("- ").append(orDash(p.getTitle()));
            if (p.getCourseName() != null) b.append(" [").append(p.getCourseName()).append("]");
            b.append("\n  ").append(orBlank(p.getDescription())).append("\n");
        }

        b.append("\nCERTIFICATIONS (").append(certs.size()).append(")\n");
        for (Certification c : certs) {
            b.append("- ").append(orDash(c.getTitle()))
              .append(" \u2014 ").append(orBlank(c.getIssuingOrganization()));
            if (c.getIssueDate() != null) b.append(" (").append(c.getIssueDate()).append(")");
            b.append("\n");
        }

        b.append("\nINTERNSHIPS (").append(internships.size()).append(")\n");
        for (Internship i : internships) {
            b.append("- ").append(orDash(i.getRole()))
              .append(" at ").append(orDash(i.getCompanyName()))
              .append(" [").append(fmtPeriod(i.getStartDate(), i.getEndDate())).append("]\n");
        }

        b.append("\nACHIEVEMENTS (").append(achievements.size()).append(")\n");
        for (Achievement a : achievements) {
            b.append("- ").append(orDash(a.getPosition()))
              .append(" \u2014 ").append(orBlank(a.getEventName()))
              .append(notBlank(a.getOrganizer()) ? ", " + a.getOrganizer() : "")
              .append("\n");
        }

        b.append("\nTARGET ROLE: ").append(orBlank(targetRole)).append("\n");
        if (notBlank(targetJobDescription)) {
            b.append("\nTARGET JOB DESCRIPTION:\n").append(targetJobDescription).append("\n");
        }

        b.append("\nGenerate the CV in plain-text following the strict output structure defined in the system prompt.");
        return b.toString();
    }

    private static String jsonString(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"':  b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\b': b.append("\\b"); break;
                case '\f': b.append("\\f"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
            }
        }
        return b.append("\"").toString();
    }

    /** Pulls the text field out of Claude's JSON response. Minimal parser to avoid bringing in a JSON lib. */
    private static String extractTextFromClaudeResponse(String json) {
        // Look for "text":"..." inside the first content block
        int idx = json.indexOf("\"text\"");
        if (idx < 0) return json;
        int colon = json.indexOf(':', idx);
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0) return json;
        StringBuilder out = new StringBuilder();
        boolean escaped = false;
        for (int i = firstQuote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                switch (c) {
                    case 'n': out.append('\n'); break;
                    case 't': out.append('\t'); break;
                    case 'r': out.append('\r'); break;
                    case '"': out.append('"'); break;
                    case '\\': out.append('\\'); break;
                    case '/': out.append('/'); break;
                    default: out.append(c);
                }
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == '"') {
                break;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static String orDash(String s) { return notBlank(s) ? s : "\u2014"; }
    private static String orBlank(String s) { return notBlank(s) ? s : ""; }
    private static boolean notBlank(String s) { return s != null && !s.trim().isEmpty(); }

    /* ============================================================
       DTOs for the Thymeleaf preview
       ============================================================ */

    public static class CvData {
        public String fullName, title, location, phone, email, linkedinUrl, githubUrl, portfolioUrl;
        public List<String> summary = new java.util.ArrayList<>();
        public String competencies = "";
        public List<CvExperience> experience = new java.util.ArrayList<>();
        public List<CvProject> projects = new java.util.ArrayList<>();
        public List<String> credentials = new java.util.ArrayList<>();
        public List<CvEducation> education = new java.util.ArrayList<>();

        // Getters required by Thymeleaf SpringEL on public fields work too,
        // but explicit getters are friendlier:
        public String getFullName() { return fullName; }
        public String getTitle() { return title; }
        public String getLocation() { return location; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }
        public String getLinkedinUrl() { return linkedinUrl; }
        public String getGithubUrl() { return githubUrl; }
        public String getPortfolioUrl() { return portfolioUrl; }
        public List<String> getSummary() { return summary; }
        public String getCompetencies() { return competencies; }
        public List<CvExperience> getExperience() { return experience; }
        public List<CvProject> getProjects() { return projects; }
        public List<String> getCredentials() { return credentials; }
        public List<CvEducation> getEducation() { return education; }
    }

    public static class CvExperience {
        public String company, title, period;
        public List<String> bullets = new java.util.ArrayList<>();
        public String getCompany() { return company; }
        public String getTitle() { return title; }
        public String getPeriod() { return period; }
        public List<String> getBullets() { return bullets; }
    }

    public static class CvProject {
        public String title, context, period, bullet;
        public String getTitle() { return title; }
        public String getContext() { return context; }
        public String getPeriod() { return period; }
        public String getBullet() { return bullet; }
    }

    public static class CvEducation {
        public String degree, institution, year;
        public String getDegree() { return degree; }
        public String getInstitution() { return institution; }
        public String getYear() { return year; }
    }

    /* ============================================================
       The master ATS-optimized CV prompt (provided by the user)
       ============================================================ */

    private static final String ATS_CV_PROMPT =
        "You are an expert ATS-optimized CV generation engine embedded in a professional career platform.\n\n" +
        "Your task is to generate a highly tailored, modern, premium CV that is optimized for:\n" +
        "1. Human recruiters (7-second scan rule)\n" +
        "2. ATS parsing systems\n" +
        "3. Job-description alignment\n\n" +
        "A CV is not a biography. It is a strategic positioning document designed to maximize hiring probability by matching employer intent, keywords, and measurable impact.\n\n" +
        "CORE GENERATION STRATEGY:\n" +
        "1. RELEVANCE \u2014 prioritize content matching the target job.\n" +
        "2. IMPACT \u2014 prefer achievements with metrics (revenue, cost, time, scale, efficiency).\n" +
        "3. KEYWORD DENSITY \u2014 naturally include job-relevant keywords in skills, bullets, summary.\n" +
        "4. 7-SECOND RULE \u2014 top of CV must show role identity, strongest metric, key strength.\n\n" +
        "ROLE ADAPTATION:\n" +
        "- Junior: Projects + Education > Experience. Emphasize learning, internships.\n" +
        "- Mid: Experience primary, Projects supporting.\n" +
        "- Senior: Leadership, scale, ownership, system design.\n\n" +
        "OUTPUT STRUCTURE (strict order):\n" +
        "1. HEADER: Full Name, Professional Title, Location (City, Country), Phone, Email, LinkedIn, GitHub/Portfolio. No labels like 'CV'.\n" +
        "2. HERO VALUE SUMMARY: 3\u20135 bullets OR 2\u20133 tight lines. Include identity, specialization, quantified achievement, keyword alignment.\n" +
        "3. CORE COMPETENCIES: 12\u201320 hard skills, comma-separated. No soft skills.\n" +
        "4. PROFESSIONAL EXPERIENCE: Company | Location, Title, Duration, 1-line role context, 3-5 bullets each with action verb + impact + metric. Filter out tasks that don't affect revenue/cost/time/risk/performance/scale.\n" +
        "5. SELECTED PROJECTS: Built [X] using [Y] achieving [Z].\n" +
        "6. CERTIFICATIONS & ACHIEVEMENTS: industry-recognized only.\n" +
        "7. EDUCATION: Degree | University | Year. Minimal unless early career.\n\n" +
        "GLOBAL CONSTRAINTS:\n" +
        "- Plain text only. No tables, columns, icons, emojis, graphics.\n" +
        "- Strong action verbs: Engineered, Architected, Scaled, Optimized, Automated, Led, Built, Reduced, Increased, Designed.\n" +
        "- At least 60% of bullets must include metrics.\n" +
        "- 1 page (junior\u2013mid), 2 pages max (senior).\n\n" +
        "Return ONLY the final CV content. No explanations, comments, or formatting notes.";
}
