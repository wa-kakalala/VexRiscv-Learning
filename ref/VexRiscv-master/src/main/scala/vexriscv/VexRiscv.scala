package vexriscv

import vexriscv.plugin._
import spinal.core._

import scala.collection.mutable.ArrayBuffer
import scala.collection.Seq

object VexRiscvConfig{
  def apply(withMemoryStage : Boolean, withWriteBackStage : Boolean, plugins : Seq[Plugin[VexRiscv]]): VexRiscvConfig = {
    val config = VexRiscvConfig()
    config.plugins ++= plugins
    config.withMemoryStage = withMemoryStage
    config.withWriteBackStage = withWriteBackStage
    config
  }

  def apply(plugins : Seq[Plugin[VexRiscv]] = ArrayBuffer()) : VexRiscvConfig = apply(true,true,plugins)
}


trait VexRiscvRegressionArg{
  def getVexRiscvRegressionArgs() : Seq[String]
}

case class VexRiscvConfig(){
  var withMemoryStage = true
  var withWriteBackStage = true
  val plugins = ArrayBuffer[Plugin[VexRiscv]]()

  def add(that : Plugin[VexRiscv]) : this.type = {plugins += that;this}
  def find[T](clazz: Class[T]): Option[T] = {
    plugins.find(_.getClass == clazz) match {
      case Some(x) => Some(x.asInstanceOf[T])
      case None => None
    }
  }
  def get[T](clazz: Class[T]): T = {
    plugins.find(_.getClass == clazz) match {
      case Some(x) => x.asInstanceOf[T]
    }
  }

  def withRvc = plugins.find(_.isInstanceOf[IBusFetcher]) match {
    case Some(x) => x.asInstanceOf[IBusFetcher].withRvc
    case None => false
  }

  def withRvf = find(classOf[FpuPlugin]) match {
    case Some(x) => true
    case None => false
  }

  def withRvd = find(classOf[FpuPlugin]) match {
    case Some(x) => x.p.withDouble
    case None => false
  }

  def withSupervisor = find(classOf[CsrPlugin]) match {
    case Some(x) => x.config.supervisorGen
    case None => false
  }

  def FLEN = if(withRvd) 64 else if(withRvf) 32 else 0

  //Default Stageables
  object IS_RVC extends Stageable(Bool)
  object BYPASSABLE_EXECUTE_STAGE   extends Stageable(Bool)
  object BYPASSABLE_MEMORY_STAGE   extends Stageable(Bool)
  object RS1   extends Stageable(Bits(32 bits))
  object RS2   extends Stageable(Bits(32 bits))
  object RS1_USE extends Stageable(Bool)
  object RS2_USE extends Stageable(Bool)
  object RESULT extends Stageable(UInt(32 bits))
  object PC extends Stageable(UInt(32 bits))
  object PC_CALC_WITHOUT_JUMP extends Stageable(UInt(32 bits))
  object INSTRUCTION extends Stageable(Bits(32 bits))
  object INSTRUCTION_ANTICIPATED extends Stageable(Bits(32 bits))
  object LEGAL_INSTRUCTION extends Stageable(Bool)
  object REGFILE_WRITE_VALID extends Stageable(Bool)
  object REGFILE_WRITE_DATA extends Stageable(Bits(32 bits))

  object MPP extends PipelineThing[UInt]
  object DEBUG_BYPASS_CACHE extends PipelineThing[Bool]

  object SRC1   extends Stageable(Bits(32 bits))
  object SRC2   extends Stageable(Bits(32 bits))
  object SRC_ADD_SUB extends Stageable(Bits(32 bits))
  object SRC_ADD extends Stageable(Bits(32 bits))
  object SRC_SUB extends Stageable(Bits(32 bits))
  object SRC_LESS extends Stageable(Bool)
  object SRC_USE_SUB_LESS extends Stageable(Bool)
  object SRC_LESS_UNSIGNED extends Stageable(Bool)
  object SRC_ADD_ZERO extends Stageable(Bool)


  object HAS_SIDE_EFFECT extends Stageable(Bool)

  //Formal verification purposes
  object FORMAL_HALT       extends Stageable(Bool)
  object FORMAL_PC_NEXT    extends Stageable(UInt(32 bits))
  object FORMAL_MEM_ADDR   extends Stageable(UInt(32 bits))
  object FORMAL_MEM_RMASK  extends Stageable(Bits(4 bits))
  object FORMAL_MEM_WMASK  extends Stageable(Bits(4 bits))
  object FORMAL_MEM_RDATA  extends Stageable(Bits(32 bits))
  object FORMAL_MEM_WDATA  extends Stageable(Bits(32 bits))
  object FORMAL_INSTRUCTION extends Stageable(Bits(32 bits))
  object FORMAL_MODE       extends Stageable(Bits(2 bits))


  object Src1CtrlEnum extends SpinalEnum(binarySequential){
    val RS, IMU, PC_INCREMENT, URS1 = newElement()   //IMU, IMZ IMJB
  }

  object Src2CtrlEnum extends SpinalEnum(binarySequential){
    val RS, IMI, IMS, PC = newElement() //TODO remplacing ZERO could avoid 32 muxes if SRC_ADD can be disabled
  }
  object SRC1_CTRL  extends Stageable(Src1CtrlEnum())
  object SRC2_CTRL  extends Stageable(Src2CtrlEnum())

  def getRegressionArgs() : Seq[String] = {
    val str = ArrayBuffer[String]()
    plugins.foreach{
      case e : VexRiscvRegressionArg => str ++= e.getVexRiscvRegressionArgs()
      case _ =>
    }
    str
  }
}

/**
VexRiscv组件，扩展自Component和Pipeline
通过newStage()方法定义了流水线的不同阶段。
- 解码 decode
- 执行 execute
- 内存 memory 条件性创建
- 写回 writeBack 条件性创建
*/
class VexRiscv(val config : VexRiscvConfig) extends Component with Pipeline{
  // 给VexRiscv定义一个别名为T
  type  T = VexRiscv
  // 用于从config 模块导入所有定义的成员，使得在当前代码文件中可以直接使用这些成员而无需前缀。
  // import config._: 下划线_表示导入config中所有的公共成员，包括变量，类，方法等。
  import config._

  //Define stages
  def newStage(): Stage = { val s = new Stage; stages += s; s }  // -> 这是一个方法，new Stage, 然后将new出来的Stage加入到stages中，并返回

  // stages start
  // 使用前面的newStage方法，定义了四个阶段
  val decode    = newStage()
  val execute   = newStage()
  // ifGen是SpinalHDL的一个工具
  val memory    = ifGen(config.withMemoryStage)    (newStage()) // 根据配置条件，是否创建
  val writeBack = ifGen(config.withWriteBackStage) (newStage()) // 根据配置条件，是否创建
  // stages end

  // 定义一个stagesFromExecute方法
  // 返回一个新的集合，包含从执行阶段(exeute)开始的所有流水线阶段
  // stages是一个包含所有流水线阶段的列表
  // dropWhile( _ != execute ) 这个方法会遍历stages列表，直到遇到execute阶段为止，会丢弃所有在execute之前的阶段
  def stagesFromExecute = stages.dropWhile(_ != execute)
  // 这种处理方法，允许在后续逻辑集中在执行阶段以及其后的阶段，便于在设计中实现更加复杂的控制逻辑或者数据流处理


  // 将配置config中定义的插件列表plugins添加到当前组件的插件集合中
  // plugins是当前VexRiscv组件的一个属性，表示该组件将使用的所有插件
  // 插件通常是用于扩展功能或者提供特定的硬件行为
  // config.plugins是在VexRiscvConfig配置中定义的插件集合，配置类通常会包含用于生成不同功能的选项和设置
  // ++= 操作符表示将右侧集合中的元素追加到左侧集合中
  // 通过这个代码，组件可以根据其配置动态加载不同的插件，这使得设计更加灵活和可扩展。
  // 插件可以用于实现各种功能，如调试、监控、流水线的优化等
  plugins ++= config.plugins

  //regression usage --> 在硬件设计和验证中，用于回归测试的特定代码或信号
  //                     这些测试旨在确保新修改没有引入错误，并且系统的功能仍然符合预期
  // 回归测试相关的信号
  // lastStageInstruction
  // lastStagePC
  // lastStageIsValid
  // lastStageIsFiring
  // 用来捕获最后阶段的状态，以便在后续测试和仿真中进行比较和分析
  // 确保每次修改后，通过检查这些信号的值来验证设计的功能是否仍然正确
  // 通过定义特定的监控信号，可以有效的评估设计的稳定性和功能正确性
  val lastStageInstruction = CombInit(stages.last.input(config.INSTRUCTION)).dontSimplifyIt().addAttribute (Verilator.public)
  val lastStagePc = CombInit(stages.last.input(config.PC)).dontSimplifyIt().addAttribute(Verilator.public)
  val lastStageIsValid = CombInit(stages.last.arbitration.isValid).dontSimplifyIt().addAttribute(Verilator.public)
  val lastStageIsFiring = CombInit(stages.last.arbitration.isFiring).dontSimplifyIt().addAttribute(Verilator.public)
/* regression usage 详细解释
  上面的代码定义了四个信号，分别用于捕获最后一个流水线阶段的关键信息，包括指令、程序计数器PC, 有效性标志和触发状态
  1. CombInit是SpinalHDL用于初始化组合逻辑信号的函数，创建一个信号，这个信号的值在每个时钟周期内保持最新
  2. stages.last获取stages列表中最后一个阶段，通常是写回阶段
  3. input(config.INSTRUCTION)访问最后阶段的输入信号，获取当前正在处理的指令
  4. input(config.PC)
  5. arbitation.isValid 检查最后阶段的仲裁状态，判断当前阶段是否有效
  6. arbitation.isFiring 检查最后阶段的仲裁状态，判断当前阶段是否正在执行
  7. dontSimplifyIt()表示在综合时不对该信号进行简化，以确保该信号在仿真和实现中保持其真实的逻辑功能
  8. addAttribute(Verilator.public)将该信号标记为公共的，以便在使用Verilator进行仿真时可以访问这些信号

  - lastStageInstruction: 捕获最后阶段的指令，便于调试和分析。
  - lastStagePc: 捕获最后阶段的 PC，便于跟踪指令流。
  - lastStageIsValid: 指示最后阶段是否有效，帮助监控流水线状态。
  - lastStageIsFiring: 指示最后阶段是否正在执行，提供运行时信息。
*/

  //Verilator perf 标记与Verilator相关的性能优化代码
  // 对decode阶段的仲裁逻辑进行优化，以提高设计性能和资源使用效率
  // decode是流水线中的阶码阶段
  // arbitration: 每个阶段的仲裁逻辑负责决定何时该阶段可以处理数据。仲裁状态通常包含多个信号，如：是否有效，是否正在处理等。
  // removeIt: 这个方法指示工具在生成电路时删除这个仲裁逻辑。它通常用于那些不再需要的或者可以被简化的逻辑，旨在减少设计的复杂性。
  // noBackendCombMerge 这个调用后阻止后端合成工具在综合时将组合逻辑合并。通常，合并组合逻辑可能会导致某些信号的延迟增加，或在某些情况下不符合设计意图。
  // 通过使用能noBackendCombMerge，设计者可以保持特定信号的组合逻辑行为不变
  decode.arbitration.removeIt.noBackendCombMerge
  if(withMemoryStage){
    memory.arbitration.removeIt.noBackendCombMerge
  }
  execute.arbitration.flushNext.noBackendCombMerge
}


