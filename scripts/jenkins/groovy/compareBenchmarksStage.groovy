def call(buildConfig, stageConfig, benchmarkFolderConfig) {

    def EXPECTED_VALUES = [
            'paribas': [
                    50: [
                            'train_time_min': 9.2,
                            'train_time_max': 11.7
                    ],
                    200: [
                            'train_time_min': 31.1,
                            'train_time_max': 35.1
                    ]
            ],
            'homesite': [
                    50: [
                            'train_time_min': 11.4,
                            'train_time_max': 13.3
                    ],
                    200: [
                            'train_time_min': 41.2,
                            'train_time_max': 46.0
                    ]
            ],
            'redhat': [
                    50: [
                            'train_time_min': 28.5,
                            'train_time_max': 33.5
                    ],
                    200: [
                            'train_time_min': 132.5,
                            'train_time_max': 139.5
                    ]
            ],
            'springleaf': [
                    50: [
                            'train_time_min': 55,
                            'train_time_max': 63.5
                    ],
                    200: [
                            'train_time_min': 464,
                            'train_time_max': 497
                    ]
            ],
            'higgs': [
                    50: [
                            'train_time_min': 92,
                            'train_time_max': 100
                    ],
                    200: [
                            'train_time_min': 510,
                            'train_time_max': 559
                    ]
            ]
    ]

    def TESTED_COLUMNS = ['train_time']

    def insideDocker = load('h2o-3/scripts/jenkins/groovy/insideDocker.groovy')

    insideDocker(benchmarkEnv, stageConfig.image, buildConfig.DOCKER_REGISTRY, 5, 'MINUTES') {
        withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: buildConfig.AWS_CREDENTIALS_ID]]) {
            String comparisonFile = benchmarkFolderConfig.getCSVPath()

            echo """
----------------------------------
###### Get Results to S3 ######"
----------------------------------
    Comparing:    | ${comparisonFile}
----------------------------------
                """
            //FIXME REMOVE
            def latestFile = sh(script:'s3cmd ls s3://test.0xdata.com/benchmarks/master/csv/gbm/ | tail -1 | awk \'{print $4}\'', returnStdout: true).trim()
            sh "s3cmd get ${latestFile} ${comparisonFile}"
            //

            def csvData = new File(comparisonFile).withReader {
                CsvParser.parseCsv(it)
            }
            echo "${csvData}"
            List<String> errorMessages = []
            for (column in TESTED_COLUMNS) {
                for (line in csvData) {
                    def datasetValues = EXPECTED_VALUES[line.dataset]
                    if (relevantValues) {
                        def ntreesValues = datasetValues[Integer.parseInt(line.ntrees)]
                        if (ntreesValues) {
                            def minValue = ntreesValues["${column}_min"]
                            if (minValue == null) {
                                error("Minimum for ${column} for ${line.dataset} with ${line.ntrees} trees cannot be found")
                            }
                            def maxValue = ntreesValues["${column}_max"]
                            if (maxValue == null) {
                                error("Maximum for ${column} for ${line.dataset} with ${line.ntrees} trees cannot be found")
                            }
                            def lineValue = Double.parseDouble(line[column])

                            if ((lineValue < minValue) || (lineValue > maxValue)) {
                                errorMessages.add("${column} for ${line.dataset} with ${line.ntrees} trees not in expected interval. Should be between ${minValue} and ${maxValue} but was ${lineValue}")
                            }
                        } else {
                            error "Cannot find EXPECTED_VALUES for ${line.dataset} with ${line.ntrees} trees"
                        }
                    } else {
                        error "Cannot find EXPECTED_VALUES for ${line.dataset}"
                    }
                }
            }
        }
    }
}

return this