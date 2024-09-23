// 定义SpinalHDL版本
val spinalVersion = "1.10.2a"

// 使用懒加载定义项目配置，root代表主项目，只有在root被sbt或者其他代码访问时，才会执行它的配置逻辑。
// 定义项目的根目录为当前目录， file(".")表示项目位于当前目录。
lazy val root = (project in file(".")).
  // 设置项目的基本属性，包括组织名、Scala版本和项目版本
  // settings用于为当前项目定义配置信息
  settings(
    // inThisBuild( List(...) )这是一种批量配置方式，设置属性如: organization, scalaVersion和version适用于整个项目
    inThisBuild(List(
      // 设置项目的组织名称，用于依赖管理
      organization := "com.github.spinalhdl",
      // 设置Scala版本，确保使用特定版本的Scala进行编译
      scalaVersion := "2.12.18",
      // 设置项目的版本号
      version      := "2.0.0"
    )),

    // 添加项目所依赖的库，库依赖定义包含库名称、版本号和组织名
    // 定义项目的依赖库，++= 操作符表示在现有依赖列表中添加新的依赖。
    libraryDependencies ++= Seq(
      // 引入SpinalHDL核心库依赖
      "com.github.spinalhdl" %% "spinalhdl-core" % spinalVersion, // com.github.spinalhal依赖的组织名，%%用于自动添加Scala版本号，生成适合该Scala版本的依赖库，spinalhdl-core依赖库的名称，%指定依赖版本
      // 引入SpinalHDL辅助库
      "com.github.spinalhdl" %% "spinalhdl-lib" % spinalVersion,
      // 添加SpinalHDL的IDSL编译器插件
      // compilerPlugin用于指定Scala编译器插件，spinalhdl-idsl-plugin是SpinalHDL特定的IDSL插件
      compilerPlugin("com.github.spinalhdl" %% "spinalhdl-idsl-plugin" % spinalVersion),
      // 添加单元测试库Scalatest，用于编写和运行测试
      "org.scalatest" %% "scalatest" % "3.2.17",
      // 添加yaml解析库，用于解析yaml格式的配置文件
      "org.yaml" % "snakeyaml" % "1.8"
    ),

    // 定义项目名称，设置为VexRiscv
    name := "VexRiscv"
  )

// 启动独立JVM进程运行程序，避免与sbt进程共享JVM
fork := true

// 使用SpinalHDL和Scalatest进行硬件描述和测试。
// 通过fork设置确保了项目在独立的JVM中运行，提高了稳定性。

/* 配置项的结构 --> Scala的链式调用
(project in file(".")).settings (
    inThisBuild ( // 用来设置项目的全局属性，这些配置项会对整个项目中的所有模块生效。
        List(
        
        )
    ),
    libraryDependencies ++= Seq(
    
    ),
    name := "xxx"  // 项目名称，标识项目
)
*/







/* %% 和 %的说明
%%和%都是用于定义依赖项的符号，在Scala构建工具sbt中有着特定的作用，帮助处理库的Scala版本兼容性。
不是Scala语言语法，是sbt特有的语法 --> 用于处理依赖库的版本管理
1. %% 的作用
使用 %% 时，sbt 会自动根据项目中指定的 scalaVersion 来查找与该 Scala 版本对应的依赖库包。
"com.github.spinalhdl" %% "spinalhdl-core" % spinalVersion
如果项目使用 Scala 2.12.18，那么 sbt 会尝试下载 spinalhdl-core_2.12 这个库。
%%：自动拼接 Scala 版本号，确保获取与当前 Scala 版本兼容的库。
2. % 的作用
单百分号 % 表示依赖项没有 Scala 版本的自动拼接功能。
使用 % 时，依赖库名是固定的，不包含 Scala 版本信息。这通常用于那些与 Scala 版本无关的库，比如 Java 库。--> 手动指定版本
"org.yaml" % "snakeyaml" % "1.8"
%：不拼接 Scala 版本号，通常用于与 Scala 版本无关的库。--> 手动指定
*/

/* Scala相关内容记录
1. val 和 lazy val 
 - val 定义不可变的变量，即常量。
 - lazy val 懒加载定义，只有在第一次访问这个变量时才会初始化，有助于提升性能，避免不必要的计算。
            lazy val延迟了变量初始化，直到它第一次被使用时才真正进行计算或者赋值，如果定义了一个lazy val, 但是在程序中从未访问过它，那么它的初始化代码就不会执行。
*/
