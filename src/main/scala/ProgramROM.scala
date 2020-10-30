import chisel3._

// A temporary program ROM
// Just for testing, use a real flash rom in real world
class ProgramROM extends Module {
  val io = IO(new Bundle {
    val address = Input(UInt(32.W))
    val value = Output(UInt(32.W))
  })
  val content = VecInit(Array(
    "h00100293".U(32.W), // li t0, 1
    "h00200313".U(32.W), // li t1, 2
    "h006303b3".U(32.W), // add t2, t1, t1
    "h005383b3".U(32.W), // add t2, t2, t0
    "h40000e37".U(32.W), // li t3, 0x40000000
    "h000e0e13".U(32.W),
    "h007e2023".U(32.W), // sw t2, 0(t3)
    // label
    "h00702223".U(32.W), // sw t2, 4(zero)
    "h00402e03".U(32.W), // lw t3, 4(zero)
    "h01c38eb3".U(32.W), // add t4, t2, t3
    "h007eeeb3".U(32.W), // or t4, t4, t2
    "h40000e37".U(32.W), // li t3, 0x40000000
    "h000e0e13".U(32.W),
    "h01de2023".U(32.W), // sw t4, 0(t3)
    "h00138393".U(32.W), // addi t2, t2, 1
    "hfe1ff06f".U(32.W), // j label
  ))

  io.value := content(io.address(31, 2))
}
