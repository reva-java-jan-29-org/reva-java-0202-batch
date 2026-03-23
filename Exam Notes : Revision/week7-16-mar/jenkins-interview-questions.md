# Jenkins Interview Questions — Beginner Level

## Table of Contents
1. [Core Concepts](#1-core-concepts)
2. [Installation & Setup](#2-installation--setup)
3. [Jobs & Builds](#3-jobs--builds)
4. [Pipelines](#4-pipelines)
5. [Plugins & Integrations](#5-plugins--integrations)
6. [Credentials & Security](#6-credentials--security)
7. [Agents & Nodes](#7-agents--nodes)
8. [CI/CD Concepts](#8-cicd-concepts)

---

## 1. Core Concepts

**Q1. What is Jenkins?**
Jenkins is an open-source automation server written in Java. It is used to automate the building, testing, and deployment of software, enabling Continuous Integration (CI) and Continuous Delivery (CD).

---

**Q2. What is Continuous Integration (CI)?**
CI is a practice where developers frequently merge code changes into a shared repository. Each merge triggers an automated build and test process so that integration issues are caught early.

---

**Q3. What is Continuous Delivery (CD)?**
CD extends CI by automatically deploying every successful build to a staging or production environment, ensuring the software is always in a releasable state.

---

**Q4. What is the difference between CI and CD?**
| CI | CD |
|---|---|
| Automates build + test | Automates deployment after build + test |
| Detects integration bugs early | Ensures code is always deployable |
| Ends at a verified build artifact | Ends at a deployed environment |

---

**Q5. What language is Jenkins written in?**
Jenkins is written in **Java** and runs on the Java Virtual Machine (JVM).

---

**Q6. What is a Jenkins job?**
A job (also called a project) is a configurable unit of work in Jenkins. It defines *what* to do — e.g., pull code, run `mvn build`, run tests, and send notifications.

---

**Q7. What is a Jenkins build?**
A build is a single execution of a Jenkins job. Each time a job runs, it creates a new build with a unique build number (e.g., `#1`, `#2`).

---

**Q8. What is the Jenkins home directory and why is it important?**
The Jenkins home directory (default: `~/.jenkins` or `/var/lib/jenkins`) stores all configuration files, job definitions, build history, plugins, and credentials. Backing it up is critical for disaster recovery.

---

**Q9. What is a Jenkins workspace?**
A workspace is the directory on the agent/node where Jenkins checks out source code and runs build commands. Each job has its own workspace.

---

**Q10. What port does Jenkins run on by default?**
Jenkins runs on port **8080** by default. This can be changed in the startup configuration.

---

## 2. Installation & Setup

**Q11. What are the prerequisites to install Jenkins?**
- Java (JDK 11 or 17 recommended for modern Jenkins)
- Sufficient memory (minimum 256 MB RAM, 1 GB+ recommended)
- Disk space for build history and artifacts

---

**Q12. How do you install Jenkins on Linux?**
```bash
# Add Jenkins repo and install (Ubuntu/Debian example)
sudo wget -O /usr/share/keyrings/jenkins-keyring.asc \
  https://pkg.jenkins.io/debian-stable/jenkins.io-2023.key
echo "deb [signed-by=...] https://pkg.jenkins.io/debian-stable binary/" \
  | sudo tee /etc/apt/sources.list.d/jenkins.list > /dev/null
sudo apt update
sudo apt install jenkins
sudo systemctl start jenkins
```

---

**Q13. What is the Jenkins WAR file?**
`jenkins.war` is a Web Application Archive file that packages Jenkins as a self-contained web application. You can run it directly with:
```bash
java -jar jenkins.war
```

---

**Q14. What is the initial admin password and where is it located?**
During first-time setup, Jenkins generates a one-time admin password stored at:
```
/var/lib/jenkins/secrets/initialAdminPassword
```
You paste this into the browser setup wizard.

---

**Q15. What are the two setup options during first-time Jenkins installation?**
1. **Install suggested plugins** — installs commonly used plugins automatically.
2. **Select plugins to install** — lets you choose specific plugins manually.

---

**Q16. How do you upgrade Jenkins?**
- **WAR**: Replace the `jenkins.war` file and restart.
- **Package manager**: Run `sudo apt upgrade jenkins` (Debian) or equivalent.
- Always back up `JENKINS_HOME` before upgrading.

---

## 3. Jobs & Builds

**Q17. What types of Jenkins jobs/projects are available?**
| Type | Purpose |
|---|---|
| Freestyle project | Simple, GUI-configured build |
| Pipeline | Code-defined build using Jenkinsfile |
| Multi-branch Pipeline | Automatically discovers branches with Jenkinsfiles |
| Folder | Organizes jobs into groups |
| Maven project | Maven-specific build with extra Maven settings |

---

**Q18. What is a Freestyle project?**
The simplest Jenkins job type. You configure everything via the Jenkins UI — source control, build triggers, build steps (shell commands, Maven goals), and post-build actions (email, archive artifacts).

---

**Q19. What is a build trigger?**
A build trigger defines *when* a Jenkins job should run. Common triggers:
- **Poll SCM** — checks for code changes on a cron schedule
- **GitHub webhook** — GitHub pushes an event to Jenkins on each commit
- **Build periodically** — cron-based schedule (e.g., `H 2 * * *` = every night at 2 AM)
- **Trigger after another job** — upstream/downstream chaining

---

**Q20. What is the difference between "Poll SCM" and a webhook?**
| Poll SCM | Webhook |
|---|---|
| Jenkins asks GitHub "any changes?" on a schedule | GitHub tells Jenkins "there was a push" instantly |
| Introduces delay (depends on poll interval) | Near real-time, no delay |
| Uses Jenkins resources even when nothing changed | Efficient — only fires on actual events |

---

**Q21. What does the build number represent in Jenkins?**
Each execution of a job increments a build counter starting at `#1`. The build number is unique per job and is used to reference logs, artifacts, and test results of that specific run.

---

**Q22. What are build artifacts?**
Artifacts are files produced by a build (e.g., `.jar`, `.war`, `.zip`) that Jenkins archives so they can be downloaded or used by downstream jobs.

---

**Q23. How do you archive artifacts in Jenkins?**
In a Freestyle job, add a **Post-build Action → Archive the artifacts** and specify a file pattern such as `**/target/*.jar`. In a Pipeline:
```groovy
archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
```

---

**Q24. What is a "downstream" job in Jenkins?**
A downstream job is one that is triggered by another (upstream) job. For example, a `Build` job triggers a `Test` job as its downstream.

---

**Q25. What are build parameters in Jenkins?**
Parameters allow users or triggers to pass dynamic values into a build at runtime (e.g., environment name, version number). Defined under **This project is parameterized** and accessed in shell steps as `$PARAM_NAME`.

---

## 4. Pipelines

**Q26. What is a Jenkins Pipeline?**
A Pipeline is a suite of plugins that lets you define your entire CI/CD process as code in a `Jenkinsfile`. It provides better version control, code review, and reuse than Freestyle jobs.

---

**Q27. What is a Jenkinsfile?**
A `Jenkinsfile` is a text file checked into the source code repository that contains the Pipeline definition. It is written in Groovy-based DSL and defines all stages, steps, and post actions.

---

**Q28. What are the two syntaxes for writing a Jenkinsfile?**
1. **Declarative** — structured, easier to read, enforced schema. Recommended for most use cases.
2. **Scripted** — more flexible, full Groovy, `node { }` blocks. Used for advanced logic.

---

**Q29. What does a basic Declarative Jenkinsfile look like?**
```groovy
pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                sh 'mvn clean package'
            }
        }
        stage('Test') {
            steps {
                sh 'mvn test'
            }
        }
    }
    post {
        success { echo 'Build passed!' }
        failure { echo 'Build failed!' }
    }
}
```

---

**Q30. What is the `agent` directive in a Jenkinsfile?**
The `agent` directive tells Jenkins *where* to run the pipeline. Common values:
- `agent any` — run on any available node
- `agent none` — no global agent; each stage must define its own
- `agent { label 'linux' }` — run on a node with the `linux` label
- `agent { docker 'maven:3.9' }` — run inside a Docker container

---

**Q31. What are `stages` and `steps` in a Pipeline?**
- **`stages`** — a container for all the `stage` blocks (e.g., Build, Test, Deploy). Represents the high-level phases.
- **`steps`** — the actual commands inside a `stage` (e.g., `sh`, `echo`, `mvn`).

---

**Q32. What is the `post` section in a Declarative Pipeline?**
`post` defines actions to run after the pipeline or a stage completes. Common conditions:
- `always` — runs every time regardless of outcome
- `success` — runs only on success
- `failure` — runs only on failure
- `unstable` — runs when build is unstable (e.g., test failures)

---

**Q33. What is the difference between `sh` and `bat` steps?**
- `sh 'command'` — runs a shell command on **Linux/macOS** agents
- `bat 'command'` — runs a batch command on **Windows** agents

---

**Q34. What is `withCredentials` in Jenkins Pipelines?**
`withCredentials` is a step that temporarily injects secret values (passwords, tokens, SSH keys) into the build environment without exposing them in logs:
```groovy
withCredentials([usernamePassword(credentialsId: 'my-creds',
                                  usernameVariable: 'USER',
                                  passwordVariable: 'PASS')]) {
    sh 'docker login -u $USER -p $PASS'
}
```

---

**Q35. What is a Multi-branch Pipeline?**
A job type that automatically scans a repository for branches containing a `Jenkinsfile` and creates a separate pipeline job for each branch. When a branch is deleted, its job is also removed.

---

**Q36. What is the `environment` block in a Jenkinsfile?**
It defines environment variables available to all steps in the pipeline:
```groovy
environment {
    IMAGE_TAG = "1.0.${env.BUILD_NUMBER}"
    APP_ENV   = 'staging'
}
```

---

**Q37. What is the `options` block in a Declarative Pipeline?**
It configures pipeline-level settings such as:
```groovy
options {
    timeout(time: 30, unit: 'MINUTES')    // fail if build takes > 30 min
    buildDiscarder(logRotator(numToKeepStr: '10'))  // keep last 10 builds
    disableConcurrentBuilds()              // no parallel runs of same job
}
```

---

## 5. Plugins & Integrations

**Q38. What is a Jenkins plugin?**
A plugin extends Jenkins functionality. Jenkins has a minimal core, and almost everything else (Git integration, Docker, Slack notifications, JUnit reports) is added via plugins from the Jenkins Plugin Index.

---

**Q39. How do you install a plugin in Jenkins?**
Go to **Manage Jenkins → Plugins → Available plugins**, search for the plugin, select it, and click **Install**. A restart may be required.

---

**Q40. Name 10 commonly used Jenkins plugins.**
| Plugin | Purpose |
|---|---|
| Git | Integrates with Git repositories |
| Pipeline | Enables Jenkinsfile-based pipelines |
| Maven Integration | Better Maven project support |
| Docker Pipeline | Use Docker containers as build agents |
| Credentials | Secure storage of secrets |
| Blue Ocean | Modern pipeline visualization UI |
| JUnit | Publishes test reports |
| Slack Notification | Sends build status to Slack |
| AnsiColor | Colors console output |
| Workspace Cleanup | Deletes workspace before/after builds |

---

**Q41. What is the Git plugin used for in Jenkins?**
It allows Jenkins to check out source code from Git repositories (GitHub, GitLab, Bitbucket) using HTTPS or SSH, and supports branch/tag filtering and shallow clones.

---

**Q42. What is Blue Ocean in Jenkins?**
Blue Ocean is a modern, visual UI for Jenkins Pipelines. It shows pipeline stages as a visual flowchart, making it easier to see which stage failed and why.

---

**Q43. What is the JUnit plugin used for?**
It parses JUnit/Surefire XML test report files and displays test results (pass/fail counts, failure details, trends) directly in the Jenkins build page.
```groovy
junit '**/target/surefire-reports/*.xml'
```

---

**Q44. What is the Workspace Cleanup plugin?**
It provides a `cleanWs()` step that deletes the workspace before or after a build, ensuring a clean state and preventing leftover files from previous builds from interfering.

---

**Q45. What is the AnsiColor plugin?**
It renders ANSI escape codes in Jenkins console output, enabling colored text for better readability. Enabled with:
```groovy
options { ansiColor('xterm') }
```

---

## 6. Credentials & Security

**Q46. What are Jenkins credentials?**
Jenkins credentials securely store secrets (passwords, API tokens, SSH keys, certificates) so they can be injected into builds without being hardcoded in the Jenkinsfile or visible in logs.

---

**Q47. What types of credentials does Jenkins support?**
- Username + Password
- SSH Username with private key
- Secret text (API token, plain string)
- Secret file
- Certificate

---

**Q48. Where do you manage credentials in Jenkins?**
**Manage Jenkins → Credentials → System → Global credentials (unrestricted)**

You assign each credential a unique **ID** (e.g., `github-credentials`) and reference that ID in the Jenkinsfile.

---

**Q49. What is role-based access control (RBAC) in Jenkins?**
RBAC (provided by the **Role-based Authorization Strategy** plugin) lets you define roles (Admin, Developer, Read-only) and assign them to users or groups, restricting what each user can see or do in Jenkins.

---

**Q50. What is the Jenkins Script Console and why is it risky?**
**Manage Jenkins → Script Console** allows running arbitrary Groovy scripts on the Jenkins master. It is powerful (can read files, stop jobs, change config) and should be restricted to admins only.

---

## 7. Agents & Nodes

**Q51. What is a Jenkins agent (node)?**
An agent (also called a node or worker) is a machine that executes build steps. The Jenkins controller/master orchestrates jobs and delegates execution to agents.

---

**Q52. What is the difference between a Jenkins master and an agent?**
| Master / Controller | Agent / Node |
|---|---|
| Manages job scheduling, UI, config | Executes the actual build steps |
| Stores build history and credentials | Has its own workspace directory |
| Should not run heavy builds itself | Can be physical, VM, Docker, cloud |

---

**Q53. How do you connect an agent to Jenkins?**
Common methods:
- **SSH** — Jenkins SSHs into the agent machine and starts the agent process
- **JNLP/WebSocket** — agent machine connects outbound to the Jenkins controller
- **Docker** — a Docker container is spun up as an on-demand agent

---

**Q54. What is a label in Jenkins?**
A label is a tag assigned to an agent (e.g., `linux`, `docker`, `high-memory`). Pipeline jobs use labels to target specific agents:
```groovy
agent { label 'linux' }
```

---

**Q55. What is an ephemeral agent?**
An ephemeral agent is created on demand for a single build and destroyed afterward (e.g., a Docker container or a cloud VM). Opposed to persistent agents that run continuously.

---

## 8. CI/CD Concepts

**Q56. What is the build pipeline in Jenkins?**
A build pipeline is a sequence of automated steps (checkout → build → test → deploy) that code changes flow through. In Jenkins, this is represented by a `Jenkinsfile` with multiple `stage` blocks.

---

**Q57. What does "fail fast" mean in a CI pipeline?**
Fail fast means the pipeline stops and reports failure as soon as any step fails, rather than continuing and accumulating more errors. This saves time and resources.

---

**Q58. What is a Jenkins shared library?**
A shared library is a Git repository containing reusable Groovy code (steps, classes) that multiple Jenkinsfiles can import with `@Library('my-lib')`. It avoids copy-pasting pipeline logic across projects.

---

**Q59. What is the difference between `clean install` and `clean package` in Maven builds within Jenkins?**
| `mvn clean package` | `mvn clean install` |
|---|---|
| Compiles, tests, and packages the JAR/WAR | Same as package + installs artifact to local `.m2` cache |
| Does NOT install to local repo | Needed when other modules depend on this artifact |
| Faster for standalone services | Required for multi-module Maven builds |

---

**Q60. What is a Jenkins webhook and how does it work?**
A webhook is an HTTP callback URL on Jenkins (e.g., `http://jenkins-host/github-webhook/`) that GitHub/GitLab calls when a push or PR event occurs. Jenkins receives the event and immediately triggers the configured pipeline — eliminating the polling delay and making builds near-instant after a commit.

Setup:
1. In GitHub repo → **Settings → Webhooks → Add webhook**
2. Set Payload URL to `http://<jenkins-host>/github-webhook/`
3. Set content type to `application/json`
4. Select events: **Just the push event** (or PRs too)

---

*Last updated: March 2026*
