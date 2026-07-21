// ADS I Class Project
// Pipelined RISC-V Core - Branch Target Buffer
//
// Chair of Electronic Design Automation, RPTU in Kaiserslautern
// File created on 05/12/2026 by Tobias Jauch (@tojauch)

/*
Branch Target Buffer (BTB): a hardware component that predicts the target address of conditional branch instructions to improve pipeline performance

Functionality (cf. slide 6-48 of the lecture slides):
    Stores target addresses and prediction information for conditional branch instructions
    On a branch instruction, checks if the instruction is in the BTB and retrieves the predicted target address and prediction state
    If the prediction is taken, the processor fetches the instruction from the predicted target address; if not taken, it continues sequentially
    Updates the BTB entry based on the actual outcome of the branch instruction (taken or not taken) and updates the prediction state accordingly

Inputs:
    PC: A 32-bit program counter representing the address of the branch instruction being fetched or executed.
    update: A 1-bit signal indicating whether the BTB should be updated with new information.
    updatePC: A 32-bit program counter associated with the branch instruction being updated.
    updateTarget: A 32-bit branch target address to be stored in the BTB.
    mispredicted: A 1-bit signal indicating whether the prediction turned out to be incorrect during execution (used to update the predictor).

Outputs:
    valid: A 1-bit signal indicating whether the BTB has a valid prediction for the provided program counter.
    target: A 32-bit signal representing the predicted branch target address when a valid prediction exists.
    predictTaken: A 1-bit signal indicating whether the branch is predicted to be taken or not.

*/

package core_tile

import chisel3._
import chisel3.util._
import uopc._

// -----------------------------------------
// Branch Target Buffer
// -----------------------------------------

  case class btbParameters(
  numSets: Int = 8, // Number of sets in the BTB
  numWays: Int = 2,  // Number of ways in the BTB (associativity)
)

class BTBEntry extends Bundle {
  val valid = Bool()
  val tag = UInt(20.W)      // Assuming 20 bits for the tag
  val target = UInt(32.W)   // Branch target address
  val predictor = UInt(2.W) // 2-bit saturating counter
}

class BTB(parameters: btbParameters = btbParameters()) extends Module {
  val io = IO(new Bundle {
    // Add I/O ports according to the specification above here
    val PC             = Input(UInt(32.W))  // Program Counter (input)
    val update         = Input(Bool())      // Signal to update the BTB
    val updatePC       = Input(UInt(32.W))  // PC for the update
    val updateTarget   = Input(UInt(32.W))  // Target address for the update
    val mispredicted   = Input(Bool())      // Indicates whether the previous prediction was wrong
    val valid          = Output(Bool())     // Indicates if there is a valid prediction
    val target         = Output(UInt(32.W)) // Predicted branch target address
    val predictTaken = Output(Bool())     // Predicted branch direction (taken or not)
  })

  //Add your implementation according to the specification in assignment 6 here. 

  val numSets = parameters.numSets
  val numWays = parameters.numWays

  val indexBits = log2Ceil(numSets) // number of index bits needed
  val tagBits = (30 - indexBits).U // number of remaining tag bits

  val btb = RegInit(VecInit(Seq.fill(numSets)(VecInit(Seq.fill(numWays)(0.U.asTypeOf(new BTBEntry))))))

  // Predictor State
  val strongNotTaken :: weakNotTaken :: strongTaken :: weakTaken :: Nil = Enum(4)
  val lru = RegInit(VecInit(Seq.fill(numSets)(0.U(1.W)))) // 1-bit LRU for 2-way


  // Extract index and tag from the PC
  val index = io.PC((indexBits - 1 + 2), 2)         // Use bits 2 - log(numSets) for indexing
  val tag   = io.PC(31, (indexBits + 2))        // Upper bits as the tag

  // Read the set
  val selectedSet = btb(index)

  // Match tag in the set
  val hits = selectedSet.map(entry => entry.valid && entry.tag === tag)
  // hit is propagated from IF stage to EX stage
  val hit = hits.reduce(_ || _) // or all values in hits. e.x [true, false] = true
  val hitWay = PriorityEncoder(hits) // index of first true

  // propagate to two cycles because we get these signals actually after EX stage
  val hit_1 = RegNext(hit)
  val hit_2 = RegNext(hit_1)

  val hitWay_1 = RegNext(hitWay)
  val hitWay_2 = RegNext(hitWay_1)

  val index_1 = RegNext(index)
  val index_2 = RegNext(index_1)

  // Output
  // these all should be updated two cycles later after execution stage.
  io.valid := hit_2
  io.target := Mux(hit_2, selectedSet(hitWay_2).target, 0.U)
  io.predictTaken := Mux(hit_2, selectedSet(hitWay_2).predictor(1), false.B) // MSB of predictor indicates direction

  // Update LRU on a hit
  when(hit) {
    lru(index) := Mux(hitWay === 0.U, 1.U, 0.U) // Mark the non-accessed way as least recently used
  }

  // Update logic
  when(io.update) {
    val updateIndex = io.updatePC((indexBits - 1 + 2), 2)
    val updateTag = io.updatePC(31, (indexBits + 2))

    val set = btb(updateIndex)
    val wayToUpdate = Mux(set(0).valid && set(1).valid, lru(updateIndex), PriorityEncoder(Seq(!set(0).valid, !set(1).valid)))

    btb(updateIndex)(wayToUpdate).valid     := true.B
    btb(updateIndex)(wayToUpdate).tag       := updateTag
    btb(updateIndex)(wayToUpdate).target    := io.updateTarget
    btb(updateIndex)(wayToUpdate).predictor := weakTaken // Initialize to weakTaken
  }

  // FSM State Transition for Predictor
  when(hit_2) {

    val predictor = selectedSet(hitWay_2).predictor

    btb(index_2)(hitWay_2).predictor := MuxCase(predictor, Seq(
      (predictor === strongTaken && !io.mispredicted) -> strongTaken,     // strongTaken -> strongTaken
      (predictor === strongTaken && io.mispredicted) -> weakTaken,        // strongTaken -> weakTaken
      (predictor === weakTaken && io.mispredicted) -> strongNotTaken,       // weakTaken -> strongNotTaken
      (predictor === weakTaken && !io.mispredicted) -> strongTaken,       // weakTaken -> strongTaken
      (predictor === weakNotTaken && !io.mispredicted) -> strongNotTaken,      // weakNotTaken -> strongTaken
      (predictor === weakNotTaken && io.mispredicted) -> strongTaken,  // weakNotTaken -> strongNotTaken
      (predictor === strongNotTaken && !io.mispredicted) -> strongNotTaken, // strongNotTaken -> weakNotTaken
      (predictor === strongNotTaken && io.mispredicted) -> weakNotTaken // strongNotTaken -> strongNotTaken
    ))
  }
}