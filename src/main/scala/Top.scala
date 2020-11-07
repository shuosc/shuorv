import chisel3._

class Top extends Module {
  val io = IO(new Bundle {
    val gpio = Output(UInt(32.W))
  })
  val programROM = Module(new ProgramROM)
  val dataBus = Module(new DataBus)
  io.gpio := dataBus.io.gpioOut
  val cpu = Module(new CPU)
  dataBus.io.read_mode := cpu.io.dataBusBundle.read_mode
  dataBus.io.addr := cpu.io.dataBusBundle.addr
  dataBus.io.dataIn := cpu.io.dataBusBundle.dataIn
  dataBus.io.maskLevel := cpu.io.dataBusBundle.maskLevel
  cpu.io.dataBusBundle.dataOut := dataBus.io.dataOut
  io.gpio := dataBus.io.gpioOut

  cpu.io.programROMBundle <> programROM.io
  cpu.io.timerInterruptPending := dataBus.io.timerInterruptPending
}
