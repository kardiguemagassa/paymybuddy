// Configuration centralisée
def config = [
    emailRecipients: "magassakara@gmail.com",
    containerName: "paymybuddy-app",
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

        stage('Docker Push') {
            when {
                allOf {
                    anyOf {
                        branch 'master'
                        branch 'develop'
                    }
                    // Docker doit être disponible ET l'image construite
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
                    echo "⚠️ Erreur dans post always: ${e.getMessage()}"
                } finally {
                    // Nettoyage du workspace
                    cleanWs()
                }
            }
        }
        failure {
            script {
                try {
                    echo "❌ Pipeline échoué - Vérifiez les logs ci-dessus"
                    // Collecte d'informations de diagnostic
                    collectDiagnosticInfo()
                } catch (Exception e) {
                    echo "⚠️ Erreur lors de la collecte de diagnostic: ${e.getMessage()}"
                }
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
// FONCTIONS UTILITAIRES AMÉLIORÉES
// =============================================================================

def validateEnvironment() {
    echo "🔍 Validation de l'environnement..."

    // Vérification des outils requis
    def requiredTools = ['mvn', 'java', 'git']
    requiredTools.each { tool ->
        try {
            sh "which ${tool}"
            echo "✅ ${tool} disponible"
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

        🐳 Docker: ${dockerStatus}
        🔑 OWASP NVD: ${status.contains('UNSTABLE') ? '⚠️ Vulnérabilités détectées' : '✅ Aucune vulnérabilité critique'}
        🚀 Cause: ${cause}
        ${deploymentInfo}

        ${status == 'SUCCESS' ? '🎉 Build réussi!' : status == 'UNSTABLE' ? '⚠️ Build instable - Vérifiez les rapports de sécurité.' : '🔍 Vérifiez les logs pour plus de détails.'}
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
} "❌ ${tool} non trouvé dans le PATH"
        }
    }

    // Vérification de l'espace disque
    sh """
        df -h . | tail -1 | awk '{print "💾 Espace disque disponible: " \$4 " (" \$5 " utilisé)"}'
    """
}

def performSonarAnalysis(config) {
    echo "🔍 Démarrage de l'analyse SonarQube..."

    withSonarQubeEnv('SonarQube') {
        withCredentials([string(credentialsId: 'sonartoken', variable: 'SONAR_TOKEN')]) {
            try {
                // Construction de la commande SonarQube adaptée à l'édition
                def sonarCommand = buildSonarCommand(config)

                echo "📋 Commande SonarQube: ${sonarCommand}"

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
    echo "🎯 Vérification du Quality Gate..."

    try {
        timeout(time: config.timeouts.qualityGate, unit: 'MINUTES') {
            def qg = waitForQualityGate()

            if (qg.status != 'OK') {
                echo "❌ Quality Gate: ${qg.status}"

                // Affichage des détails si disponibles
                if (qg.conditions) {
                    echo "📊 Détails des conditions:"
                    qg.conditions.each { condition ->
                        echo "  • ${condition.metricName}: ${condition.actualValue} (seuil: ${condition.errorThreshold})"
                    }
                }

                // En fonction de la branche, on peut être plus ou moins strict
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

// ✅ FONCTION CORRIGÉE POUR OWASP DEPENDENCY CHECK AVEC NVD API KEY
def runDependencyCheckWithNVDKey(config) {
    try {
        echo "🔒 Vérification des dépendances OWASP avec NVD API Key..."

        // Utilisation des credentials pour la clé NVD API
        withCredentials([string(credentialsId: 'nvd-api-key', variable: 'NVD_API_KEY')]) {
            echo "🔑 Clé NVD API configurée"
            echo "🔍 Lancement de l'analyse OWASP avec mise à jour NVD..."

            timeout(time: config.timeouts.owaspCheck, unit: 'MINUTES') {
                def checkCommand = """
                    mvn org.owasp:dependency-check-maven:check \
                        -DnvdApiKey=\${NVD_API_KEY} \
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
                        -B -X
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
        echo "⚠️ Problème avec OWASP Dependency Check: ${errorMessage}"

        if (errorMessage.contains("timeout") || errorMessage.contains("Timeout")) {
            echo "⏰ OWASP Dependency Check interrompu pour timeout"
            currentBuild.result = 'UNSTABLE'
        } else if (errorMessage.contains("403") || errorMessage.contains("NVD Returned Status Code: 403")) {
            echo "🔑 Problème d'authentification avec l'API NVD"
            echo "💡 Vérifiez que la clé API 'nvd-api-key' est correctement configurée dans Jenkins"
            currentBuild.result = 'UNSTABLE'
        } else if (errorMessage.contains("CVE") || errorMessage.contains("vulnerability")) {
            echo "🚨 Vulnérabilités critiques détectées - Pipeline marqué comme instable"
            currentBuild.result = 'UNSTABLE'
        } else {
            echo "🚫 Erreur inattendue - Arrêt du pipeline"
            throw e
        }
    }
}

def archiveOwaspReports() {
    // Archivage des différents formats de rapport OWASP
    def reportFormats = [
        'dependency-check-report.html',
        'dependency-check-report.xml',
        'dependency-check-report.json',
        'dependency-check-report.csv'
    ]

    reportFormats.each { format ->
        if (fileExists("target/${format}")) {
            archiveArtifacts artifacts: "target/${format}", allowEmptyArchive: true
            echo "✅ Rapport ${format} archivé"
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
        echo "⚠️ Aucun rapport OWASP HTML généré"
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

def publishTestAndCoverageResults() {
    // Publication des résultats de tests avec junit
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

    // Publication du rapport de couverture JaCoCo
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
        echo "⚠️ Erreur lors de la collecte de diagnostic: ${e.getMessage()}"
    }
}

def checkDockerAvailability() {
    try {
        def result = sh(
            script: '''
                # Vérification avec retry
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
            echo "❌ Docker non disponible ou non fonctionnel"
            echo "💡 Le pipeline continuera sans les étapes Docker"
            echo "💡 Vérifiez que Docker est installé et que le daemon est démarré"
            echo "💡 Vérifiez les permissions de l'utilisateur Jenkins"
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
    ║ ☕ Java: ${env.JAVA_HOME}
    ║ 📦 Maven: ${env.MAVEN_HOME}
    ║ 🐳 Docker: ${env.DOCKER_AVAILABLE == "true" ? "✅ Disponible" : "❌ Indisponible"}
    ║ 🌍 Environnement: ${env.ENV_NAME}
    ║ 🚪 Port: ${env.HTTP_PORT}
    ║ 🏷️  Tag: ${env.CONTAINER_TAG}
    ║ 📧 Email: ${config.emailRecipients}
    ║ 🔍 SonarQube: ${config.sonar.communityEdition ? "Community Edition" : "Developer Edition+"}
    ║ 📊 Projet SonarQube: ${env.SONAR_PROJECT_KEY}
    ║ 🔑 OWASP NVD API: Configurée via Jenkins Credentials
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

        // Vérification de l'image
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
        error