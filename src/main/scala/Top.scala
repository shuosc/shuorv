import chisel3._
import chisel3.util._

class Top extends Module {

  import CommandType._

  val io = IO(new Bundle {
    val gpio = Output(UInt(32.W))
  })

  val pc = RegInit(0.U(32.W))
  pc := pc + 4.U

  val programROM = Module(new ProgramROM)
  programROM.io.address := pc

  val instruction = Wire(UInt(32.W))
  instruction := programROM.io.value

  val regFile = Module(new RegFile)
  regFile.io.addressInput := instruction(11, 7)
  regFile.io.addressA := instruction(19, 15)
  regFile.io.addressB := instruction(24, 20)
  regFile.io.input := 0xdead.U
  regFile.io.writeEnable := false.B

  val addressSpace = Module(new DataBus)
  addressSpace.io.read_mode := true.B
  addressSpace.io.maskLevel := Mask.WORD
  addressSpace.io.addr := 0.U
  addressSpace.io.dataIn := 0.U
  io.gpio := addressSpace.io.gpioOut

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
    addressSpace.io.addr := (regFile.io.outputA.asSInt() + immGen.io.result).asUInt()
    addressSpace.io.maskLevel := instruction(13, 12)
    // second part of load
    switch(instruction(14, 12)) {
      is("b000".U) {
        regFile.io.input := addressSpace.io.dataOut(7, 0)
      }
      is("b001".U) {
        regFile.io.input := addressSpace.io.dataOut(15, 0)
      }
      is("b010".U) {
        regFile.io.input := addressSpace.io.dataOut
      }
      is("b100".U) {
        regFile.io.input := addressSpace.io.dataOut
      }
      is("b101".U) {
        regFile.io.input := addressSpace.io.dataOut
      }
    }
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
        addressSpace.io.addr := (regFile.io.outputA.asSInt() + immGen.io.result).asUInt()
        addressSpace.io.maskLevel := instruction(13, 12)
        // stall once for waiting for the load result come out
        pc := pc
        stall := true.B
      }
      is(STORE) {
        addressSpace.io.addr := (regFile.io.outputA.asSInt() + immGen.io.result).asUInt()
        addressSpace.io.read_mode := false.B
        addressSpace.io.maskLevel := instruction(13, 12)
        addressSpace.io.dataIn := regFile.io.outputB
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
      is(ENV) {
        // todo: we'll need this after implement interrupt
      }
    }
  }
}
