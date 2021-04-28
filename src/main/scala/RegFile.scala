import chisel3._

class RegFile extends Module {
  val io = IO(new Bundle {
    val addressA = Input(Bits(5.W))
    val outputA = Output(Bits(32.W))

    val addressB = Input(Bits(5.W))
    val outputB = Output(Bits(32.W))

    val addressInput = Input(Bits(5.W))
    val input = Input(Bits(32.W))

    val writeEnable = Input(Bool())
  })
  val regs = Reg(Vec(32, UInt(32.W)))
  io.outputA := regs(io.addressA)
  io.outputB := regs(io.addressB)
  when(io.writeEnable & io.addressInput.orR()) {
    regs(io.addressInput) := io.input
  }
}
