@Library('dynatrace@master') _

def _VERSION = 'UNKNOWN'
def _TAG = 'UNKNOWN'
def _TAG_DEV = 'UNKNOWN'
def _TAG_STAGING = 'UNKNOWN'

pipeline {
  agent {
    label 'maven'
  }
  environment {
    APP_NAME = "queue-master"
    ARTEFACT_ID = "sockshop-" + "${env.APP_NAME}"
    NL_DT_TAG="app:${env.APP_NAME},environment:dev"
    QUEUEMASTER_ANOMALIEFILE="$WORKSPACE/monspec/queue-master_anomalieDection.json"
    DYNATRACEID="${env.DT_ACCOUNTID}"
    DYNATRACEAPIKEY="${env.DT_API_TOKEN}"
    NLAPIKEY="${env.NL_WEB_API_KEY}"
    OUTPUTSANITYCHECK="$WORKSPACE/infrastructure/sanitycheck.json"
    DYNATRACEPLUGINPATH="$WORKSPACE/lib/DynatraceIntegration-3.0.1-SNAPSHOT.jar"
    BASICCHECKURI="/health"

  }
  stages {
    stage('Maven build') {
      steps {
        // checkout scm
        checkout([$class: 'GitSCM', 
          branches: [[name: "${env.BRANCH_NAME}"]], 
          doGenerateSubmoduleConfigurations: false, 
          extensions: [], 
          userRemoteConfigs: [[url: "https://github.com/${env.GITHUB_ORGANIZATION}/${env.APP_NAME}"]]
        ])
        script {
          _VERSION = readFile('version').trim()
          _TAG = "${env.DOCKER_REGISTRY_URL}:5000/sockshop-registry/${env.ARTEFACT_ID}"
          _TAG_DEV = "${_TAG}:${_VERSION}-${env.BUILD_NUMBER}"
          _TAG_STAGING = "${_TAG}:${_VERSION}"
        }
        container('maven') {
            sh "mvn -B clean package -DdynatraceId=$DYNATRACEID -DneoLoadWebAPIKey=$NLAPIKEY -DdynatraceApiKey=$DYNATRACEAPIKEY -Dtags=${NL_DT_TAG} -DoutPutReferenceFile=$OUTPUTSANITYCHECK -DcustomActionPath=$DYNATRACEPLUGINPATH -DjsonAnomalieDetectionFile=$QUEUEMASTER_ANOMALIEFILE"
            sh "chmod -R 777 $WORKSPACE/target/neoload/"

        }
      }
    }
    stage('Docker build') {
      when {
        expression {
          return env.BRANCH_NAME ==~ 'release/.*' || env.BRANCH_NAME ==~'master'
        }
      }
      steps {
        container('docker') {
          sh "docker build -t ${_TAG_DEV} ."
        }
      }
    }
    stage('Docker push to registry'){
      when {
        expression {
          return env.BRANCH_NAME ==~ 'release/.*' || env.BRANCH_NAME ==~'master'
        }
      }
      steps {
        container('docker') {
          withCredentials([usernamePassword(credentialsId: 'registry-creds', passwordVariable: 'TOKEN', usernameVariable: 'USER')]) {
            sh "docker login --username=anything --password=${TOKEN} ${env.DOCKER_REGISTRY_URL}:5000"
            sh "docker tag ${_TAG_DEV} ${_TAG_DEV}"
            sh "docker push ${_TAG_DEV}"
          }
        }
      }
    }
    stage('Deploy to dev namespace') {
      when {
        expression {
          return env.BRANCH_NAME ==~ 'release/.*' || env.BRANCH_NAME ==~'master'
        }
      }
      steps {
        container('kubectl') {
          sh "sed -i 's#image: .*#image: ${_TAG_DEV}#' manifest/queue-master.yml"
          sh "kubectl -n dev apply -f manifest/queue-master.yml"
        }
      }
    }
    stage('DT Deploy Event') {
        when {
            expression {
            return env.BRANCH_NAME ==~ 'release/.*' || env.BRANCH_NAME ==~'master'
            }
        }
        steps {
          container("curl") {
            // send custom deployment event to Dynatrace
            sh "curl -X POST \"$DT_TENANT_URL/api/v1/events?Api-Token=$DT_API_TOKEN\" -H \"accept: application/json\" -H \"Content-Type: application/json\" -d \"{ \\\"eventType\\\": \\\"CUSTOM_DEPLOYMENT\\\", \\\"attachRules\\\": { \\\"tagRule\\\" : [{ \\\"meTypes\\\" : [\\\"SERVICE\\\"], \\\"tags\\\" : [ { \\\"context\\\" : \\\"CONTEXTLESS\\\", \\\"key\\\" : \\\"app\\\", \\\"value\\\" : \\\"${env.APP_NAME}\\\" }, { \\\"context\\\" : \\\"CONTEXTLESS\\\", \\\"key\\\" : \\\"environment\\\", \\\"value\\\" : \\\"dev\\\" } ] }] }, \\\"deploymentName\\\":\\\"${env.JOB_NAME}\\\", \\\"deploymentVersion\\\":\\\"${_VERSION}\\\", \\\"deploymentProject\\\":\\\"\\\", \\\"ciBackLink\\\":\\\"${env.BUILD_URL}\\\", \\\"source\\\":\\\"Jenkins\\\", \\\"customProperties\\\": { \\\"Jenkins Build Number\\\": \\\"${env.BUILD_ID}\\\",  \\\"Git commit\\\": \\\"${env.GIT_COMMIT}\\\" } }\" "
          }
        }
    }
    stage('Start NeoLoad infrastructure') {

        steps {
                container('kubectl') {
                    script {
                     sh "kubectl create -f $WORKSPACE/infrastructure/infrastructure/neoload/lg/docker-compose.yml"
                    }
                }
        }

    }
    stage('Run health check in dev') {
      when {
        expression {
          return env.BRANCH_NAME ==~ 'release/.*' || env.BRANCH_NAME ==~'master'
        }
      }
      steps {
        echo "Waiting for the service to start..."
        sleep 300

        container('neoload') {

         script {

            sh "mkdir -p /home/jenkins/.neotys/neoload"
            sh "cp $WORKSPACE/infrastructure/infrastructure/neoload/license.lic /home/jenkins/.neotys/neoload/"
            status =sh(script:"/neoload/bin/NeoLoadCmd -project $WORKSPACE/target/neoload/queuemaster_NeoLoad/queuemaster_NeoLoad.nlp -testResultName HealthCheck_queuemaster_${BUILD_NUMBER} -description HealthCheck_queuemaster_${BUILD_NUMBER} -nlweb -L Population_BasicCheckTesting=$WORKSPACE/infrastructure/infrastructure/neoload/lg/remote.txt -L Population_Dynatrace_Integration=$WORKSPACE/infrastructure/infrastructure/neoload/lg/local.txt -nlwebToken $NLAPIKEY -variables host=${env.APP_NAME}.dev.svc,port=80,basicPath=${BASICCHECKURI} -launch DynatraceSanityCheck -noGUI", returnStatus: true)

            if (status != 0) {
                      currentBuild.result = 'FAILED'
                      error "Health check in dev failed."
            }
         }

        }
      }
    }
    stage('Sanity Check') {
              steps {
                container('neoload') {
                  script {
                         status =sh(script:"/neoload/bin/NeoLoadCmd -project $WORKSPACE/target/neoload/queuemaster_NeoLoad/queuemaster_NeoLoad.nlp -testResultName DynatraceSanityCheck_carts_${BUILD_NUMBER} -description DynatraceSanityCheck_carts_${BUILD_NUMBER} -nlweb -L  Population_Dynatrace_SanityCheck=$WORKSPACE/infrastructure/infrastructure/neoload/lg/local.txt -nlwebToken $NLAPIKEY -variables host=${env.APP_NAME}.dev.svc,port=80 -launch DYNATRACE_SANITYCHECK  -noGUI", returnStatus: true)

                         if (status != 0) {
                              currentBuild.result = 'FAILED'
                              error "Health check in dev failed."
                         }
                   }
                 }
                 container('git') {
                   echo "push ${OUTPUTSANITYCHECK}"
                   //---add the push of the sanity check---
                   withCredentials([usernamePassword(credentialsId: 'git-credentials-acm', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                       sh "git config --global user.email ${env.GITHUB_USER_EMAIL}"
                       sh "git stash"
                      // sh "git pull https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/${env.GITHUB_ORGANIZATION}/queue-master origin master -r"
                       sh "git add ${OUTPUTSANITYCHECK}"
                       sh "git commit -m 'Update Sanity_Check_${BUILD_NUMBER} ${env.APP_NAME} version ${env.VERSION}'"
                     //  sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/${env.GITHUB_ORGANIZATION}/queue-master ${GITORIGIN} master"
                   }
                 }

              }
    }
    stage('Run functional check in dev') {
      when {
        expression {
          return env.BRANCH_NAME ==~ 'release/.*' || env.BRANCH_NAME ==~'master'
        }
      }
      steps {
        container('neoload') {
         script {

                status =sh(script:"/neoload/bin/NeoLoadCmd -project $WORKSPACE/target/neoload/queuemaster_NeoLoad/queuemaster_NeoLoad.nlp -testResultName FuncTest_queuemaster_${BUILD_NUMBER} -description FuncCheck_queuemaster_${BUILD_NUMBER} -nlweb -L Population_BasicCheckTesting=$WORKSPACE/infrastructure/infrastructure/neoload/lg/remote.txt -L Population_Dynatrace_Integration=$WORKSPACE/infrastructure/infrastructure/neoload/lg/local.txt -nlwebToken $NLAPIKEY -variables host=${env.APP_NAME}.dev.svc,port=80,basicPath=${BASICCHECKURI} -launch QueueMaster_Load -noGUI", returnStatus: true)
                if (status != 0) {
                    currentBuild.result = 'FAILED'
                    error "Load Test on cart."
              }
         }

        }
      }
    }
    stage('Mark artifact for staging namespace') {
      when {
        expression {
          return env.BRANCH_NAME ==~ 'release/.*'
        }
      }
      steps {
        container('docker'){
          sh "docker tag ${_TAG_DEV} ${_TAG_STAGING}"
          sh "docker push ${_TAG_STAGING}"
        }
      }
    }
    stage('Deploy to staging') {
      when {
        beforeAgent true
        expression {
          return env.BRANCH_NAME ==~ 'release/.*'
        }
      }
      steps {
        build job: "k8s-deploy-staging",
          parameters: [
            string(name: 'APP_NAME', value: "${env.APP_NAME}"),
            string(name: 'TAG_STAGING', value: "${_TAG_STAGING}"),
            string(name: 'VERSION', value: "${_VERSION}")
          ]
      }
    }
  }
  post {
          always {
            container('kubectl') {
                   script {
                    echo "delete neoload infrastructure"
                    sh "kubectl delete svc nl-lg-queuemaster -n cicd-neotys"
                    sh "kubectl delete pod nl-lg-queuemaster -n cicd-neotys --grace-period=0 --force"
                   }
            }
          }

        }
}
