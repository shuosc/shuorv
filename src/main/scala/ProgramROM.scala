import chisel3._

class ProgramROMBundle extends Bundle {
  val address = Input(UInt(32.W))
  val value = Output(UInt(32.W))
}

// A temporary program ROM
// Just for testing, use a real flash rom in real world
class ProgramROM extends Module {
  val io = IO(new ProgramROMBundle)
  // todo
  val content = VecInit(Array("h00000013".U(32.W)))

  val interruptContent = VecInit(Array("h00000013".U(32.W)))

  when(io.address < "h80010000".U) {
    io.value := content((io.address - "h80000000".U) (31, 2))
  }.otherwise {
    io.value := interruptContent((io.address - "h80010000".U) (31, 2))
  }
}
