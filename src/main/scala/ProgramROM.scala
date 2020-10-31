import chisel3._

class ProgramROMBundle extends Bundle {
  val address = Input(UInt(32.W))
  val value = Output(UInt(32.W))
}

// A temporary program ROM
// Just for testing, use a real flash rom in real world
class ProgramROM extends Module {
  val io = IO(new ProgramROMBundle)
  val content = VecInit(Array(
    "h80020f37".U(32.W), // li t5, 0x80020000
    "h00100293".U(32.W), // li t0, 1
    "h00200313".U(32.W), // li t1, 2
    "h006303b3".U(32.W), // add t2, t1, t1
    "h005383b3".U(32.W), // add t2, t2, t0
    "h10012e37".U(32.W), // li t3, 0x10012000
    "h000e0e13".U(32.W),
    "h007e2023".U(32.W), // sw t2, 0(t3)
    // label
    "h007f2223".U(32.W), // sw t2, 4(t5)
    "h004f2e03".U(32.W), // lw t3, 4(t5)
    "h01c38eb3".U(32.W), // add t4, t2, t3
    "h007eeeb3".U(32.W), // or t4, t4, t2
    "h10012e37".U(32.W), // li t3, 0x10012000
    "h000e0e13".U(32.W),
    "h01de2023".U(32.W), // sw t2, 0(t3)
    "h00138393".U(32.W), // addi t2, t2, 1
    "hfe1ff06f".U(32.W), // j label
  ))

  io.value := content((io.address - "h80000000".U) (31, 2))
}
