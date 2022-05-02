import chisel3._
import chisel3.iotesters._
import org.scalatest.{FlatSpec, Matchers}
import chisel3.util._

class SerialRecieveControllerTest(serialRecieveController: SerialRecieveController) extends PeekPokeTester(serialRecieveController) {
    poke(serialRecieveController.io.rxWire, true)
    poke(serialRecieveController.io.readMode, true)
    poke(serialRecieveController.io.address, Registers.STATUS)
    poke(serialRecieveController.io.maskLevel, Mask.NONE)
    step(1)

    def send(data: Int): Unit = {
      poke(serialRecieveController.io.rxWire, false)
      step(38400 / 9600)

      for (bitIndex <- 0 to 7) {
        poke(serialRecieveController.io.rxWire, (data & (1 << bitIndex)) != 0)
        step(38400 / 9600)
      }
      poke(serialRecieveController.io.rxWire, true)
      step(38400 / 9600)
    }

    send(0x55)
    send(0xAA)
    send(0xF0)
    send(0x0F)

    poke(serialRecieveController.io.readMode, true)
    poke(serialRecieveController.io.address, Registers.DATA)
    poke(serialRecieveController.io.maskLevel, Mask.WORD)
    step(1)
    expect(serialRecieveController.io.dataOut, 0x55.U)
    step(1)
    poke(serialRecieveController.io.address, Registers.STATUS)
    poke(serialRecieveController.io.maskLevel, Mask.WORD)
    step(1)
    expect(serialRecieveController.io.dataOut, "b00000000_00000000_000000_0000_01_00".U)
    poke(serialRecieveController.io.maskLevel, Mask.NONE)
    step(1)
    send(0xbb)
    
    poke(serialRecieveController.io.address, Registers.DATA)
    poke(serialRecieveController.io.maskLevel, Mask.WORD)
    step(1)
    expect(serialRecieveController.io.dataOut, 0xAA.U)
    step(1)
    step(1)
    expect(serialRecieveController.io.dataOut, 0xF0.U)
    step(1)
    poke(serialRecieveController.io.address, Registers.STATUS)
    poke(serialRecieveController.io.maskLevel, Mask.WORD)
    step(1)
    expect(serialRecieveController.io.dataOut, "b00000000_00000000_000000_0000_11_01".U)
    step(1)
    poke(serialRecieveController.io.address, Registers.DATA)
    poke(serialRecieveController.io.maskLevel, Mask.WORD)
    step(1)
    expect(serialRecieveController.io.dataOut, 0x0F.U)
    step(1)
    poke(serialRecieveController.io.address, Registers.STATUS)
    poke(serialRecieveController.io.maskLevel, Mask.WORD)
    step(1)
    expect(serialRecieveController.io.dataOut, "b00000000_00000000_000000_0000_00_01".U)
    step(1)
    poke(serialRecieveController.io.address, Registers.DATA)
    poke(serialRecieveController.io.maskLevel, Mask.WORD)
    step(1)
    expect(serialRecieveController.io.dataOut, 0xbb.U)
    step(1)
    poke(serialRecieveController.io.address, Registers.STATUS)
    poke(serialRecieveController.io.maskLevel, Mask.WORD)
    step(1)
    expect(serialRecieveController.io.dataOut, "b00000000_00000000_000000_0000_01_01".U)
}

class SerialRecieveControllerSpec extends FlatSpec with Matchers {
  behavior of "SerialRecieveControllerSpec"

  it should "read and write successfully" in {
    chisel3.iotesters.Driver.execute(Array("--generate-vcd-output", "on"), () => new SerialRecieveController(38400, 9600, 2)) { serialRecieveController =>
      new SerialRecieveControllerTest(serialRecieveController)
    } should be(true)
  }
}

class SerialTransmitControllerTest(serialTransmitController: SerialTransmitController) extends PeekPokeTester(serialTransmitController) {
    def receive(): Int = {
      var result: Int = 0
      // start bit
      step(38400 / 9600)
      for (bitIndex <- 0 to 7) {
        result |= peek(serialTransmitController.io.txWire).intValue << bitIndex
        step(38400 / 9600)
      }
      // end bit
      step(38400 / 9600)
      return result
    }

    poke(serialTransmitController.io.readMode, false)
    poke(serialTransmitController.io.address, Registers.DATA)
    poke(serialTransmitController.io.maskLevel, Mask.WORD)
    poke(serialTransmitController.io.dataIn, 0x55.U)
    step(1)
    poke(serialTransmitController.io.dataIn, 0xAA.U)
    step(1)
    poke(serialTransmitController.io.maskLevel, Mask.NONE)
    expect(receive().U, 0x55.U)
    step(1)
    expect(receive().U, 0xAA.U)
    step(1)
    poke(serialTransmitController.io.readMode, true)
    poke(serialTransmitController.io.address, Registers.STATUS)
    poke(serialTransmitController.io.maskLevel, Mask.WORD)
    step(1)
    expect(serialTransmitController.io.dataOut, "b00000000_00000000_000000_0000_10_10".U)
    expect(serialTransmitController.io.txWire, true.B)

    poke(serialTransmitController.io.readMode, false)
    poke(serialTransmitController.io.address, Registers.DATA)
    poke(serialTransmitController.io.maskLevel, Mask.WORD)
    poke(serialTransmitController.io.dataIn, 0x0F.U)
    step(1)
    poke(serialTransmitController.io.readMode, true)
    poke(serialTransmitController.io.address, Registers.STATUS)
    poke(serialTransmitController.io.maskLevel, Mask.WORD)
    step(1)
    expect(serialTransmitController.io.dataOut, "b00000000_00000000_000000_0000_11_10".U)
}

class SerialTransmitControllerSpec extends FlatSpec with Matchers {
  behavior of "SerialTransmitControllerSpec"

  it should "read and write successfully" in {
    chisel3.iotesters.Driver.execute(Array("--generate-vcd-output", "on"), () => new SerialTransmitController(38400, 9600, 2)) { serialTransmitController =>
      new SerialTransmitControllerTest(serialTransmitController)
    } should be(true)
  }
}

class SerialControllerTest(serialController: SerialController) extends PeekPokeTester(serialController) {
  // since we tested recieve and transmit seperatedly in their own test cases,
  // we test async recieve and transmit here
  // we persume send a bit needs 8 cycles
  // we will send 0x55 to the client, and the client will send 0xAA to the host

  // first send the byte 0x55 to the tx FIFO buffer
  poke(serialController.io.rxWire, true)
  poke(serialController.io.readMode, false)
  poke(serialController.io.address, "b100".U)
  poke(serialController.io.maskLevel, Mask.WORD)
  poke(serialController.io.dataIn, 0x55.U)
  step(1)
  // the transmitter's transmitBufferIndexFromHost should be 1
  poke(serialController.io.readMode, true.B)
  poke(serialController.io.address, "b101".U)
  step(1)
  expect(serialController.io.dataOut, "b00000000_00000000_000000_0000_01_00".U)
  // txWire should been pull down to show transmitting has been started
  poke(serialController.io.maskLevel, Mask.NONE)
  step(1)
  expect(serialController.io.txWire, false.B)
  // recieve part should not be affected
  poke(serialController.io.maskLevel, Mask.WORD)
  poke(serialController.io.address, "b001".U)
  step(1)
  expect(serialController.io.dataOut, "b00000000_00000000_000000_0000_00_00".U)
  // now the client start send data
  poke(serialController.io.maskLevel, Mask.NONE)
  poke(serialController.io.rxWire, false.B)
  def checkAndSend(txBit: Bool, rxBit: Bool): Unit = {
    step(6)
    // now the nth bit of the whole byte 0x55, ie. txBit should be on txWire
    expect(serialController.io.txWire, txBit)
    step(2)
    // now we should send the nth bit of byte 0xaa, ie. put rxBit on rxWire
    poke(serialController.io.rxWire, rxBit)
  }
  checkAndSend(true.B, false.B)
  checkAndSend(false.B, true.B)
  checkAndSend(true.B, false.B)
  checkAndSend(false.B, true.B)
  checkAndSend(true.B, false.B)
  checkAndSend(false.B, true.B)
  checkAndSend(true.B, false.B)
  checkAndSend(false.B, true.B)
  // wait for the transmit end
  step(6)
  poke(serialController.io.rxWire, true.B)
  step(1)
  // txWire should be pull up to show the sending process is done
  expect(serialController.io.txWire, true.B)
  step(1)
  // we should read the data successfully from the recieve buffer
  poke(serialController.io.readMode, true.B)
  poke(serialController.io.address, "b000".U)
  poke(serialController.io.maskLevel, Mask.WORD)
  step(1)
  expect(serialController.io.dataOut, 0xAA.U)
  step(1)
  // check the status register
  poke(serialController.io.address, "b001".U)
  step(1)
  expect(serialController.io.dataOut, "b00000000_00000000_000000_0000_01_01".U)
  step(1)
  poke(serialController.io.address, "b101".U)
  step(1)
  expect(serialController.io.dataOut, "b00000000_00000000_000000_0000_01_01".U)
}

class SerialControllerSpec extends FlatSpec with Matchers {
  behavior of "SerialControllerSpec"

  it should "read and write successfully" in {
    chisel3.iotesters.Driver.execute(Array("--generate-vcd-output", "on"), () => new SerialController(76800, 9600, 2)) { serialController =>
      new SerialControllerTest(serialController)
    } should be(true)
  }
}