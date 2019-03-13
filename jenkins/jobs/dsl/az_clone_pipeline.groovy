def containerFolder = "${PROJECT_NAME}/Cloud_Provision/IaaS/Cloning"

buildPipelineView(containerFolder + '/Cloning_Pipeline') {
    title('Environment_Cloning')
    displayedBuilds(10)
    selectedJob('Set_Cloning_Parameters')
	showPipelineDefinitionHeader()
    showPipelineParameters()
	consoleOutputLinkStyle(OutputStyle.NewWindow)
    refreshFrequency(3)
}