# Use an appropriate base image with Java
FROM openjdk:20

# Install FUSE
RUN apt-get update && apt-get install -y fuse

# Set the working directory
WORKDIR /app

# Copy your application code to the container
COPY . .

# Ensure that the FUSE library is available
RUN ldconfig

# Compile your Java application (if necessary)
# RUN javac -cp ".:path/to/your/jars/*" source/Mp3VirtualFS.java

# Run your Java application
CMD ["java", "-cp", ".:path/to/your/jars/*", "source.Mp3VirtualFS"]
