import mill._
import mill.scalalib._
import publish._
import $ivy.`de.tototec::de.tobiasroeser.mill.integrationtest:0.3.3`
import de.tobiasroeser.mill.integrationtest._
import $ivy.`de.tototec::de.tobiasroeser.mill.vcs.version_mill0.7:0.1.2`
import de.tobiasroeser.mill.vcs.version.VcsVersion
import mill.api.Loose

val baseDir = build.millSourcePath
val rtMillVersion = build.version

sealed trait Deps {
  def millVersion: String
  def scalaVersion: String

  val millMain = ivy"com.lihaoyi::mill-main:${millVersion}"
  val millScalalib = ivy"com.lihaoyi::mill-scalalib:${millVersion}"

  val allDeps = Agg(millMain, millScalalib)
}
object Deps_0_10_0 extends Deps {
  override def millVersion = "0.10.0"
  override def scalaVersion = "2.13.8"
}
object Deps_0_7 extends Deps {
  override def millVersion = "0.7.0"
  override def scalaVersion = "2.13.2"
}
object Deps_0_6 extends Deps {
  override def millVersion = "0.6.0"
  override def scalaVersion = "2.12.10"
}

val crossVersions = Map(
  "0.10" -> Deps_0_10_0,
  "0.7" -> Deps_0_7,
  "0.6" -> Deps_0_6,
)

val itestVersions = Seq(
  "0.7.4", "0.7.3", "0.7.2", "0.7.1", "0.7.0",
  "0.6.3", "0.6.2", "0.6.1", "0.6.0"
)

object antlr extends mill.Cross[AntlrCross](crossVersions.keys.toSeq: _*)
class AntlrCross(val millPlatform: String) extends CrossScalaModule with PublishModule {
  val deps: Deps = crossVersions(millPlatform)

  override def crossScalaVersion = deps.scalaVersion
  override def compileIvyDeps: T[Loose.Agg[Dep]] = deps.allDeps

  override def millSourcePath: os.Path = os.pwd

  override def ivyDeps: T[Loose.Agg[Dep]] = Agg(ivy"org.antlr:antlr4:4.8-1")

  override def publishVersion: T[String] = "0.2.0"

  override def artifactSuffix = s"_mill${millPlatform}_${artifactScalaVersion()}"
//  override def publishVersion: T[String] = VcsVersion.vcsState().format()

  override def artifactName: T[String] = "mill-antlr"

  override def pomSettings = PomSettings(
    description = "Antlr support for mill builds.",
    organization = "co.actioniq",
    url = "https://github.com/ActionIQ/mill-antlr",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("ActionIQ", "mill-antlr"),
    developers = Seq(
      Developer("nitay", "Nitay Joffe", "https://github.com/nitay"),
      Developer("ml86", "Markus Lottmann", "https://github.com/ml86")
    )
  )
}

object itest extends Cross[ItestCross](itestVersions: _*)
class ItestCross(itestVersion: String)  extends MillIntegrationTestModule {
  val millApiVersion = itestVersion.split("[.]").take(2).mkString(".")
  override def millSourcePath: os.Path = super.millSourcePath / os.up
  override def millTestVersion = itestVersion
  override def pluginsUnderTest = Seq(antlr(millApiVersion))
}

object P extends Module {
  // Update the millw script.
  def millw() = T.command {
    val target = mill.modules.Util.download("https://raw.githubusercontent.com/lefou/millw/master/millw")
    val millw = baseDir / "mill"
    val res = os.proc(
      "sed", s"""s,\\(^DEFAULT_MILL_VERSION=\\).*$$,\\1${scala.util.matching.Regex.quoteReplacement(rtMillVersion())},""",
      target.path.toIO.getAbsolutePath()).call(cwd = baseDir)
    os.write.over(millw, res.out.text())
    os.perms.set(millw, os.perms(millw) + java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE)
    target
  }
}
