import sys
sys.path.insert(1,"../../../")
import h2o
from tests import pyunit_utils
from h2o.estimators.gbm import H2OGradientBoostingEstimator
from random import randint

# HEXDEV-700: GBM reproducibility issue.
def gbm_reproducibility():

  # run GBM twice with true_reproducibility = True
  seedv = randint(1,10000000000)
  ntree = 50

  auc2 = runGBM(seedv, True, ntree)

  if (randint(1,10) > 5): # only run 1 of the two tests to save time
    auc1 = runGBM(seedv, True, ntree)
    pyunit_utils.equal_two_arrays(auc1, auc2, 1e-10, True)  # should be equal in this case
  else:
    auc3 = runGBM(seedv, False, ntree)  # threshold should be different this run.
    assert not(pyunit_utils.equal_two_arrays(auc2, auc3, 1e-10, False)), "parameter true_reproducibility is not working."


def runGBM(seedV, repo, ntree):
  cars = h2o.import_file(path=pyunit_utils.locate("bigdata/laptop/jira/reproducibility_issue.csv.zip"))
  gbm = H2OGradientBoostingEstimator(distribution='bernoulli', ntrees=ntree, seed= seedV, max_depth = 4,
                                   min_rows = 7, true_reproducibility=repo)
  gbm.train(x=list(range(2,365)), y="response", training_frame=cars)
  if repo:
    print("Model run time (ms) with reproducibility is {0}".format(gbm._model_json["output"]["run_time"]))
  else:
    print("Model run time (ms) without reproducibility is {0}".format(gbm._model_json["output"]["run_time"]))
  auc = pyunit_utils.extract_from_twoDimTable(gbm._model_json['output']['training_metrics']._metric_json['thresholds_and_metric_scores'], 'threshold', takeFirst=False)
  h2o.remove(cars)
  h2o.remove(gbm)
  return auc

if __name__ == "__main__":
  pyunit_utils.standalone_test(gbm_reproducibility)
else:
  gbm_reproducibility()