# Module 12 — Jenkins CI/CD Pipeline for Microservices

**Type:** Hands-on Implementation + Deep Concepts
**Duration:** ~5–6 hours
**Prerequisites:** Module 9 (Docker + Jib images built and tested), Git repo set up on GitHub
**Goal:** Set up a fully automated Jenkins CI/CD pipeline that compiles, tests, builds Docker images using Jib, pushes them to Docker Hub, and redeploys the full stack via Docker Compose — all triggered automatically on every `git push`.

---

## What You Will Build

```
Developer pushes code to GitHub
         │
         ▼
  GitHub Webhook fires
         │
         ▼
┌─────────────────────────────────────────────────────┐
│                  JENKINS PIPELINE                   │
│                                                     │
│  Stage 1: Checkout  →  Pull latest code             │
│  Stage 2: Build     →  mvn compile (all services)   │
│  Stage 3: Test      →  mvn test   (unit tests)      │
│  Stage 4: Jib Push  →  Build + push images to Hub   │
│  Stage 5: Deploy    →  docker compose up on server  │
└─────────────────────────────────────────────────────┘
         │
         ▼
  All 6 services live and reachable via :8080
```

---

## Learning Objectives

By the end of this module you will be able to:

1. Explain what Jenkins is, how it works, and why organisations use it over cloud-hosted CI tools
2. Install Jenkins on macOS (local) and understand production server setup
3. Configure Jenkins plugins for Java, Maven, Docker, and Git
4. Write a **declarative Jenkinsfile** that models a real-world pipeline
5. Store secrets (Docker Hub credentials, passwords) safely in Jenkins Credentials Manager
6. Trigger builds automatically via GitHub webhooks
7. Understand and distinguish **CI** (Continuous Integration) from **CD** (Continuous Delivery / Deployment)
8. Interpret pipeline stage logs and debug build failures

---

## 12.1 CI/CD Fundamentals — Concepts First

### What is Continuous Integration (CI)?

CI is the practice of **automatically building and testing every code change** as soon as it is pushed to the shared repository.

**Without CI:**
```
Dev A works alone for 2 weeks  →  Merges  →  CONFLICT HELL
Dev B works alone for 2 weeks  →  Merges  →  Tests fail
                                             Nobody knows why
                                             "It worked on my machine"
```

**With CI:**
```
Dev A pushes  →  Pipeline runs in 5 minutes  →  Tests pass ✓
Dev B pushes  →  Pipeline runs in 5 minutes  →  Test fails ✗
                                              →  Dev B gets email immediately
                                              →  Bug is fixed while still fresh
```

The core rule: **integrate early and often so problems surface immediately**.

---

### What is Continuous Delivery (CD)?

CD extends CI by **automatically preparing a release-ready artifact** after every successful build. Every green build is deployable — it just waits for a human to press "Deploy to Production".

```
CI Pipeline passes  →  Docker image built  →  Pushed to registry
                                          →  Staging deployed automatically
                                          →  Human approves  →  Prod deploy
```

### What is Continuous Deployment?

The final step: **no human approval**. Every green build goes straight to production automatically. Used by Netflix, Amazon, etc.

```
Code pushed  →  Tests pass  →  AUTO-DEPLOYED to production
```

---

### CI/CD Pipeline Stages (Universal)

```
┌───────────┐   ┌───────────┐   ┌───────────┐   ┌───────────┐   ┌──────────┐
│  SOURCE   │ → │   BUILD   │ → │   TEST    │ → │  PACKAGE  │ → │  DEPLOY  │
│           │   │           │   │           │   │           │   │          │
│ git push  │   │ mvn       │   │ unit test │   │ jib:build │   │ compose  │
│ triggers  │   │ compile   │   │ integr.   │   │ push img  │   │ up -d    │
│ webhook   │   │           │   │ test      │   │ to hub    │   │          │
└───────────┘   └───────────┘   └───────────┘   └───────────┘   └──────────┘
```

---

## 12.2 What is Jenkins?

Jenkins is an **open-source automation server** written in Java. It is the most widely used CI/CD tool in enterprise environments.

### Jenkins vs GitHub Actions vs GitLab CI

| Feature | Jenkins | GitHub Actions | GitLab CI |
|---|---|---|---|
| Hosting | Self-hosted (you control) | GitHub cloud | GitLab cloud/self |
| Cost | Free (infra cost only) | Free tier + paid minutes | Free tier + paid |
| Flexibility | Extremely high | High | High |
| Plugin ecosystem | 1800+ plugins | GitHub Marketplace | Limited |
| Learning curve | Steeper | Gentler | Medium |
| Best for | Enterprise, air-gapped | Open source projects | GitLab users |
| Runs on | Your server | GitHub's servers | GitLab's servers |

**Why Jenkins for enterprises?**
- Full control over the build environment
- No dependency on an external cloud
- Works with any Git host (GitHub, GitLab, Bitbucket, Azure DevOps)
- Compliance: build artifacts never leave your infrastructure
- 1800+ plugins for any tool imaginable

### Jenkins Architecture

```
┌────────────────────────────────────────────────────┐
│                  JENKINS MASTER                    │
│                                                    │
│  • Hosts the Jenkins UI (port 8085)                │
│  • Schedules jobs                                  │
│  • Manages credentials                             │
│  • Coordinates agents                              │
│  • Stores build history / logs                     │
└───────────────────┬────────────────────────────────┘
                    │  (distributes work to)
        ┌───────────┼───────────┐
        ▼           ▼           ▼
   ┌─────────┐ ┌─────────┐ ┌─────────┐
   │ Agent 1 │ │ Agent 2 │ │ Agent 3 │
   │ (Linux) │ │ (macOS) │ │ (Win)   │
   │         │ │         │ │         │
   │ Runs    │ │ Runs    │ │ Runs    │
   │ builds  │ │ builds  │ │ builds  │
   └─────────┘ └─────────┘ └─────────┘
```

For our setup: **Jenkins master = build agent** (single machine, common for learning and small teams).

---

## 12.3 Installing Jenkins on macOS (Local Setup)

### Step 1 — Install Java (if not already installed)

Jenkins requires Java 17. Check if you have it:

```bash
java -version
```

Expected output:
```
openjdk version "17.x.x" ...
```

If missing, install via Homebrew:

```bash
brew install openjdk@17
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
java -version
```

---

### Step 2 — Install Jenkins via Homebrew

```bash
brew install jenkins-lts
```

`jenkins-lts` installs the **Long Term Support** version — the most stable, recommended for production.

Verify:
```bash
jenkins-lts --version
```

> **Note:** Homebrew installs the binary as `jenkins-lts`, not `jenkins`. Always use `jenkins-lts` as the command name.

---

### Step 3 — Start Jenkins

```bash
brew services start jenkins-lts
```

This starts Jenkins as a background service and sets it to auto-start on login.

Check it is running:
```bash
brew services list | grep jenkins
```

Expected:
```
jenkins-lts  started  vishalshah ~/Library/LaunchAgents/homebrew.mxcl.jenkins-lts.plist
```

---

### Step 4 — Open Jenkins UI

Open your browser and navigate to:
```
http://localhost:8080
```

> **Note:** Jenkins LTS via Homebrew runs on port **8080** by default on macOS.

---

### Step 5 — Unlock Jenkins

Jenkins will show an "Unlock Jenkins" screen. It wants an initial admin password stored in a file.

Get the password:
```bash
cat /opt/homebrew/var/jenkins/secrets/initialAdminPassword
```

Copy the output and paste it into the browser. Click **Continue**.

> **What is this?** This one-time password protects Jenkins from being configured by someone else before you get to it.

---

### Step 6 — Install Suggested Plugins

On the "Customize Jenkins" screen, click **"Install suggested plugins"**.

This installs: Git, GitHub, Maven, Pipeline, Credentials, etc.

Wait for all plugins to install (2–5 minutes depending on internet speed).

---

### Step 7 — Create Admin User

Fill in:
- Username: `admin`
- Password: something you will remember (e.g., `Admin123`)
- Full name: Your name
- Email: your email

Click **Save and Continue → Save and Finish → Start using Jenkins**.

---

### Step 8 — Verify Jenkins Home Directory

Jenkins stores all its data here:
```bash
ls /opt/homebrew/var/jenkins/
```

Important subdirectories:
```
jobs/           ← all pipeline definitions and build history
workspace/      ← checked-out code for each build
plugins/        ← installed plugins
secrets/        ← credentials and secret files
logs/           ← jenkins.log
```

---

## 12.3-B Installing Jenkins on Windows

> Skip this section if you are on macOS. This covers the exact same outcome — Jenkins running locally — but using the Windows MSI installer.

### Prerequisites Check

Open **Command Prompt** (Win+R → type `cmd` → Enter) and verify Java is installed:

```cmd
java -version
```

If you see an error, install Java 17 first:

1. Go to: **https://adoptium.net/temurin/releases/**
2. Filter: Version = **17**, OS = **Windows**, Architecture = **x64**
3. Download the `.msi` file (e.g., `OpenJDK17U-jdk_x64_windows_hotspot_17.x.x.msi`)
4. Run the installer — tick **"Set JAVA_HOME variable"** and **"Add to PATH"** during setup
5. Open a **new** Command Prompt and verify: `java -version`

---

### Step 1 — Download the Jenkins Windows Installer

1. Go to: **https://www.jenkins.io/download/**
2. Under **"Long-Term Support Release"** (LTS — stable column), click **Windows**
3. This downloads a file named `jenkins.msi` (around 90 MB)

> Always use the **LTS** version — not the Weekly version — for stability.

---

### Step 2 — Run the Installer

1. Double-click `jenkins.msi`
2. Click **Next** on the welcome screen
3. **Destination Folder** — leave the default:
   ```
   C:\Program Files\Jenkins\
   ```
   Click **Next**
4. **Service Logon Credentials** screen:
   - Select **"Run service as LocalSystem"** (simplest for local dev)
   - Click **Next**

   > For a real server, create a dedicated `jenkins` Windows user account and use that instead of LocalSystem.

5. **Port** — leave as **8080** (or change if 8080 is taken by something else)
   - Click **Test Port** — it should say "Port is available" (green)
   - Click **Next**
6. **Java Home Directory** — Jenkins will auto-detect your Java 17 install
   - Verify the path shown points to your JDK 17 (not JRE, not a different version)
   - Click **Next**
7. **Firewall Exception** — tick **"Allow access"** so Jenkins can receive webhooks
   - Click **Next**
8. Click **Install** — Windows will ask for admin permission, click **Yes**
9. Click **Finish**

Jenkins is now installed as a **Windows Service** and starts automatically.

---

### Step 3 — Verify Jenkins is Running

Open **Services** to confirm:
- Press Win+R → type `services.msc` → Enter
- Find **"Jenkins"** in the list
- Status should be: **Running**
- Startup type: **Automatic** (starts on boot)

Or check from Command Prompt:
```cmd
sc query jenkins
```

Expected output includes: `STATE : 4 RUNNING`

---

### Step 4 — Open Jenkins UI

Open your browser and go to:
```
http://localhost:8080
```

You should see the **"Unlock Jenkins"** page.

---

### Step 5 — Unlock Jenkins

Get the initial admin password. On Windows it is at:

```
C:\ProgramData\Jenkins\.jenkins\secrets\initialAdminPassword
```

Open it in Notepad:
```cmd
notepad "C:\ProgramData\Jenkins\.jenkins\secrets\initialAdminPassword"
```

Copy the contents (a long hex string) and paste it into the browser. Click **Continue**.

> **Can't find the file?** Try this alternate location:
> ```
> C:\Windows\System32\config\systemprofile\AppData\Local\Jenkins\.jenkins\secrets\initialAdminPassword
> ```

---

### Step 6 — Install Suggested Plugins

Click **"Install suggested plugins"** and wait for all plugins to download and install (2–5 minutes).

---

### Step 7 — Create Admin User

Fill in your username, password, name, and email. Click **Save and Continue → Save and Finish → Start using Jenkins**.

Jenkins is now fully set up on Windows. Continue from **Section 12.4** — everything from that point is identical regardless of OS.

---

### Managing Jenkins on Windows

**Stop / Start / Restart from Services:**
- Win+R → `services.msc` → find **Jenkins** → right-click → Stop / Start / Restart

**Or from Command Prompt (as Administrator):**
```cmd
net stop  Jenkins
net start Jenkins
```

**Jenkins home directory on Windows (important to know):**
```
C:\ProgramData\Jenkins\.jenkins\
```
This is where job configs, build history, credentials, and plugins are stored.

**View Jenkins logs:**
```
C:\Program Files\Jenkins\jenkins.out.log
```
Or from the Jenkins UI: **Manage Jenkins → System Log → All Jenkins Logs**

---

### Windows-Specific: Adding Maven to PATH

Jenkins needs Maven accessible on PATH. If `mvn -version` fails in a Jenkins pipeline:

1. Download Maven: **https://maven.apache.org/download.cgi** → Binary zip archive
2. Extract to: `C:\tools\apache-maven-3.9.x\`
3. Add to PATH:
   - Win+S → "Environment Variables" → **Edit the system environment variables**
   - Click **Environment Variables**
   - Under **System variables** → find `Path` → click **Edit**
   - Click **New** → add: `C:\tools\apache-maven-3.9.x\bin`
   - Click **OK** on all dialogs
4. Open a **new** Command Prompt: `mvn -version` should now work
5. In Jenkins: **Manage Jenkins → Tools → Maven installations**
   - Name: `Maven-3`
   - Untick "Install automatically"
   - MAVEN_HOME: `C:\tools\apache-maven-3.9.x`

---

### Windows-Specific: Docker with Jenkins

For the deploy stage, ensure Docker Desktop is installed and running:

1. Download: **https://www.docker.com/products/docker-desktop/**
2. Install Docker Desktop for Windows (requires WSL 2)
3. After install, open Docker Desktop and let it start fully
4. Add the Jenkins service account to the docker-users group:
   ```cmd
   net localgroup docker-users "NT AUTHORITY\SYSTEM" /add
   ```
5. Restart the Jenkins service after adding it to the group

Test in Command Prompt:
```cmd
docker --version
docker compose version
```

---

## 12.4 Configuring Jenkins for Our Project

### Step 1 — Install Required Plugins

Go to: **Manage Jenkins → Plugins → Available plugins**

Search for and install each of the following (check the checkbox, then click **Install**):

| Plugin | Purpose |
|---|---|
| **Pipeline** | Enables Jenkinsfile-based declarative pipelines |
| **Git** | Checkout from Git repositories |
| **GitHub Integration** | Receive webhooks from GitHub |
| **Maven Integration** | Maven build support |
| **Docker Pipeline** | Docker commands inside pipelines |
| **Credentials Binding** | Inject secrets as environment variables |
| **Blue Ocean** | Modern pipeline UI (optional but recommended) |
| **AnsiColor** | Colored console output |

After selecting all, click **Install**. Tick **"Restart Jenkins when installation is complete"**.

Wait for Jenkins to restart, then log in again.

---

### Step 2 — Configure JDK in Jenkins

Go to: **Manage Jenkins → Tools**

Scroll to **JDK installations** → Click **Add JDK**:
- Name: `Java-17`
- JAVA_HOME: `/opt/homebrew/opt/openjdk@17` (on Apple Silicon) or `/usr/local/opt/openjdk@17` (Intel Mac)

> **How to find JAVA_HOME:**
> ```bash
> /usr/libexec/java_home -v 17
> ```
> Copy the output path and paste it as JAVA_HOME.

---

### Step 3 — Configure Maven in Jenkins

Scroll to **Maven installations** → Click **Add Maven**:
- Name: `Maven-3`
- Check: **Install automatically**
- Version: `3.9.x` (latest stable)

Click **Save**.

> Jenkins will auto-download Maven the first time a build runs. You don't need Maven installed locally on the Jenkins machine.

---

### Step 4 — Store Docker Hub Credentials

This is critical — we never hardcode passwords in the Jenkinsfile.

Go to: **Manage Jenkins → Credentials → System → Global credentials → Add Credentials**

Fill in:
- Kind: **Username with password**
- Scope: Global
- Username: `your-dockerhub-username`
- Password: `your-dockerhub-password` (or Docker Hub Access Token)
- ID: `dockerhub-credentials`   ← **this exact ID is referenced in the Jenkinsfile**
- Description: `Docker Hub login`

Click **Create**.

> **Best practice:** Use a Docker Hub **Access Token** instead of your real password. Generate one at: hub.docker.com → Account Settings → Security → New Access Token.

---

### Step 5 — Store GitHub Credentials (for private repos)

If your repository is private, add GitHub credentials too:

Go to: **Manage Jenkins → Credentials → System → Global credentials → Add Credentials**

- Kind: **Username with password**
- Username: `your-github-username`
- Password: `your-github-personal-access-token`
- ID: `github-credentials`
- Description: `GitHub PAT`

> Generate a GitHub PAT: github.com → Settings → Developer settings → Personal access tokens → Generate new token. Scopes needed: `repo`, `admin:repo_hook`.

---

## 12.5 Understanding Jib (Quick Recap)

Since our images are already built using Jib, let's understand why Jib works so well in a CI pipeline.

### Traditional Docker Build vs Jib

**Traditional flow:**
```
mvn package          →  creates fat JAR
docker build         →  needs Docker daemon running
docker push          →  pushes to registry
```

**Jib flow:**
```
mvn jib:build        →  builds AND pushes directly to registry
                        no Docker daemon needed on the CI server
                        no Dockerfile needed
                        layer caching is automatic
```

This means Jenkins does NOT need Docker to be running as a daemon to build images. Jib talks directly to the Docker Hub API using HTTP.

### Jib Configuration in pom.xml (reminder)

Each service has this in `pom.xml`:

```xml
<plugin>
    <groupId>com.google.cloud.tools</groupId>
    <artifactId>jib-maven-plugin</artifactId>
    <version>3.4.0</version>
    <configuration>
        <to>
            <image>your-dockerhub-username/user-service:${project.version}</image>
            <tags>
                <tag>latest</tag>
            </tags>
        </to>
        <container>
            <jvmFlags>
                <jvmFlag>-Xms256m</jvmFlag>
                <jvmFlag>-Xmx512m</jvmFlag>
            </jvmFlags>
            <ports>
                <port>8081</port>
            </ports>
        </container>
    </configuration>
</plugin>
```

In the Jenkinsfile we call:
```bash
mvn -pl user-service jib:build \
    -Djib.to.auth.username=$DOCKER_USER \
    -Djib.to.auth.password=$DOCKER_PASS
```

The `-pl user-service` flag tells Maven to only build that submodule. `-Djib.to.auth.*` passes credentials without storing them in `pom.xml`.

---

## 12.6 Writing the Jenkinsfile

The **Jenkinsfile** is a text file checked into your Git repository (at the root of the project). Jenkins reads it automatically when a build is triggered.

There are two syntaxes:
- **Scripted Pipeline** — Groovy DSL, flexible but complex
- **Declarative Pipeline** — structured, easier to read ← **we use this**

### Create the Jenkinsfile

Create this file at the project root:
```
week6-02-microservices-mini-project-03102026/Jenkinsfile
```

```groovy
// ─────────────────────────────────────────────────────────────────────────────
// Jenkinsfile — E-Commerce Microservices CI/CD Pipeline
//
// Pipeline stages:
//   1. Checkout   — pull latest code from GitHub
//   2. Build      — compile all Maven modules
//   3. Test       — run unit tests, fail fast on any failure
//   4. Jib Build  — build Docker images and push to Docker Hub
//   5. Deploy     — SSH to server and run docker compose up
//
// Required Jenkins credentials:
//   • dockerhub-credentials  (Username + Password)
//   • github-credentials     (Username + Password, for private repos)
//   • deploy-server-ssh      (SSH Username + Private Key, for remote deploy)
// ─────────────────────────────────────────────────────────────────────────────

pipeline {

    // ── Agent ─────────────────────────────────────────────────────────────
    // 'any' means: run on any available agent (our master doubles as agent)
    agent any

    // ── Environment Variables ─────────────────────────────────────────────
    // These are available throughout all stages as $VAR or env.VAR
    environment {
        // Docker Hub username — change this to YOUR username
        DOCKER_HUB_USER    = 'your-dockerhub-username'

        // The 'dockerhub-credentials' ID must match what you created in Jenkins
        // withCredentials injects DOCKER_HUB_PSW (password) automatically
        DOCKER_HUB_CREDS   = credentials('dockerhub-credentials')

        // Docker image tag — use the Git commit SHA for traceability
        // GIT_COMMIT is a built-in Jenkins environment variable
        IMAGE_TAG          = "${env.GIT_COMMIT?.take(8) ?: 'latest'}"

        // Services to build images for (space-separated)
        SERVICES           = 'user-service product-service order-service config-server eureka-server api-gateway'

        // Maven options — skip JavaDoc, run in offline mode after first build
        MAVEN_OPTS         = '-Dmaven.test.failure.ignore=false'
    }

    // ── Options ──────────────────────────────────────────────────────────
    options {
        // Keep only the last 10 builds to save disk space
        buildDiscarder(logRotator(numToKeepStr: '10'))

        // Fail the build if it runs longer than 30 minutes
        timeout(time: 30, unit: 'MINUTES')

        // Add timestamps to every console log line
        timestamps()

        // Color the console output (requires AnsiColor plugin)
        ansiColor('xterm')

        // Do not run concurrent builds of the same pipeline
        disableConcurrentBuilds()
    }

    // ── Triggers ─────────────────────────────────────────────────────────
    triggers {
        // Poll GitHub every 5 minutes as a fallback if webhooks are not set up
        // Format: cron — M H DOM MON DOW
        // 'H/5 * * * *' = every 5 minutes (H adds jitter to spread load)
        pollSCM('H/5 * * * *')

        // Primary trigger: GitHub webhook (configured separately, see Section 12.8)
        // When webhook fires, Jenkins receives a POST to /github-webhook/
        githubPush()
    }

    // ── Stages ────────────────────────────────────────────────────────────
    stages {

        // ── Stage 1: Checkout ─────────────────────────────────────────────
        stage('Checkout') {
            steps {
                echo '═══════════════════════════════════'
                echo ' STAGE 1 — Checkout Source Code'
                echo '═══════════════════════════════════'

                // checkout scm = checkout from the SCM (Source Control Management)
                // configured on the Jenkins pipeline job itself
                // Jenkins automatically uses the branch that triggered the build
                checkout scm

                // Print info about what we just checked out
                sh '''
                    echo "Branch    : $GIT_BRANCH"
                    echo "Commit    : $GIT_COMMIT"
                    echo "Author    : $GIT_AUTHOR_NAME"
                    echo "Workspace : $WORKSPACE"
                    echo ""
                    echo "Project structure:"
                    ls -la
                '''
            }
        }

        // ── Stage 2: Build ────────────────────────────────────────────────
        stage('Build') {
            steps {
                echo '═══════════════════════════════════'
                echo ' STAGE 2 — Maven Build'
                echo '═══════════════════════════════════'

                // Navigate into the project directory
                // -pl specifies which modules to build
                // -am = also build dependency modules
                // -DskipTests = skip tests here; we run them in the next stage
                sh '''
                    cd week6-02-microservices-mini-project-03102026
                    mvn clean compile \
                        -DskipTests \
                        --batch-mode \
                        --no-transfer-progress
                '''
                // --batch-mode     = no interactive prompts, machine-friendly output
                // --no-transfer-progress = suppress download progress bars (cleaner logs)
            }
        }

        // ── Stage 3: Test ─────────────────────────────────────────────────
        stage('Test') {
            steps {
                echo '═══════════════════════════════════'
                echo ' STAGE 3 — Unit Tests'
                echo '═══════════════════════════════════'

                sh '''
                    cd week6-02-microservices-mini-project-03102026
                    mvn test \
                        --batch-mode \
                        --no-transfer-progress
                '''
            }

            // post — runs AFTER this stage, regardless of success/failure
            post {
                always {
                    // Publish JUnit test results so they appear in the Jenkins UI
                    // Jenkins looks for Surefire XML reports in target/surefire-reports/
                    junit(
                        testResults: '**/target/surefire-reports/*.xml',
                        allowEmptyResults: true
                    )
                }
                failure {
                    echo '❌ Tests FAILED — check the Test Results tab above'
                }
                success {
                    echo '✅ All tests passed'
                }
            }
        }

        // ── Stage 4: Build & Push Docker Images with Jib ─────────────────
        stage('Docker Build & Push') {
            // Only push images when on the main branch (not feature branches)
            when {
                branch 'main'
            }

            steps {
                echo '═══════════════════════════════════'
                echo ' STAGE 4 — Jib: Build & Push Images'
                echo '═══════════════════════════════════'

                // withCredentials injects the Docker Hub password into DOCKER_HUB_CREDS_PSW
                // and username into DOCKER_HUB_CREDS_USR
                // These variables exist ONLY inside this block — not logged, not leaked
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh '''
                        cd week6-02-microservices-mini-project-03102026

                        echo "Building and pushing images tagged: $IMAGE_TAG"

                        # ── Config Server ──────────────────────────────────
                        echo "→ config-server"
                        mvn -pl config-server jib:build \
                            -Djib.to.image="$DOCKER_USER/config-server:$IMAGE_TAG" \
                            -Djib.to.tags="latest" \
                            -Djib.to.auth.username="$DOCKER_USER" \
                            -Djib.to.auth.password="$DOCKER_PASS" \
                            --batch-mode --no-transfer-progress

                        # ── Eureka Server ──────────────────────────────────
                        echo "→ eureka-server"
                        mvn -pl eureka-server jib:build \
                            -Djib.to.image="$DOCKER_USER/eureka-server:$IMAGE_TAG" \
                            -Djib.to.tags="latest" \
                            -Djib.to.auth.username="$DOCKER_USER" \
                            -Djib.to.auth.password="$DOCKER_PASS" \
                            --batch-mode --no-transfer-progress

                        # ── User Service ───────────────────────────────────
                        echo "→ user-service"
                        mvn -pl user-service jib:build \
                            -Djib.to.image="$DOCKER_USER/user-service:$IMAGE_TAG" \
                            -Djib.to.tags="latest" \
                            -Djib.to.auth.username="$DOCKER_USER" \
                            -Djib.to.auth.password="$DOCKER_PASS" \
                            --batch-mode --no-transfer-progress

                        # ── Product Service ────────────────────────────────
                        echo "→ product-service"
                        mvn -pl product-service jib:build \
                            -Djib.to.image="$DOCKER_USER/product-service:$IMAGE_TAG" \
                            -Djib.to.tags="latest" \
                            -Djib.to.auth.username="$DOCKER_USER" \
                            -Djib.to.auth.password="$DOCKER_PASS" \
                            --batch-mode --no-transfer-progress

                        # ── Order Service ──────────────────────────────────
                        echo "→ order-service"
                        mvn -pl order-service jib:build \
                            -Djib.to.image="$DOCKER_USER/order-service:$IMAGE_TAG" \
                            -Djib.to.tags="latest" \
                            -Djib.to.auth.username="$DOCKER_USER" \
                            -Djib.to.auth.password="$DOCKER_PASS" \
                            --batch-mode --no-transfer-progress

                        # ── API Gateway ────────────────────────────────────
                        echo "→ api-gateway"
                        mvn -pl api-gateway jib:build \
                            -Djib.to.image="$DOCKER_USER/api-gateway:$IMAGE_TAG" \
                            -Djib.to.tags="latest" \
                            -Djib.to.auth.username="$DOCKER_USER" \
                            -Djib.to.auth.password="$DOCKER_PASS" \
                            --batch-mode --no-transfer-progress

                        echo "All images pushed successfully!"
                    '''
                }
            }
        }

        // ── Stage 5: Deploy ───────────────────────────────────────────────
        stage('Deploy') {
            // Only deploy when on main branch
            when {
                branch 'main'
            }

            steps {
                echo '═══════════════════════════════════'
                echo ' STAGE 5 — Deploy via Docker Compose'
                echo '═══════════════════════════════════'

                // For local Jenkins (Jenkins and deploy target are the same machine):
                sh '''
                    cd week6-02-microservices-mini-project-03102026

                    echo "Pulling latest images from Docker Hub..."
                    docker compose pull

                    echo "Recreating containers with zero-downtime rolling restart..."
                    docker compose up -d --remove-orphans

                    echo "Waiting 30 seconds for services to initialize..."
                    sleep 30

                    echo "Service status:"
                    docker compose ps
                '''
            }
        }

    }  // end stages

    // ── Post (global) ─────────────────────────────────────────────────────
    // Runs after ALL stages complete, regardless of outcome
    post {

        success {
            echo '╔══════════════════════════════════╗'
            echo '║  ✅  PIPELINE SUCCEEDED           ║'
            echo '╚══════════════════════════════════╝'
        }

        failure {
            echo '╔══════════════════════════════════╗'
            echo '║  ❌  PIPELINE FAILED              ║'
            echo '╚══════════════════════════════════╝'
        }

        unstable {
            // Unstable = build compiled but some tests failed
            echo '⚠️  Pipeline is UNSTABLE (test failures)'
        }

        always {
            // Clean up workspace after every build to save disk space
            // Comment this out if you need to inspect files after a failure
            cleanWs()
        }
    }

}
```

---

## 12.7 Creating the Jenkins Pipeline Job

### Step 1 — Create a New Pipeline Job

1. From the Jenkins dashboard, click **"+ New Item"** (top-left)
2. Enter name: `ecommerce-microservices`
3. Select **"Pipeline"**
4. Click **OK**

---

### Step 2 — Configure the Pipeline

You will see a configuration page with several sections. Fill them in:

**General tab:**
- Description: `CI/CD pipeline for the E-Commerce Microservices project`
- Check: **"GitHub project"**
  - Project URL: `https://github.com/your-username/your-repo-name/`

**Build Triggers tab:**
- Check: **"GitHub hook trigger for GITScm polling"** ← this enables webhook builds
- Check: **"Poll SCM"** (as fallback)
  - Schedule: `H/5 * * * *`

**Pipeline tab:**
- Definition: **"Pipeline script from SCM"**
  - SCM: **Git**
  - Repository URL: `https://github.com/your-username/your-repo-name.git`
  - Credentials: Select `github-credentials` (if private repo)
  - Branch Specifier: `*/main`
  - Script Path: `week6-02-microservices-mini-project-03102026/Jenkinsfile`

Click **Save**.

---

### Step 3 — Run the First Build Manually

On the pipeline page, click **"Build Now"** (left sidebar).

Jenkins will:
1. Clone your repository
2. Read the Jenkinsfile
3. Execute each stage in order
4. Report results

Click on the build number (`#1`) → click **"Console Output"** to watch the live log.

---

### Step 4 — Understand the Pipeline View

After a successful build, the pipeline page shows a **Stage View**:

```
Checkout | Build | Test | Docker Build & Push | Deploy
  ✅     |  ✅   |  ✅  |        ✅           |   ✅
  12s    |  45s  | 1m20s|       4m30s         |  42s
```

- Green = passed
- Red = failed
- Yellow = unstable
- Grey = skipped (e.g., due to `when` condition)

Click any stage box to see just that stage's logs.

---

## 12.8 Setting Up GitHub Webhooks

A webhook tells GitHub to send an HTTP POST request to Jenkins every time someone pushes code. Without this, Jenkins would only find out via polling (every 5 minutes).

### Step 1 — Jenkins Must Be Accessible from GitHub

GitHub needs to reach your Jenkins. Options:

**Option A — ngrok (for local dev/learning):**
```bash
# Install ngrok
brew install ngrok

# Expose local Jenkins on port 8085 to the internet
ngrok http 8080
```

ngrok will show:
```
Forwarding  https://abc123.ngrok.io  →  http://localhost:8085
```

Use `https://abc123.ngrok.io` as your Jenkins URL below.

**Option B — Jenkins on a real server:** Use the server's public IP or domain name directly.

---

### Step 2 — Configure the Webhook on GitHub

1. Open your GitHub repository
2. Go to: **Settings → Webhooks → Add webhook**
3. Fill in:
   - **Payload URL:** `https://abc123.ngrok.io/github-webhook/`
     - Note the trailing slash — it's required
   - **Content type:** `application/json`
   - **Secret:** Leave blank for now (add one later for security)
   - **Which events:** Select **"Just the push event"**
4. Click **Add webhook**

GitHub will immediately send a test ping. You'll see a green tick if Jenkins received it.

---

### Step 3 — Test the Webhook

Make a small change to any file in the repo and push:

```bash
git add .
git commit -m "test: trigger Jenkins CI pipeline"
git push origin main
```

Within seconds, Jenkins should start a new build automatically. Watch the pipeline page refresh.

---

### Step 4 — Secure the Webhook (Production)

Add a secret to the webhook so Jenkins can verify requests actually come from GitHub (not from an attacker sending fake webhooks):

1. Generate a secret: `openssl rand -hex 20`
2. Add it to GitHub webhook settings → **Secret** field
3. In Jenkins: **Manage Jenkins → Configure System → GitHub → GitHub Server**
   - Add the same secret as a credential

---

## 12.9 Credentials Management — Best Practices

### Why Never Hardcode Passwords

```groovy
// ❌ WRONG — password visible in Git history forever
sh "mvn jib:build -Djib.to.auth.password=Root123"

// ✅ CORRECT — password injected at runtime, never stored in code
withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials',
    usernameVariable: 'USER', passwordVariable: 'PASS')]) {
    sh "mvn jib:build -Djib.to.auth.password=$PASS"
}
```

### Jenkins Credentials Types

| Kind | Use case |
|---|---|
| Username with password | Docker Hub, GitHub, MySQL |
| SSH Username with private key | SSH to remote servers |
| Secret text | API tokens, JWT secrets |
| Secret file | .env files, certificates |
| Certificate | PKI/SSL keystores |

### Credential Scope

- **Global** — available to all jobs (what we use)
- **System** — only for Jenkins internals
- **Folder** — only for jobs inside that folder (useful in large teams)

---

## 12.10 Multi-Branch Pipeline (Advanced)

Instead of one pipeline for `main` only, a **Multi-Branch Pipeline** automatically creates a pipeline for every branch and PR.

### When to Use It

```
feature/login-page   → pipeline runs CI (no deploy)
feature/new-products → pipeline runs CI (no deploy)
main                 → pipeline runs CI + deploy
```

### Setting Up

1. Jenkins dashboard → **"+ New Item"**
2. Name: `ecommerce-multibranch`
3. Select **"Multibranch Pipeline"**
4. **Branch Sources → Add source → GitHub**
   - Credentials: `github-credentials`
   - Repository: your repo URL
5. **Behaviors:** Add → Filter by name (include: `main`, `feature/*`, `hotfix/*`)
6. **Build Configuration:** Mode = by Jenkinsfile
   - Script path: `week6-02-microservices-mini-project-03102026/Jenkinsfile`
7. **Scan Repository Triggers:** 1 minute (Jenkins scans for new branches)
8. Click **Save**

Jenkins immediately scans the repo and creates a pipeline for each branch it finds.

---

## 12.11 Understanding the Declarative Jenkinsfile Syntax

### Top-Level Structure

```groovy
pipeline {
    agent { ... }          // WHERE to run
    environment { ... }    // VARIABLES available to all stages
    options { ... }        // PIPELINE BEHAVIOR (timeout, retention, etc.)
    triggers { ... }       // WHAT kicks off a build
    stages {
        stage('name') {
            when { ... }   // CONDITION (run only if...)
            steps { ... }  // WHAT TO DO
            post { ... }   // WHAT AFTER (success/failure/always)
        }
    }
    post { ... }           // GLOBAL post-pipeline actions
}
```

### The `agent` Directive

```groovy
agent any                          // any available agent
agent none                         // no default — each stage picks its own
agent { label 'linux' }            // only agents with 'linux' label
agent { docker 'maven:3.9-java17' } // run inside a Docker container
```

### The `when` Directive

```groovy
when { branch 'main' }              // only on main branch
when { not { branch 'main' } }      // only NOT on main (feature branches)
when { changeset '**/pom.xml' }     // only when pom.xml changed
when { environment name: 'DEPLOY_TO', value: 'prod' }  // env var check
when {
    allOf {                         // ALL conditions must be true
        branch 'main'
        not { changeRequest() }     // not a PR
    }
}
```

### The `sh` Step

```groovy
sh 'echo hello'                     // single line
sh '''
    echo line 1                     // multi-line (triple-quote)
    echo line 2
    mvn clean install
'''
sh(script: 'echo hello', returnStdout: true).trim()  // capture output
```

### The `withCredentials` Step

```groovy
withCredentials([
    usernamePassword(
        credentialsId: 'dockerhub-credentials',
        usernameVariable: 'DOCKER_USER',
        passwordVariable: 'DOCKER_PASS'
    ),
    string(
        credentialsId: 'my-api-token',
        variable: 'API_TOKEN'
    )
]) {
    sh 'docker login -u $DOCKER_USER -p $DOCKER_PASS'
}
// Variables are masked in logs: docker login -u myuser -p ****
```

### Parallel Stages

Build multiple services simultaneously to save time:

```groovy
stage('Jib Push All Services') {
    parallel {
        stage('user-service') {
            steps { sh 'mvn -pl user-service jib:build ...' }
        }
        stage('product-service') {
            steps { sh 'mvn -pl product-service jib:build ...' }
        }
        stage('order-service') {
            steps { sh 'mvn -pl order-service jib:build ...' }
        }
    }
}
```

This runs all three Jib builds at the same time, cutting the push stage time by ~3x.

---

## 12.12 Environment-Specific Deployments

A real pipeline deploys to different environments based on the branch:

```
feature/* branches  →  CI only (build + test)
develop branch      →  CI + deploy to STAGING
main branch         →  CI + deploy to PRODUCTION (after manual approval)
```

### Manual Approval Gate

Add this between staging and production deploy:

```groovy
stage('Approve Production Deploy') {
    when { branch 'main' }
    steps {
        // Pause the pipeline and wait for a human to click "Proceed"
        // Times out after 24 hours if nobody approves
        timeout(time: 24, unit: 'HOURS') {
            input(
                message: 'Deploy to PRODUCTION?',
                ok: 'Yes, deploy now',
                submitter: 'admin,tech-lead',    // only these users can approve
                parameters: [
                    choice(
                        name: 'ENVIRONMENT',
                        choices: ['production', 'abort'],
                        description: 'Select target'
                    )
                ]
            )
        }
    }
}
```

---

## 12.13 Notifications

### Email Notifications

```groovy
post {
    failure {
        mail(
            to: 'team@yourcompany.com',
            subject: "FAILED: ${env.JOB_NAME} #${env.BUILD_NUMBER}",
            body: """
                Build failed!
                Job: ${env.JOB_NAME}
                Build: ${env.BUILD_NUMBER}
                URL: ${env.BUILD_URL}

                Check the console: ${env.BUILD_URL}console
            """
        )
    }
}
```

Configure SMTP: **Manage Jenkins → Configure System → E-mail Notification**

### Slack Notifications (if your team uses Slack)

1. Install **Slack Notification** plugin
2. **Manage Jenkins → Configure System → Slack**
   - Workspace: your-workspace
   - Credential: add Slack Bot Token
   - Channel: `#ci-alerts`

```groovy
post {
    success {
        slackSend(color: 'good', message: "✅ ${env.JOB_NAME} #${env.BUILD_NUMBER} passed")
    }
    failure {
        slackSend(color: 'danger', message: "❌ ${env.JOB_NAME} #${env.BUILD_NUMBER} FAILED — ${env.BUILD_URL}")
    }
}
```

---

## 12.14 Full Pipeline — Optimized Version (Parallel Jib Builds)

This is the production-ready version of the Jenkinsfile with parallel image building for speed:

```groovy
pipeline {

    agent any

    environment {
        DOCKER_HUB_USER = 'your-dockerhub-username'
        IMAGE_TAG       = "${env.GIT_COMMIT?.take(8) ?: 'latest'}"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '10'))
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        disableConcurrentBuilds()
    }

    triggers {
        githubPush()
        pollSCM('H/5 * * * *')
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                sh 'echo "Building commit: $GIT_COMMIT on branch: $GIT_BRANCH"'
            }
        }

        stage('Build & Test') {
            steps {
                sh '''
                    cd week6-02-microservices-mini-project-03102026
                    mvn clean verify --batch-mode --no-transfer-progress
                '''
                // 'verify' = compile + test + package (all in one)
            }
            post {
                always {
                    junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('Push Images') {
            when { branch 'main' }

            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    // Run all 6 Jib builds IN PARALLEL
                    parallel(
                        'config-server': {
                            sh """
                                cd week6-02-microservices-mini-project-03102026
                                mvn -pl config-server jib:build \
                                    -Djib.to.image="${DOCKER_USER}/config-server:${IMAGE_TAG}" \
                                    -Djib.to.tags=latest \
                                    -Djib.to.auth.username="${DOCKER_USER}" \
                                    -Djib.to.auth.password="${DOCKER_PASS}" \
                                    --batch-mode --no-transfer-progress
                            """
                        },
                        'eureka-server': {
                            sh """
                                cd week6-02-microservices-mini-project-03102026
                                mvn -pl eureka-server jib:build \
                                    -Djib.to.image="${DOCKER_USER}/eureka-server:${IMAGE_TAG}" \
                                    -Djib.to.tags=latest \
                                    -Djib.to.auth.username="${DOCKER_USER}" \
                                    -Djib.to.auth.password="${DOCKER_PASS}" \
                                    --batch-mode --no-transfer-progress
                            """
                        },
                        'user-service': {
                            sh """
                                cd week6-02-microservices-mini-project-03102026
                                mvn -pl user-service jib:build \
                                    -Djib.to.image="${DOCKER_USER}/user-service:${IMAGE_TAG}" \
                                    -Djib.to.tags=latest \
                                    -Djib.to.auth.username="${DOCKER_USER}" \
                                    -Djib.to.auth.password="${DOCKER_PASS}" \
                                    --batch-mode --no-transfer-progress
                            """
                        },
                        'product-service': {
                            sh """
                                cd week6-02-microservices-mini-project-03102026
                                mvn -pl product-service jib:build \
                                    -Djib.to.image="${DOCKER_USER}/product-service:${IMAGE_TAG}" \
                                    -Djib.to.tags=latest \
                                    -Djib.to.auth.username="${DOCKER_USER}" \
                                    -Djib.to.auth.password="${DOCKER_PASS}" \
                                    --batch-mode --no-transfer-progress
                            """
                        },
                        'order-service': {
                            sh """
                                cd week6-02-microservices-mini-project-03102026
                                mvn -pl order-service jib:build \
                                    -Djib.to.image="${DOCKER_USER}/order-service:${IMAGE_TAG}" \
                                    -Djib.to.tags=latest \
                                    -Djib.to.auth.username="${DOCKER_USER}" \
                                    -Djib.to.auth.password="${DOCKER_PASS}" \
                                    --batch-mode --no-transfer-progress
                            """
                        },
                        'api-gateway': {
                            sh """
                                cd week6-02-microservices-mini-project-03102026
                                mvn -pl api-gateway jib:build \
                                    -Djib.to.image="${DOCKER_USER}/api-gateway:${IMAGE_TAG}" \
                                    -Djib.to.tags=latest \
                                    -Djib.to.auth.username="${DOCKER_USER}" \
                                    -Djib.to.auth.password="${DOCKER_PASS}" \
                                    --batch-mode --no-transfer-progress
                            """
                        }
                    )
                }
            }
        }

        stage('Deploy') {
            when { branch 'main' }
            steps {
                sh '''
                    cd week6-02-microservices-mini-project-03102026
                    docker compose pull
                    docker compose up -d --remove-orphans
                    sleep 30
                    docker compose ps
                '''
            }
        }

    }

    post {
        success { echo '✅ Pipeline completed successfully' }
        failure { echo '❌ Pipeline failed — check stage logs above' }
        always  { cleanWs() }
    }
}
```

---

## 12.15 Debugging Failed Builds

### Common Failures and Solutions

#### Problem: "Permission denied" when running docker compose

```
Got permission denied while trying to connect to the Docker daemon socket
```

**Cause:** Jenkins runs as the `jenkins` user which is not in the `docker` group.

**Fix:**
```bash
sudo usermod -aG docker jenkins
sudo systemctl restart jenkins    # Linux
brew services restart jenkins-lts  # macOS
```

---

#### Problem: Jib fails with 401 Unauthorized

```
Caused by: com.google.cloud.tools.jib.http.ResponseException: 401 Unauthorized
```

**Cause:** Wrong Docker Hub credentials in Jenkins.

**Fix:**
1. Go to **Manage Jenkins → Credentials**
2. Delete and recreate the `dockerhub-credentials` entry
3. Make sure you use an Access Token (not your Docker Hub password if 2FA is enabled)

---

#### Problem: Tests fail because of missing database

```
Cannot connect to MySQL at localhost:3306
```

**Cause:** Integration tests try to hit a real DB that doesn't exist on the Jenkins server.

**Fix:** Use the H2 in-memory database for tests (already configured in our project under test scope in `pom.xml`). Make sure your `application-test.properties` sets:
```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
```

---

#### Problem: Build hangs at "Waiting for agent"

**Cause:** Jenkins has no available executor slots.

**Fix:**
- **Manage Jenkins → Manage Nodes and Clouds → Built-In Node**
- Set **Number of executors** to `2` (or more)

---

#### Problem: "Jenkinsfile not found"

```
ERROR: Unable to find Jenkinsfile from git
```

**Cause:** The Script Path field in the job config doesn't match the actual file location.

**Fix:** In the pipeline job config → Pipeline tab → Script Path, make sure it matches exactly:
```
week6-02-microservices-mini-project-03102026/Jenkinsfile
```

---

## 12.16 Blue Ocean — Modern Pipeline UI

Blue Ocean is a Jenkins plugin that gives a beautiful, modern view of your pipelines.

### Accessing Blue Ocean

If you installed the Blue Ocean plugin:
1. From any pipeline page, click **"Open Blue Ocean"** (left sidebar)
2. Or navigate directly to: `http://localhost:8080/blue`

### What Blue Ocean Shows

```
╔══════════════════════════════════════════════════════════════╗
║  ecommerce-microservices  #5  ✅  main  3m 42s ago           ║
╠══════════════════════════════════════════════════════════════╣
║                                                              ║
║  ●──────●──────●──────●──────●                               ║
║  Start  Build  Test  Push  Deploy  End                       ║
║         ✅     ✅    ✅    ✅                                  ║
║         45s    92s   4m3s  38s                               ║
║                                                              ║
║  [ View Logs ]  [ Artifacts ]  [ Test Results ]              ║
╚══════════════════════════════════════════════════════════════╝
```

Click any stage circle → see just that stage's console output.

---

## 12.17 Jenkins Configuration as Code (JCasC)

Instead of clicking through the Jenkins UI to configure everything, you can define Jenkins configuration in YAML files and commit them to Git. This is called **Configuration as Code**.

### Why JCasC?

- Reproducible: destroy and recreate Jenkins in minutes
- Version controlled: track every Jenkins config change in Git
- Team-friendly: everyone can review changes via PRs

### Example: jenkins.yaml

```yaml
# jenkins.yaml — complete Jenkins configuration
jenkins:
  systemMessage: "E-Commerce CI/CD Jenkins"
  numExecutors: 2
  mode: NORMAL

  securityRealm:
    local:
      allowsSignup: false
      users:
        - id: "admin"
          password: "${JENKINS_ADMIN_PASS}"  # from env var

  authorizationStrategy:
    loggedInUsersCanDoAnything:
      allowAnonymousRead: false

credentials:
  system:
    domainCredentials:
      - credentials:
          - usernamePassword:
              scope: GLOBAL
              id: "dockerhub-credentials"
              username: "${DOCKER_HUB_USER}"
              password: "${DOCKER_HUB_PASS}"
              description: "Docker Hub login"

tool:
  jdk:
    installations:
      - name: "Java-17"
        home: "/opt/homebrew/opt/openjdk@17"
  maven:
    installations:
      - name: "Maven-3"
        properties:
          - installSource:
              installers:
                - maven:
                    id: "3.9.6"
```

Install the **Configuration as Code** plugin, then point it to this file under:
**Manage Jenkins → Configuration as Code → Path to config file**

---

## 12.18 Full CI/CD Flow — End to End Walkthrough

Let's walk through exactly what happens from `git push` to services running:

```
Step 1: Developer writes code and pushes to GitHub
────────────────────────────────────────────────────
$ git add .
$ git commit -m "feat: add product search endpoint"
$ git push origin main

Step 2: GitHub detects the push
────────────────────────────────────────────────────
GitHub fires a POST request to your Jenkins webhook URL:
POST https://abc123.ngrok.io/github-webhook/
{
  "ref": "refs/heads/main",
  "commits": [...],
  "repository": {...}
}

Step 3: Jenkins receives the webhook
────────────────────────────────────────────────────
Jenkins parses the webhook payload
Identifies which pipeline to trigger (ecommerce-microservices)
Queues a new build

Step 4: Jenkins starts the build
────────────────────────────────────────────────────
[Stage: Checkout]
  → git clone https://github.com/.../repo.git
  → checks out commit abc12345

[Stage: Build]
  → cd week6-02-microservices-mini-project-03102026
  → mvn clean compile -DskipTests
  → All 6 modules compiled in 45 seconds ✅

[Stage: Test]
  → mvn test
  → user-service:     12 tests passed ✅
  → product-service:  8 tests passed  ✅
  → order-service:    15 tests passed ✅
  → Test report published to Jenkins UI

[Stage: Docker Build & Push]  (parallel)
  → Jib builds config-server   → pushed as your-user/config-server:abc12345
  → Jib builds eureka-server   → pushed as your-user/eureka-server:abc12345
  → Jib builds user-service    → pushed as your-user/user-service:abc12345
  → Jib builds product-service → pushed as your-user/product-service:abc12345
  → Jib builds order-service   → pushed as your-user/order-service:abc12345
  → Jib builds api-gateway     → pushed as your-user/api-gateway:abc12345
  → All 6 images tagged :latest also

[Stage: Deploy]
  → docker compose pull   (pulls the new :latest images)
  → docker compose up -d  (recreates only changed containers)
  → mysql:          unchanged, stays running
  → config-server:  new image → restarted
  → eureka-server:  new image → restarted
  → user-service:   new image → restarted
  → product-service: new image → restarted (contains our new search endpoint)
  → order-service:  new image → restarted
  → api-gateway:    new image → restarted

Step 5: All services healthy
────────────────────────────────────────────────────
docker compose ps:
  NAME              STATUS              PORTS
  mysql             Up (healthy)        3307->3306/tcp
  config-server     Up (healthy)        8888->8888/tcp
  eureka-server     Up (healthy)        8761->8761/tcp
  user-service      Up (healthy)
  product-service   Up (healthy)
  order-service     Up (healthy)
  api-gateway       Up (healthy)        8080->8080/tcp

Step 6: Developer gets notification
────────────────────────────────────────────────────
Jenkins marks build #5 as SUCCESS
Email / Slack notification sent: "✅ ecommerce-microservices #5 passed"
Total time: 6 minutes from push to deployed
```

---

## 12.19 Interview Questions

**Q1. What is the difference between CI and CD?**

CI (Continuous Integration) is the practice of automatically building and testing every code change. CD (Continuous Delivery) adds automatic preparation and staging of releases. Continuous Deployment goes further — every green build goes to production automatically without human approval.

**Q2. What is a Jenkinsfile and where does it live?**

A Jenkinsfile is a text file that defines a Jenkins pipeline using Groovy DSL (either declarative or scripted syntax). It lives in the root of the Git repository so the pipeline definition is version-controlled alongside the code. Jenkins reads it automatically when a build is triggered.

**Q3. Why use Jib instead of a Dockerfile for CI/CD?**

Jib builds Docker images without needing a Docker daemon running on the CI server. It talks directly to the container registry API, handles layer caching automatically, and integrates into the Maven build without a separate `docker build` step. This simplifies CI setup and speeds up builds because only changed layers are pushed.

**Q4. How do you prevent passwords from appearing in Jenkins logs?**

Use the `withCredentials` block which injects credentials as environment variables scoped only to that block. Jenkins automatically masks the password value in console output, replacing it with `****`. Never pass passwords as command-line arguments directly in `sh` steps without `withCredentials`.

**Q5. What is a webhook and how does it work?**

A webhook is an HTTP callback. GitHub sends a POST request to a Jenkins URL every time an event (like a push) occurs. Jenkins receives it, identifies the pipeline to trigger, and starts a build. This is faster and more responsive than polling (where Jenkins checks GitHub every N minutes).

**Q6. What is the `agent` directive in a Jenkinsfile?**

The `agent` directive specifies WHERE a pipeline or stage runs. `agent any` uses any available executor on any Jenkins node. `agent { label 'linux' }` runs only on nodes tagged `linux`. `agent { docker 'maven:3.9' }` runs inside a specific Docker container, providing a clean, isolated build environment.

**Q7. How do you run stages in parallel in Jenkins?**

Use the `parallel` block inside a stage's `steps`. Each key-value pair in the parallel map is an independent parallel branch. Jenkins runs all branches simultaneously on available executors. This is useful for building multiple Docker images at the same time.

**Q8. What is the difference between a scripted and declarative pipeline?**

Scripted pipelines use raw Groovy DSL — very flexible but harder to read. Declarative pipelines use a structured syntax with predefined blocks (`pipeline`, `stages`, `steps`, `post`) — easier to read, validates syntax before running, and supports the Blue Ocean visual view. Declarative is recommended for most use cases.

**Q9. What happens when a Jenkins build is marked "unstable"?**

Unstable means the build compiled and ran but some tests failed. It is different from "failed" where the build process itself errored out. `junit` marks a build unstable when test failures are found. The `MAVEN_OPTS=-Dmaven.test.failure.ignore=false` flag makes test failures break the build (mark as failed instead of unstable).

**Q10. How would you deploy to multiple environments (staging, production) from one pipeline?**

Use `when { branch 'develop' }` to deploy to staging on every merge to develop, and `when { branch 'main' }` with an `input` step for a manual approval gate before deploying to production. The same Jenkinsfile handles both environments with environment-specific configuration passed as environment variables.

---

## Summary

```
What we built:
─────────────
✅ Jenkins installed locally via Homebrew
✅ Plugins configured: Git, Pipeline, Docker, Credentials, Blue Ocean
✅ JDK 17 and Maven configured in Jenkins Tools
✅ Docker Hub credentials stored securely (never in code)
✅ Declarative Jenkinsfile with 5 stages:
      Checkout → Build → Test → Jib Push → Deploy
✅ Parallel Jib builds (all 6 services built simultaneously)
✅ GitHub webhook triggers automatic builds on push
✅ Manual approval gate for production deployments
✅ Test results published to Jenkins UI
✅ Workspace cleaned after every build

CI/CD Pipeline Summary:
───────────────────────
Developer pushes code
    → GitHub webhook fires (< 1 second)
    → Jenkins picks up build
    → Checkout + Compile + Test (~ 2 minutes)
    → Jib builds all 6 images in parallel (~ 3-4 minutes)
    → Docker Compose redeploys changed services (~ 1 minute)
    → All services live at http://localhost:8080 (~ 6-7 minutes total)
```
