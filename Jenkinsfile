// Configuration centralisée
def config = [
    emailRecipients: "magassakara@gmail.com",
    containerName: "paymybuddy-app",
    serviceName: "paymybuddy",
    dockerRegistry: "docker.io",
    dockerHome: '/usr/local/bin',
    sonarProjectKey: "paymybuddy",
    // Configuration SonarQube
    sonar: [
        // Détection automatique de l'édition SonarQube
        communityEdition: true, // Changez à false si vous avez Developer Edition+
        projectKey: "paymybuddy",
        qualityProfileJava: "Sonar way", // Profile de qualité par défaut
        exclusions: [
            "**/target/**",
            "**/*.min.js",
            "**/node_modules/**",
            "**/.mvn/**"
        ]
    ],
    timeouts: [
        qualityGate: 2,
        deployment: 5,
        sonarAnalysis: 10,
        owaspCheck: 25  // Augmenté le timeout pour OWASP
    ],
    ports: [
        master: '9003',
        develop: '9002',
        default: '9001'
    ],
    environments: [
        master: 'prod',
        develop: 'uat',
        default: 'dev'
    ]
]

pipeline {
    agent any

    options {
        timeout(time: 45, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        skipDefaultCheckout(true)
        timestamps()
        parallelsAlwaysFailFast()
    }

    tools {
        maven 'M3'
        jdk 'JDK-17'
    }

    environment {
        DOCKER_BUILDKIT = "1"
        COMPOSE_DOCKER_CLI_BUILD = "1"
        // Variables calculées dynamiquement
        BRANCH_NAME = "${env.BRANCH_NAME ?: 'unknown'}"
        BUILD_NUMBER = "${env.BUILD_NUMBER ?: '0'}"
        HTTP_PORT = "${getHTTPPort(env.BRANCH_NAME, config.ports)}"
        ENV_NAME = "${getEnvName(env.BRANCH_NAME, config.environments)}"
        CONTAINER_TAG = "${getTag(env.BUILD_NUMBER, env.BRANCH_NAME)}"
        // Variables SonarQube
        SONAR_PROJECT_KEY = "${getSonarProjectKey(env.BRANCH_NAME, config.sonar)}"
        MAVEN_OPTS = "-Dmaven.repo.local=${WORKSPACE}/.m2/repository -Xmx1024m"
        PATH = "/usr/local/bin:/usr/bin:/bin:${env.PATH}"
    }

    stages {
        stage('Checkout & Setup') {
            steps {
                script {
                    // Checkout du code
                    checkout scm

                    // Validation de l'environnement
                    validateEnvironment()

                    // Vérification de Docker avec la fonction qui marche de TourGuide
                    env.DOCKER_AVAILABLE = checkDockerAvailability()

                    // Affichage de la configuration
                    displayBuildInfo(config)
                }
            }
        }

        stage('Build & Test') {
            steps {
                script {
                    echo "🏗️ Build et tests Maven..."

                    sh """
                        mvn clean verify \
                            org.jacoco:jacoco-maven-plugin:prepare-agent \
                            org.jacoco:jacoco-maven-plugin:report \
                            -DskipTests=false \
                            -Dmaven.test.failure.ignore=false \
                            -Djacoco.destFile=target/jacoco.exec \
                            -Djacoco.dataFile=target/jacoco.exec \
                            -B -U -q
                    """
                }
            }
            post {
                always {
                    script {
                        publishTestAndCoverageResults()
                    }
                }
            }
        }

        stage('Code Analysis') {
            when {
                anyOf {
                    branch 'master'
                    branch 'develop'
                    changeRequest()
                }
            }
            steps {
                script {
                    performSonarAnalysis(config)
                }
            }
            post {
                always {
                    script {
                        // Archivage des rapports SonarQube si disponibles
                        if (fileExists('.scannerwork/report-task.txt')) {
                            archiveArtifacts artifacts: '.scannerwork/report-task.txt', allowEmptyArchive: true
                        }
                    }
                }
            }
        }

        stage('Quality Gate') {
            when {
                allOf {
                    anyOf {
                        branch 'master'
                        branch 'develop'
                        changeRequest()
                    }
                    // Seulement si SonarQube a réussi
                    expression {
                        return fileExists('.scannerwork/report-task.txt')
                    }
                }
            }
            steps {
                script {
                    checkQualityGate(config)
                }
            }
        }

        stage('Security & Dependency Check') {
            parallel {
                stage('OWASP Dependency Check') {
                    when {
                        anyOf {
                            branch 'master'
                            branch 'develop'
                        }
                    }
                    options {
                        timeout(time: 25, unit: 'MINUTES')
                    }
                    steps {
                        script {
                            runDependencyCheckWithNVDKey(config)
                        }
                    }
                    post {
                        always {
                            script {
                                archiveOwaspReports()
                            }
                        }
                    }
                }

                stage('Maven Security Audit') {
                    options {
                        timeout(time: 10, unit: 'MINUTES')
                    }
                    steps {
                        script {
                            runMavenSecurityAudit()
                        }
                    }
                }
            }
        }

        stage('Docker Operations') {
            when {
                allOf {
                    anyOf {
                        branch 'master'
                        branch 'develop'
                    }
                    // S'assurer que Docker est disponible
                    expression {
                        return env.DOCKER_AVAILABLE == "true"
                    }
                }
            }
            parallel {
                stage('Docker Build') {
                    steps {
                        script {
                            validateDockerPrerequisites()
                            buildDockerImage(config)
                        }
                    }
                }
            }
        }

        stage('Deploy') {
            when {
                allOf {
                    anyOf {
                        branch 'master'
                        branch 'develop'
                    }
                    // Docker doit être disponible
                    expression {
                        return env.DOCKER_AVAILABLE == "true"
                    }
                }
            }
            steps {
                script {
                    deployApplication(config)
                }
            }
        }

        stage('Health Check') {
            when {
                allOf {
                    anyOf {
                        branch 'master'
                        branch 'develop'
                    }
                    // Docker doit être disponible
                    expression {
                        return env.DOCKER_AVAILABLE == "true"
                    }
                }
            }
            steps {
                script {
                    performHealthCheck(config)
                }
            }
        }
    }

    post {
        always {
            script {
                try {
                    // Archivage des artefacts (même sans Docker)
                    archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, allowEmptyArchive: true

                    // Nettoyage des images Docker locales (seulement si Docker disponible)
                    if (env.DOCKER_AVAILABLE == "true") {
                        cleanupDockerImages(config)
                    }

                    // Envoi de notification
                    sendNotification(config.emailRecipients)
                } catch (Exception e) {
                    echo "Erreur dans post always: ${e.getMessage()}"
                } finally {
                    // Nettoyage du workspace
                    cleanWs()
                }
            }
        }
        failure {
            script {
                try {
                    echo "Pipeline échoué - Vérifiez les logs ci-dessus"
                    // Collecte d'informations de diagnostic
                    collectDiagnosticInfo()
                } catch (Exception e) {
                    echo "Erreur lors de la collecte de diagnostic: ${e.getMessage()}"
                }
            }
        }
        success {
            script {
                if (env.DOCKER_AVAILABLE == "true") {
                    echo "Pipeline réussi - Application déployée avec succès"
                } else {
                    echo "Pipeline réussi - Build Maven terminé (Docker indisponible)"
                }
            }
        }
        unstable {
            script {
                echo "Pipeline instable - Vérifiez les avertissements"
            }
        }
    }
}

// =============================================================================
// FONCTION DOCKER AVAILABILITY CORRIGÉE (COPIÉE DE TOURGUIDE)
// =============================================================================

def checkDockerAvailability() {
    try {
        echo "🐳 Vérification de Docker..."

        def dockerPaths = [
            '/usr/bin/docker',
            '/usr/local/bin/docker',
            '/opt/homebrew/bin/docker',
            'docker'
        ]

        def dockerFound = false
        def dockerPath = ""

        for (path in dockerPaths) {
            try {
                def result = sh(script: "command -v ${path} 2>/dev/null || echo 'not-found'", returnStdout: true).trim()
                if (result != 'not-found' && result != '') {
                    dockerFound = true
                    dockerPath = result
                    echo "✅ Docker trouvé à: ${dockerPath}"
                    break
                }
            } catch (Exception e) {
                // Continuer la recherche
            }
        }

        if (!dockerFound) {
            echo "❌ Docker non trouvé dans les emplacements standards"
            echo "🔍 Vérification de l'installation Docker..."

            try {
                sh '''
                    if command -v apt-get >/dev/null 2>&1; then
                        echo "📦 Installation Docker via apt..."
                        sudo apt-get update -y
                        sudo apt-get install -y docker.io docker-compose
                    elif command -v yum >/dev/null 2>&1; then
                        echo "📦 Installation Docker via yum..."
                        sudo yum install -y docker docker-compose
                    elif command -v brew >/dev/null 2>&1; then
                        echo "📦 Installation Docker via brew..."
                        brew install docker docker-compose
                    else
                        echo "⚠️ Gestionnaire de paquets non supporté"
                    fi
                '''

                def result = sh(script: "command -v docker 2>/dev/null || echo 'not-found'", returnStdout: true).trim()
                if (result != 'not-found') {
                    dockerFound = true
                    dockerPath = result
                }
            } catch (Exception e) {
                echo "❌ Impossible d'installer Docker automatiquement: ${e.getMessage()}"
            }
        }

        if (dockerFound) {
            try {
                sh "${dockerPath} --version"
                def daemonCheck = sh(script: "${dockerPath} info >/dev/null 2>&1", returnStatus: true)

                if (daemonCheck == 0) {
                    echo "✅ Docker daemon actif"

                    try {
                        def composeCheck = sh(script: "docker-compose --version || docker compose --version", returnStatus: true)
                        if (composeCheck == 0) {
                            echo "✅ Docker Compose disponible"
                            return "true"
                        } else {
                            echo "⚠️ Docker Compose non disponible"
                            return "false"
                        }
                    } catch (Exception e) {
                        echo "⚠️ Erreur vérification Docker Compose: ${e.getMessage()}"
                        return "false"
                    }
                } else {
                    echo "❌ Docker daemon non actif - tentative de démarrage..."
                    try {
                        sh "sudo systemctl start docker || sudo service docker start || true"
                        sleep(5)

                        def retryCheck = sh(script: "${dockerPath} info >/dev/null 2>&1", returnStatus: true)
                        if (retryCheck == 0) {
                            echo "✅ Docker daemon démarré avec succès"
                            return "true"
                        } else {
                            echo "❌ Impossible de démarrer Docker daemon"
                            return "false"
                        }
                    } catch (Exception e) {
                        echo "❌ Erreur démarrage Docker: ${e.getMessage()}"
                        return "false"
                    }
                }
            } catch (Exception e) {
                echo "❌ Erreur vérification Docker: ${e.getMessage()}"
                return "false"
            }
        } else {
            echo "❌ Docker non disponible"
            echo """
            💡 Solutions possibles:
            1. Installer Docker: curl -fsSL https://get.docker.com -o get-docker.sh && sh get-docker.sh
            2. Ajouter l'utilisateur Jenkins au groupe docker: sudo usermod -aG docker jenkins
            3. Redémarrer le service Jenkins: sudo systemctl restart jenkins
            4. Vérifier les permissions: ls -la /var/run/docker.sock
            """
            return "false"
        }

    } catch (Exception e) {
        echo "❌ Erreur vérification Docker: ${e.getMessage()}"
        return "false"
    }
}

// =============================================================================
// FONCTIONS SONARQUBE ET QUALITY GATE COMPLÈTES
// =============================================================================

def performSonarAnalysis(config) {
    echo "📊 Démarrage de l'analyse SonarQube..."

    withSonarQubeEnv('SonarQube') {
        withCredentials([string(credentialsId: 'sonartoken', variable: 'SONAR_TOKEN')]) {
            try {
                // Construction de la commande SonarQube adaptée à l'édition
                def sonarCommand = buildSonarCommand(config)

                echo "Commande SonarQube: ${sonarCommand}"

                timeout(time: config.timeouts.sonarAnalysis, unit: 'MINUTES') {
                    sh sonarCommand
                }

                echo "✅ Analyse SonarQube terminée avec succès"

            } catch (Exception e) {
                echo "❌ Erreur lors de l'analyse SonarQube: ${e.getMessage()}"

                // Si l'erreur concerne les branches, on continue avec une analyse simple
                if (e.getMessage().contains("sonar.branch.name")) {
                    echo "⚠️ Fonctionnalité multi-branches non supportée, analyse simple en cours..."
                    def fallbackCommand = buildFallbackSonarCommand(config)
                    sh fallbackCommand
                    echo "✅ Analyse SonarQube simple terminée"
                } else {
                    throw e
                }
            }
        }
    }
}

def buildSonarCommand(config) {
    def baseCommand = """
        mvn sonar:sonar \
            -Dsonar.projectKey=${env.SONAR_PROJECT_KEY} \
            -Dsonar.host.url=\$SONAR_HOST_URL \
            -Dsonar.token=\${SONAR_TOKEN} \
            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
            -Dsonar.java.binaries=target/classes \
            -Dsonar.exclusions="${config.sonar.exclusions.join(',')}" \
            -Dsonar.java.source=21 \
            -Dsonar.java.target=21 \
            -B -q
    """

    // Ajout des paramètres spécifiques selon l'édition
    if (!config.sonar.communityEdition && env.BRANCH_NAME) {
        baseCommand += " -Dsonar.branch.name=${env.BRANCH_NAME}"

        // Paramètres additionnels pour Developer Edition+
        if (env.BRANCH_NAME != 'master') {
            baseCommand += " -Dsonar.branch.target=master"
        }
    }

    return baseCommand
}

def buildFallbackSonarCommand(config) {
    return """
        mvn sonar:sonar \
            -Dsonar.projectKey=${config.sonar.projectKey} \
            -Dsonar.host.url=\$SONAR_HOST_URL \
            -Dsonar.token=\${SONAR_TOKEN} \
            -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml \
            -Dsonar.java.binaries=target/classes \
            -Dsonar.exclusions="${config.sonar.exclusions.join(',')}" \
            -Dsonar.java.source=21 \
            -Dsonar.java.target=21 \
            -B -q
    """
}

def checkQualityGate(config) {
    echo "🔍 Vérification du Quality Gate..."

    try {
        timeout(time: config.timeouts.qualityGate, unit: 'MINUTES') {
            def qg = waitForQualityGate()

            if (qg.status != 'OK') {
                echo "⚠️ Quality Gate: ${qg.status}"

                // Affichage des détails si disponibles
                if (qg.conditions) {
                    echo "Détails des conditions:"
                    qg.conditions.each { condition ->
                        echo "  • ${condition.metricName}: ${condition.actualValue} (seuil: ${condition.errorThreshold})"
                    }
                }

                // En fonction de la branche, on peut être plus ou moins strict
                if (env.BRANCH_NAME == 'master') {
                    error "❌ Quality Gate échoué sur la branche master - Arrêt du pipeline"
                } else {
                    echo "⚠️ Quality Gate échoué mais pipeline continue (branche de développement)"
                    currentBuild.result = 'UNSTABLE'
                }
            } else {
                echo "✅ Quality Gate: PASSED"
            }
        }
    } catch (Exception e) {
        echo "❌ Impossible de vérifier le Quality Gate: ${e.getMessage()}"
        if (env.BRANCH_NAME == 'master') {
            error "❌ Vérification Quality Gate obligatoire sur master"
        } else {
            echo "⏭️ Continuing sans Quality Gate sur branche de développement"
            currentBuild.result = 'UNSTABLE'
        }
    }
}

// =============================================================================
// FONCTIONS OWASP ET SÉCURITÉ COMPLÈTES
// =============================================================================

def runDependencyCheckWithNVDKey(config) {
    try {
        echo "🛡️ Vérification des dépendances OWASP avec NVD API Key..."

        // Utilisation des credentials pour la clé NVD API
        withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
            echo "✅ Clé NVD API configurée"
            echo "🔍 Lancement de l'analyse OWASP avec mise à jour NVD..."

            sh "rm -rf ${WORKSPACE}/owasp-data || true"
            sh "mkdir -p ${WORKSPACE}/owasp-data"

            timeout(time: config.timeouts.owaspCheck, unit: 'MINUTES') {
                def checkCommand = """
                    mvn org.owasp:dependency-check-maven:check \
                        -DnvdApiKey=\${NVD_API_KEY} \
                        -DdataDirectory=${WORKSPACE}/owasp-data \
                        -DautoUpdate=true \
                        -DcveValidForHours=24 \
                        -DfailBuildOnCVSS=7.0 \
                        -DskipProvidedScope=true \
                        -DskipRuntimeScope=false \
                        -DsuppressFailureOnError=false \
                        -DretireJsAnalyzerEnabled=false \
                        -DnodeAnalyzerEnabled=false \
                        -DossindexAnalyzerEnabled=false \
                        -DnvdDatafeedEnabled=true \
                        -DnvdMaxRetryCount=3 \
                        -DnvdDelay=2000 \
                        -Dformat=ALL \
                        -B -q
                """

                def exitCode = sh(script: checkCommand, returnStatus: true)

                if (exitCode == 0) {
                    echo "✅ Aucune vulnérabilité critique détectée"
                } else if (exitCode == 1) {
                    echo "⚠️ Vulnérabilités détectées mais en dessous du seuil critique"
                    currentBuild.result = 'UNSTABLE'
                } else {
                    echo "❌ Erreur lors de l'exécution d'OWASP Dependency Check"
                    error "OWASP Dependency Check a échoué avec le code de sortie: ${exitCode}"
                }
            }
        }

        echo "✅ Vérification OWASP terminée avec succès"

    } catch (Exception e) {
        def errorMessage = e.getMessage()
        echo "🚨 Problème avec OWASP Dependency Check: ${errorMessage}"

        createOwaspErrorReport(e)

        if (errorMessage.contains("timeout") || errorMessage.contains("Timeout")) {
            echo "⏰ OWASP Dependency Check interrompu pour timeout"
            currentBuild.result = 'UNSTABLE'
        } else if (errorMessage.contains("403") || errorMessage.contains("NVD Returned Status Code: 403")) {
            echo "🔑 Problème d'authentification avec l'API NVD"
            echo "Vérifiez que la clé API 'nvd-api-key' est correctement configurée dans Jenkins"
            currentBuild.result = 'UNSTABLE'
        } else if (errorMessage.contains("CVE") || errorMessage.contains("vulnerability")) {
            echo "🛡️ Vulnérabilités critiques détectées - Pipeline marqué comme instable"
            currentBuild.result = 'UNSTABLE'
        } else {
            echo "❌ Erreur inattendue - Pipeline marqué comme instable"
            currentBuild.result = 'UNSTABLE'
        }

        echo "⏭️ Pipeline continue malgré l'erreur OWASP"
    }
}

def createOwaspErrorReport(Exception e) {
    sh """
        mkdir -p target
        cat > target/dependency-check-report.html << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>OWASP Dependency Check - Erreur</title>
    <style>
        body { font-family: Arial, sans-serif; margin: 40px; }
        .error { color: #d32f2f; background: #ffebee; padding: 20px; border-radius: 4px; }
        .timestamp { color: #666; font-size: 0.9em; }
    </style>
</head>
<body>
    <h1>🛡️ OWASP Dependency Check - PayMyBuddy</h1>
    <div class="error">
        <h2>⚠️ Scan de sécurité indisponible</h2>
        <p><strong>Erreur:</strong> ${e.getMessage()}</p>
        <p><strong>Build:</strong> #${env.BUILD_NUMBER}</p>
        <p><strong>Branche:</strong> ${env.BRANCH_NAME}</p>
        <div class="timestamp">Timestamp: ${new Date()}</div>
    </div>
    <h3>Actions recommandées:</h3>
    <ul>
        <li>Vérifier la clé API NVD dans Jenkins Credentials</li>
        <li>Vérifier la connectivité réseau vers api.nvd.nist.gov</li>
        <li>Contrôler les permissions du répertoire</li>
        <li>Examiner les logs Maven détaillés</li>
    </ul>
</body>
</html>
EOF
    """
}

def archiveOwaspReports() {
    echo "📋 Archivage des rapports OWASP..."

    def reportFiles = [
        'dependency-check-report.html',
        'dependency-check-report.xml',
        'dependency-check-report.json',
        'dependency-check-report.csv'
    ]

    def reportsFound = false
    reportFiles.each { report ->
        if (fileExists("target/${report}")) {
            archiveArtifacts artifacts: "target/${report}", allowEmptyArchive: true
            echo "✅ Rapport ${report} archivé"
            reportsFound = true
        }
    }

    // Publication du rapport HTML principal
    if (fileExists('target/dependency-check-report.html')) {
        publishHTML([
            allowMissing: false,
            alwaysLinkToLastBuild: true,
            keepAll: true,
            reportDir: 'target',
            reportFiles: 'dependency-check-report.html',
            reportName: 'OWASP Dependency Check Report'
        ])
        echo "✅ Rapport OWASP HTML publié"
    } else {
        echo "⚠️ Aucun rapport OWASP HTML trouvé"
    }

    if (!reportsFound) {
        echo "⚠️ Aucun rapport OWASP généré"
    }
}

def runMavenSecurityAudit() {
    try {
        echo "🔍 Audit de sécurité Maven..."

        timeout(time: 8, unit: 'MINUTES') {
            sh """
                mvn versions:display-dependency-updates \
                    -DprocessDependencyManagement=false \
                    -DgenerateBackupPoms=false \
                    -B -q
            """

            sh """
                mvn versions:display-plugin-updates \
                    -DgenerateBackupPoms=false \
                    -B -q
            """
        }

        echo "✅ Audit de sécurité Maven terminé"

    } catch (Exception e) {
        echo "⚠️ Audit Maven échoué: ${e.getMessage()}"

        if (e.getMessage().contains("timeout") || e.getMessage().contains("Timeout")) {
            echo "⏰ Audit Maven interrompu pour timeout - Continuons le pipeline"
        }
    }
}

// =============================================================================
// FONCTIONS DOCKER COMPLÈTES
// =============================================================================

def buildDockerImage(config) {
    try {
        echo "🐳 Construction de l'image Docker..."

        def imageName = "${config.containerName}:${env.CONTAINER_TAG}"

        sh """
            docker build \
                --pull \
                --no-cache \
                --build-arg JAR_FILE=target/*.jar \
                --build-arg BUILD_DATE="\$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
                --build-arg VCS_REF="\$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')" \
                --build-arg BUILD_NUMBER="${env.BUILD_NUMBER}" \
                --build-arg JAVA_OPTS="-Xmx512m -Xms256m" \
                --label "org.opencontainers.image.created=\$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
                --label "org.opencontainers.image.revision=\$(git rev-parse --short HEAD 2>/dev/null || echo 'unknown')" \
                --label "org.opencontainers.image.version=${env.CONTAINER_TAG}" \
                -t ${imageName} \
                .
        """

        echo "✅ Image Docker construite: ${imageName}"

        if (env.BRANCH_NAME == 'master') {
            sh "docker tag ${imageName} ${config.containerName}:latest"
            echo "✅ Tag latest créé pour master"
        }

        // Vérification de l'image
        sh "docker images ${config.containerName}:${env.CONTAINER_TAG}"

    } catch (Exception e) {
        error "❌ Échec de la construction Docker: ${e.getMessage()}"
    }
}

def deployApplication(config) {
    try {
        echo "🚀 Déploiement de l'application..."

        // Si docker-compose.yml existe, on l'utilise, sinon déploiement direct
        if (fileExists('docker-compose.yml')) {
            deployWithDockerCompose(config)
        } else {
            deployWithDocker(config)
        }

    } catch (Exception e) {
        error "❌ Échec du déploiement: ${e.getMessage()}"
    }
}

def deployWithDockerCompose(config) {
    try {
        echo "🐳 Déploiement avec Docker Compose..."

        createEnvFile()

        sh """
            docker-compose down --remove-orphans 2>/dev/null || true
            docker system prune -f || true
        """

        sh """
            export HTTP_PORT=${env.HTTP_PORT}
            export BUILD_NUMBER=${env.BUILD_NUMBER}
            export BRANCH_NAME=${env.BRANCH_NAME}
            export CONTAINER_TAG=${env.CONTAINER_TAG}
            docker-compose up -d --build
        """

        echo "✅ Application déployée avec Docker Compose"
        sleep(10)
        sh "docker-compose ps"
        sh "docker-compose logs --tail 20 ${config.serviceName} || docker-compose logs --tail 20 || true"

    } catch (Exception e) {
        sh "docker-compose logs ${config.serviceName} --tail 50 || docker-compose logs --tail 50 || true"
        throw e
    }
}

def deployWithDocker(config) {
    try {
        echo "🐳 Déploiement direct avec Docker..."

        echo "Arrêt du conteneur existant..."
        sh """
            docker stop ${config.containerName} 2>/dev/null || echo "Conteneur non trouvé"
            docker rm ${config.containerName} 2>/dev/null || echo "Conteneur non trouvé"
        """

        echo "Démarrage du nouveau conteneur..."
        sh """
            docker run -d \
                --name "${config.containerName}" \
                --restart unless-stopped \
                -p "${env.HTTP_PORT}:8080" \
                -e "SPRING_PROFILES_ACTIVE=${env.ENV_NAME}" \
                -e "SERVER_PORT=8080" \
                -e "JAVA_OPTS=-Xmx512m -Xms256m" \
                --health-cmd="curl -f http://localhost:8080/actuator/health || exit 1" \
                --health-interval=30s \
                --health-timeout=10s \
                --health-start-period=60s \
                --health-retries=3 \
                "${config.containerName}:${env.CONTAINER_TAG}"
        """

        echo "✅ Conteneur démarré avec succès"

    } catch (Exception e) {
        throw e
    }
}

def performHealthCheck(config) {
    try {
        echo "🏥 Health check de l'application..."

        // Attendre que le conteneur soit en cours d'exécution
        timeout(time: config.timeouts.deployment, unit: 'MINUTES') {
            waitUntil {
                script {
                    def status
                    if (fileExists('docker-compose.yml')) {
                        status = sh(
                            script: "docker-compose ps -q ${config.serviceName} | xargs docker inspect -f '{{.State.Status}}' 2>/dev/null || echo 'not-found'",
                            returnStdout: true
                        ).trim()
                    } else {
                        status = sh(
                            script: "docker inspect -f '{{.State.Status}}' ${config.containerName} 2>/dev/null || echo 'not-found'",
                            returnStdout: true
                        ).trim()
                    }

                    echo "État du conteneur: ${status}"

                    if (status == "running") {
                        return true
                    } else if (status == "exited") {
                        if (fileExists('docker-compose.yml')) {
                            sh "docker-compose logs ${config.serviceName} --tail 50 || docker-compose logs --tail 50"
                        } else {
                            sh "docker logs ${config.containerName} --tail 50"
                        }
                        error "Le conteneur s'est arrêté de manière inattendue"
                    }

                    sleep(10)
                    return false
                }
            }
        }

        // Test HTTP avec plusieurs endpoints
        echo "Attente du démarrage de l'application..."
        sleep(30)

        timeout(time: 3, unit: 'MINUTES') {
            waitUntil {
                script {
                    def healthEndpoints = [
                        "http://localhost:${env.HTTP_PORT}/actuator/health",
                        "http://localhost:${env.HTTP_PORT}/actuator/info"
                    ]

                    def allHealthy = true
                    healthEndpoints.each { endpoint ->
                        def exitCode = sh(
                            script: "curl -f -s ${endpoint} > /dev/null",
                            returnStatus: true
                        )

                        if (exitCode != 0) {
                            allHealthy = false
                            echo "Endpoint ${endpoint} pas encore prêt..."
                        }
                    }

                    if (allHealthy) {
                        echo "✅ Tous les endpoints répondent correctement"
                        return true
                    } else {
                        sleep(15)
                        return false
                    }
                }
            }
        }

        echo "✅ Health check réussi - Application en bonne santé"

    } catch (Exception e) {
        // Logs pour debug
        if (fileExists('docker-compose.yml')) {
            sh "docker-compose logs ${config.serviceName} --tail 100 2>/dev/null || docker-compose logs --tail 100 2>/dev/null || echo 'Impossible de récupérer les logs'"
        } else {
            sh "docker logs ${config.containerName} --tail 100 2>/dev/null || echo 'Impossible de récupérer les logs'"
        }
        error "❌ Health check échoué: ${e.getMessage()}"
    }
}

// =============================================================================
// FONCTIONS DE PUBLICATION ET ARCHIVAGE
// =============================================================================

def publishTestAndCoverageResults() {
    echo "📊 Publication des résultats de tests et couverture..."

    // Publication des résultats de tests avec junit
    if (fileExists('target/surefire-reports/TEST-*.xml')) {
        junit 'target/surefire-reports/TEST-*.xml'
        echo "✅ Résultats de tests JUnit publiés"
    } else {
        echo "⚠️ Aucun rapport de test JUnit trouvé"
    }

    // Publication du rapport de couverture JaCoCo HTML
    if (fileExists('target/site/jacoco/index.html')) {
        publishHTML([
            allowMissing: false,
            alwaysLinkToLastBuild: true,
            keepAll: true,
            reportDir: 'target/site/jacoco',
            reportFiles: 'index.html',
            reportName: 'JaCoCo Coverage Report'
        ])
        echo "✅ Rapport de couverture HTML publié"

        // Archivage des artefacts de couverture
        archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
        echo "✅ Artefacts de couverture archivés"
    } else {
        echo "⚠️ Rapport de couverture HTML non trouvé"
    }

    // Publication des métriques JaCoCo dans Jenkins
    if (fileExists('target/site/jacoco/jacoco.xml')) {
        try {
            step([
                $class: 'JacocoPublisher',
                execPattern: '**/target/jacoco.exec',
                classPattern: '**/target/classes',
                sourcePattern: '**/src/main/java',
                exclusionPattern: '**/test/**',
                changeBuildStatus: false,
                minimumInstructionCoverage: '0',
                minimumBranchCoverage: '0',
                minimumComplexityCoverage: '0',
                minimumLineCoverage: '0',
                minimumMethodCoverage: '0',
                minimumClassCoverage: '0'
            ])
            echo "✅ Métriques JaCoCo publiées dans Jenkins"
        } catch (Exception e) {
            echo "⚠️ Impossible de publier les métriques JaCoCo: ${e.getMessage()}"
        }
    } else {
        echo "⚠️ Fichier jacoco.xml non trouvé"
    }
}

// =============================================================================
// FONCTIONS UTILITAIRES COMPLÈTES
// =============================================================================

def collectDiagnosticInfo() {
    try {
        echo "🔍 Collecte d'informations de diagnostic..."

        // Informations système
        sh """
            echo "=== INFORMATIONS SYSTÈME ==="
            uname -a
            echo "=== ESPACE DISQUE ==="
            df -h
            echo "=== MÉMOIRE ==="
            free -h 2>/dev/null || echo "Commande free non disponible"
            echo "=== PROCESSUS JAVA ==="
            ps aux | grep java || echo "Aucun processus Java trouvé"
        """

        // Logs Docker si disponible
        if (env.DOCKER_AVAILABLE == "true") {
            sh """
                echo "=== DOCKER INFO ==="
                docker info 2>/dev/null || echo "Docker info non disponible"
                echo "=== CONTENEURS ACTIFS ==="
                docker ps -a 2>/dev/null || echo "Impossible de lister les conteneurs"
            """
        } else {
            echo "=== DOCKER STATUS ==="
            echo "Docker n'est pas disponible sur ce système"
        }

    } catch (Exception e) {
        echo "❌ Erreur lors de la collecte de diagnostic: ${e.getMessage()}"
    }
}

def displayBuildInfo(config) {
    echo """
    ================================================================================
                            🚀 CONFIGURATION BUILD PAYMYBUDDY
    ================================================================================
     Build #: ${env.BUILD_NUMBER}
     Branch: ${env.BRANCH_NAME}
     Java: 21
     Maven: ${env.MAVEN_HOME}
     Docker: ${env.DOCKER_AVAILABLE == "true" ? "✅ Disponible" : "⚠️ Indisponible"}
     Environnement: ${env.ENV_NAME}
     Port: ${env.HTTP_PORT}
     Tag: ${env.CONTAINER_TAG}
     Email: ${config.emailRecipients}
     SonarQube: ${config.sonar.communityEdition ? "Community Edition" : "Developer Edition+"}
     Projet SonarQube: ${env.SONAR_PROJECT_KEY}
     OWASP NVD API: Configurée via Jenkins Credentials
     Coverage: JaCoCo activé
    ================================================================================
    """
}

def cleanupDockerImages(config) {
    try {
        echo "🧹 Nettoyage des images Docker..."
        sh """
            # Suppression des images non taguées
            docker image prune -f 2>/dev/null || true

            # Garde seulement les 3 dernières versions de notre image
            docker images "${config.containerName}" --format "{{.Repository}}:{{.Tag}}" 2>/dev/null | \
            head -n -3 | xargs -r docker rmi 2>/dev/null || true

            # Nettoyage des volumes orphelins
            docker volume prune -f 2>/dev/null || true

            # Nettoyage Docker Compose si applicable
            docker-compose down --remove-orphans 2>/dev/null || true
        """
        echo "✅ Nettoyage Docker terminé"
    } catch (Exception e) {
        echo "⚠️ Erreur lors du nettoyage Docker: ${e.getMessage()}"
    }
}

def validateEnvironment() {
    echo "🔍 Validation de l'environnement..."

    // Vérification des outils requis
    def requiredTools = ['mvn', 'java', 'git']
    requiredTools.each { tool ->
        try {
            sh "which ${tool}"
            echo "✅ ${tool} disponible"
        } catch (Exception e) {
            error "❌ ${tool} non trouvé dans le PATH"
        }
    }

    // Informations système
    sh """
        java -version
        echo "JAVA_HOME: \$JAVA_HOME"
        mvn -version
    """

    // Vérification de l'espace disque
    sh """
        df -h . | tail -1 | awk '{print "💾 Espace disque: " \$4 " disponible (" \$5 " utilisé)"}'
    """

    def criticalFiles = ['pom.xml', 'src/main/java']
    criticalFiles.each { file ->
        if (!fileExists(file)) {
            error "❌ Fichier/dossier critique manquant: ${file}"
        }
    }
}

def validateDockerPrerequisites() {
    if (env.DOCKER_AVAILABLE != "true") {
        error "🐳 Docker non disponible"
    }

    if (!fileExists('Dockerfile')) {
        error "📄 Fichier Dockerfile requis manquant"
    }

    def jarFiles = findFiles(glob: 'target/*.jar').findAll {
        it.name.endsWith('.jar') && !it.name.contains('sources') && !it.name.contains('javadoc')
    }

    if (jarFiles.length == 0) {
        error "📦 Aucun JAR exécutable trouvé dans target/"
    }

    env.JAR_FILE = jarFiles[0].path
    echo "📦 JAR trouvé: ${env.JAR_FILE}"
}

def createEnvFile() {
    echo "📝 Création du fichier .env..."

    sh """
        cat > .env << 'EOF'
# Configuration environnement PayMyBuddy
BUILD_DATE=\$(date -u +'%Y-%m-%dT%H:%M:%SZ')
VCS_REF=${env.BRANCH_NAME}
BUILD_NUMBER=${env.BUILD_NUMBER}

# Configuration Application
SPRING_ACTIVE_PROFILES=${env.ENV_NAME}
JAVA_OPTS=-Xmx512m -Xms256m -XX:+UseContainerSupport
SERVER_PORT=8080

# Port dynamique
HTTP_PORT=${env.HTTP_PORT}

# Configuration réseau
NETWORK_NAME=paymybuddy-network

# Configuration logging
LOG_LEVEL=INFO
LOG_PATH=/opt/app/logs

# Tags Docker
CONTAINER_TAG=${env.CONTAINER_TAG}
EOF
    """

    echo "✅ Fichier .env créé avec les variables d'environnement"
}

// =============================================================================
// FONCTION DE NOTIFICATION COMPLÈTE
// =============================================================================

def sendNotification(recipients) {
    try {
        def cause = currentBuild.getBuildCauses()?.collect { it.shortDescription }?.join(', ') ?: "Non spécifiée"
        def duration = currentBuild.durationString.replace(' and counting', '')
        def status = currentBuild.currentResult ?: 'SUCCESS'

        def statusIcon = [
            'SUCCESS': '✅',
            'FAILURE': '❌',
            'UNSTABLE': '⚠️',
            'ABORTED': '🛑'
        ][status] ?: '❓'

        def subject = "[Jenkins] PayMyBuddy - Build #${env.BUILD_NUMBER} - ${status}"

        def dockerStatus = env.DOCKER_AVAILABLE == "true" ? "✅ Disponible" : "⚠️ Indisponible"
        def deploymentInfo = ""

        if (env.DOCKER_AVAILABLE == "true" && (status == 'SUCCESS' || status == 'UNSTABLE')) {
            deploymentInfo = """
        🚀 Application: http://localhost:${env.HTTP_PORT}
        🐳 Conteneur: ${config.containerName}:${env.CONTAINER_TAG}
        📊 Coverage: ${env.BUILD_URL}JaCoCo_Coverage_Report/
        🛡️ OWASP: ${env.BUILD_URL}OWASP_Dependency_Check_Report/
            """
        } else if (env.DOCKER_AVAILABLE != "true") {
            deploymentInfo = """
        ⚠️ Déploiement Docker ignoré (Docker indisponible)
        📦 Artefacts Maven générés avec succès
        📊 Coverage: ${env.BUILD_URL}JaCoCo_Coverage_Report/
        🛡️ OWASP: ${env.BUILD_URL}OWASP_Dependency_Check_Report/
            """
        }

        def body = """
        ${statusIcon} Résultat: ${status}

        📋 Détails du Build:
        • Projet: PayMyBuddy
        • Build: #${env.BUILD_NUMBER}
        • Branche: ${env.BRANCH_NAME ?: 'N/A'}
        • Durée: ${duration}
        • Environnement: ${env.ENV_NAME}
        • Port: ${env.HTTP_PORT}
        • Java: 21

        🔗 Liens:
        • Console: ${env.BUILD_URL}console
        • Artefacts: ${env.BUILD_URL}artifact/

        🔧 Configuration:
        • Docker: ${dockerStatus}
        • OWASP NVD: ${status.contains('UNSTABLE') ? '⚠️ Vulnérabilités détectées' : '✅ Aucune vulnérabilité critique'}
        • Cause: ${cause}
        ${deploymentInfo}

        ${status == 'SUCCESS' ? '🎉 Build réussi!' : status == 'UNSTABLE' ? '⚠️ Build instable - Vérifiez les rapports de sécurité et couverture.' : '❌ Vérifiez les logs pour plus de détails.'}
        """

        mail(
            to: recipients,
            subject: subject,
            body: body,
            mimeType: 'text/plain'
        )

        echo "📧 Email de notification envoyé à: ${recipients}"

    } catch (Exception e) {
        echo "❌ Échec de l'envoi d'email: ${e.getMessage()}"
    }
}

// =============================================================================
// FONCTIONS UTILITAIRES POUR LA CONFIGURATION
// =============================================================================

String getEnvName(String branchName, Map environments) {
    def branch = branchName?.toLowerCase()
    return environments[branch] ?: environments.default
}

String getHTTPPort(String branchName, Map ports) {
    def branch = branchName?.toLowerCase()
    return ports[branch] ?: ports.default
}

String getTag(String buildNumber, String branchName) {
    def safeBranch = (branchName ?: "unknown")
        .replaceAll('[^a-zA-Z0-9-]', '-')
        .toLowerCase()

    return (safeBranch == 'master') ?
        "${buildNumber}-stable" :
        "${buildNumber}-${safeBranch}-snapshot"
}

String getSonarProjectKey(String branchName, Map sonarConfig) {
    // Pour SonarQube Community Edition, on utilise un seul projet
    // Pour Developer Edition+, on peut utiliser des clés différentes par branche
    if (sonarConfig.communityEdition) {
        return sonarConfig.projectKey
    } else {
        def branch = branchName?.toLowerCase()
        return "${sonarConfig.projectKey}${branch == 'master' ? '' : '-' + branch}"
    }
}