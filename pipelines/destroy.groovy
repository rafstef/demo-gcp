#!/usr/bin/env groovy

def call (String message,String multipleChoise,String defaultChoice,int time) {
    def userInput = defaultChoice
    try{
        timeout(time: time, unit: 'SECONDS') {
        userInput = input message: message, ok: 'OK', parameters: [choice(name: 'USERINPUT', choices: multipleChoise, defaultChoice: defaultChoice, description: message)] }
    } catch (err) {
        defaultChoice
    }
    echo "User select : ${userInput}"
    userInput
}

pipeline {
    agent any
    tools {
        terraform 'terraform'
    }
    stages{
        stage('Login Step') {
            steps {
                withCredentials([file(credentialsId: "GCP_SERVICE_ACCOUNT", variable: 'BACKEND_CREDENTIALS')]){
                    script {
                        sh "mkdir -p credentials"
                        sh "cat $GCP_CREDENTIALS > credentias/gcp-credentials.json"
                        env.GOOGLE_APPLICATION_CREDENTIALS="credentials/gcp-credentials.json"
                    }
                }  
            }
        }
        stage('TF Init and validations checks') {
            steps {
                sh "terraform version"
                sh "terraform init -no-color"
                sh "terraform validate -no-color"
            }
        }
        stage('TF Plan creation') {
            steps {
                sh "terraform plan -destroy -no-color -input=false -out terraform-plan-${env_name}.plan"
            }
        }
        stage('TF Destroy') {
            steps {
                approve_plan=buildStep_askUserInput("Apply Terraform plan?","NO\nYES","NO",300)
                if( approve_plan == "YES"){
                    sh "terraform apply -destroy -no-color -input=false terraform-plan-${env_name}.plan"
                }else{
                    echo "TF destroy not approved. Skip Apply . . . "
                }
            }
        }
    }
    post {
        always {
            cleanWs(cleanWhenNotBuilt: false,
            deleteDirs: true,
            disableDeferredWipeout: true,
            notFailBuild: true,
            patterns: [[pattern: '.gitignore', type: 'INCLUDE'],
            [pattern: '.propsfile', type: 'EXCLUDE']])
        }
    }
}