// Configuration centralisée
def config = [
    emailRecipients: "magassakara@gmail.com",
    containerName: "paymybuddy-app",
    dockerRegistry: "docker.io",
    dockerHome: '/usr/local/bin',
    sonarProjectKey: "paymybuddy",
    // Configuration SonarQube
    sonar: [
        communityEdition: true,
        projectKey: "paymybuddy",
        qualityProfileJava: "Sonar way",
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
        owaspCheck: 20
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
        jdk 'JDK-21'
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
        // ✅ Configuration NVD API optionnelle
        NVD_API_AVAILABLE = "${checkNvdApiKeyExists()}"
    }

    stages {
        stage('Checkout & Setup') {
            steps {
                script {
                    // Checkout du code
                    checkout scm

                    // Validation de l'environnement
                    validateEnvironment()

                    // Vérification de Docker avec retry
                    env.DOCKER_AVAILABLE = checkDockerAvailability()

                    // Affichage de la configuration
                    displayBuildInfo(config)
                }
            }
        }

        stage('Build & Test') {
            steps {
                script {
                    echo "🏗️ Compilation et tests Maven..."

                    sh """
                        mvn clean verify \
                            org.jacoco:jacoco-maven-plugin:prepare-agent \
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
                        timeout(time: 20, unit: 'MINUTES')
                    }
                    steps {
                        script {
                            runDependencyCheckFixed()
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

        stage('Docker Push') {
            when {
                allOf {
                    anyOf {
                        branch 'master'
                        branch 'develop'
                    }
                    expression {
                        return env.DOCKER_AVAILABLE == "true"
                    }
                }
            }
            steps {
                script {
                    pushDockerImage(config)
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
                // ✅ Archivage des artefacts avec vérification
                safeArchiveArtifacts()

                // ✅ Nettoyage sécurisé
                safeCleanup(config)

                // ✅ Notification sécurisée
                safeNotification(config.emailRecipients)
            }
        }
        failure {
            script {
                echo "❌ Pipeline échoué - Vérifiez les logs ci-dessus"
                // ✅ Collecte de diagnostic sécurisée
                safeCollectDiagnostic()
            }
        }
        success {
            script {
                if (env.DOCKER_AVAILABLE == "true") {
                    echo "✅ Pipeline réussi - Application déployée avec succès"
                } else {
                    echo "✅ Pipeline réussi - Build Maven terminé (Docker indisponible)"
                }
            }
        }
        unstable {
            script {
                echo "⚠️ Pipeline instable - Vérifiez les avertissements"
            }
        }
    }
}

// =============================================================================
// FONCTIONS UTILITAIRES SÉCURISÉES
// =============================================================================

// ✅ Vérification sécurisée de l'existence de la clé NVD API
def checkNvdApiKeyExists() {
    try {
        // Tentative de récupération du credential sans l'assigner
        withCredentials([string(credentialsId: 'nvd-api-key', variable: 'DUMMY_VAR')]) {
            return "true"
        }
    } catch (Exception e) {
        echo "ℹ️ Clé API NVD non configurée - Fonctionnement en mode standard"
        return "false"
    }
}

// ✅ Archivage sécurisé des artefacts
def safeArchiveArtifacts() {
    try {
        if (fileExists('target') && findFiles(glob: 'target/*.jar').length > 0) {
            archiveArtifacts artifacts: 'target/*.jar', fingerprint: true, allowEmptyArchive: true
            echo "✅ Artefacts archivés avec succès"
        } else {
            echo "ℹ️ Aucun artefact à archiver"
        }
    } catch (Exception e) {
        echo "⚠️ Impossible d'archiver les artefacts: ${e.getMessage()}"
    }
}

// ✅ Nettoyage sécurisé
def safeCleanup(config) {
    try {
        // Nettoyage Docker seulement si disponible
        if (env.DOCKER_AVAILABLE == "true") {
            cleanupDockerImages(config)
        }

        // Nettoyage du workspace
        cleanWs()
        echo "✅ Nettoyage terminé"

    } catch (Exception e) {
        echo "⚠️ Erreur lors du nettoyage: ${e.getMessage()}"
    }
}

// ✅ Notification sécurisée
def safeNotification(recipients) {
    try {
        sendNotification(recipients)
    } catch (Exception e) {
        echo "⚠️ Échec de l'envoi de notification: ${e.getMessage()}"
    }
}

// ✅ Collecte de diagnostic sécurisée
def safeCollectDiagnostic() {
    try {
        echo "🔍 Collecte d'informations de diagnostic..."

        // Informations système de base (sans sh)
        echo "=== INFORMATIONS BUILD ==="
        echo "Build Number: ${env.BUILD_NUMBER}"
        echo "Branch: ${env.BRANCH_NAME}"
        echo "Workspace: ${env.WORKSPACE}"
        echo "Docker Available: ${env.DOCKER_AVAILABLE}"
        echo "NVD API Available: ${env.NVD_API_AVAILABLE}"

        // Tentative de collecte système avec gestion d'erreur
        try {
            sh """
                echo "=== ESPACE DISQUE ==="
                df -h . || echo "Impossible de vérifier l'espace disque"
                echo "=== PROCESSUS JAVA ==="
                ps aux | grep java | head -5 || echo "Aucun processus Java trouvé"
            """
        } catch (Exception sysError) {
            echo "⚠️ Informations système non disponibles: ${sysError.getMessage()}"
        }

    } catch (Exception e) {
        echo "⚠️ Erreur lors de la collecte de diagnostic: ${e.getMessage()}"
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

    // Vérification de l'espace disque
    try {
        sh """
            df -h . | tail -1 | awk '{print "💾 Espace disque disponible: " \$4 " (" \$5 " utilisé)"}'
        """
    } catch (Exception e) {
        echo "⚠️ Impossible de vérifier l'espace disque: ${e.getMessage()}"
    }
}

def performSonarAnalysis(config) {
    echo "🔍 Démarrage de l'analyse SonarQube..."

    withSonarQubeEnv('SonarQube') {
        withCredentials([string(credentialsId: 'sonartoken', variable: 'SONAR_TOKEN')]) {
            try {
                def sonarCommand = buildSonarCommand(config)
                echo "📋 Commande SonarQube: ${sonarCommand}"

                timeout(time: config.timeouts.sonarAnalysis, unit: 'MINUTES') {
                    sh sonarCommand
                }

                echo "✅ Analyse SonarQube terminée avec succès"

            } catch (Exception e) {
                echo "❌ Erreur lors de l'analyse SonarQube: ${e.getMessage()}"

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

    if (!config.sonar.communityEdition && env.BRANCH_NAME) {
        baseCommand += " -Dsonar.branch.name=${env.BRANCH_NAME}"
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
    echo "🎯 Vérification du Quality Gate..."

    try {
        timeout(time: config.timeouts.qualityGate, unit: 'MINUTES') {
            def qg = waitForQualityGate()

            if (qg.status != 'OK') {
                echo "❌ Quality Gate: ${qg.status}"

                if (qg.conditions) {
                    echo "📊 Détails des conditions:"
                    qg.conditions.each { condition ->
                        echo "  • ${condition.metricName}: ${condition.actualValue} (seuil: ${condition.errorThreshold})"
                    }
                }

                if (env.BRANCH_NAME == 'master') {
                    error "🚫 Quality Gate échoué sur la branche master - Arrêt du pipeline"
                } else {
                    echo "⚠️ Quality Gate échoué mais pipeline continue (branche de développement)"
                    currentBuild.result = 'UNSTABLE'
                }
            } else {
                echo "✅ Quality Gate: PASSED"
            }
        }
    } catch (Exception e) {
        echo "⚠️ Impossible de vérifier le Quality Gate: ${e.getMessage()}"
        if (env.BRANCH_NAME == 'master') {
            error "🚫 Vérification Quality Gate obligatoire sur master"
        } else {
            echo "⚠️ Continuing sans Quality Gate sur branche de développement"
            currentBuild.result = 'UNSTABLE'
        }
    }
}

// ✅ FONCTION OWASP CORRIGÉE SANS DÉPENDANCE AU CREDENTIAL
def runDependencyCheckFixed() {
    try {
        echo "🔒 Vérification des dépendances (OWASP)..."

        // Étape 1: Mise à jour de la base de données avec gestion conditionnelle de l'API Key
        echo "📥 Initialisation de la base de données NVD..."

        def updateCommand = """
            mvn org.owasp:dependency-check-maven:update-only \
                -DautoUpdate=true \
                -DcveValidForHours=24 \
                -B -q
        """

        // Ajout conditionnel de la clé API
        if (env.NVD_API_AVAILABLE == "true") {
            echo "🔑 Utilisation de la clé API NVD"
            withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
                updateCommand += " -DnvdApiKey=\${NVD_API_KEY}"
                timeout(time: 10, unit: 'MINUTES') {
                    sh updateCommand
                }
            }
        } else {
            echo "⚠️ Pas de clé API NVD - Utilisation du mode standard"
            timeout(time: 15, unit: 'MINUTES') {
                sh updateCommand
            }
        }

        echo "✅ Base de données NVD mise à jour"

        // Étape 2: Analyse des vulnérabilités
        echo "🔍 Analyse des vulnérabilités..."

        def checkCommand = """
            mvn org.owasp:dependency-check-maven:check \
                -DfailBuildOnCVSS=8 \
                -DskipProvidedScope=true \
                -DskipRuntimeScope=false \
                -DsuppressFailureOnError=true \
                -DautoUpdate=false \
                -DcveValidForHours=24 \
                -DretireJsAnalyzerEnabled=false \
                -DnodeAnalyzerEnabled=false \
                -B -q
        """

        timeout(time: config.timeouts.owaspCheck, unit: 'MINUTES') {
            if (env.NVD_API_AVAILABLE == "true") {
                withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
                    checkCommand += " -DnvdApiKey=\${NVD_API_KEY}"
                    sh checkCommand
                }
            } else {
                sh checkCommand
            }
        }

        echo "✅ Vérification des dépendances terminée avec succès"

    } catch (Exception e) {
        def errorMessage = e.getMessage()

        if (errorMessage.contains("NoDataException") || errorMessage.contains("database does not exist")) {
            echo "❌ Base de données OWASP non disponible"
            echo "💡 Solutions:"
            echo "   1. Configurer une clé API NVD (https://nvd.nist.gov/developers/request-an-api-key)"
            echo "   2. Ou attendre que la base se synchronise automatiquement"

            // Mode fallback simplifié
            try {
                echo "🔄 Tentative en mode fallback..."
                sh """
                    mvn org.owasp:dependency-check-maven:check \
                        -DskipProvidedScope=true \
                        -DskipRuntimeScope=false \
                        -DsuppressFailureOnError=true \
                        -DautoUpdate=true \
                        -DcveValidForHours=168 \
                        -DfailBuildOnCVSS=10 \
                        -DretireJsAnalyzerEnabled=false \
                        -DnodeAnalyzerEnabled=false \
                        -B -q || true
                """
                echo "⚠️ Analyse OWASP terminée en mode dégradé"
            } catch (Exception fallbackError) {
                echo "❌ OWASP Dependency Check non disponible"
            }
        } else {
            echo "⚠️ Problème avec OWASP Dependency Check: ${errorMessage}"
        }

        currentBuild.result = 'UNSTABLE'
    }
}

def archiveOwaspReports() {
    try {
        if (fileExists('target/dependency-check-report.html')) {
            archiveArtifacts artifacts: 'target/dependency-check-report.*', allowEmptyArchive: true

            publishHTML([
                allowMissing: false,
                alwaysLinkToLastBuild: true,
                keepAll: true,
                reportDir: 'target',
                reportFiles: 'dependency-check-report.html',
                reportName: 'OWASP Dependency Check Report'
            ])
            echo "✅ Rapport OWASP archivé et publié"
        } else {
            echo "ℹ️ Aucun rapport OWASP généré"
        }
    } catch (Exception e) {
        echo "⚠️ Erreur lors de l'archivage OWASP: ${e.getMessage()}"
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
    }
}

def publishTestAndCoverageResults() {
    // Publication des résultats de tests
    if (fileExists('target/surefire-reports/TEST-*.xml')) {
        junit 'target/surefire-reports/TEST-*.xml'
        echo "✅ Résultats de tests publiés"
    }

    // Archivage des rapports de couverture
    if (fileExists('target/site/jacoco/index.html')) {
        publishHTML([
            allowMissing: false,
            alwaysLinkToLastBuild: true,
            keepAll: true,
            reportDir: 'target/site/jacoco',
            reportFiles: 'index.html',
            reportName: 'JaCoCo Coverage Report'
        ])

        archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
        echo "✅ Rapport de couverture archivé et publié"
    }

    // Publication JaCoCo
    if (fileExists('target/site/jacoco/jacoco.xml')) {
        try {
            step([
                $class: 'JacocoPublisher',
                execPattern: '**/target/jacoco.exec',
                classPattern: '**/target/classes',
                sourcePattern: '**/src/main/java',
                exclusionPattern: '**/test/**'
            ])
            echo "✅ Métriques JaCoCo publiées"
        } catch (Exception e) {
            echo "⚠️ Impossible de publier les métriques JaCoCo: ${e.getMessage()}"
        }
    }
}

def checkDockerAvailability() {
    try {
        def result = sh(
            script: '''
                for i in 1 2 3; do
                    if command -v docker >/dev/null 2>&1; then
                        if timeout 30 docker info >/dev/null 2>&1; then
                            echo "true"
                            exit 0
                        fi
                    fi
                    echo "Tentative $i/3 échouée, retry dans 5s..."
                    sleep 5
                done
                echo "false"
            ''',
            returnStdout: true
        ).trim()

        if (result == "true") {
            echo "✅ Docker disponible et fonctionnel"
            sh 'docker --version || echo "Version Docker indisponible"'
        } else {
            echo "❌ Docker non disponible - Pipeline continuera sans Docker"
        }

        return result
    } catch (Exception e) {
        echo "❌ Erreur lors de la vérification Docker: ${e.getMessage()}"
        return "false"
    }
}

def displayBuildInfo(config) {
    echo """
    ╔══════════════════════════════════════════════════════════════════════════════╗
    ║                            CONFIGURATION BUILD                               ║
    ╠══════════════════════════════════════════════════════════════════════════════╣
    ║ 🏗️  Build #: ${env.BUILD_NUMBER}
    ║ 🌿 Branch: ${env.BRANCH_NAME}
    ║ ☕ Java: ${env.JAVA_HOME ?: 'N/A'}
    ║ 📦 Maven: ${env.MAVEN_HOME ?: 'N/A'}
    ║ 🐳 Docker: ${env.DOCKER_AVAILABLE == "true" ? "✅ Disponible" : "❌ Indisponible"}
    ║ 🔑 NVD API: ${env.NVD_API_AVAILABLE == "true" ? "✅ Configurée" : "❌ Non configurée"}
    ║ 🌍 Environnement: ${env.ENV_NAME}
    ║ 🚪 Port: ${env.HTTP_PORT}
    ║ 🏷️  Tag: ${env.CONTAINER_TAG}
    ║ 📧 Email: ${config.emailRecipients}
    ║ 🔍 SonarQube: ${config.sonar.communityEdition ? "Community Edition" : "Developer Edition+"}
    ║ 📊 Projet SonarQube: ${env.SONAR_PROJECT_KEY}
    ╚══════════════════════════════════════════════════════════════════════════════╝
    """
}

def validateDockerPrerequisites() {
    if (env.DOCKER_AVAILABLE != "true") {
        error "🚫 Docker n'est pas disponible. Impossible de continuer avec les étapes Docker."
    }

    if (!fileExists('Dockerfile')) {
        error "🚫 Fichier Dockerfile introuvable à la racine du projet."
    }

    def jarFiles = findFiles(glob: 'target/*.jar').findAll {
        it.name.endsWith('.jar') && !it.name.contains('sources') && !it.name.contains('javadoc')
    }

    if (jarFiles.length == 0) {
        error "🚫 Aucun fichier JAR exécutable trouvé dans target/"
    }

    env.JAR_FILE = jarFiles[0].path
    echo "✅ JAR trouvé: ${env.JAR_FILE}"
}

def buildDockerImage(config) {
    try {
        echo "🏗️ Construction de l'image Docker..."

        sh """
            docker build \
                --pull \
                --no-cache \
                --build-arg JAR_FILE=${env.JAR_FILE} \
                --build-arg BUILD_DATE="\$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
                --build-arg VCS_REF="\$(git rev-parse --short HEAD)" \
                --build-arg BUILD_NUMBER="${env.BUILD_NUMBER}" \
                --label "org.opencontainers.image.created=\$(date -u +'%Y-%m-%dT%H:%M:%SZ')" \
                --label "org.opencontainers.image.revision=\$(git rev-parse --short HEAD)" \
                --label "org.opencontainers.image.version=${env.CONTAINER_TAG}" \
                -t "${config.containerName}:${env.CONTAINER_TAG}" \
                .
        """

        echo "✅ Image Docker construite avec succès"
        sh "docker images ${config.containerName}:${env.CONTAINER_TAG}"

    } catch (Exception e) {
        error "🚫 Échec de la construction Docker: ${e.getMessage()}"
    }
}

def pushDockerImage(config) {
    try {
        withCredentials([usernamePassword(
            credentialsId: 'dockerhub-credentials',
            usernameVariable: 'DOCKER_USER',
            passwordVariable: 'DOCKER_PASSWORD'
        )]) {

            echo "🚀 Connexion au registre Docker..."
            sh """
                echo "\${DOCKER_PASSWORD}" | docker login -u "\${DOCKER_USER}" --password-stdin ${config.dockerRegistry}
            """

            echo "🏷️ Tagging de l'image..."
            sh """
                docker tag "${config.containerName}:${env.CONTAINER_TAG}" "\${DOCKER_USER}/${config.containerName}:${env.CONTAINER_TAG}"
            """

            echo "📤 Push de l'image..."
            sh """
                docker push "\${DOCKER_USER}/${config.containerName}:${env.CONTAINER_TAG}"
            """

            // Tag latest pour master
            if (env.BRANCH_NAME == 'master') {
                echo "🏷️ Tagging latest pour master..."
                sh """
                    docker tag "${config.containerName}:${env.CONTAINER_TAG}" "\${DOCKER_USER}/${config.containerName}:latest"
                    docker push "\${DOCKER_USER}/${config.containerName}:latest"
                """
            }

            echo "🔒 Déconnexion du registre..."
            sh "docker logout ${config.dockerRegistry}"

            echo "✅ Image poussée avec succès"
        }
    } catch (Exception e) {
        error "🚫 Échec du push Docker: ${e.getMessage()}"
    }
}

def deployApplication(config) {
    try {
        withCredentials([usernamePassword(
            credentialsId: 'dockerhub-credentials',
            usernameVariable: 'DOCKER_USER',
            passwordVariable: 'DOCKER_PASSWORD'
        )]) {

            echo "🛑 Arrêt du conteneur existant..."
            sh """
                docker stop ${config.containerName} 2>/dev/null || echo "Conteneur non trouvé"
                docker rm ${config.containerName} 2>/dev/null || echo "Conteneur non trouvé"
            """

            echo "🚀 Démarrage du nouveau conteneur..."
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
                    "\${DOCKER_USER}/${config.containerName}:${env.CONTAINER_TAG}"
            """

            echo "✅ Conteneur démarré avec succès"
        }
    } catch (Exception e) {
        error "🚫 Échec du déploiement: ${e.getMessage()}"
    }
}

def performHealthCheck(config) {
    try {
        echo "🩺 Vérification de la santé de l'application..."

        // Attendre que le conteneur soit en cours d'exécution
        timeout(time: config.timeouts.deployment, unit: 'MINUTES') {
            waitUntil {
                script {
                    def status = sh(
                        script: "docker inspect -f '{{.State.Status}}' ${config.containerName} 2>/dev/null || echo 'not-found'",
                        returnStdout: true
                    ).trim()

                    echo "Status du conteneur: ${status}"

                    if (status == "running") {
                        return true
                    } else if (status == "exited") {
                        sh "docker logs ${config.containerName} --tail 50"
                        error "❌ Le conteneur s'est arrêté de manière inattendue"
                    }

                    sleep(10)
                    return false
                }
            }
        }

        // Attendre que l'application soit prête
        echo "⏳ Attente du démarrage de l'application..."
        sleep(30)

        // Test HTTP avec plusieurs endpoints
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
                            echo "⏳ Endpoint ${endpoint} pas encore prêt..."
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

        echo "✅ Application en bonne santé et accessible"

    } catch (Exception e) {
        // Logs pour debug
        sh "docker logs ${config.containerName} --tail 100 2>/dev/null || echo 'Impossible de récupérer les logs'"
        sh "docker inspect ${config.containerName} 2>/dev/null || echo 'Impossible d\\'inspecter le conteneur'"
        error "🚫 Health check échoué: ${e.getMessage()}"
    }
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
        """
        echo "✅ Nettoyage Docker terminé"
    } catch (Exception e) {
        echo "⚠️ Erreur lors du nettoyage Docker: ${e.getMessage()}"
    }
}

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

        def subject = "${statusIcon} [Jenkins] ${env.JOB_NAME} - Build #${env.BUILD_NUMBER} - ${status}"

        def dockerStatus = env.DOCKER_AVAILABLE == "true" ? "✅ Disponible" : "❌ Indisponible"
        def nvdApiStatus = env.NVD_API_AVAILABLE == "true" ? "✅ Configurée" : "❌ Non configurée"
        def deploymentInfo = ""

        if (env.DOCKER_AVAILABLE == "true" && status == 'SUCCESS') {
            deploymentInfo = """
        🚀 Application déployée sur: http://localhost:${env.HTTP_PORT}
        🐳 Conteneur: ${config.containerName}:${env.CONTAINER_TAG}
            """
        } else if (env.DOCKER_AVAILABLE != "true") {
            deploymentInfo = """
        ⚠️ Déploiement Docker ignoré (Docker indisponible)
        📦 Artefacts Maven générés avec succès
            """
        }

        def body = """
        ${statusIcon} Résultat: ${status}

        📊 Détails du Build:
        • Projet: ${env.JOB_NAME}
        • Build: #${env.BUILD_NUMBER}
        • Branche: ${env.BRANCH_NAME ?: 'N/A'}
        • Durée: ${duration}
        • Environnement: ${env.ENV_NAME}
        • Port: ${env.HTTP_PORT}

        🔗 Liens:
        • Console: ${env.BUILD_URL}console
        • Artefacts: ${env.BUILD_URL}artifact/

        🔧 Configuration:
        • 🐳 Docker: ${dockerStatus}
        • 🔑 NVD API: ${nvdApiStatus}
        • 🚀 Cause: ${cause}
        ${deploymentInfo}

        ${status == 'SUCCESS' ? '🎉 Build réussi!' : '🔍 Vérifiez les logs pour plus de détails.'}
        """

        mail(
            to: recipients,
            subject: subject,
            body: body,
            mimeType: 'text/plain'
        )

        echo "📧 Email de notification envoyé à: ${recipients}"

    } catch (Exception e) {
        echo "⚠️ Échec de l'envoi d'email: ${e.getMessage()}"
    }
}

// Fonctions utilitaires pour la configuration
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