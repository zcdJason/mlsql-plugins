package tech.mlsql.plugins.mllib.ets

import org.apache.spark.ml.param.Param
import org.apache.spark.sql.expressions.UserDefinedFunction
import org.apache.spark.sql.{DataFrame, SparkSession}
import streaming.dsl.auth.TableAuthResult
import streaming.dsl.mmlib._
import streaming.dsl.mmlib.algs.classfication.BaseClassification
import streaming.dsl.mmlib.algs.param.{BaseParams, WowParams}
import streaming.dsl.mmlib.algs.{CodeExampleText, Functions, MetricValue}
import tech.mlsql.dsl.auth.ETAuth
import tech.mlsql.dsl.auth.dsl.mmlib.ETMethod.ETMethod
import tech.mlsql.plugins.mllib.app.MLSQLMllib
import tech.mlsql.version.VersionCompatibility

/**
 * 31/5/2021 WilliamZhu(allwefantasy@gmail.com)
 */
class ClassificationEvaluator(override val uid: String) extends SQLAlg
  with VersionCompatibility with Functions with WowParams with ETAuth with BaseClassification {
  def this() = this(BaseParams.randomUID())

  /**
   * run table as  ClassificationEvaluator.`` where labelCol="label";
   */
  override def train(df: DataFrame, path: String, params: Map[String, String]): DataFrame = {
    batchPredict(df, path, params)
  }

  override def batchPredict(df: DataFrame, path: String, params: Map[String, String]): DataFrame = {
    val items = multiclassClassificationEvaluate(df, (evaluator) => {
      evaluator.setLabelCol(params.getOrElse(labelCol.name, "label"))
      evaluator.setPredictionCol("prediction")
    })
    import df.sparkSession.implicits._
    df.sparkSession.createDataset[MetricValue](items).toDF()
  }

  override def load(sparkSession: SparkSession, path: String, params: Map[String, String]): Any = ???

  override def predict(sparkSession: SparkSession, _model: Any, name: String, params: Map[String, String]): UserDefinedFunction = ???

  override def supportedVersions: Seq[String] = {
    MLSQLMllib.versions
  }

  override def auth(etMethod: ETMethod, path: String, params: Map[String, String]): List[TableAuthResult] = {
    List()
  }

  override def modelType: ModelType = AlgType

  override def doc: Doc = Doc(HtmlDoc,
    """
      |Compute f1|weightedPrecision|weightedRecall|accuracy for predicted table.
    """.stripMargin)


  override def codeExample: Code = Code(SQLCode, CodeExampleText.jsonStr +
    """
      |predict data as RandomForest.`/tmp/model` as predicted_table;
      |run predicted_table as ClassificationEvaluator.``;
    """.stripMargin)

  override def explainParams(sparkSession: SparkSession): DataFrame = {
    _explainParams(sparkSession)
  }

  final val labelCol: Param[String] = new Param[String](this, "labelCol", "default: label")

}
