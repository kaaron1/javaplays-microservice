# javaplays-microservice

## How to get this running?

### Create a Bluemix MQLight service as named in manifest.yml of this project
    cf cs MQLight standard <name of the service>

### Download the Project Zip, Extract the zip, change the names of the application and services in the manifest.yml file...
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
### Push the app to Bluemix
    cf push

### SAMPLE APPLICATION URL

http://alchemyauthormqlight-java-postuterine-gerrymanderer.mybluemix.net/

# Privacy Notice

Sample web applications that include this package may be configured to track deployments to [IBM Bluemix](https://www.bluemix.net/) and other Cloud Foundry platforms. The following information is sent to a [Deployment Tracker](https://github.com/IBM-Bluemix/cf-deployment-tracker-service) service on each deployment:

* Node.js package version
* Node.js repository URL
* Application Name (`application_name`)
* Space ID (`space_id`)
* Application Version (`application_version`)
* Application URIs (`application_uris`)

This data is collected from the `package.json` file in the sample application and the `VCAP_APPLICATION` environment variable in IBM Bluemix and other Cloud Foundry platforms. This data is used by IBM to track metrics around deployments of sample applications to IBM Bluemix to measure the usefulness of our examples, so that we can continuously improve the content we offer to you. Only deployments of sample applications that include code to ping the Deployment Tracker service will be tracked.
