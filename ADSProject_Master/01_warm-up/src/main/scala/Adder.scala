// ADS I Class Project
// Chisel Introduction
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 18/10/2022 by Tobias Jauch (@tojauch)

package adder

import chisel3._
import chisel3.util._


/** 
  * half adder class 
  */
class HalfAdder extends Module{
  
  val io = IO(new Bundle {
    val a = Input(UInt(1.W))  
    val b = Input(UInt(1.W))  
    val s = Output(UInt(1.W)) // sum
    val c = Output(UInt(1.W)) // carry out
    })


  io.s := (~io.a & io.b) | (io.a & ~io.b)
  io.c := io.a & io.b

}

/** 
  * full adder class 
  */
class FullAdder extends Module{

  val io = IO(new Bundle {
    val a  = Input(UInt(1.W))  
    val b  = Input(UInt(1.W))  // carry in
    val ci = Input(UInt(1.W)) 
    val s  = Output(UInt(1.W)) // sum
    val co = Output(UInt(1.W)) // carry out
    })


  val HalfAdder1 = Module(new HalfAdder)
  val HalfAdder2 = Module(new HalfAdder)

  HalfAdder1.io.a := io.a
  HalfAdder1.io.b := io.b

  HalfAdder2.io.a := HalfAdder1.io.s
  HalfAdder2.io.b := io.ci

  io.s  := HalfAdder2.io.s
  io.co := HalfAdder1.io.c | HalfAdder2.io.c

}

/** 
  * 4-bit adder class 
  * An n - bit adder can be build using one half adder and n-1 full adders
  */
class FourBitAdder extends Module{

  val io = IO(new Bundle {
    val a  = Input(UInt(4.W))  // first number
    val b  = Input(UInt(4.W))  // second number
    val sum  = Output(UInt(4.W)) // sum
    val overflow = Output(UInt(1.W)) // carry out (used to detect overflow)
    })

  val HalfAdder = Module(new HalfAdder)
  val FullAdder1 = Module(new FullAdder) 
  val FullAdder2 = Module(new FullAdder) 
  val FullAdder3 = Module(new FullAdder) 

  HalfAdder.io.a   := io.a(0)
  HalfAdder.io.b   := io.b(0)
  val sum0         = HalfAdder.io.s

  FullAdder1.io.a  := io.a(1)
  FullAdder1.io.b  := io.b(1)
  FullAdder1.io.ci := HalfAdder.io.c
  val sum1         = FullAdder1.io.s

  FullAdder2.io.a  := io.a(2)
  FullAdder2.io.b  := io.b(2)
  FullAdder2.io.ci := FullAdder1.io.co
  val sum2         = FullAdder2.io.s

  FullAdder3.io.a  := io.a(3)
  FullAdder3.io.b  := io.b(3)
  FullAdder3.io.ci := FullAdder2.io.co
  val sum3         = FullAdder3.io.s


  io.sum              := Cat(sum3, sum2, sum1, sum0).asUInt
  io.overflow         := FullAdder3.io.co
}
