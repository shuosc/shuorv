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
    "h00004fb7".U(32.W), // li t6, 0x4000
    "h000f8f93".U(32.W),
    "h01200513".U(32.W), // li a0, 0x12
    "h00afa023".U(32.W), // sw a0, 0(t6)
    "h00000513".U(32.W), // li a0, 0x0
    "h00afa223".U(32.W), // sw a0, 4(t6)

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
    "h01de2023".U(32.W), // sw t4, 0(t3)
    "h00138393".U(32.W), // addi t2, t2, 1
    "hfe1ff06f".U(32.W), // j label
  ))

  val interruptContent = VecInit(Array(
    "h000fa503".U(32.W), // lw a0, 0(t6)
    "h00a50533".U(32.W), // add a0, a0, a0
    "h00afa023".U(32.W), // sw a0, 0(t6)
    "h100125b7".U(32.W), // li a1, 0x10012000
    "h00058593".U(32.W),
    "h00a5a023".U(32.W), // sw a0, 0(a1)
    "h30200073".U(32.W), // mret
  ))

  when(io.address < "h80010000".U) {
    io.value := content((io.address - "h80000000".U) (31, 2))
  }.otherwise {
    io.value := interruptContent((io.address - "h80010000".U) (31, 2))
  }
}
