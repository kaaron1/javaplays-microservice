# javaplays-microservice
_______________________________________________
# How to get this running?

## Create a Bluemix MQLight service as named in manifest.yml of this project
    cf cs MQLight standard <name of the service>

## Download the Project Zip, Extract the zip, change the names of the application and services
    ---
    applications:
    - name: microservice-alchemybackend
      memory: 256M
      instances: 2
      no-route: true
      path: backend/AlchemyMQLightBackendWorker.jar
      buildpack: https://github.com/cloudfoundry/java-buildpack
      services:
      - ecodcnc-microservice
    - name: ecodcnc-microservice-alchemyfrontend
      memory: 256M
      instances: 1
      host: microservice-mqlightfrontend
      path: frontEnd/FrontEndMQLightPubSubApp-1.0.war
      services:
      - ecodcnc-microservice
## Push the app to Bluemix
    cf push

### SAMPLE APPLICATION URL

http://alchemyauthormqlight-java-postuterine-gerrymanderer.mybluemix.net/
    
