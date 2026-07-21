// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package readserial

import chisel3._
import chisel3.util._


/** controller class */
class Controller extends Module{
  
  val io = IO(new Bundle {
    val rxd      = Input(Bits(1.W))
    val cnt_s    = Input(UInt(3.W))
    val valid    = Output(Bool())
    val cnt_en   = Output(Bool())
    })

  val s_idle :: s_transmission :: Nil = Enum(2)
  val state = RegInit(s_idle)

  val cnt_en = RegInit(false.B)
  val valid  = RegInit(false.B)

  when(reset.asBool){
    state     := s_idle
    cnt_en := false.B
    valid  := false.B
  }.elsewhen(state === s_idle){
    valid  := false.B

    when(io.rxd === 0.B){
      cnt_en := true.B
      state     := s_transmission
    }

  }.elsewhen(state === s_transmission){

    when(io.cnt_s === 7.U){
      valid  := true.B
      cnt_en := false.B
      state     := s_idle
    }
  }

  io.valid  := valid
  io.cnt_en := cnt_en

}


/** counter class */
class Counter extends Module{
  
  val io = IO(new Bundle {
    val cnt_en   = Input(Bool())
    val cnt_s    = Output(UInt(3.W))
    })

  val counter = RegInit(0.U(3.W))

  when(reset.asBool){
    counter := 0.U
  }.elsewhen(io.cnt_en){
    counter := (counter + 1.U) % 8.U
  }.elsewhen(!io.cnt_en){
    counter := 0.U
  }

  io.cnt_s := counter

}

/** shift register class */
class ShiftRegister extends Module{
  
  val io = IO(new Bundle {
    val rxd     = Input(Bits(1.W))
    val data    = Output(Bits(8.W))
    })

  val data_buffer = Reg(Bits(8.W))

  data_buffer := Cat(data_buffer(6,0), io.rxd)
  io.data     := data_buffer

}

/** 
  * The design readserial is a serial receiver. It scans an input line (“serial bus”) named rxd for serial
  * transmissions of data bytes. A transmission begins with a start bit ‘0’ followed by 8 data bits. The
  * most significant bit (MSB) is transmitted first. There is no parity bit and no stop bit. After the last
  * data bit has been transferred a new transmission (beginning with a start bit, ‘0’) may immediately
  * follow. If there is no new transmission the bus line goes high (‘1’, this is considered the “idle” bus
  * signal). In this case the receiver waits until the next transmission begins.
  * The outputs of the design are an 8-bit parallel data signal and a valid signal. The valid signal goes
  * high (‘1’) for one clock cycle after the last serial bit has been transmitted, indicating that a new data
  * byte is ready.
  */
class ReadSerial extends Module{
  
  val io = IO(new Bundle {
    val rxd    = Input(Bits(1.W))
    val valid  = Output(Bool())
    val data   = Output(Bits(8.W))
    })


  // instantiation of modules
  val controller = Module(new Controller)
  val counter    = Module(new Counter)
  val shiftReg   = Module(new ShiftRegister)

  // connections between modules
  controller.io.rxd   := io.rxd
  controller.io.cnt_s := counter.io.cnt_s

  counter.io.cnt_en   := controller.io.cnt_en

  // global I/O 
  shiftReg.io.rxd     := io.rxd

  io.data             := shiftReg.io.data
  io.valid            := controller.io.valid

}

