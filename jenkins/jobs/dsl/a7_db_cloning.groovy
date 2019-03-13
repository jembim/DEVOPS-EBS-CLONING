def containerFolder = "${PROJECT_NAME}/Cloud_Provision/IaaS/Cloning"
def createJob = freeStyleJob(containerFolder + '/Run_Database_Cloning')

createJob.with {
    description('')
    parameters {
        stringParam('IP_ADDRESS', '', '')
		stringParam('HOSTNAME', '', '')
        stringParam('DOMAIN', '', '')
        stringParam('SOURCE_DATABASE_NAME', '', '')
		stringParam('DATABASE_NAME', '', '')
		stringParam('APP_RUN_EDITION_DIR', '', '')
        stringParam('APP_PATCH_EDITION_DIR', '', '')
        stringParam('DATA_TOP_ONE', '', '')
        stringParam('DATA_TOP_TWO', '', '')
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

        copyArtifacts('Extract_DB_Archives') {
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
echo "# =====================> RUNNING DATABASE ADCFGCLONE.PL <========================== #"
echo "#####################################################################################"

ansible-playbook -i target-host --private-key=${CUSTOM_WORKSPACE}/${SSH_KEY} -u opc --become --become-user root -e "target=ebs apps_password=${APPS_PASSWORD} \
db_name=${DATABASE_NAME} db_name_lower=${DATABASE_NAME_LOWER} source_db_name=${SOURCE_DATABASE_NAME} db_host=${HOSTNAME} db_port=${DB_PORT} \
db_host_ip=${DOMAIN_IP} domain=${DOMAIN} data_top_one=${DATA_TOP_ONE} data_top_two=${DATA_TOP_TWO}" --tags "dbtier" site.yml

		''')
	}

    publishers {

        archiveArtifacts('**/*')

        downstreamParameterized {
            trigger('Run_Application_Cloning') {
                condition('SUCCESS')
                parameters {
                    currentBuild()
                }
            }
        }
    }
}