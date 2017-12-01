setwd(normalizePath(dirname(R.utils::commandArgs(asValues=TRUE)$"f")))
source("../../../scripts/h2o-r-test-setup.R")

test.gbm <- function() {
  # Determine model sizes
  seeds = c(987654321, 123456789, 1029384756)
  index = sample(c(1:length(seeds)))[1]
  ntree=50
  
  auc_run1 <- TrainGBM(seeds[index], TRUE, ntree)
  if (runif(1,0,1) > 0.5) { # only run 1 set of these tests.
    auc_run2 <- TrainGBM(seeds[index], TRUE, ntree)
    expect_equal(auc_run1, auc_run2)
  } else {
    auc_run3 <- TrainGBM(seeds[index], FALSE, ntree)
    auc_h2o <- as.data.frame(auc_run1)  # extract h2o gbm auc threshold values
    auc_h2o3 <- as.data.frame(auc_run3)
    stopifnot(length(setdiff(auc_h2o, auc_h2o3))>0)   # throw an error if  matching answer
  }
}

# borrowed from Megan K
TrainGBM <- function(seedNum, trueRepo, nt) {
  # data_path <- locate("bigdata/laptop/jira/reproducibility_issue.csv.zip")
  # temp <- h2o.importFile(data_path, parse=FALSE)
  # x <- h2o.parseSetup(temp, chunk_size=18691584)
  data <- h2o.importFile(locate("bigdata/laptop/jira/reproducibility_issue.csv.zip"))
  gbm_v1 <- h2o.gbm(x=2:365, y='response', training_frame = data,
          distribution = "bernoulli", ntrees = nt, seed = seedNum, max_depth = 4, min_rows = 7,
          score_tree_interval=nt, true_reproducibility=trueRepo
  )
  auc_gbm = gbm_v1@model$training_metrics@metrics$thresholds_and_metric_scores$threshold
  h2o.rm(data)
  h2o.rm(gbm_v1)
  auc_gbm
}

doTest("GBM reproducibility test", test.gbm)
