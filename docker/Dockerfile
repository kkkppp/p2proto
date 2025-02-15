# Use the official Tomcat image from Docker Hub
FROM tomcat:9.0-jdk17

# Optional: remove the default ROOT application if you want
#RUN rm -rf /usr/local/tomcat/webapps/ROOT

# Copy the WAR file to the webapps directory
# If you want the application to be accessible at http://localhost:8080/,
# name the file ROOT.war instead of my-app.war.
COPY build/libs/p2proto.war /usr/local/tomcat/webapps/

# Expose port 8080
EXPOSE 8080

# The default Tomcat start command
CMD ["catalina.sh", "run"]
