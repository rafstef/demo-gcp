#!/usr/bin/env groovy

def askUserInput(String message,String multipleChoise,String defaultChoice,int time) {
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

def getEnvName(branchName) {
    if("origin/develop".equals(branchName)) {
        return "DEV";
    } else if ("origin/master".equals(branchName)) {
        return "PROD";
    } else if ("origin/release".equals(branchName)) {
        return "PREPROD";
    }
}

pipeline {
    agent any
    environment {
        TERRAFORM_PLAN_FILE="DEMO"
        ENV_NAME = getEnvName(env.GIT_BRANCH)
    }
    tools {
        terraform 'terraform'
    }
    stages{
        stage ("Setup Deployment Environment") {
            steps {
                script {
                    echo "branch name: ${env.GIT_BRANCH}"
                    echo "env name: ${env.ENV_NAME.toLowerCase()}"
                    load "envvars/${env.ENV_NAME.toLowerCase()}.groovy"
                }
            }
        }

        stage('Login Step') {
            steps {
                withCredentials([file(credentialsId: "GCP_SERVICE_ACCOUNT", variable: 'GCP_CREDENTIALS')]){
                    script {
                        sh "mkdir -p credentials"
                        sh "cat $GCP_CREDENTIALS > credentials/gcp-credentials.json"
                        env.GOOGLE_APPLICATION_CREDENTIALS="credentials/gcp-credentials.json"
                    }
                }
            }
        }
        stage('TF Init and validations checks') {
            steps {
                sh "terraform version"
                sh "terraform init -no-color"
                sh "terraform workspace select ${env.ENV_NAME} -no-color"
                sh "terraform validate -no-color"
            }
        }
        stage('TF Plan creation') {
            steps {
                sh "terraform version"
                sh "terraform init -no-color"
                sh "terraform workspace select ${env.ENV_NAME} -no-color"
                sh "terraform plan -destroy -no-color -input=false -out terraform-plan-${env_name}.plan"
            }
        }
        stage('TF Destroy') {
            steps {
                script{
                    approve_plan=askUserInput("Apply Terraform destroy plan?","NO\nYES","NO",300)
                    if( approve_plan == "YES"){
                        sh "terraform version"
                        sh "terraform init -no-color"
                        sh "terraform workspace select ${env.ENV_NAME} -no-color"
                        sh "terraform apply -destroy -no-color -input=false terraform-plan-${env_name}.plan"
                    }else{
                        echo "TF destroy plan not approved. Skip Apply . . . "
                    }
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
