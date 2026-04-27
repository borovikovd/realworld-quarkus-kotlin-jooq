# Change Management — Compensating Controls

This repository is solo-maintained. GitHub does not allow a pull request author to approve their own pull request, so a hard `required_approving_review_count >= 1` rule would make every change unmergeable. Instead, the change-management control objective is met by the following enforced compensating controls. This document is the auditor-facing rationale; the enforced configuration is the evidence.

**Standards mapped:** SOC 2 CC8.1 (change management), CC7.1 (vulnerability management), CC6.6 (supply chain). ISO 27001 A.8.32 (change management), A.8.8 (vulnerabilities), A.5.19 / A.5.21 (supplier relationships, ICT supply chain).

## Enforced controls

1. **All changes go through a pull request.** Direct pushes to `master` are blocked by branch protection for everyone, including repository admins (`enforce_admins: true`). Force-push and branch deletion are disabled.
2. **No admin bypass.** `enforce_admins: true` removes the GitHub-default admin override. The maintainer cannot push past the gate without first disabling protection — an action that is logged in the GitHub audit log and constitutes a documented break-glass event.
3. **Required automated checks (must all pass before merge):**
   - `build` — full Gradle gate: codegen → compile → tests (incl. ArchUnit) → ktlint → detekt → SpotBugs/FindSecBugs.
   - `Semgrep` — SAST.
   - `OWASP Dependency-Check` — SCA / CVE scanning of dependencies (CC7.1, A.8.8).
   - `SpotBugs + FindSecBugs` — bytecode security analysis.
   - `Gitleaks` — secret detection (A.5.17, A.8.24).
4. **Dependabot auto-merge is gated by the same required checks.** The auto-merge workflow uses `gh pr merge --auto`, which waits for every required status check. A dependency update with a known CVE cannot land because `OWASP Dependency-Check` will fail and block the merge.
5. **Conventional commits with scoped subjects** (`feat/fix/refactor/test/chore/docs(scope): subject`). Every change has a structured, greppable rationale in the git log.
6. **Immutable history.** `allow_force_pushes: false` and `allow_deletions: false` mean the audit trail (commits, PR discussions, check runs) cannot be rewritten after the fact.

## Audit trail

| Evidence | Location | Retention |
|----------|----------|-----------|
| Change record | Pull request (title, description, diff, linked issues) | Repository lifetime |
| Authorization | PR author identity, signed-off commits | Repository lifetime |
| Test/security results | GitHub Actions check runs and SARIF uploads | GitHub default (90 days for logs/artifacts) |
| Vulnerability findings | GitHub Security tab (Code scanning alerts) | Until dismissed/resolved |
| Branch-protection bypass | GitHub audit log | Per GitHub plan retention |

## Out-of-scope changes

If a change must bypass these controls (e.g. emergency hotfix while CI is down), the maintainer:

1. Records justification in the PR or commit message.
2. Restores branch protection within the same working day.
3. Logs the event for the next compliance review.

## Re-evaluation

When a second maintainer joins the project, the compensating-control rationale no longer applies. At that point set `required_pull_request_reviews.required_approving_review_count = 1` and remove this exception.
