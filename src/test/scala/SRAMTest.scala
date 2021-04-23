import chisel3._
import chisel3.iotesters._
import org.scalatest.{FlatSpec, Matchers}

// Though the SRAM implementation is from chisel's example,
// we still have a test for it, for demonstrating the function of this example SRAM
class SRAMTest(sram: SRAM) extends PeekPokeTester(sram) {
  val cases = Array(
    (false.B, 0.U(10.W), Vector(true.B, true.B, true.B, true.B), "hd8c7b6a5".U(32.W), "0"),
    (false.B, 4.U(10.W), Vector(true.B, true.B, false.B, false.B), "h0000b6a5".U(32.W), "0"),
    (false.B, 4.U(10.W), Vector(false.B, false.B, true.B, false.B), "h00c70000".U(32.W), "0"),
    (false.B, 4.U(10.W), Vector(false.B, false.B, false.B, true.B), "hd8000000".U(32.W), "0"),

    (true.B, 0.U(10.W), Vector(false.B, false.B, false.B, false.B), "h00000000".U(32.W), "hd8c7b6a5"),
    (true.B, 4.U(10.W), Vector(false.B, false.B, false.B, false.B), "h00000000".U(32.W), "hd8c7b6a5"),

    (false.B, 5.U(10.W), Vector(true.B, false.B, false.B, false.B), "h000000e9".U(32.W), "0"),
    (true.B, 4.U(10.W), Vector(false.B, false.B, false.B, false.B), "h00000000".U(32.W), "hd8c7b6a5"),
    (true.B, 5.U(10.W), Vector(false.B, false.B, false.B, false.B), "h00000000".U(32.W), "h000000e9"),
    
    (false.B, "h0FB8".U(12.W), Vector(true.B, true.B, true.B, true.B), "h01010101".U(32.W), "0"),
    (true.B, "h0FB8".U(12.W), Vector(false.B, false.B, false.B, false.B), "h00000000".U(32.W), "h01010101")
    )
  for ((enable, address, mask, dataIn, expectedDataOut) <- cases) {
    poke(sram.io.enable, enable)
    poke(sram.io.addr, address)
    poke(sram.io.mask(0), mask(0))
    poke(sram.io.mask(1), mask(1))
    poke(sram.io.mask(2), mask(2))
    poke(sram.io.mask(3), mask(3))
    poke(sram.io.dataIn(0), dataIn(7, 0))
    poke(sram.io.dataIn(1), dataIn(15, 8))
    poke(sram.io.dataIn(2), dataIn(23, 16))
    poke(sram.io.dataIn(3), dataIn(31, 24))

    step(1)

    if (expectedDataOut != "0") {
      expect(sram.io.dataOut(0), expectedDataOut.U(7, 0))
      expect(sram.io.dataOut(1), expectedDataOut.U(15, 8))
      expect(sram.io.dataOut(2), expectedDataOut.U(23, 16))
      expect(sram.io.dataOut(3), expectedDataOut.U(31, 24))
    }
  }
}

class SRAMSpec extends FlatSpec with Matchers {
  behavior of "SRAMSpec"

  it should "read and write successfully" in {
    chisel3.iotesters.Driver.execute(Array("--generate-vcd-output", "on"), () => new SRAM) { sram =>
      new SRAMTest(sram)
    } should be(true)
  }
}

class ByteAddressedSRAMTest(sram: ByteAddressedSRAM) extends PeekPokeTester(sram) {

  import Mask._

  val cases = Array(
    (false.B, 0.U(12.W), WORD, "hd8c7b6a5".U(32.W), "0"),
    (true.B, 0.U(12.W), WORD, "h00000000".U(32.W), "hd8c7b6a5"),
    (true.B, 2.U(12.W), HALF_WORD, "h00000000".U(32.W), "h0000d8c7"),
    (true.B, 3.U(12.W), BYTE, "h00000000".U(32.W), "h000000d8"))
  for ((enable_read, address, maskLevel, dataIn, expectedDataOut) <- cases) {
    poke(sram.io.readMode, enable_read)
    poke(sram.io.address, address)
    poke(sram.io.maskLevel, maskLevel)
    poke(sram.io.dataIn, dataIn)

    step(1)

    if (expectedDataOut != "0") {
      expect(sram.io.dataOut, expectedDataOut.U)
    }
  }
}

class ByteAddressedSRAMSpec extends FlatSpec with Matchers {
  behavior of "ByteAddressedSRAMSpec"

  it should "read and write successfully" in {
    chisel3.iotesters.Driver.execute(Array("--generate-vcd-output", "on"), () => new ByteAddressedSRAM) { sram =>
      new ByteAddressedSRAMTest(sram)
    } should be(true)
  }
}

