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
  val regs = Reg(Vec(31, UInt(32.W)))
  if (io.addressA == 0.U) {
    io.outputA := 0.U
  } else {
    io.outputA := regs(io.addressA - 1.U)
  }
  if (io.addressB == 0.U) {
    io.outputB := 0.U
  } else {
    io.outputB := regs(io.addressB - 1.U)
  }
  when(io.writeEnable & io.addressInput.orR()) {
    regs(io.addressInput - 1.U) := io.input
  }
}
