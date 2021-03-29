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
//<_start>:
"h00021197".U(32.W),    //auipc x3,0x21
"h80018193".U(32.W),    //addi  x3,x3,-2048 # //<__global_pointer$>
"h00021117".U(32.W),    //auipc x2,0x21
"hff810113".U(32.W),    //addi  x2,x2,-8 # //<__freertos_irq_stack_top>
"h00000517".U(32.W),    //auipc x10,0x0
"h20450513".U(32.W),    //addi  x10,x10,516 # //<__data_lma>
"h00020597".U(32.W),    //auipc x11,0x20
"hfe858593".U(32.W),    //addi  x11,x11,-24 # //<__bss_start>
"h00020617".U(32.W),    //auipc x12,0x20
"hfe060613".U(32.W),    //addi  x12,x12,-32 # //<__bss_start>
"h00c5fc63".U(32.W),    //bgeu  x11,x12,//<clear_bss_section>
//<load_store_loop>:
"h00052283".U(32.W),    //lw    x5,0(x10)
"h0055a023".U(32.W),    //sw    x5,0(x11)
"h00450513".U(32.W),    //addi  x10,x10,4
"h00458593".U(32.W),    //addi  x11,x11,4
"hfec5e8e3".U(32.W),    //bltu  x11,x12,//<load_store_loop>
//<clear_bss_section>:
"h00020517".U(32.W),    //auipc x10,0x20
"hfc050513".U(32.W),    //addi  x10,x10,-64 # //<__bss_start>
"h00020597".U(32.W),    //auipc x11,0x20
"hfb858593".U(32.W),    //addi  x11,x11,-72 # //<__bss_start>
"h00b57863".U(32.W),    //bgeu  x10,x11,//<done>
//<store_zero_loop>:
"h00052023".U(32.W),    //sw    x0,0(x10)
"h00450513".U(32.W),    //addi  x10,x10,4
"hfeb56ce3".U(32.W),    //bltu  x10,x11,//<store_zero_loop>
//<done>:
"h180000ef".U(32.W),    //jal   x1,//<_init>
"h130000ef".U(32.W),    //jal   x1,//<main>
//<loop>:
"h0000006f".U(32.W),    //jal   x0,//<loop>
//<trap_entry>:
"hf8010113".U(32.W),    //addi  x2,x2,-128
"h00112223".U(32.W),    //sw    x1,4(x2)
"h00212423".U(32.W),    //sw    x2,8(x2)
"h00312623".U(32.W),    //sw    x3,12(x2)
"h00412823".U(32.W),    //sw    x4,16(x2)
"h00512a23".U(32.W),    //sw    x5,20(x2)
"h00612c23".U(32.W),    //sw    x6,24(x2)
"h00712e23".U(32.W),    //sw    x7,28(x2)
"h02812023".U(32.W),    //sw    x8,32(x2)
"h02912223".U(32.W),    //sw    x9,36(x2)
"h02a12423".U(32.W),    //sw    x10,40(x2)
"h02b12623".U(32.W),    //sw    x11,44(x2)
"h02c12823".U(32.W),    //sw    x12,48(x2)
"h02d12a23".U(32.W),    //sw    x13,52(x2)
"h02e12c23".U(32.W),    //sw    x14,56(x2)
"h02f12e23".U(32.W),    //sw    x15,60(x2)
"h05012023".U(32.W),    //sw    x16,64(x2)
"h05112223".U(32.W),    //sw    x17,68(x2)
"h05212423".U(32.W),    //sw    x18,72(x2)
"h05312623".U(32.W),    //sw    x19,76(x2)
"h05412823".U(32.W),    //sw    x20,80(x2)
"h05512a23".U(32.W),    //sw    x21,84(x2)
"h05612c23".U(32.W),    //sw    x22,88(x2)
"h05712e23".U(32.W),    //sw    x23,92(x2)
"h07812023".U(32.W),    //sw    x24,96(x2)
"h07912223".U(32.W),    //sw    x25,100(x2)
"h07a12423".U(32.W),    //sw    x26,104(x2)
"h07b12623".U(32.W),    //sw    x27,108(x2)
"h07c12823".U(32.W),    //sw    x28,112(x2)
"h07d12a23".U(32.W),    //sw    x29,116(x2)
"h07e12c23".U(32.W),    //sw    x30,120(x2)
"h07f12e23".U(32.W),    //sw    x31,124(x2)
"h34202573".U(32.W),    //csrrs x10,mcause,x0
"h341025f3".U(32.W),    //csrrs x11,mepc,x0
//<test_if_asynchronous>:
"h01f55613".U(32.W),    //srli  x12,x10,0x1f
"h00060663".U(32.W),    //beq   x12,x0,//<handle_synchronous>
"h094000ef".U(32.W),    //jal   x1,//<trap_handler>
"h00c0006f".U(32.W),    //jal   x0,//<asynchronous_return>
//<handle_synchronous>:
"h00458593".U(32.W),    //addi  x11,x11,4
"h34159073".U(32.W),    //csrrw x0,mepc,x11
//<asynchronous_return>:
"h00412083".U(32.W),    //lw    x1,4(x2)
"h00812103".U(32.W),    //lw    x2,8(x2)
"h00c12183".U(32.W),    //lw    x3,12(x2)
"h01012203".U(32.W),    //lw    x4,16(x2)
"h01412283".U(32.W),    //lw    x5,20(x2)
"h01812303".U(32.W),    //lw    x6,24(x2)
"h01c12383".U(32.W),    //lw    x7,28(x2)
"h02012403".U(32.W),    //lw    x8,32(x2)
"h02412483".U(32.W),    //lw    x9,36(x2)
"h02812503".U(32.W),    //lw    x10,40(x2)
"h02c12583".U(32.W),    //lw    x11,44(x2)
"h03012603".U(32.W),    //lw    x12,48(x2)
"h03412683".U(32.W),    //lw    x13,52(x2)
"h03812703".U(32.W),    //lw    x14,56(x2)
"h03c12783".U(32.W),    //lw    x15,60(x2)
"h04012803".U(32.W),    //lw    x16,64(x2)
"h04412883".U(32.W),    //lw    x17,68(x2)
"h04812903".U(32.W),    //lw    x18,72(x2)
"h04c12983".U(32.W),    //lw    x19,76(x2)
"h05012a03".U(32.W),    //lw    x20,80(x2)
"h05412a83".U(32.W),    //lw    x21,84(x2)
"h05812b03".U(32.W),    //lw    x22,88(x2)
"h05c12b83".U(32.W),    //lw    x23,92(x2)
"h06012c03".U(32.W),    //lw    x24,96(x2)
"h06412c83".U(32.W),    //lw    x25,100(x2)
"h06812d03".U(32.W),    //lw    x26,104(x2)
"h06c12d83".U(32.W),    //lw    x27,108(x2)
"h07012e03".U(32.W),    //lw    x28,112(x2)
"h07412e83".U(32.W),    //lw    x29,116(x2)
"h07812f03".U(32.W),    //lw    x30,120(x2)
"h07c12f83".U(32.W),    //lw    x31,124(x2)
"h08010113".U(32.W),    //addi  x2,x2,128
"h30200073".U(32.W),    //mret
//<trap_handler>:
"h0000006f".U(32.W),    //jal   x0,//<trap_handler>
//<main>:
"hfe010113".U(32.W),    //addi  x2,x2,-32
"h00812e23".U(32.W),    //sw    x8,28(x2)
"h02010413".U(32.W),    //addi  x8,x2,32
"hfe042623".U(32.W),    //sw    x0,-20(x8)
"hfe042423".U(32.W),    //sw    x0,-24(x8)
"h100127b7".U(32.W),    //lui   x15,0x10012
"hfe842703".U(32.W),    //lw    x14,-24(x8)
"h00e7a023".U(32.W),    //sw    x14,0(x15) # //<__stack_size+0x10010000>
"hfec42783".U(32.W),    //lw    x15,-20(x8)
"h00178793".U(32.W),    //addi  x15,x15,1
"hfef42623".U(32.W),    //sw    x15,-20(x8)
"hfec42703".U(32.W),    //lw    x14,-20(x8)
"h00a00793".U(32.W),    //addi  x15,x0,10
"hfee7d0e3".U(32.W),    //bge   x15,x14,//<main+0x14>
"hfe042623".U(32.W),    //sw    x0,-20(x8)
"hfe842783".U(32.W),    //lw    x15,-24(x8)
"h00178793".U(32.W),    //addi  x15,x15,1
"hfef42423".U(32.W),    //sw    x15,-24(x8)
"hfcdff06f".U(32.W),    //jal   x0,//<main+0x14>
//<_init>:
"hff010113".U(32.W),    //addi  x2,x2,-16
"h00812623".U(32.W),    //sw    x8,12(x2)
"h01010413".U(32.W),    //addi  x8,x2,16
"h800007b7".U(32.W),    //lui   x15,0x80000
"h06c78793".U(32.W),    //addi  x15,x15,108 # //<__freertos_irq_stack_top+0xfffdf06c>
"h30579073".U(32.W),    //csrrw x0,mtvec,x15
"h000027b7".U(32.W),    //lui   x15,0x2
"h88878793".U(32.W),    //addi  x15,x15,-1912 # 1888 <__stack_size-0x778>
"h30079073".U(32.W),    //csrrw x0,mstatus,x15
"h00000013".U(32.W),    //addi  x0,x0,0
"h00c12403".U(32.W),    //lw    x8,12(x2)
"h01010113".U(32.W),    //addi  x2,x2,16
"h00008067".U(32.W)    //jalr  x0,0(x1)
//<__freertos_irq_stack_top-0x2000>:
//<.comment>:
    )
    )

  val interruptContent = VecInit(Array(
    "h000fa503".U(32.W), // lw a0, 0(t6)
    "h00a50533".U(32.W), // add a0, a0, a0
    "h00afa023".U(32.W), // sw a0, 0(t6)
    "h100125b7".U(32.W), // li a1, 0x10012000
    "h00058593".U(32.W),
    "h00a5a023".U(32.W), // sw a0, 0(a1)
    // mret
    "h30200073".U(32.W)))

  when(io.address < "h80010000".U) {
    io.value := content((io.address - "h80000000".U) (31, 2))
  }.otherwise {
    io.value := interruptContent((io.address - "h80010000".U) (31, 2))
  }
}
