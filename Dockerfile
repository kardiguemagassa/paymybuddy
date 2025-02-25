FROM openjdk:21-jdk-slim

# Définir le répertoire de travail
WORKDIR /app

# Copier le fichier JAR dans l'image
COPY target/paymybuddy-0.0.1-SNAPSHOT.jar app.jar

# Exposer le port 8080 (port par défaut de Spring Boot)
EXPOSE 8080

# Commande pour exécuter l'application
ENTRYPOINT ["java", "-jar", "app.jar"]
