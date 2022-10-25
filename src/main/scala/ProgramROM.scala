import chisel3._

class ProgramROMBundle extends Bundle {
  val address = Input(UInt(32.W))
  val value = Output(UInt(32.W))
}

// A temporary program ROM
// Just for testing, use a real flash rom in real world
class ProgramROM extends Module {
  val io = IO(new ProgramROMBundle)
  
  val codeContent = VecInit(Array(
    "h10012537".U(32.W),
    "h00100593".U(32.W),
    "h00b52023".U(32.W)
  ))
  
  when(io.address < "h8000000c".U) {
    io.value := codeContent((io.address - "h80000000".U) (31, 2))
  }.otherwise {
    io.value := 0xdead.U
  }
}