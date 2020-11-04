import chisel3._
import chisel3.util._

class CPUBundle extends Bundle {
  val programROMBundle = Flipped(new ProgramROMBundle)
  val dataBusBundle = Flipped(new DataBusBundle)
  val timerInterruptPending = Input(Bool())
}

class CPU extends Module {

  import CommandType._

  val io = IO(new CPUBundle)

  val pc = RegInit("h80000000".U(32.W))
  pc := pc + 4.U
  io.programROMBundle.address := pc

  val instruction = Wire(UInt(32.W))
  instruction := io.programROMBundle.value

  io.dataBusBundle.read_mode := true.B
  io.dataBusBundle.maskLevel := Mask.WORD
  io.dataBusBundle.addr := 0.U
  io.dataBusBundle.dataIn := 0.U

  val csr = Module(new CSR)
  csr.io.write_en := false.B
  csr.io.flipStatusMIE := false.B
  csr.io.address := 0.U
  csr.io.input_value := 0.U
  csr.io.timerInterruptPending := io.timerInterruptPending

  val regFile = Module(new RegFile)
  regFile.io.addressInput := instruction(11, 7)
  regFile.io.addressA := instruction(19, 15)
  regFile.io.addressB := instruction(24, 20)
  regFile.io.input := 0xdead.U
  regFile.io.writeEnable := false.B

  val immGen = Module(new Imm)
  immGen.io.instruction := instruction

  val branchCondition = Module(new BranchCondition)
  branchCondition.io.A := regFile.io.outputA
  branchCondition.io.B := regFile.io.outputB
  branchCondition.io.op := instruction(14, 12)

  val alu = Module(new ALU)
  alu.io.A := 0xdead.U
  alu.io.B := 0xdead.U
  alu.io.op := 0xdead.U

  val stall = RegInit(false.B)

  when(stall) {
    stall := false.B
    regFile.io.writeEnable := true.B
    io.dataBusBundle.addr := (regFile.io.outputA.asSInt() + immGen.io.result).asUInt()
    io.dataBusBundle.maskLevel := instruction(13, 12)
    // second part of load
    switch(instruction(14, 12)) {
      is("b000".U) {
        regFile.io.input := io.dataBusBundle.dataOut(7, 0)
      }
      is("b001".U) {
        regFile.io.input := io.dataBusBundle.dataOut(15, 0)
      }
      is("b010".U) {
        regFile.io.input := io.dataBusBundle.dataOut
      }
      is("b100".U) {
        regFile.io.input := io.dataBusBundle.dataOut
      }
      is("b101".U) {
        regFile.io.input := io.dataBusBundle.dataOut
      }
    }
  }.elsewhen(csr.io.interruptPending) {
    csr.io.write_en := true.B
    csr.io.flipStatusMIE := true.B
    csr.io.address := CSRAddress.mepc
    csr.io.input_value := pc + 4.U
    pc := csr.io.pcOnInterrupt
  }.otherwise {
    switch(instruction(6, 2)) {
      is(LUI) {
        regFile.io.input := immGen.io.result.asUInt()
        regFile.io.writeEnable := true.B
      }
      is(AUIPC) {
        alu.io.A := immGen.io.result.asUInt()
        alu.io.B := pc
        alu.io.op := ALUOperation.ADD

        regFile.io.input := alu.io.result
        regFile.io.writeEnable := true.B
      }
      is(JAL) {
        alu.io.A := 4.U
        alu.io.B := pc
        alu.io.op := ALUOperation.ADD

        regFile.io.input := alu.io.result
        regFile.io.writeEnable := true.B
        pc := (pc.asSInt() + immGen.io.result).asUInt()
      }
      is(JALR) {
        alu.io.A := regFile.io.outputA
        alu.io.B := immGen.io.result.asUInt()
        alu.io.op := ALUOperation.ADD

        regFile.io.input := pc + 4.U
        regFile.io.writeEnable := true.B
        pc := alu.io.result & "hfffffffe".U
      }
      is(BRANCH) {
        when(branchCondition.io.take) {
          pc := (pc.asSInt() + immGen.io.result).asUInt()
        }
      }
      is(LOAD) {
        // first part of load: send a load command to addressSpace
        io.dataBusBundle.addr := (regFile.io.outputA.asSInt() + immGen.io.result).asUInt()
        io.dataBusBundle.maskLevel := instruction(13, 12)
        // stall once for waiting for the load result come out
        pc := pc
        stall := true.B
      }
      is(STORE) {
        io.dataBusBundle.addr := (regFile.io.outputA.asSInt() + immGen.io.result).asUInt()
        io.dataBusBundle.read_mode := false.B
        io.dataBusBundle.maskLevel := instruction(13, 12)
        io.dataBusBundle.dataIn := regFile.io.outputB
      }
      is(CALCULATE_IMM) {
        regFile.io.writeEnable := true.B
        alu.io.A := regFile.io.outputA
        alu.io.B := immGen.io.result.asUInt()
        alu.io.op := Cat(0.U(1), instruction(14, 12))
        regFile.io.input := alu.io.result
      }
      is(CALCULATE_REG) {
        regFile.io.writeEnable := true.B
        alu.io.A := regFile.io.outputA
        alu.io.B := regFile.io.outputB
        alu.io.op := Cat(0.U(1), instruction(14, 12))
        regFile.io.input := alu.io.result
      }
      is(FENCE) {
        // we don't have multicore, pipeline, etc. now, so we don't need this command
      }
      is(SYSTEM) {
        switch(instruction(31, 20)) {
          is(SystemCommand.MRET) {
            csr.io.flipStatusMIE := true.B
            csr.io.address := CSRAddress.mepc
            pc := csr.io.output_value
          }
        }
      }
    }
  }
}

object CommandType extends Enumeration {
  val LUI = "b01101".U
  val AUIPC = "b00101".U
  val JAL = "b11011".U
  val JALR = "b11001".U
  val BRANCH = "b11000".U
  val LOAD = "b00000".U
  val STORE = "b01000".U
  val CALCULATE_IMM = "b00100".U
  val CALCULATE_REG = "b01100".U
  val FENCE = "b00011".U
  val SYSTEM = "b11100".U
}

object SystemCommand extends Enumeration {
  val MRET = "h302".U
  val ECALL = "b000".U
  val EBREAK = "b001".U
}
