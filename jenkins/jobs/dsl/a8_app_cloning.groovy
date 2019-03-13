def containerFolder = "${PROJECT_NAME}/Cloud_Provision/IaaS/Cloning"
def createJob = freeStyleJob(containerFolder + '/Run_Application_Cloning')

createJob.with {
    description('')
    parameters {
        stringParam('IP_ADDRESS', '', '')
		stringParam('HOSTNAME', '', '')
        stringParam('DOMAIN', '', '')
		stringParam('DATABASE_NAME', '', '')
		stringParam('APP_RUN_EDITION_DIR', '', '')
        stringParam('APP_PATCH_EDITION_DIR', '', '')
        stringParam('DATABASE_NAME_LOWER', '', '')
		stringParam('NEXUS_USERNAME', '', '')
		stringParam('APP_URL', '', '')
		stringParam('DB_BIN_URL', '', '')
		stringParam('DB_DATA_URL', '', '')
		stringParam('DB_ORADATA_URL', '', '')
		stringParam('DB_ARCHIVE_URL', '', '')
		stringParam('APPS_PASSWORD', '', '')
        stringParam('WEBLOGIC_PASSWORD', '', '')
		stringParam('NEXUS_PASSWORD', '', '')
		stringParam('DOMAIN_IP', '', '')
		stringParam('ANSIBLE_CFG', '', '')
		stringParam('CUSTOM_WORKSPACE', '', '')
        stringParam('SSH_KEY', '', '')
        stringParam('DB_PORT', '', '')
    }

    logRotator {
        numToKeep(10)
        artifactNumToKeep(10)
    }

    concurrentBuild(true)
    label('postgres')
    customWorkspace('$CUSTOM_WORKSPACE')

    wrappers {
        colorizeOutput('css')
    }

    steps {

        copyArtifacts('Run_Database_Cloning') {
            includePatterns('**/*')
            fingerprintArtifacts(true)
            buildSelector {
                upstreamBuild(true)
                latestSuccessful(false)
            }
        }

        shell('''#!/bin/bash

export ANSIBLE_CONFIG=${ANSIBLE_CFG}
export ANSIBLE_FORCE_COLOR=true

echo "#####################################################################################"
echo "# ====================> RUNNING APPLICATION ADCFGCLONE.PL <======================== #"
echo "#####################################################################################"

ansible-playbook -i target-host --private-key=${CUSTOM_WORKSPACE}/${SSH_KEY} -u opc --become --become-user root -e "target=ebs apps_password=${APPS_PASSWORD} \
wls_password=${WEBLOGIC_PASSWORD} db_name=${DATABASE_NAME} db_name_lower=${DATABASE_NAME_LOWER} db_host=${HOSTNAME} db_port=${DB_PORT} \
db_host_ip=${DOMAIN_IP} domain=${DOMAIN} app_run_dir=${APP_RUN_EDITION_DIR} app_patch_dir=${APP_PATCH_EDITION_DIR}" \
--tags "appstier" site.yml

		''')
	}
}