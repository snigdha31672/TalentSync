$project = "C:\Users\Snigdha\Downloads\job-recruitment-Snigdha\job-recruitment-Snigdha"
$maven = "C:\Users\Snigdha\Downloads\apache-maven-3.9.16-bin\apache-maven-3.9.16\bin\mvn.cmd"

$env:JAVA_HOME = "C:\Program Files\Java\jdk-21.0.12"

# Start Eureka Server
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$project'; `$env:JAVA_HOME='$env:JAVA_HOME'; & '$maven' -pl eureka-server spring-boot:run"

Start-Sleep -Seconds 10

# Start backend services
$services = @(
    "auth-service",
    "job-service",
    "application-service",
    "recruitment-service",
    "api-gateway"
)

foreach ($service in $services) {
    Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$project'; `$env:JAVA_HOME='$env:JAVA_HOME'; & '$maven' -pl $service spring-boot:run"
    Start-Sleep -Seconds 3
}

# Start frontend
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$project\frontend'; npm run dev"

Write-Host "TalentSync startup commands have been launched."

