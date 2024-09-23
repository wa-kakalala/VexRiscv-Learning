import mill._, scalalib._

val spinalVersion = "1.10.1"

object ivys {
  val sv = "2.11.12"
  val spinalCore = ivy"com.github.spinalhdl::spinalhdl-core:$spinalVersion"
  val spinalLib = ivy"com.github.spinalhdl::spinalhdl-lib:$spinalVersion"
  val spinalPlugin = ivy"com.github.spinalhdl::spinalhdl-idsl-plugin:$spinalVersion"
  val scalatest = ivy"org.scalatest::scalatest:3.2.5"
  val macroParadise = ivy"org.scalamacros:::paradise:2.1.1"
  val yaml = ivy"org.yaml:snakeyaml:1.8"
}

trait Common extends ScalaModule  {
  override def scalaVersion = ivys.sv
  override def scalacPluginIvyDeps = Agg(ivys.macroParadise, ivys.spinalPlugin)
  override def ivyDeps = Agg(ivys.spinalCore, ivys.spinalLib, ivys.yaml, ivys.scalatest)
  override def scalacOptions = Seq("-Xsource:2.11")
}

object VexRiscv extends Common with SbtModule{
  // 重写了两个方法
  override def millSourcePath = os.pwd
  override def moduleDeps: Seq[JavaModule] = super.moduleDeps

  // 定义了一个内部对象test用于测试相关的内容
  object test extends SbtModuleTests with TestModule.ScalaTest
  // SbtMoudleTests这个类或者特质包含用于测试sbt模块的通用功能和测试逻辑
  // TestModule.ScalaTest是一个用于ScalaTest测试框架的特质，ScalaTest是Scala生态中一个广泛使用的测试框架，TestModule.ScalaTest提供了与ScalaTest相关的测试支持
}

/* 含义
1. object关键字
object定义了一个单例对象( signleton object )意味着VexRisc是一个唯一的实例，全局共享且不可实例化。
Scala中的单例对象类似于Java中的静态类成员，但Scala通过object关键字将其单独定义。
特点:
    - 单例对象只有一个实例，不能通过new关键字创建。
    - 可以包含字段、方法和代码块，也可以继承类或者特质(traits)
实例:
    object MySingleton {
    def hello() = println("Hello, Singleton!")
    }

    MySingleton.hello()  // 直接调用单例对象的方法
2. 继承 extends Common
extends用于继承某个类或者实现某个特质(trait)。
特点:
    - 可以复用父类Common中的字段和方法
    - VexRisc可以重写override父类中的方法或者定义新的方法。
extends既可以继承类，也可以实现特质
3. 混入特质 with SbtModule
with 关键字用于混入(mixin)特质，在Scala中一个对象类可以通过extends继承一个类或者特质，但是可以通过with混入额外的特质，这样可以实现多继承的效果。
混入特质意味着VexRisc也拥有SbtModule特质中的字段和方法。
特质(trait)是Scala种类似于接口的概念，可以包含实现代码，也可以通过with和进行组合
这种设计方法非常适合Scala项目中进行模块化开发，允许开发者通过特质(trait)实现多个功能的组合和复用。
*/

