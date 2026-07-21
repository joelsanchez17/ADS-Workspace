// ADS I Class Project
// Pipelined RISC-V Core - Common Definitions
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/09/2026 by Tobias Jauch (@tojauch)

/*
Global Definitions and Data Types

Enumerations:
    uopc: ChiselEnum defining micro-operation codes for all supported RV32I instructions:
        R-type instructions 
        I-type instructions
        NOP (no operation, default case)

This enum is used throughout the pipeline:
    Decode stage assigns uop based on instruction fields
    Execute stage maps uop to ALU operations
*/

package core_tile

import chisel3._
import chisel3.experimental.ChiselEnum

// -----------------------------------------
// Global Definitions and Data Types
// -----------------------------------------

object uopc extends ChiselEnum {

  // default case
  val isNOP    = Value(0x00.U)

  // R-type instructions
  val isADD   = Value(0x01.U)
  val isSUB   = Value(0x02.U)
  val isXOR   = Value(0x03.U)
  val isOR    = Value(0x04.U)
  val isAND   = Value(0x05.U)
  val isSLL   = Value(0x06.U)
  val isSRL   = Value(0x07.U)
  val isSRA   = Value(0x08.U)
  val isSLT   = Value(0x09.U)
  val isSLTU  = Value(0x0A.U)

  // I-type instructions
  val isADDI  = Value(0x10.U)  
  val isXORI  = Value(0x11.U)
  val isORI   = Value(0x12.U)
  val isANDI  = Value(0x13.U)
  val isSLTI  = Value(0x14.U)
  val isSLTIU = Value(0x15.U)
  val isSLLI  = Value(0x16.U)
  val isSRLI  = Value(0x17.U)
  val isSRAI  = Value(0x18.U)
}

import uopc._
