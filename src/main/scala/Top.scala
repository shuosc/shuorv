import chisel3._
import chisel3.stage.ChiselGeneratorAnnotation

class Top extends Module {
  val io = IO(new Bundle {
    val A = Input(UInt(32.W))
    val B = Input(UInt(32.W))
    val op = Input(UInt(32.W))
    val result = Output(UInt(32.W))
  })
  val alu = Module(new ALU)
  alu.io.A := io.A
  alu.io.B := io.B
  alu.io.op := io.op
  io.result := alu.io.result
}

object Top extends App {
  val stage = new chisel3.stage.ChiselStage
  stage.execute(
    Array[String](),
    Seq(ChiselGeneratorAnnotation(() => new Top()))
  )
}
