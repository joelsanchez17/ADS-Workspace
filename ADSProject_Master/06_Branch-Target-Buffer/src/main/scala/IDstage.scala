// ADS I Class Project
// Pipelined RISC-V Core - ID Stage
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 01/09/2026 by Tobias Jauch (@tojauch)

/*
Instruction Decode (ID) Stage: decoding and operand fetch

Extracted Fields from 32-bit Instruction (see RISC-V specification for reference):
    opcode: instruction format identifier
    funct3: selects variant within instruction format
    funct7: further specifies operation type (R-type only)
    rd: destination register address
    rs1: first source register address
    rs2: second source register address
    imm: 12-bit immediate value (I-type, sign-extended)

Register File Interfaces:
    regFileReq_A, regFileResp_A: read port for rs1 operand
    regFileReq_B, regFileResp_B: read port for rs2 operand

Internal Signals:
    Combinational decoders for instructions

Functionality:
    Decode opcode to determine instruction and identify operation (ADD, SUB, XOR, ...)
    Output: uop (operation code), rd, operandA (from rs1), operandB (rs2 or immediate)

Outputs:
    uop: micro-operation code (identifies instruction type)
    rd: destination register index
    operandA: first operand
    operandB: second operand 
    XcptInvalid: exception flag for invalid instructions
*/

package core_tile

import chisel3._
import chisel3.util._
import uopc._

// -----------------------------------------
// Decode Stage
// -----------------------------------------

class ID extends Module {
  val io = IO(new Bundle {
    val regFileReq_A  = Flipped(new regFileReadReq) 
    val regFileResp_A = Flipped(new regFileReadResp) 
    val regFileReq_B  = Flipped(new regFileReadReq) 
    val regFileResp_B = Flipped(new regFileReadResp) 
    val instr         = Input(UInt(32.W))
    val uop           = Output(uopc())
    val rs1           = Output(UInt(5.W))
    val rs2           = Output(UInt(5.W))
    val rd            = Output(UInt(5.W))
    val operandA      = Output(UInt(32.W))
    val operandB      = Output(UInt(32.W))
    val imm           = Output(UInt(32.W))
    val wrEN          = Output(Bool())
    val XcptInvalid   = Output(Bool())
  })

  val opcode  = io.instr(6, 0)
  io.rd      := io.instr(11, 7)
  val funct3  = io.instr(14, 12)
  val rs1     = io.instr(19, 15)

  // R-Type
  val funct7  = io.instr(31, 25)
  val rs2     = io.instr(24, 20)

  // Immediates
  val immI = Cat(Fill(20, io.instr(31)), io.instr(31, 20))
  val immB = Cat(Fill(19, io.instr(31)), io.instr(31), io.instr(7), io.instr(30, 25), io.instr(11, 8), 0.U(1.W))
  val immJ = Cat(Fill(11, io.instr(31)), io.instr(31), io.instr(19, 12), io.instr(20), io.instr(30, 21), 0.U(1.W))
  
  // Initialize signals
  val isInvalid = WireInit(true.B)
  io.wrEN := false.B
  
  isInvalid := true.B
  io.uop := isNOP

  when(opcode === "b0110011".U){
    when(funct3 === "b000".U){
      when(funct7 === "b0000000".U){
        io.uop := isADD
        io.wrEN := true.B
        isInvalid := false.B
      }.elsewhen(funct7 === "b0100000".U){
        io.uop := isSUB
        io.wrEN := true.B
        isInvalid := false.B
      }
    }.elsewhen(funct3 === "b100".U){
      when(funct7 === "b0000000".U){
        io.uop := isXOR
        io.wrEN := true.B
        isInvalid := false.B
      }
    }.elsewhen(funct3 === "b110".U){
      when(funct7 === "b0000000".U){
        io.uop := isOR
        io.wrEN := true.B
        isInvalid := false.B
      }
    }.elsewhen(funct3 === "b111".U){
      when(funct7 === "b0000000".U){
        io.uop := isAND
        io.wrEN := true.B
        isInvalid := false.B
      }
    }.elsewhen(funct3 === "b001".U){
      when(funct7 === "b0000000".U){
        io.uop := isSLL
        io.wrEN := true.B
        isInvalid := false.B
      }
    }.elsewhen(funct3 === "b101".U){
      when(funct7 === "b0000000".U){
        io.uop := isSRL
        io.wrEN := true.B
        isInvalid := false.B
      }.elsewhen(funct7 === "b0100000".U){
        io.uop := isSRA
        io.wrEN := true.B
        isInvalid := false.B
      }
    }.elsewhen(funct3 === "b010".U){
      when(funct7 === "b0000000".U){
        io.uop := isSLT
        io.wrEN := true.B
        isInvalid := false.B
      }
    }.elsewhen(funct3 === "b011".U){
      when(funct7 === "b0000000".U){
        io.uop := isSLTU
        io.wrEN := true.B
        isInvalid := false.B
      }
    }
  }.elsewhen(opcode === "b0010011".U){
    when(funct3 === "b000".U){
      io.uop := isADDI
      io.wrEN := true.B
      isInvalid := false.B
    }.elsewhen(funct3 === "b100".U){
      io.uop := isXORI
      io.wrEN := true.B
      isInvalid := false.B
    }.elsewhen(funct3 === "b110".U){
      io.uop := isORI
      io.wrEN := true.B
      isInvalid := false.B
    }.elsewhen(funct3 === "b111".U){
      io.uop := isANDI
      io.wrEN := true.B
      isInvalid := false.B
    }.elsewhen(funct3 === "b010".U){
      io.uop := isSLTI
      io.wrEN := true.B
      isInvalid := false.B
    }.elsewhen(funct3 === "b011".U){
      io.uop := isSLTIU
      io.wrEN := true.B
      isInvalid := false.B
    }.elsewhen(funct3 === "b001".U){
      when(funct7 === "b0000000".U){
        io.uop := isSLLI
        io.wrEN := true.B
        isInvalid := false.B
      }
    }.elsewhen(funct3 === "b101".U){
      when(funct7 === "b0000000".U){
        io.uop := isSRLI
        io.wrEN := true.B
        isInvalid := false.B
      }.elsewhen(funct7 === "b0100000".U){
        io.uop := isSRAI
        io.wrEN := true.B
        isInvalid := false.B
      }
    }
  }.elsewhen(opcode === "b1100011".U){
    when(funct3 === "b000".U){
      io.uop := isBEQ
      isInvalid := false.B
    }.elsewhen(funct3 === "b001".U){
      io.uop := isBNE
      isInvalid := false.B
    }.elsewhen(funct3 === "b100".U){
      io.uop := isBLT
      isInvalid := false.B
    }.elsewhen(funct3 === "b101".U){
      io.uop := isBGE
      isInvalid := false.B
    }.elsewhen(funct3 === "b110".U){
      io.uop := isBLTU
      isInvalid := false.B
    }.elsewhen(funct3 === "b111".U){
      io.uop := isBGEU
      isInvalid := false.B
    }
  }.elsewhen(opcode === "b1101111".U){
    io.uop := isJAL
    io.wrEN := true.B
    isInvalid := false.B
  }.elsewhen(opcode === "b1100111".U){
    when(funct3 === "b000".U){
      io.uop := isJALR
      io.wrEN := true.B
      isInvalid := false.B
    }
  }

  io.XcptInvalid := isInvalid

  // Operands
  io.regFileReq_A.addr := rs1
  io.regFileReq_B.addr := rs2

  // Control Signals for Forwarding Unit
  io.rs1 := rs1
  io.rs2 := rs2

  io.operandA := io.regFileResp_A.data
  io.operandB := Mux(opcode === "b0110011".U || opcode === "b1100011".U, io.regFileResp_B.data,
    Mux(opcode === "b0010011".U, immI, 0.U))
  io.imm := MuxLookup(opcode, 0.U(32.W), Array(
    "b0010011".U -> immI,
    "b1100111".U -> immI,
    "b1100011".U -> immB,
    "b1101111".U -> immJ
  ))
  
}
