import chisel3._

class Top extends Module {
  val io = IO(new Bundle {
    val gpio = Output(UInt(32.W))
  })
  val programROM = Module(new ProgramROM)
  val dataBus = Module(new DataBus)
  io.gpio := dataBus.io.gpioOut
  val cpu = Module(new CPU)
  dataBus.io.readMode := cpu.io.dataBusBundle.readMode
  dataBus.io.address := cpu.io.dataBusBundle.address
  dataBus.io.dataIn := cpu.io.dataBusBundle.dataIn
  dataBus.io.maskLevel := cpu.io.dataBusBundle.maskLevel
  cpu.io.dataBusBundle.dataOut := dataBus.io.dataOut
  io.gpio := dataBus.io.gpioOut

  cpu.io.programROMBundle <> programROM.io
  cpu.io.timerInterruptPending := dataBus.io.timerInterruptPending
}

object TopDriver extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new Top, args)
}