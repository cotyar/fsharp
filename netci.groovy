import jobs.generation.Utilities;

def project = GithubProject
def branch = GithubBranchName

def osList = ['Windows_NT'] //'Ubuntu', 'OSX', 'CentOS7.1'

def machineLabelMap = ['Ubuntu':'ubuntu-doc',
                       'OSX':'mac',
                       'Windows_NT':'windows-elevated',
                       'CentOS7.1' : 'centos-71']

def static getBuildJobName(def configuration, def os) {
    return configuration.toLowerCase() + '_' + os.toLowerCase()
}

[true, false].each { isPullRequest ->
    ['Debug', 'Release_ci_part1', Release_ci_part2].each { configuration ->
        osList.each { os ->

            def lowerConfiguration = configuration.toLowerCase()

            // Calculate job name
            def jobName = getBuildJobName(configuration, os)

            def buildCommand = '';

            def buildFlavor= '';
            if (configuration == "Debug") {
                buildFlavor = "Debug"
                build_args = ""
            }
            else {
                buildFlavor = "Release"
                if (configuration == "Release_ci_part1") {
                    build_args = "ci_part1"
                }
                else {
                    build_args = "ci_part1"
                }
            }

            if (os == 'Windows_NT') {
                buildCommand = ".\\build.cmd ${buildFlavor} $(build_args)"
            }
            else {
                buildCommand = "./build.sh ${buildFlavor} $(build_args)"
            }

            def newJobName = Utilities.getFullJobName(project, jobName, isPullRequest)
            def newJob = job(newJobName) {
                label(machineLabelMap[os])
                steps {
                    if (os == 'Windows_NT') {
                        // Batch
                        batchFile(buildCommand)
                    }
                    else {
                        // Shell
                        shell(buildCommand)
                    }
                }
            }

            // TODO: set to false after tests are fully enabled
            def skipIfNoTestFiles = true

            Utilities.setMachineAffinity(newJob, os, 'latest-or-auto')
            Utilities.standardJobSetup(newJob, project, isPullRequest, "*/${branch}")
            Utilities.addXUnitDotNETResults(newJob, 'tests/TestResults/**/*_Xml.xml', skipIfNoTestFiles)
            Utilities.addArchival(newJob, "${buildFlavor}/**")

            if (isPullRequest) {
                Utilities.addGithubPRTriggerForBranch(newJob, branch, "${os} ${configuration} Build")
            }
            else {
                Utilities.addGithubPushTrigger(newJob)
            }
        }
    }
}
